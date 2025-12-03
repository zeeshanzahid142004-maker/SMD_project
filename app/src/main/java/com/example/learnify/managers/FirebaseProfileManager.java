package com.example.learnify.managers;

import android.util.Log;

import com.google.firebase. firestore.FirebaseFirestore;

import java.util. HashMap;
import java. util.Map;

public class FirebaseProfileManager {

    private static final String TAG = "FirebaseProfileManager";
    private static final String COLLECTION_USERS = "users";

    // Field names
    private static final String FIELD_PROFILE_IMAGE_URL = "profileImageUrl";
    private static final String FIELD_PROFILE_IMAGE_FILE_ID = "profileImageFileId";

    // Legacy field names (for backward compatibility)
    private static final String FIELD_DRIVE_PHOTO_URL = "drivePhotoUrl";
    private static final String FIELD_DRIVE_FILE_ID = "driveFileId";

    private final FirebaseFirestore db;

    public FirebaseProfileManager() {
        this. db = FirebaseFirestore.getInstance();
    }

    /**
     * Callback for profile image operations
     */
    public interface ProfileImageCallback {
        void onSuccess(String imageUrl, String fileId);
        void onNotFound();
        void onError(Exception e);
    }

    /**
     * Callback for update operations
     */
    public interface UpdateCallback {
        void onSuccess();
        void onError(Exception e);
    }

    /**
     * Save profile image URL to Firestore
     */
    public void saveProfileImageUrl(String userId, String imageUrl, String fileId, UpdateCallback callback) {
        if (userId == null || userId.isEmpty()) {
            if (callback != null) {
                callback. onError(new Exception("Invalid user ID"));
            }
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put(FIELD_PROFILE_IMAGE_URL, imageUrl);
        updates.put(FIELD_PROFILE_IMAGE_FILE_ID, fileId);

        // Also update legacy fields for backward compatibility
        updates.put(FIELD_DRIVE_PHOTO_URL, imageUrl);
        updates.put(FIELD_DRIVE_FILE_ID, fileId);

        db.collection(COLLECTION_USERS)
                .document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Profile image URL saved for user: " + userId);
                    if (callback != null) {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to save profile image URL", e);
                    // Try to set (create) if update fails (document might not exist)
                    db.collection(COLLECTION_USERS)
                            .document(userId)
                            .set(updates, com.google.firebase. firestore.SetOptions.merge())
                            .addOnSuccessListener(aVoid -> {
                                Log. d(TAG, "✅ Profile image URL set for user: " + userId);
                                if (callback != null) {
                                    callback.onSuccess();
                                }
                            })
                            .addOnFailureListener(e2 -> {
                                Log.e(TAG, "❌ Failed to set profile image URL", e2);
                                if (callback != null) {
                                    callback.onError(e2);
                                }
                            });
                });
    }

    /**
     * Get profile image URL from Firestore
     */
    public void getProfileImageUrl(String userId, ProfileImageCallback callback) {
        if (userId == null || userId.isEmpty()) {
            callback.onError(new Exception("Invalid user ID"));
            return;
        }

        db.collection(COLLECTION_USERS)
                . document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Try new field names first
                        String imageUrl = documentSnapshot.getString(FIELD_PROFILE_IMAGE_URL);
                        String fileId = documentSnapshot. getString(FIELD_PROFILE_IMAGE_FILE_ID);

                        // Fall back to legacy field names
                        if (imageUrl == null || imageUrl.isEmpty()) {
                            imageUrl = documentSnapshot.getString(FIELD_DRIVE_PHOTO_URL);
                            fileId = documentSnapshot.getString(FIELD_DRIVE_FILE_ID);
                        }

                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            Log.d(TAG, "✅ Found profile image URL for user: " + userId);
                            callback.onSuccess(imageUrl, fileId);
                        } else {
                            Log.d(TAG, "⚠️ No profile image URL found for user: " + userId);
                            callback.onNotFound();
                        }
                    } else {
                        Log. d(TAG, "⚠️ User document not found: " + userId);
                        callback.onNotFound();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to get profile image URL", e);
                    callback.onError(e);
                });
    }

    /**
     * Delete profile image URL from Firestore
     */
    public void deleteProfileImageUrl(String userId, UpdateCallback callback) {
        if (userId == null || userId.isEmpty()) {
            if (callback != null) {
                callback. onError(new Exception("Invalid user ID"));
            }
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put(FIELD_PROFILE_IMAGE_URL, null);
        updates.put(FIELD_PROFILE_IMAGE_FILE_ID, null);
        updates.put(FIELD_DRIVE_PHOTO_URL, null);
        updates. put(FIELD_DRIVE_FILE_ID, null);

        db.collection(COLLECTION_USERS)
                . document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log. d(TAG, "✅ Profile image URL deleted for user: " + userId);
                    if (callback != null) {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    Log. e(TAG, "❌ Failed to delete profile image URL", e);
                    if (callback != null) {
                        callback.onError(e);
                    }
                });
    }

    /**
     * Update user profile with custom fields
     */
    public void updateUserProfile(String userId, Map<String, Object> updates, UpdateCallback callback) {
        if (userId == null || userId.isEmpty()) {
            if (callback != null) {
                callback.onError(new Exception("Invalid user ID"));
            }
            return;
        }

        db.collection(COLLECTION_USERS)
                .document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ User profile updated for: " + userId);
                    if (callback != null) {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to update user profile", e);
                    if (callback != null) {
                        callback.onError(e);
                    }
                });
    }

    /**
     * Check if user has a profile image
     */
    public void hasProfileImage(String userId, ProfileImageCallback callback) {
        getProfileImageUrl(userId, callback);
    }
}