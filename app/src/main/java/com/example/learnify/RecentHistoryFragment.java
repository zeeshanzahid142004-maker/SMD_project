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
import androidx.annotation.Nullable;
import androidx. fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class RecentHistoryFragment extends Fragment {

    private static final String TAG = "RecentHistoryFragment";

    private RecyclerView rvRecentHistory;
    private LinearLayout llEmptyHistory;
    private MaterialButton btnViewMore;
    private HistoryAdapter adapter;
    private List<QuizAttempt> recentAttempts = new ArrayList<>();

    private QuizAttemptRepository attemptRepository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_recent_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Log.d(TAG, "📜 RecentHistoryFragment created");

        attemptRepository = new QuizAttemptRepository();

        // Initialize views
        rvRecentHistory = view.findViewById(R.id. rv_recent_history);
        llEmptyHistory = view.findViewById(R.id.ll_empty_history);
        btnViewMore = view.findViewById(R.id.btn_view_more_history);

        // Setup RecyclerView
        rvRecentHistory.setLayoutManager(new LinearLayoutManager(getContext()));

        // ⭐ CREATE ADAPTER CORRECTLY
        adapter = new HistoryAdapter(recentAttempts, new HistoryAdapter.OnHistoryActionListener() {
            @Override
            public void onDownload(QuizAttempt attempt, int position) {
                startActivity(new Intent(getActivity(), HistoryActivity.class));
            }

            @Override
            public void onToggleFavorite(QuizAttempt attempt, int position) {
                attempt.isFavorite = !attempt.isFavorite;
                attemptRepository.markAsFavorite(attempt.attemptId, attempt.isFavorite);
                adapter.updateItem(position, attempt);
                String msg = attempt.isFavorite ? "❤️ Favourited!" : "Removed from favourites";
                Toast.makeText(getContext(), msg, Toast. LENGTH_SHORT).show();
            }
        });
        rvRecentHistory.setAdapter(adapter);

        // View More button
        btnViewMore.setOnClickListener(v -> {
            Log.d(TAG, "📜 Opening HistoryActivity");
            startActivity(new Intent(getActivity(), HistoryActivity.class));
        });

        // Load recent attempts
        loadRecentHistory();
    }

    private void loadRecentHistory() {
        Log.d(TAG, "📥 Loading recent history (up to 5).. .");

        attemptRepository.getRecentAttempts(5, new QuizAttemptRepository.OnAttemptsLoadedListener() {
            @Override
            public void onAttemptsLoaded(List<QuizAttempt> attempts) {
                recentAttempts.clear();
                recentAttempts.addAll(attempts);

                if (recentAttempts.isEmpty()) {
                    llEmptyHistory.setVisibility(View.VISIBLE);
                    rvRecentHistory.setVisibility(View.GONE);
                    btnViewMore.setVisibility(View.GONE);
                } else {
                    llEmptyHistory.setVisibility(View.GONE);
                    rvRecentHistory.setVisibility(View.VISIBLE);
                    btnViewMore.setVisibility(View.VISIBLE);
                    adapter.notifyDataSetChanged();
                    Log.d(TAG, "✅ Loaded " + recentAttempts.size() + " recent attempts");
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "❌ Failed to load recent history", e);
                llEmptyHistory.setVisibility(View.VISIBLE);
                rvRecentHistory.setVisibility(View.GONE);
                btnViewMore.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadRecentHistory(); // Refresh when returning
    }
}