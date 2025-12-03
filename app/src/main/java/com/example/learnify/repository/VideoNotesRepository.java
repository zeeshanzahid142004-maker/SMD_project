package com.example.learnify.repository;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VideoNotesRepository {

    private static final String TAG = "VideoNotesRepository";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    /**
     * Save or update video notes
     */
    public void saveNotes(String videoUrl, String content, OnNoteSavedListener listener) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            listener.onError(new Exception("User not authenticated"));
            return;
        }

        String userId = user.getUid();
        String noteId = generateNoteId(videoUrl);

        Map<String, Object> noteData = new HashMap<>();
        noteData.put("videoUrl", videoUrl);
        noteData.put("content", content);
        noteData.put("updatedAt", new Date());
        noteData.put("userId", userId);

        db.collection("users").document(userId)
                .collection("videoNotes").document(noteId)
                .set(noteData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Notes saved successfully");
                    listener.onNoteSaved();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to save notes", e);
                    listener.onError(e);
                });
    }

    /**
     * Load notes for a specific video
     */
    public void loadNotes(String videoUrl, OnNotesLoadedListener listener) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            listener.onError(new Exception("User not authenticated"));
            return;
        }

        String userId = user.getUid();
        String noteId = generateNoteId(videoUrl);

        db.collection("users").document(userId)
                .collection("videoNotes").document(noteId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String content = documentSnapshot.getString("content");
                        Date updatedAt = documentSnapshot.getDate("updatedAt");
                        Log.d(TAG, "✅ Notes loaded successfully");
                        listener.onNotesLoaded(content, updatedAt);
                    } else {
                        Log.d(TAG, "No notes found for this video");
                        listener.onNotesLoaded(null, null);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to load notes", e);
                    listener.onError(e);
                });
    }

    /**
     * Get all saved video notes
     */
    public void getAllNotes(OnAllNotesLoadedListener listener) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            listener.onError(new Exception("User not authenticated"));
            return;
        }

        String userId = user.getUid();

        db.collection("users").document(userId)
                .collection("videoNotes")
                .orderBy("updatedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<VideoNote> notes = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        VideoNote note = doc.toObject(VideoNote.class);
                        notes.add(note);
                    }
                    Log.d(TAG, "✅ Loaded " + notes.size() + " notes");
                    listener.onAllNotesLoaded(notes);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to load all notes", e);
                    listener.onError(e);
                });
    }

    /**
     * Delete notes for a specific video
     */
    public void deleteNotes(String videoUrl, OnNoteDeletedListener listener) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            listener.onError(new Exception("User not authenticated"));
            return;
        }

        String userId = user.getUid();
        String noteId = generateNoteId(videoUrl);

        db.collection("users").document(userId)
                .collection("videoNotes").document(noteId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Notes deleted successfully");
                    listener.onNoteDeleted();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to delete notes", e);
                    listener.onError(e);
                });
    }

    /**
     * Toggle favorite status for a video note
     */
    public void toggleFavorite(String videoUrl, boolean isFavorite, OnFavoriteToggledListener listener) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            listener.onError(new Exception("User not authenticated"));
            return;
        }

        String userId = user.getUid();
        String noteId = generateNoteId(videoUrl);

        Map<String, Object> updates = new HashMap<>();
        updates.put("isFavorite", isFavorite);

        db.collection("users").document(userId)
                .collection("videoNotes").document(noteId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Favorite toggled successfully");
                    listener.onFavoriteToggled(isFavorite);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to toggle favorite", e);
                    listener.onError(e);
                });
    }

    /**
     * Generate consistent note ID from video URL
     */
    private String generateNoteId(String videoUrl) {
        // Use video URL hash as ID to ensure uniqueness
        return String.valueOf(videoUrl.hashCode()).replace("-", "");
    }

    // Callback Interfaces

    public interface OnNoteSavedListener {
        void onNoteSaved();
        void onError(Exception e);
    }

    public interface OnNotesLoadedListener {
        void onNotesLoaded(String content, Date updatedAt);
        void onError(Exception e);
    }

    public interface OnAllNotesLoadedListener {
        void onAllNotesLoaded(List<VideoNote> notes);
        void onError(Exception e);
    }

    public interface OnNoteDeletedListener {
        void onNoteDeleted();
        void onError(Exception e);
    }

    public interface OnFavoriteToggledListener {
        void onFavoriteToggled(boolean isFavorite);
        void onError(Exception e);
    }

    // Model class
    public static class VideoNote {
        public String videoUrl;
        public String content;
        public Date updatedAt;
        public String userId;
        public boolean isFavorite;

        public VideoNote() {}

        public VideoNote(String videoUrl, String content, Date updatedAt, String userId) {
            this.videoUrl = videoUrl;
            this.content = content;
            this.updatedAt = updatedAt;
            this.userId = userId;
            this.isFavorite = false;
        }

        public VideoNote(String videoUrl, String content, Date updatedAt, String userId, boolean isFavorite) {
            this.videoUrl = videoUrl;
            this.content = content;
            this.updatedAt = updatedAt;
            this.userId = userId;
            this.isFavorite = isFavorite;
        }
    }
}