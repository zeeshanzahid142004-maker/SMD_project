package com.example.learnify;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java. util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuizHistoryRepository {

    private static final String TAG = "QuizHistoryRepository";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    /**
     * Save quiz attempt to Firebase history
     */
    public void saveQuizAttempt(String quizId, String quizTitle, int correctCount,
                                int totalQuestions, List<QuizQuestion> questions,
                                boolean isDownloaded, boolean isFavorite) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Log.e(TAG, "❌ User not authenticated");
            return;
        }

        String userId = user.getUid();

        Map<String, Object> historyEntry = new HashMap<>();
        historyEntry.put("quizId", quizId);
        historyEntry.put("quizTitle", quizTitle);
        historyEntry.put("score", correctCount);
        historyEntry.put("totalQuestions", totalQuestions);
        historyEntry.put("percentage", (correctCount * 100 / totalQuestions));
        historyEntry. put("attemptedAt", new Date());
        historyEntry.put("isDownloaded", isDownloaded);
        historyEntry.put("isFavorite", isFavorite);

        db.collection("users").document(userId)
                .collection("quizHistory"). document(quizId + "_" + System.currentTimeMillis())
                .set(historyEntry)
                .addOnSuccessListener(aVoid -> {
                    Log. d(TAG, "✅ Quiz attempt saved to history");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to save quiz history", e);
                });
    }

    /**
     * Mark quiz as downloaded AND save full quiz data
     */
    public void markAsDownloaded(String quizId, String quizTitle, List<QuizQuestion> questions) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Log.e(TAG, "❌ User not authenticated");
            return;
        }

        String userId = user.getUid();
        int totalQuestions = questions != null ? questions.size() : 0;

        Map<String, Object> downloadEntry = new HashMap<>();
        downloadEntry.put("quizId", quizId);
        downloadEntry. put("quizTitle", quizTitle);
        downloadEntry.put("totalQuestions", totalQuestions);
        downloadEntry.put("fullQuizData", questions);
        downloadEntry.put("downloadedAt", new Date());
        downloadEntry.put("isFavorite", false);
        downloadEntry.put("lastScore", 0);
        downloadEntry.put("attemptCount", 0);
        downloadEntry.put("lastAttemptedAt", null);

        db. collection("users").document(userId)
                .collection("downloads").document(quizId)
                .set(downloadEntry)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Quiz marked as downloaded");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to download quiz", e);
                });
    }

    /**
     * Mark quiz as favorite
     */
    public void markAsFavorite(String quizId, boolean isFavorite) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        String userId = user. getUid();

        Map<String, Object> updates = new HashMap<>();
        updates. put("isFavorite", isFavorite);

        db. collection("users").document(userId)
                .collection("downloads").document(quizId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Quiz favorite status updated");
                })
                . addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to update favorite", e);
                });
    }

    /**
     * Update attempt count and last score after retaking quiz
     */
    public void updateQuizAttempt(String quizId, int newScore, int totalQuestions) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        String userId = user.getUid();

        db.collection("users").document(userId)
                .collection("downloads").document(quizId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        int currentAttempts = documentSnapshot.getLong("attemptCount").intValue();

                        Map<String, Object> updates = new HashMap<>();
                        updates.put("attemptCount", currentAttempts + 1);
                        updates.put("lastScore", newScore);
                        updates.put("lastAttemptedAt", new Date());

                        db. collection("users").document(userId)
                                .collection("downloads"). document(quizId)
                                .update(updates)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "✅ Quiz attempt updated");
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "❌ Failed to update attempt", e);
                                });
                    }
                });
    }

    /**
     * Get downloaded quiz by ID for retaking
     */
    public void getDownloadedQuiz(String quizId, OnQuizFetchedListener listener) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            listener.onError(new Exception("User not authenticated"));
            return;
        }

        String userId = user.getUid();

        db.collection("users").document(userId)
                . collection("downloads").document(quizId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        List<QuizQuestion> questions = (List<QuizQuestion>) documentSnapshot.get("fullQuizData");
                        listener.onQuizFetched(questions);
                    } else {
                        listener.onError(new Exception("Quiz not found"));
                    }
                })
                . addOnFailureListener(listener::onError);
    }

    public interface OnQuizFetchedListener {
        void onQuizFetched(List<QuizQuestion> questions);
        void onError(Exception e);
    }
}