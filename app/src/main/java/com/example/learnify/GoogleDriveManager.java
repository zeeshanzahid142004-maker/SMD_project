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

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.Permission;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Manages profile photo storage in Google Drive
 * FREE alternative to Firebase Storage
 */
public class GoogleDriveManager {

    private static final String TAG = "GoogleDriveManager";
    private static final String FOLDER_NAME = "Learnify Profile Photos";
    private static final String NOTES_FOLDER_NAME = "Learnify Note Images";
    private static final String APP_FOLDER_MIME_TYPE = "application/vnd.google-apps.folder";

    private final Context context;
    private final Drive driveService;
    private final Executor executor;

    public interface UploadCallback {
        void onSuccess(String driveUrl, String fileId);
        void onError(Exception e);
    }

    public interface DownloadCallback {
        void onSuccess(Bitmap bitmap);
        void onError(Exception e);
    }

    public GoogleDriveManager(Context context) {
        this.context = context;
        this.executor = Executors.newSingleThreadExecutor();
        this.driveService = getDriveService();
    }

    /**
     * Initialize Google Drive service
     */
    private Drive getDriveService() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);

        if (account == null) {
            Log.e(TAG, "❌ No Google account signed in");
            return null;
        }

        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                context, Collections.singleton(DriveScopes.DRIVE_FILE)
        );
        credential.setSelectedAccount(account.getAccount());

        return new Drive.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
        )
                .setApplicationName("Learnify")
                .build();

    }

    /**
     * Upload profile photo to Google Drive
     */
    public void uploadProfilePhoto(Uri imageUri, String userId, UploadCallback callback) {
        if (driveService == null) {
            callback.onError(new Exception("Drive service not initialized. Please sign in with Google."));
            return;
        }

        executor.execute(() -> {
            try {
                Log.d(TAG, "📤 Uploading photo to Drive for user: " + userId);

                // Compress image first
                Bitmap bitmap = loadAndCompressBitmap(imageUri);
                java.io.File tempFile = saveBitmapToFile(bitmap, userId);

                // Find or create app folder
                String folderId = getOrCreateAppFolder();

                // Delete old photo if exists
                deleteOldPhoto(userId);

                // Upload new photo
                File fileMetadata = new File();
                fileMetadata.setName(userId + "_profile.jpg");
                fileMetadata.setParents(Collections.singletonList(folderId));
                fileMetadata.setDescription("Learnify profile photo for " + userId);

                FileContent mediaContent = new FileContent("image/jpeg", tempFile);

                File uploadedFile = driveService.files()
                        .create(fileMetadata, mediaContent)
                        .setFields("id, webContentLink, webViewLink")
                        .execute();

                // Make file publicly viewable
                makeFilePublic(uploadedFile.getId());

                String fileId = uploadedFile.getId();
                String driveUrl = "https://drive.google.com/uc?id=" + fileId;

                Log.d(TAG, "✅ Photo uploaded successfully");
                Log.d(TAG, "File ID: " + fileId);
                Log.d(TAG, "Drive URL: " + driveUrl);

                // Clean up temp file
                tempFile.delete();

                callback.onSuccess(driveUrl, fileId);

            } catch (Exception e) {
                Log.e(TAG, "❌ Upload failed", e);
                callback.onError(e);
            }
        });
    }

    /**
     * Get or create the app's folder in Drive
     */
    private String getOrCreateAppFolder() throws Exception {
        // Search for existing folder
        FileList result = driveService.files().list()
                .setQ("mimeType='" + APP_FOLDER_MIME_TYPE + "' and name='" + FOLDER_NAME + "' and trashed=false")
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute();

        if (!result.getFiles().isEmpty()) {
            String folderId = result.getFiles().get(0).getId();
            Log.d(TAG, "✅ Found existing folder: " + folderId);
            return folderId;
        }

        // Create new folder
        File folderMetadata = new File();
        folderMetadata.setName(FOLDER_NAME);
        folderMetadata.setMimeType(APP_FOLDER_MIME_TYPE);

        File folder = driveService.files()
                .create(folderMetadata)
                .setFields("id")
                .execute();

        Log.d(TAG, "✅ Created new folder: " + folder.getId());
        return folder.getId();
    }

    /**
     * Delete old profile photo for user
     */
    private void deleteOldPhoto(String userId) {
        try {
            String query = "name contains '" + userId + "_profile' and trashed=false";
            FileList result = driveService.files().list()
                    .setQ(query)
                    .setSpaces("drive")
                    .setFields("files(id)")
                    .execute();

            for (File file : result.getFiles()) {
                driveService.files().delete(file.getId()).execute();
                Log.d(TAG, "🗑️ Deleted old photo: " + file.getId());
            }
        } catch (Exception e) {
            Log.w(TAG, "⚠️ Could not delete old photo", e);
        }
    }

    /**
     * Make file publicly viewable (read-only)
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

            Log.d(TAG, "✅ File made public");
        } catch (Exception e) {
            Log.w(TAG, "⚠️ Could not make file public", e);
        }
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
    private java.io.File saveBitmapToFile(Bitmap bitmap, String userId) throws Exception {
        java.io.File tempFile = new java.io.File(context.getCacheDir(), userId + "_temp.jpg");
        FileOutputStream fos = new FileOutputStream(tempFile);

        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos);
        fos.flush();
        fos.close();

        Log.d(TAG, "💾 Saved to temp file: " + tempFile.length() + " bytes");
        return tempFile;
    }

    /**
     * Download photo from Drive URL
     */
    public void downloadPhoto(String driveUrl, DownloadCallback callback) {
        if (driveService == null) {
            callback.onError(new Exception("Drive service not initialized"));
            return;
        }

        executor.execute(() -> {
            try {
                // Extract file ID from URL
                String fileId = extractFileIdFromUrl(driveUrl);

                if (fileId == null) {
                    callback.onError(new Exception("Invalid Drive URL"));
                    return;
                }

                Log.d(TAG, "📥 Downloading photo from Drive: " + fileId);

                // Download file
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                driveService.files().get(fileId)
                        .executeMediaAndDownloadTo(outputStream);

                byte[] bytes = outputStream.toByteArray();
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);

                if (bitmap != null) {
                    Log.d(TAG, "✅ Photo downloaded successfully");
                    callback.onSuccess(bitmap);
                } else {
                    callback.onError(new Exception("Failed to decode image"));
                }

            } catch (Exception e) {
                Log.e(TAG, "❌ Download failed", e);
                callback.onError(e);
            }
        });
    }

    /**
     * Extract file ID from Drive URL
     */
    private String extractFileIdFromUrl(String url) {
        if (url == null) return null;

        // Format: https://drive.google.com/uc?id=FILE_ID
        if (url.contains("id=")) {
            String[] parts = url.split("id=");
            if (parts.length > 1) {
                return parts[1].split("&")[0];
            }
        }

        return null;
    }

    /**
     * Check if Drive service is available
     */
    public boolean isAvailable() {
        return driveService != null;
    }

    /**
     * Upload note image (screenshot) to Google Drive
     */
    public void uploadNoteImage(Bitmap bitmap, String videoUrl, UploadCallback callback) {
        if (driveService == null) {
            callback.onError(new Exception("Drive service not initialized. Please sign in with Google."));
            return;
        }

        executor.execute(() -> {
            try {
                Log.d(TAG, "📤 Uploading note image to Drive");

                // Create a unique filename based on timestamp
                String sanitizedUrl;
                if (videoUrl != null && !videoUrl.isEmpty()) {
                    String sanitized = videoUrl.replaceAll("[^a-zA-Z0-9]", "_");
                    sanitizedUrl = sanitized.substring(0, Math.min(20, sanitized.length()));
                } else {
                    sanitizedUrl = "note";
                }
                String fileName = "note_" + sanitizedUrl + "_" + System.currentTimeMillis() + ".jpg";
                
                // Save bitmap to temp file
                java.io.File tempFile = saveBitmapToFile(bitmap, "note_temp_" + System.currentTimeMillis());

                // Find or create notes folder
                String folderId = getOrCreateNotesFolder();

                // Upload new photo
                File fileMetadata = new File();
                fileMetadata.setName(fileName);
                fileMetadata.setParents(Collections.singletonList(folderId));
                fileMetadata.setDescription("Learnify note screenshot");

                FileContent mediaContent = new FileContent("image/jpeg", tempFile);

                File uploadedFile = driveService.files()
                        .create(fileMetadata, mediaContent)
                        .setFields("id, webContentLink, webViewLink")
                        .execute();

                // Make file publicly viewable
                makeFilePublic(uploadedFile.getId());

                String fileId = uploadedFile.getId();
                String driveUrl = "https://drive.google.com/uc?id=" + fileId;

                Log.d(TAG, "✅ Note image uploaded successfully");
                Log.d(TAG, "File ID: " + fileId);
                Log.d(TAG, "Drive URL: " + driveUrl);

                // Clean up temp file
                tempFile.delete();

                callback.onSuccess(driveUrl, fileId);

            } catch (Exception e) {
                Log.e(TAG, "❌ Note image upload failed", e);
                callback.onError(e);
            }
        });
    }

    /**
     * Get or create the notes images folder in Drive
     */
    private String getOrCreateNotesFolder() throws Exception {
        // Search for existing folder
        FileList result = driveService.files().list()
                .setQ("mimeType='" + APP_FOLDER_MIME_TYPE + "' and name='" + NOTES_FOLDER_NAME + "' and trashed=false")
                .setSpaces("drive")
                .setFields("files(id, name)")
                .execute();

        if (!result.getFiles().isEmpty()) {
            String folderId = result.getFiles().get(0).getId();
            Log.d(TAG, "✅ Found existing notes folder: " + folderId);
            return folderId;
        }

        // Create new folder
        File folderMetadata = new File();
        folderMetadata.setName(NOTES_FOLDER_NAME);
        folderMetadata.setMimeType(APP_FOLDER_MIME_TYPE);

        File folder = driveService.files()
                .create(folderMetadata)
                .setFields("id")
                .execute();

        Log.d(TAG, "✅ Created new notes folder: " + folder.getId());
        return folder.getId();
    }
}