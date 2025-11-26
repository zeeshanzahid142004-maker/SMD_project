package com.example.learnify;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget. TextView;
import android.widget. Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx. fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class FavouritesFragment extends Fragment {

    private static final String TAG = "FavouritesFragment";

    private RecyclerView rvFavourites;
    private LinearLayout llEmptyFavourites;
    private TextView tvEmptyMessage;
    private HistoryAdapter adapter;
    private List<QuizAttempt> favouritesList = new ArrayList<>();

    private QuizAttemptRepository attemptRepository;
    private FirebaseAuth auth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_favourites, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Log.d(TAG, "❤️ FavouritesFragment created");

        attemptRepository = new QuizAttemptRepository();
        auth = FirebaseAuth.getInstance();

        // Initialize views
        rvFavourites = view.findViewById(R.id.rv_favourites);
        llEmptyFavourites = view.findViewById(R.id. ll_empty_favourites);
        tvEmptyMessage = view. findViewById(R.id.tv_empty_message);

        // Setup RecyclerView
        rvFavourites.setLayoutManager(new LinearLayoutManager(getContext()));

        // ⭐ CREATE ADAPTER CORRECTLY
        adapter = new HistoryAdapter(favouritesList, new HistoryAdapter.OnHistoryActionListener() {
            @Override
            public void onDownload(QuizAttempt attempt, int position) {
                Toast.makeText(getContext(), "Opening History to download", Toast.LENGTH_SHORT). show();
            }

            @Override
            public void onToggleFavorite(QuizAttempt attempt, int position) {
                toggleFavorite(attempt, position);
            }
        });
        rvFavourites.setAdapter(adapter);

        // Load favourite quizzes
        loadFavourites();
    }

    /**
     * Load all favourite quizzes from Firebase
     */
    private void loadFavourites() {
        Log.d(TAG, "📥 Loading favourite quizzes.. .");

        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            showEmptyState("Please log in to view favourites");
            Log.e(TAG, "❌ User not authenticated");
            return;
        }

        // Get all quiz attempts and filter by favourite
        attemptRepository.getAllAttempts(new QuizAttemptRepository.OnAttemptsLoadedListener() {
            @Override
            public void onAttemptsLoaded(List<QuizAttempt> attempts) {
                favouritesList.clear();

                // Filter only favourite quizzes
                for (QuizAttempt attempt : attempts) {
                    if (attempt.isFavorite) {
                        favouritesList. add(attempt);
                        Log.d(TAG, "❤️ Found favourite: " + attempt.quizTitle);
                    }
                }

                if (favouritesList.isEmpty()) {
                    showEmptyState("No favourite quizzes yet.\nMark quizzes as favourite!");
                    Log.d(TAG, "📭 No favourites found");
                } else {
                    showContent();
                    adapter.notifyDataSetChanged();
                    Log.d(TAG, "✅ Loaded " + favouritesList.size() + " favourite quizzes");
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "❌ Failed to load favourites", e);
                showEmptyState("Failed to load favourites");
                Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast. LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Toggle favourite status
     */
    private void toggleFavorite(QuizAttempt attempt, int position) {
        attempt.isFavorite = !attempt.isFavorite;

        Log.d(TAG, "❤️ Toggling favourite: " + attempt.isFavorite);

        attemptRepository.markAsFavorite(attempt. attemptId, attempt.isFavorite);

        if (! attempt.isFavorite) {
            // Remove from list if unfavourited
            favouritesList.remove(position);
            adapter.notifyItemRemoved(position);

            if (favouritesList.isEmpty()) {
                showEmptyState("No favourite quizzes yet");
            }
        } else {
            adapter.updateItem(position, attempt);
        }

        String message = attempt.isFavorite ? "❤️ Added to favourites" : "Removed from favourites";
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void showEmptyState(String message) {
        llEmptyFavourites.setVisibility(View.VISIBLE);
        rvFavourites.setVisibility(View.GONE);
        tvEmptyMessage.setText(message);
    }

    private void showContent() {
        llEmptyFavourites.setVisibility(View.GONE);
        rvFavourites.setVisibility(View.VISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadFavourites(); // Refresh when returning
    }
}