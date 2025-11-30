package com. example.learnify;

import android. content.Context;
import android.graphics.Bitmap;
import android.graphics. BitmapFactory;
import android.net.Uri;
import android.util.Log;

import com.google. android.gms.auth.api.signin.GoogleSignInAccount;
import com. google.api.client.http.InputStreamContent;
import com. google.api.client.http.javanet.NetHttpTransport;
import com.google. api.client.json.gson.GsonFactory;
import com.google.api.services. drive.Drive;
import com.google. api.services.drive.DriveScopes;
import com. google.api.services.drive.model. File;
import com.google.api. services.drive.model.FileList;
import com.google. api.services.drive.model.Permission;
import com. google.auth.http.HttpCredentialsAdapter;
import com.google. auth.oauth2. AccessToken;
import com.google.auth. oauth2.GoogleCredentials;

import java.io. ByteArrayInputStream;
import java. io.ByteArrayOutputStream;
import java.io. InputStream;
import java.util.Collections;
import java. util.Date;
import java. util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DriveProfileService {

    private static final String TAG = "DriveProfileService";
    private static final String PROFILE_PHOTO_NAME = "learnify_profile_photo";
    private static final int MAX_IMAGE_SIZE = 500;
    private static final int COMPRESSION_QUALITY = 80;

    private Drive driveService;
    private Context context;
    private boolean initialized = false;
    private final ExecutorService executor = Executors. newSingleThreadExecutor();

    public interface Callback {
        void onSuccess(String imageUrl, String fileId);
        void onError(Exception e);
    }

    /**
     * Initialize the Drive service with a Google account
     */
    public void initialize(GoogleSignInAccount account, Context context) {
        this. context = context. getApplicationContext();

        if (account == null) {
            Log.e(TAG, "❌ Cannot initialize: account is null");
            initialized = false;
            return;
        }

        try {
            // Get the OAuth token from the account
            String token = account.getIdToken();

            // Create credentials from the token
            // Note: We'll use a simpler approach with GoogleAccountCredential
            com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential credential =
                    com.google.api. client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
                            .usingOAuth2(context, Collections.singleton(DriveScopes.DRIVE_FILE));
            credential.setSelectedAccount(account.getAccount());

            driveService = new Drive.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    credential
            )
                    .setApplicationName("Learnify")
                    .build();

            initialized = true;
            Log.d(TAG, "✅ DriveProfileService initialized successfully");

        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to initialize DriveProfileService", e);
            initialized = false;
        }
    }

    /**
     * Check if service is initialized
     */
    public boolean isInitialized() {
        return initialized && driveService != null;
    }

    /**
     * Upload profile picture to Google Drive
     */
    public void uploadProfilePicture(Uri imageUri, Callback callback) {
        if (!isInitialized()) {
            callback.onError(new Exception("Drive service not initialized"));
            return;
        }

        executor.execute(() -> {
            try {
                // 1. Delete old profile photo if exists
                deleteOldProfilePhoto();

                // 2.  Compress and prepare image
                byte[] imageData = compressImage(imageUri);
                if (imageData == null || imageData.length == 0) {
                    callback.onError(new Exception("Failed to process image"));
                    return;
                }

                Log.d(TAG, "📤 Uploading image (" + imageData. length / 1024 + " KB).. .");

                // 3. Create file metadata
                File fileMetadata = new File();
                fileMetadata.setName(PROFILE_PHOTO_NAME + "_" + System.currentTimeMillis() + ".jpg");
                fileMetadata. setMimeType("image/jpeg");

                // 4.  Upload to Drive
                ByteArrayInputStream inputStream = new ByteArrayInputStream(imageData);
                InputStreamContent mediaContent = new InputStreamContent("image/jpeg", inputStream);
                mediaContent.setLength(imageData.length);

                File uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                        .setFields("id, name, webContentLink, webViewLink")
                        .execute();

                String fileId = uploadedFile.getId();
                Log.d(TAG, "✅ File uploaded with ID: " + fileId);

                // 5. Make file publicly accessible
                Permission permission = new Permission();
                permission.setType("anyone");
                permission. setRole("reader");
                driveService.permissions().create(fileId, permission). execute();

                Log.d(TAG, "✅ File made public");

                // 6. Generate direct URL
                String imageUrl = "https://drive.google.com/uc?export=view&id=" + fileId;
                Log.d(TAG, "✅ Image URL: " + imageUrl);

                callback.onSuccess(imageUrl, fileId);

            } catch (Exception e) {
                Log.e(TAG, "❌ Upload failed", e);
                callback.onError(e);
            }
        });
    }

    /**
     * Delete old profile photos to save space
     */
    private void deleteOldProfilePhoto() {
        try {
            FileList result = driveService. files(). list()
                    .setQ("name contains '" + PROFILE_PHOTO_NAME + "' and trashed = false")
                    .setSpaces("drive")
                    .setFields("files(id, name)")
                    .execute();

            if (result.getFiles() != null) {
                for (File file : result.getFiles()) {
                    try {
                        driveService.files(). delete(file.getId()).execute();
                        Log.d(TAG, "🗑️ Deleted old photo: " + file. getName());
                    } catch (Exception e) {
                        Log.w(TAG, "Failed to delete old photo: " + file.getName());
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to search for old photos", e);
        }
    }

    /**
     * Delete a specific file by ID
     */
    public void deleteFile(String fileId, Callback callback) {
        if (!isInitialized() || fileId == null || fileId.isEmpty()) {
            if (callback != null) {
                callback. onError(new Exception("Invalid parameters"));
            }
            return;
        }

        executor. execute(() -> {
            try {
                driveService.files(). delete(fileId). execute();
                Log.d(TAG, "🗑️ File deleted: " + fileId);
                if (callback != null) {
                    callback.onSuccess(null, fileId);
                }
            } catch (Exception e) {
                Log. e(TAG, "❌ Failed to delete file", e);
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }

    /**
     * Compress image to reduce size
     */
    private byte[] compressImage(Uri imageUri) {
        try {
            InputStream inputStream = context. getContentResolver().openInputStream(imageUri);
            if (inputStream == null) return null;

            // Decode with inSampleSize to reduce memory
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();

            // Calculate sample size
            int sampleSize = 1;
            int width = options.outWidth;
            int height = options.outHeight;

            while (width / sampleSize > MAX_IMAGE_SIZE * 2 || height / sampleSize > MAX_IMAGE_SIZE * 2) {
                sampleSize *= 2;
            }

            // Decode with sample size
            options.inJustDecodeBounds = false;
            options.inSampleSize = sampleSize;
            inputStream = context.getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();

            if (bitmap == null) return null;

            // Scale to max size
            int originalWidth = bitmap.getWidth();
            int originalHeight = bitmap. getHeight();
            float scale = Math.min(
                    (float) MAX_IMAGE_SIZE / originalWidth,
                    (float) MAX_IMAGE_SIZE / originalHeight
            );

            if (scale < 1) {
                int newWidth = Math.round(originalWidth * scale);
                int newHeight = Math.round(originalHeight * scale);
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
                if (scaledBitmap != bitmap) {
                    bitmap. recycle();
                }
                bitmap = scaledBitmap;
            }

            // Compress to JPEG
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY, outputStream);
            bitmap.recycle();

            byte[] result = outputStream.toByteArray();
            Log.d(TAG, "📷 Image compressed: " + result.length / 1024 + " KB");
            return result;

        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to compress image", e);
            return null;
        }
    }

    /**
     * Get direct viewable URL for a file
     */
    public static String getDirectUrl(String fileId) {
        if (fileId == null || fileId.isEmpty()) return null;
        return "https://drive. google.com/uc?export=view&id=" + fileId;
    }
}