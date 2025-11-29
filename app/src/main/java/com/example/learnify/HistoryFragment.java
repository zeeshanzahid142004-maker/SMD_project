package com.example.learnify;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android. widget.ImageView;
import android.widget. LinearLayout;
import android.widget. Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation. Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment. app.Fragment;
import androidx. recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com. google.android.material.chip. ChipGroup;
import com. facebook.shimmer.ShimmerFrameLayout;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class HistoryFragment extends Fragment {

    private static final String TAG = "HistoryFragment";

    private RecyclerView rvHistory;
    private LinearLayout llEmptyState;
    private TextView tvEmptyMessage;
    private TextView tvNoResults;
    private SearchView searchView;
    private HistoryAdapter adapter;
    private List<QuizAttempt> allAttempts = new ArrayList<>();
    private List<QuizAttempt> filteredAttempts = new ArrayList<>();

    private ChipGroup filterChipGroup;
    private Chip chipAll, chipFavorites, chipRetaken, chipHighScore, chipNeedsReview;

    private Spinner sortSpinner;
    private ImageView btnSortDirection;
    private String currentSortOption = "Newest First";
    private boolean isAscending = false;
    private int currentFilterChipId = -1;

    private ShimmerFrameLayout shimmerContainer;
    private QuizAttemptRepository attemptRepository;
    private FirebaseFirestore db;

    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private static final long SEARCH_DEBOUNCE_DELAY = 300;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        attemptRepository = new QuizAttemptRepository();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        rvHistory = view. findViewById(R.id.rv_history);
        llEmptyState = view.findViewById(R. id.ll_empty_state);
        tvEmptyMessage = view.findViewById(R.id.tv_empty_message);
        tvNoResults = view.findViewById(R.id.tv_no_results);
        searchView = view. findViewById(R.id.search_view);

        filterChipGroup = view.findViewById(R.id.filter_chip_group);
        chipAll = view.findViewById(R.id.chip_all);
        chipFavorites = view.findViewById(R. id.chip_favorites);
        chipRetaken = view.findViewById(R.id.chip_retaken);
        chipHighScore = view.findViewById(R.id.chip_high_score);
        chipNeedsReview = view. findViewById(R.id.chip_needs_review);

        sortSpinner = view.findViewById(R. id.sort_spinner);
        btnSortDirection = view.findViewById(R.id.btn_sort_direction);
        shimmerContainer = view.findViewById(R.id.shimmer_container);

        // Setup RecyclerView
        rvHistory.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new HistoryAdapter(filteredAttempts, new HistoryAdapter.OnHistoryActionListener() {
            @Override
            public void onDownload(QuizAttempt attempt, int position) {
                downloadQuiz(attempt, position);
            }

            @Override
            public void onToggleFavorite(QuizAttempt attempt, int position) {
                toggleFavorite(attempt, position);
            }
        });
        rvHistory.setAdapter(adapter);

        setupSearch();
        setupFilterChips();
        setupSortSpinner();
        setupSortDirectionToggle();

        loadAllHistory();
    }

    private void setupFilterChips() {
        if (chipAll == null) return;

        currentFilterChipId = R.id.chip_all;

        filterChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                chipAll.setChecked(true);
                currentFilterChipId = R.id.chip_all;
            } else {
                currentFilterChipId = checkedIds.get(0);
            }
            applyFiltersAndSort();
        });
    }

    private void setupSortSpinner() {
        if (sortSpinner == null) return;

        String[] sortOptions = {"Newest First", "Oldest First", "Highest Score", "Lowest Score"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout. simple_spinner_item, sortOptions);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(spinnerAdapter);

        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentSortOption = sortOptions[position];
                applyFiltersAndSort();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setupSortDirectionToggle() {
        if (btnSortDirection == null) return;

        updateSortDirectionIcon();

        btnSortDirection.setOnClickListener(v -> {
            isAscending = !isAscending;
            updateSortDirectionIcon();
            applyFiltersAndSort();
        });
    }

    private void updateSortDirectionIcon() {
        if (btnSortDirection != null) {
            btnSortDirection.setRotation(isAscending ? 90 : 270);
        }
    }

    private void applyFiltersAndSort() {
        filteredAttempts. clear();

        for (QuizAttempt attempt : allAttempts) {
            boolean matchesFilter = false;

            if (currentFilterChipId == R.id.chip_all) {
                matchesFilter = true;
            } else if (currentFilterChipId == R.id. chip_favorites) {
                matchesFilter = attempt.isFavorite;
            } else if (currentFilterChipId == R.id. chip_retaken) {
                matchesFilter = attempt.attemptNumber > 1;
            } else if (currentFilterChipId == R.id.chip_high_score) {
                matchesFilter = attempt.percentage > 70;
            } else if (currentFilterChipId == R.id.chip_needs_review) {
                matchesFilter = attempt. percentage < 50;
            }

            if (matchesFilter) {
                filteredAttempts.add(attempt);
            }
        }

        // Sort
        switch (currentSortOption) {
            case "Newest First":
            case "Oldest First":
                if (isAscending) {
                    Collections.sort(filteredAttempts, (a, b) -> compareDates(a. attemptedAt, b.attemptedAt, true));
                } else {
                    Collections.sort(filteredAttempts, (a, b) -> compareDates(a.attemptedAt, b. attemptedAt, false));
                }
                break;
            case "Highest Score":
            case "Lowest Score":
                if (isAscending) {
                    Collections.sort(filteredAttempts, (a, b) -> Integer.compare(a.percentage, b.percentage));
                } else {
                    Collections.sort(filteredAttempts, (a, b) -> Integer.compare(b.percentage, a.percentage));
                }
                break;
        }

        if (filteredAttempts.isEmpty()) {
            showNoResults();
        } else {
            hideNoResults();
        }

        adapter.updateFullList(filteredAttempts);
        adapter.notifyDataSetChanged();
    }

    private int compareDates(Date a, Date b, boolean ascending) {
        if (a == null && b == null) return 0;
        if (a == null) return 1;
        if (b == null) return -1;
        return ascending ? a.compareTo(b) : b.compareTo(a);
    }

    private void setupSearch() {
        if (searchView == null) return;

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                performSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }
                searchRunnable = () -> performSearch(newText);
                searchHandler.postDelayed(searchRunnable, SEARCH_DEBOUNCE_DELAY);
                return true;
            }
        });
    }

    private void performSearch(String query) {
        if (adapter != null) {
            adapter.getFilter().filter(query, count -> {
                if (count == 0 && !query.isEmpty()) {
                    showNoResults();
                } else {
                    hideNoResults();
                }
            });
        }
    }

    private void showNoResults() {
        if (tvNoResults != null) tvNoResults.setVisibility(View.VISIBLE);
    }

    private void hideNoResults() {
        if (tvNoResults != null) tvNoResults. setVisibility(View.GONE);
    }

    private void loadAllHistory() {
        showShimmer();

        attemptRepository.getAllAttempts(new QuizAttemptRepository.OnAttemptsLoadedListener() {
            @Override
            public void onAttemptsLoaded(List<QuizAttempt> loadedAttempts) {
                hideShimmer();
                allAttempts.clear();
                allAttempts.addAll(loadedAttempts);

                if (allAttempts. isEmpty()) {
                    showEmptyState();
                } else {
                    showContent();
                    applyFiltersAndSort();
                }
            }

            @Override
            public void onError(Exception e) {
                hideShimmer();
                Log. e(TAG, "Failed to load history", e);
                showEmptyState();
            }
        });
    }

    private void downloadQuiz(QuizAttempt attempt, int position) {
        db.collection("quizzes"). document(attempt.quizId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Quiz quiz = doc.toObject(Quiz.class);
                        if (quiz != null) {
                            attemptRepository.markAsDownloaded(attempt. attemptId, attempt.quizId, attempt.quizTitle, quiz.getQuestions());
                            attempt.isDownloaded = true;
                            adapter.updateItem(position, attempt);
                            Toast.makeText(getContext(), "✅ Downloaded!", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void toggleFavorite(QuizAttempt attempt, int position) {
        attempt.isFavorite = !attempt.isFavorite;
        attemptRepository.markAsFavorite(attempt.attemptId, attempt.isFavorite);
        adapter.updateItem(position, attempt);
        Toast.makeText(getContext(), attempt.isFavorite ? "❤️ Favourited!" : "Removed", Toast.LENGTH_SHORT).show();
    }

    private void showEmptyState() {
        if (llEmptyState != null) llEmptyState.setVisibility(View.VISIBLE);
        if (rvHistory != null) rvHistory.setVisibility(View. GONE);
    }

    private void showContent() {
        if (llEmptyState != null) llEmptyState.setVisibility(View.GONE);
        if (rvHistory != null) rvHistory.setVisibility(View.VISIBLE);
    }

    private void showShimmer() {
        if (shimmerContainer != null) {
            shimmerContainer.setVisibility(View.VISIBLE);
            shimmerContainer.startShimmer();
        }
        if (rvHistory != null) rvHistory.setVisibility(View. GONE);
    }

    private void hideShimmer() {
        if (shimmerContainer != null) {
            shimmerContainer. stopShimmer();
            shimmerContainer.setVisibility(View.GONE);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAllHistory();
    }
}