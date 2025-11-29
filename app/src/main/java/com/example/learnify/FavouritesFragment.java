package com.example.learnify;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class FavouritesFragment extends Fragment {

    private static final String TAG = "FavouritesFragment";

    private RecyclerView rvFavourites;
    private LinearLayout layoutEmpty;
    private ProgressBar progressBar;
    private ShimmerFrameLayout shimmerContainer;

    private HistoryAdapter adapter;
    private List<QuizAttempt> favoriteAttempts = new ArrayList<>();

    private QuizAttemptRepository attemptRepository;
    private FirebaseFirestore db;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        attemptRepository = new QuizAttemptRepository();
        db = FirebaseFirestore.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_favourites, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvFavourites = view.findViewById(R.id.rv_favourites);
        layoutEmpty = view.findViewById(R.id.layout_empty);
        progressBar = view.findViewById(R.id.progress_bar);
        shimmerContainer = view.findViewById(R.id.shimmer_container);

        rvFavourites.setLayoutManager(new LinearLayoutManager(getContext()));

        // ⭐ CRITICAL: Setting up the adapter with the Download Listener
        adapter = new HistoryAdapter(favoriteAttempts, new HistoryAdapter.OnHistoryActionListener() {
            @Override
            public void onDownload(QuizAttempt attempt, int position) {
                // Call the download logic
                downloadQuiz(attempt, position);
            }

            @Override
            public void onToggleFavorite(QuizAttempt attempt, int position) {
                // Handle unfavoriting (remove from list)
                removeFavorite(attempt, position);
            }
        });

        rvFavourites.setAdapter(adapter);

        loadFavorites();
    }

    private void loadFavorites() {
        // Show shimmer before loading
        showShimmer();

        // Fetch all attempts and filter for favorites
        attemptRepository.getAllAttempts(new QuizAttemptRepository.OnAttemptsLoadedListener() {
            @Override
            public void onAttemptsLoaded(List<QuizAttempt> attempts) {
                if (getContext() == null) return;

                // Hide shimmer after data loads
                hideShimmer();
                
                favoriteAttempts.clear();
                for (QuizAttempt attempt : attempts) {
                    if (attempt.isFavorite()) {
                        favoriteAttempts.add(attempt);
                    }
                }

                progressBar.setVisibility(View.GONE);

                if (favoriteAttempts.isEmpty()) {
                    layoutEmpty.setVisibility(View.VISIBLE);
                    rvFavourites.setVisibility(View.GONE);
                } else {
                    layoutEmpty.setVisibility(View.GONE);
                    rvFavourites.setVisibility(View.VISIBLE);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onError(Exception e) {
                if (getContext() == null) return;
                
                // Hide shimmer on error
                hideShimmer();
                
                progressBar.setVisibility(View.GONE);
                Log.e(TAG, "Error loading favorites", e);
            }
        });
    }

    // ✅ THE MISSING LOGIC: Fetch Quiz Data + Save to Downloads
    private void downloadQuiz(QuizAttempt attempt, int position) {
        Toast.makeText(getContext(), "Downloading...", Toast.LENGTH_SHORT).show();

        // 1. Fetch the full quiz data (questions) from "quizzes" collection
        db.collection("quizzes").document(attempt.getQuizId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Quiz quiz = documentSnapshot.toObject(Quiz.class);
                        if (quiz != null) {

                            // 2. Save to "Downloads" via Repository
                            attemptRepository.markAsDownloaded(
                                    attempt.getAttemptId(),
                                    attempt.getQuizId(),
                                    attempt.getQuizTitle(),
                                    quiz.getQuestions()
                            );

                            // 3. Update UI
                            attempt.setDownloaded(true);
                            adapter.updateItem(position, attempt);
                            Toast.makeText(getContext(), "✅ Downloaded!", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "Quiz data not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Download failed", e);
                    Toast.makeText(getContext(), "Download failed", Toast.LENGTH_SHORT).show();
                });
    }

    private void removeFavorite(QuizAttempt attempt, int position) {
        // 1. Update Repository
        attemptRepository.markAsFavorite(attempt.getAttemptId(), false);

        // 2. Remove from UI list immediately
        if (position >= 0 && position < favoriteAttempts.size()) {
            favoriteAttempts.remove(position);
            adapter.notifyItemRemoved(position);
            adapter.notifyItemRangeChanged(position, favoriteAttempts.size());
        }

        if (favoriteAttempts.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
        }

        Toast.makeText(getContext(), "Removed from favorites", Toast.LENGTH_SHORT).show();
    }

    private void showShimmer() {
        if (shimmerContainer != null) {
            shimmerContainer.setVisibility(View.VISIBLE);
            shimmerContainer.startShimmer();
            if (rvFavourites != null) rvFavourites.setVisibility(View.GONE);
            if (layoutEmpty != null) layoutEmpty.setVisibility(View.GONE);
            if (progressBar != null) progressBar.setVisibility(View.GONE);
        }
    }

    private void hideShimmer() {
        if (shimmerContainer != null) {
            shimmerContainer.stopShimmer();
            shimmerContainer.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFavorites(); // Refresh in case changed elsewhere
    }
}