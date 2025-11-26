package com.example.learnify;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuizAttemptRepository {

    private static final String TAG = "QuizAttemptRepository";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    /**
     * Save quiz attempt to history
     */
    public void saveQuizAttempt(String quizId, String quizTitle, int correctCount,
                                int totalQuestions, List<QuizQuestion> questions) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Log.e(TAG, "❌ User not authenticated");
            return;
        }

        String userId = user.getUid();
        String attemptId = quizId + "_" + System.currentTimeMillis();

        Map<String, Object> historyEntry = new HashMap<>();
        historyEntry.put("attemptId", attemptId); // Storing it in the data too for redundancy
        historyEntry.put("quizId", quizId);
        historyEntry.put("quizTitle", quizTitle);
        historyEntry.put("score", correctCount);
        historyEntry.put("totalQuestions", totalQuestions);
        historyEntry.put("percentage", totalQuestions > 0 ? (correctCount * 100 / totalQuestions) : 0);
        historyEntry.put("attemptedAt", new Date());
        historyEntry.put("isDownloaded", false);
        historyEntry.put("isFavorite", false);

        db.collection("users").document(userId)
                .collection("quizHistory").document(attemptId)
                .set(historyEntry)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Quiz attempt saved to history");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to save quiz history", e);
                });
    }

    /**
     * Get recent quiz attempts (up to 5)
     */
    public void getRecentAttempts(int limit, OnAttemptsLoadedListener listener) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            listener.onError(new Exception("User not authenticated"));
            return;
        }

        String userId = user.getUid();

        db.collection("users").document(userId)
                .collection("quizHistory")
                .orderBy("attemptedAt", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<QuizAttempt> attempts = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        QuizAttempt attempt = doc.toObject(QuizAttempt.class);
                        // ✅ CRITICAL FIX: Manually set the ID from the document key
                        attempt.attemptId = doc.getId();
                        attempts.add(attempt);
                    }
                    Log.d(TAG, "✅ Loaded " + attempts.size() + " recent attempts");
                    listener.onAttemptsLoaded(attempts);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to load recent attempts", e);
                    listener.onError(e);
                });
    }

    /**
     * Get ALL quiz attempts (for HistoryActivity)
     */
    public void getAllAttempts(OnAttemptsLoadedListener listener) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            listener.onError(new Exception("User not authenticated"));
            return;
        }

        String userId = user.getUid();

        db.collection("users").document(userId)
                .collection("quizHistory")
                .orderBy("attemptedAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<QuizAttempt> attempts = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        QuizAttempt attempt = doc.toObject(QuizAttempt.class);
                        // ✅ CRITICAL FIX: Manually set the ID from the document key
                        attempt.attemptId = doc.getId();
                        attempts.add(attempt);
                    }
                    Log.d(TAG, "✅ Loaded " + attempts.size() + " total attempts");
                    listener.onAttemptsLoaded(attempts);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to load attempts", e);
                    listener.onError(e);
                });
    }

    /**
     * Mark attempt as downloaded
     */
    public void markAsDownloaded(String attemptId, String quizId, String quizTitle,
                                 List<QuizQuestion> questions) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;
        if (attemptId == null) { Log.e(TAG, "Attempt ID is null!"); return; }

        String userId = user.getUid();

        // Update history entry
        db.collection("users").document(userId)
                .collection("quizHistory").document(attemptId)
                .update("isDownloaded", true)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "✅ Marked as downloaded in history"))
                .addOnFailureListener(e -> Log.e(TAG, "❌ Failed to mark download", e));

        // Also save to downloads collection
        Map<String, Object> downloadEntry = new HashMap<>();
        downloadEntry.put("quizId", quizId);
        downloadEntry.put("quizTitle", quizTitle);
        downloadEntry.put("totalQuestions", questions != null ? questions.size() : 0);
        downloadEntry.put("fullQuizData", questions);
        downloadEntry.put("downloadedAt", new Date());
        downloadEntry.put("isFavorite", false);
        downloadEntry.put("lastScore", 0);
        downloadEntry.put("attemptCount", 0);

        db.collection("users").document(userId)
                .collection("downloads").document(quizId)
                .set(downloadEntry)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "✅ Quiz saved to downloads"))
                .addOnFailureListener(e -> Log.e(TAG, "❌ Failed to save download", e));
    }

    /**
     * Mark attempt as favorite
     */
    public void markAsFavorite(String attemptId, boolean isFavorite) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        // ✅ Safety Check to prevent Crash
        if (attemptId == null) {
            Log.e(TAG, "❌ Cannot toggle favorite: attemptId is null");
            return;
        }

        String userId = user.getUid();

        db.collection("users").document(userId)
                .collection("quizHistory").document(attemptId)
                .update("isFavorite", isFavorite)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "✅ Favorite status updated"))
                .addOnFailureListener(e -> Log.e(TAG, "❌ Failed to update favorite", e));
    }

    public interface OnAttemptsLoadedListener {
        void onAttemptsLoaded(List<QuizAttempt> attempts);
        void onError(Exception e);
    }
}