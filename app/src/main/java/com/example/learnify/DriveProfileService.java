package com.example.learnify;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.Permission;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Service for managing profile pictures in Google Drive's app-specific hidden folder (appDataFolder).
 * This folder is hidden from the user and provides private storage for the app.
 */
public class DriveProfileService {

    private static final String TAG = "DriveProfileService";
    private static final String PROFILE_PREFIX = "profile_";

    private Drive driveService;
    private Context context;
    private final Executor executor;

    /**
     * Callback interface for upload operations
     */
    public interface Callback {
        void onSuccess(String imageUrl, String fileId);
        void onError(Exception e);
    }

    public DriveProfileService() {
        this.executor = Executors.newSingleThreadExecutor();
    }

    /**
     * Initialize the Drive service with user's OAuth credentials
     *
     * @param account The GoogleSignInAccount with Drive appData scope
     * @param context Application context
     */
    public void initialize(GoogleSignInAccount account, Context context) {
        this.context = context;

        if (account == null || account.getAccount() == null) {
            Log.e(TAG, "❌ No Google account provided for initialization");
            return;
        }

        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                context, Collections.singleton(DriveScopes.DRIVE_APPDATA)
        );
        credential.setSelectedAccount(account.getAccount());

        driveService = new Drive.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
        )
                .setApplicationName("Learnify")
                .build();

        Log.d(TAG, "✅ Drive service initialized with appDataFolder scope");
    }

    /**
     * Check if the service is initialized and ready to use
     */
    public boolean isInitialized() {
        return driveService != null && context != null;
    }

    /**
     * Upload profile picture to Google Drive app-specific hidden folder
     *
     * @param imageUri URI of the image to upload
     * @param callback Callback for success/error
     */
    public void uploadProfilePicture(Uri imageUri, Callback callback) {
        if (!isInitialized()) {
            callback.onError(new Exception("Drive service not initialized. Please sign in with Google."));
            return;
        }

        executor.execute(() -> {
            try {
                Log.d(TAG, "📤 Uploading profile picture to Drive appDataFolder");

                // Compress and save image to temp file
                Bitmap bitmap = loadAndCompressBitmap(imageUri);
                String fileName = PROFILE_PREFIX + System.currentTimeMillis() + ".jpg";
                java.io.File tempFile = saveBitmapToFile(bitmap, fileName);

                // Delete any existing profile pictures
                deleteOldProfilePictures();

                // Create file metadata for appDataFolder
                File fileMetadata = new File();
                fileMetadata.setName(fileName);
                fileMetadata.setParents(Collections.singletonList("appDataFolder"));
                fileMetadata.setDescription("Learnify profile picture");

                FileContent mediaContent = new FileContent("image/jpeg", tempFile);

                // Upload to appDataFolder
                File uploadedFile = driveService.files()
                        .create(fileMetadata, mediaContent)
                        .setFields("id, name")
                        .execute();

                String fileId = uploadedFile.getId();

                // Make file publicly accessible for viewing
                makeFilePublic(fileId);

                // Generate direct URL for the uploaded file
                String directUrl = getDirectUrl(fileId);

                Log.d(TAG, "✅ Profile picture uploaded successfully");
                Log.d(TAG, "File ID: " + fileId);
                Log.d(TAG, "Direct URL: " + directUrl);

                // Clean up temp file
                tempFile.delete();

                callback.onSuccess(directUrl, fileId);

            } catch (Exception e) {
                Log.e(TAG, "❌ Profile picture upload failed", e);
                callback.onError(e);
            }
        });
    }

    /**
     * Delete old profile pictures from appDataFolder
     */
    public void deleteOldProfilePicture(String fileId) {
        if (!isInitialized() || fileId == null || fileId.isEmpty()) {
            return;
        }

        executor.execute(() -> {
            try {
                driveService.files().delete(fileId).execute();
                Log.d(TAG, "🗑️ Deleted profile picture: " + fileId);
            } catch (Exception e) {
                Log.w(TAG, "⚠️ Could not delete profile picture: " + fileId, e);
            }
        });
    }

    /**
     * Delete all old profile pictures from appDataFolder
     */
    private void deleteOldProfilePictures() {
        try {
            String query = "name contains '" + PROFILE_PREFIX + "'";
            FileList result = driveService.files().list()
                    .setSpaces("appDataFolder")
                    .setQ(query)
                    .setFields("files(id, name)")
                    .execute();

            for (File file : result.getFiles()) {
                driveService.files().delete(file.getId()).execute();
                Log.d(TAG, "🗑️ Deleted old profile picture: " + file.getName());
            }
        } catch (Exception e) {
            Log.w(TAG, "⚠️ Could not delete old profile pictures", e);
        }
    }

    /**
     * Make file publicly accessible (read-only)
     */
    private void makeFilePublic(String fileId) {
        try {
            Permission permission = new Permission()
                    .setType("anyone")
                    .setRole("reader");

            driveService.permissions()
                    .create(fileId, permission)
                    .setFields("id")
                    .execute();

            Log.d(TAG, "✅ File made publicly accessible");
        } catch (Exception e) {
            Log.w(TAG, "⚠️ Could not make file public", e);
        }
    }

    /**
     * Generate direct URL for viewing the file
     *
     * @param fileId Google Drive file ID
     * @return Direct URL for the file
     */
    public String getDirectUrl(String fileId) {
        return "https://drive.google.com/uc?id=" + fileId;
    }

    /**
     * Load and compress bitmap from URI
     */
    private Bitmap loadAndCompressBitmap(Uri imageUri) throws Exception {
        InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
        Bitmap original = BitmapFactory.decodeStream(inputStream);
        inputStream.close();

        // Compress to reasonable size (500x500)
        int maxSize = 500;
        int width = original.getWidth();
        int height = original.getHeight();

        float scale = Math.min(
                (float) maxSize / width,
                (float) maxSize / height
        );

        int newWidth = Math.round(width * scale);
        int newHeight = Math.round(height * scale);

        return Bitmap.createScaledBitmap(original, newWidth, newHeight, true);
    }

    /**
     * Save bitmap to temporary file
     */
    private java.io.File saveBitmapToFile(Bitmap bitmap, String fileName) throws Exception {
        java.io.File tempFile = new java.io.File(context.getCacheDir(), fileName);
        FileOutputStream fos = new FileOutputStream(tempFile);

        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos);
        fos.flush();
        fos.close();

        Log.d(TAG, "💾 Saved to temp file: " + tempFile.length() + " bytes");
        return tempFile;
    }
}
