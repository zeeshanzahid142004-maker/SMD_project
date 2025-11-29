package com.example.learnify;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget. TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx. annotation.Nullable;
import androidx. fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class DownloadsFragment extends Fragment {

    private static final String TAG = "DownloadsFragment";

    private RecyclerView rvDownloads;
    private TextView tvEmptyState;
    private LinearLayout llEmpty;
    private DownloadedQuizAdapter adapter;
    private List<DownloadedQuizItem> downloadedQuizzes = new ArrayList<>();

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private QuizHistoryRepository historyRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_downloads, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Log.d(TAG, "DownloadsFragment view created");

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        historyRepository = new QuizHistoryRepository();

        // Initialize views
        rvDownloads = view.findViewById(R.id. rv_downloads);
        tvEmptyState = view.findViewById(R.id.tv_empty_state);
        llEmpty = view. findViewById(R.id.ll_empty_state);

        // Setup RecyclerView
        rvDownloads.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new DownloadedQuizAdapter(downloadedQuizzes, new DownloadedQuizAdapter. OnQuizActionListener() {
            @Override
            public void onRetakeQuiz(DownloadedQuizItem quiz) {
                retakeQuiz(quiz);
            }

            @Override
            public void onReviewQuiz(DownloadedQuizItem quiz) {
                reviewQuiz(quiz);
            }

            @Override
            public void onToggleFavorite(DownloadedQuizItem quiz, int position) {
                toggleFavorite(quiz, position);
            }

            @Override
            public void onDeleteDownload(DownloadedQuizItem quiz, int position) {
                deleteDownload(quiz, position);
            }
        });
        rvDownloads.setAdapter(adapter);

        // Load downloaded quizzes
        loadDownloadedQuizzes();
    }

    /**
     * Load all downloaded quizzes from Firebase for current user
     */
    private void loadDownloadedQuizzes() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Log.e(TAG, "❌ User not authenticated");
            showEmptyState();
            return;
        }

        String userId = user.getUid();
        Log.d(TAG, "📥 Loading downloads for user: " + userId);

        db.collection("users").document(userId)
                .collection("downloads")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    downloadedQuizzes.clear();

                    if (querySnapshot.isEmpty()) {
                        Log. d(TAG, "No downloaded quizzes found");
                        showEmptyState();
                        return;
                    }

                    for (QueryDocumentSnapshot document : querySnapshot) {
                        try {
                            DownloadedQuizItem quiz = document.toObject(DownloadedQuizItem.class);
                            if (quiz != null) {
                                downloadedQuizzes.add(quiz);
                                Log.d(TAG, "✅ Loaded: " + quiz.quizTitle);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing downloaded quiz", e);
                        }
                    }

                    adapter.notifyDataSetChanged();
                    showContent();
                    Log.d(TAG, "✅ Loaded " + downloadedQuizzes.size() + " downloaded quizzes");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to load downloads", e);
                    Toast.makeText(getContext(), "Failed to load downloads: " + e.getMessage(), Toast. LENGTH_SHORT).show();
                    showEmptyState();
                });
    }

    /**
     * Review a downloaded quiz - opens QuizReviewActivity
     */
    private void reviewQuiz(DownloadedQuizItem quiz) {
        if (quiz.fullQuizData == null || quiz.fullQuizData.isEmpty()) {
            Toast.makeText(getContext(), "Quiz data not found", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "📖 Reviewing quiz: " + quiz.quizTitle);

        Intent intent = new Intent(getActivity(), QuizReviewActivity.class);
        intent.putExtra("QUIZ_ID", quiz.quizId);
        intent.putExtra("QUIZ_TITLE", quiz.quizTitle);
        intent.putExtra("SCORE", quiz.lastScore);
        intent.putExtra("TOTAL_QUESTIONS", quiz.totalQuestions);
        intent.putExtra("QUESTIONS", new ArrayList<>(quiz.fullQuizData));

        startActivity(intent);
    }

    /**
     * Retake a downloaded quiz
     */
    private void retakeQuiz(DownloadedQuizItem quiz) {
        if (quiz.fullQuizData == null || quiz.fullQuizData.isEmpty()) {
            Toast.makeText(getContext(), "Quiz data not found", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "🔄 Retaking quiz: " + quiz.quizTitle);

        Intent intent = new Intent(getActivity(), QuizActivity.class);
        intent.putExtra("QUIZ_DATA", new ArrayList<>(quiz.fullQuizData));
        intent.putExtra("QUIZ_ID", quiz.quizId);
        intent.putExtra("QUIZ_TITLE", quiz.quizTitle);
        intent.putExtra("IS_DOWNLOADED", true);

        startActivity(intent);
    }

    /**
     * Toggle favorite status for a quiz
     */
    private void toggleFavorite(DownloadedQuizItem quiz, int position) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        quiz.isFavorite = !quiz.isFavorite;
        String userId = user.getUid();

        Log.d(TAG, "❤️ Toggling favorite for: " + quiz.quizTitle + " -> " + quiz.isFavorite);

        db.collection("users").document(userId)
                .collection("downloads").document(quiz.quizId)
                .update("isFavorite", quiz.isFavorite)
                .addOnSuccessListener(aVoid -> {
                    Log. d(TAG, "✅ Favorite status updated");
                    adapter.updateItem(position, quiz);
                    Toast.makeText(getContext(),
                            quiz.isFavorite ? "❤️ Added to favorites" : "Removed from favorites",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to update favorite", e);
                    // Revert the change
                    quiz.isFavorite = !quiz.isFavorite;
                    Toast.makeText(getContext(), "Failed to update", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Delete a downloaded quiz
     */
    private void deleteDownload(DownloadedQuizItem quiz, int position) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) return;

        String userId = user.getUid();

        Log.d(TAG, "🗑️ Deleting download: " + quiz.quizTitle);

        db.collection("users").document(userId)
                . collection("downloads").document(quiz.quizId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Quiz deleted from downloads");
                    adapter.removeItem(position);
                    Toast.makeText(getContext(), "Quiz removed from downloads", Toast.LENGTH_SHORT).show();

                    if (downloadedQuizzes.isEmpty()) {
                        showEmptyState();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to delete download", e);
                    Toast. makeText(getContext(), "Failed to delete: " + e.getMessage(), Toast. LENGTH_SHORT).show();
                });
    }

    private void showEmptyState() {
        if (llEmpty != null && rvDownloads != null) {
            llEmpty.setVisibility(View.VISIBLE);
            rvDownloads.setVisibility(View.GONE);
        }
    }

    private void showContent() {
        if (llEmpty != null && rvDownloads != null) {
            llEmpty.setVisibility(View.GONE);
            rvDownloads.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh downloads when returning to this fragment
        loadDownloadedQuizzes();
    }
}