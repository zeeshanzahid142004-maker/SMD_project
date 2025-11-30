package com.example.learnify;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles Firebase Firestore operations for user profile data,
 * specifically managing profile image URLs and metadata.
 */
public class FirebaseProfileManager {

    private static final String TAG = "FirebaseProfileManager";
    private static final String USERS_COLLECTION = "users";

    private final FirebaseFirestore db;

    /**
     * Callback for profile image URL retrieval
     */
    public interface ProfileImageCallback {
        void onSuccess(String imageUrl, String fileId);
        void onNotFound();
        void onError(Exception e);
    }

    /**
     * Callback for profile update operations
     */
    public interface UpdateCallback {
        void onSuccess();
        void onError(Exception e);
    }

    public FirebaseProfileManager() {
        this.db = FirebaseFirestore.getInstance();
    }

    /**
     * Save profile image URL and file ID to Firestore
     *
     * @param userId   Firebase user ID
     * @param imageUrl Google Drive direct URL for the image
     * @param fileId   Google Drive file ID (for deletion purposes)
     */
    public void saveProfileImageUrl(@NonNull String userId, @NonNull String imageUrl, 
                                     @NonNull String fileId, UpdateCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("profileImageUrl", imageUrl);
        updates.put("profileImageFileId", fileId);

        db.collection(USERS_COLLECTION).document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Profile image URL saved successfully");
                    if (callback != null) {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to save profile image URL", e);
                    // Try to set instead of update (in case document doesn't exist)
                    db.collection(USERS_COLLECTION).document(userId)
                            .set(updates, com.google.firebase.firestore.SetOptions.merge())
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "✅ Profile image URL set successfully (merge)");
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
     * Retrieve profile image URL from Firestore
     *
     * @param userId   Firebase user ID
     * @param callback Callback for the result
     */
    public void getProfileImageUrl(@NonNull String userId, @NonNull ProfileImageCallback callback) {
        db.collection(USERS_COLLECTION).document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String imageUrl = documentSnapshot.getString("profileImageUrl");
                        String fileId = documentSnapshot.getString("profileImageFileId");
                        
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            Log.d(TAG, "✅ Profile image URL retrieved: " + imageUrl);
                            callback.onSuccess(imageUrl, fileId);
                        } else {
                            Log.d(TAG, "ℹ️ No profile image URL found for user");
                            callback.onNotFound();
                        }
                    } else {
                        Log.d(TAG, "ℹ️ User document not found");
                        callback.onNotFound();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to get profile image URL", e);
                    callback.onError(e);
                });
    }

    /**
     * Update user profile with arbitrary fields
     *
     * @param userId   Firebase user ID
     * @param updates  Map of field names to values
     * @param callback Callback for the result
     */
    public void updateUserProfile(@NonNull String userId, @NonNull Map<String, Object> updates, 
                                   UpdateCallback callback) {
        db.collection(USERS_COLLECTION).document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ User profile updated successfully");
                    if (callback != null) {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to update user profile", e);
                    // Try to set instead of update (in case document doesn't exist)
                    db.collection(USERS_COLLECTION).document(userId)
                            .set(updates, com.google.firebase.firestore.SetOptions.merge())
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "✅ User profile set successfully (merge)");
                                if (callback != null) {
                                    callback.onSuccess();
                                }
                            })
                            .addOnFailureListener(e2 -> {
                                Log.e(TAG, "❌ Failed to set user profile", e2);
                                if (callback != null) {
                                    callback.onError(e2);
                                }
                            });
                });
    }

    /**
     * Clear profile image data from Firestore
     *
     * @param userId   Firebase user ID
     * @param callback Callback for the result
     */
    public void clearProfileImage(@NonNull String userId, UpdateCallback callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("profileImageUrl", null);
        updates.put("profileImageFileId", null);

        db.collection(USERS_COLLECTION).document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Profile image data cleared");
                    if (callback != null) {
                        callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to clear profile image data", e);
                    if (callback != null) {
                        callback.onError(e);
                    }
                });
    }
}
