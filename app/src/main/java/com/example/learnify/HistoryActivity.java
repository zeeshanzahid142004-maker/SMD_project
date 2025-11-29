package com.example. learnify;

import android. os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android. util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget. TextView;
import android.widget. Toast;

import androidx.annotation. Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material. appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class HistoryActivity extends BaseActivity {

    private static final String TAG = "HistoryActivity";

    private RecyclerView rvHistory;
    private LinearLayout llEmptyState;
    private TextView tvEmptyMessage;
    private TextView tvNoResults;
    private MaterialToolbar toolbar;
    private SearchView searchView;
    private HistoryAdapter adapter;
    private List<QuizAttempt> allAttempts = new ArrayList<>();
    private List<QuizAttempt> filteredAttempts = new ArrayList<>();

    // Filter chips
    private ChipGroup filterChipGroup;
    private Chip chipAll, chipFavorites, chipRetaken, chipHighScore, chipNeedsReview;
    
    // Sort spinner and direction toggle
    private Spinner sortSpinner;
    private ImageView btnSortDirection;
    private String currentSortOption = "Newest First";
    private boolean isAscending = false; // false = descending (newest first), true = ascending (oldest first)
    private int currentFilterChipId = -1;

    private QuizAttemptRepository attemptRepository;
    private FirebaseFirestore db;

    // Debounce handler for search
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private static final long SEARCH_DEBOUNCE_DELAY = 300; // 300ms

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        Log.d(TAG, "📜 HistoryActivity opened");

        attemptRepository = new QuizAttemptRepository();
        db = FirebaseFirestore. getInstance();

        // Initialize views
        toolbar = findViewById(R.id.toolbar);
        rvHistory = findViewById(R.id.rv_history);
        llEmptyState = findViewById(R.id.ll_empty_state);
        tvEmptyMessage = findViewById(R.id.tv_empty_message);
        tvNoResults = findViewById(R.id.tv_no_results);
        searchView = findViewById(R.id.search_view);
        
        // Filter chips
        filterChipGroup = findViewById(R.id.filter_chip_group);
        chipAll = findViewById(R.id.chip_all);
        chipFavorites = findViewById(R.id.chip_favorites);
        chipRetaken = findViewById(R.id.chip_retaken);
        chipHighScore = findViewById(R.id.chip_high_score);
        chipNeedsReview = findViewById(R.id.chip_needs_review);
        
        // Sort spinner and direction toggle
        sortSpinner = findViewById(R.id.sort_spinner);
        btnSortDirection = findViewById(R.id.btn_sort_direction);

        // Setup toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // Setup RecyclerView
        rvHistory.setLayoutManager(new LinearLayoutManager(this));

        // ⭐ CREATE ADAPTER CORRECTLY
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

        // Setup search functionality
        setupSearch();
        
        // Setup filter chips
        setupFilterChips();
        
        // Setup sort spinner and direction toggle
        setupSortSpinner();
        setupSortDirectionToggle();

        // Load all history
        loadAllHistory();
    }

    private void setupFilterChips() {
        currentFilterChipId = R.id.chip_all;
        
        filterChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                // If nothing is checked, select "All" by default
                chipAll.setChecked(true);
                currentFilterChipId = R.id.chip_all;
            } else {
                currentFilterChipId = checkedIds.get(0);
            }
            applyFiltersAndSort();
        });
    }

    private void setupSortSpinner() {
        String[] sortOptions = {"Newest First", "Oldest First", "Highest Score", "Lowest Score"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_spinner_item, sortOptions);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(spinnerAdapter);
        
        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentSortOption = sortOptions[position];
                applyFiltersAndSort();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }

    private void setupSortDirectionToggle() {
        // Initial icon state (descending = arrow down at 270 degrees)
        updateSortDirectionIcon();
        
        btnSortDirection.setOnClickListener(v -> {
            // Toggle direction
            isAscending = !isAscending;
            updateSortDirectionIcon();
            applyFiltersAndSort();
        });
    }

    private void updateSortDirectionIcon() {
        // Arrow up (90 degrees) = ascending, Arrow down (270 degrees) = descending
        btnSortDirection.setRotation(isAscending ? 90 : 270);
    }

    private void applyFiltersAndSort() {
        filteredAttempts.clear();
        
        // Apply filter based on selected chip
        for (QuizAttempt attempt : allAttempts) {
            boolean matchesFilter = false;
            
            if (currentFilterChipId == R.id.chip_all) {
                matchesFilter = true;
            } else if (currentFilterChipId == R.id.chip_favorites) {
                matchesFilter = attempt.isFavorite;
            } else if (currentFilterChipId == R.id.chip_retaken) {
                matchesFilter = attempt.attemptNumber > 1;
            } else if (currentFilterChipId == R.id.chip_high_score) {
                matchesFilter = attempt.percentage > 70;
            } else if (currentFilterChipId == R.id.chip_needs_review) {
                matchesFilter = attempt.percentage < 50;
            }
            
            if (matchesFilter) {
                filteredAttempts.add(attempt);
            }
        }
        
        // Apply sorting with direction toggle
        switch (currentSortOption) {
            case "Newest First":
            case "Oldest First":
                // For date-based sorting, apply the direction toggle
                if (isAscending) {
                    Collections.sort(filteredAttempts, (a, b) -> compareDatesAscending(a.attemptedAt, b.attemptedAt));
                } else {
                    Collections.sort(filteredAttempts, (a, b) -> compareDatesDescending(a.attemptedAt, b.attemptedAt));
                }
                break;
            case "Highest Score":
            case "Lowest Score":
                // For score-based sorting, apply the direction toggle
                if (isAscending) {
                    Collections.sort(filteredAttempts, (a, b) -> Integer.compare(a.percentage, b.percentage));
                } else {
                    Collections.sort(filteredAttempts, (a, b) -> Integer.compare(b.percentage, a.percentage));
                }
                break;
        }
        
        // Update UI
        if (filteredAttempts.isEmpty()) {
            showNoResults();
        } else {
            hideNoResults();
        }
        
        adapter.updateFullList(filteredAttempts);
        adapter.notifyDataSetChanged();
    }

    private void setupSearch() {
        if (searchView != null) {
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    performSearch(query);
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    // Debounce search input
                    if (searchRunnable != null) {
                        searchHandler.removeCallbacks(searchRunnable);
                    }
                    searchRunnable = () -> performSearch(newText);
                    searchHandler.postDelayed(searchRunnable, SEARCH_DEBOUNCE_DELAY);
                    return true;
                }
            });

            // Handle close button
            searchView.setOnCloseListener(() -> {
                if (adapter != null) {
                    adapter.getFilter().filter("");
                }
                hideNoResults();
                return false;
            });
        }
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
        if (tvNoResults != null) {
            tvNoResults.setVisibility(View.VISIBLE);
        }
    }

    private void hideNoResults() {
        if (tvNoResults != null) {
            tvNoResults.setVisibility(View.GONE);
        }
    }

    private void loadAllHistory() {
        Log.d(TAG, "📥 Loading all history...");

        attemptRepository.getAllAttempts(new QuizAttemptRepository.OnAttemptsLoadedListener() {
            @Override
            public void onAttemptsLoaded(List<QuizAttempt> loadedAttempts) {
                allAttempts.clear();
                allAttempts.addAll(loadedAttempts);

                if (allAttempts.isEmpty()) {
                    showEmptyState();
                } else {
                    showContent();
                    applyFiltersAndSort();
                    Log.d(TAG, "✅ Loaded " + allAttempts.size() + " history items");
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "❌ Failed to load history", e);
                Toast.makeText(HistoryActivity.this, "Failed to load history", Toast.LENGTH_SHORT).show();
                showEmptyState();
            }
        });
    }

    private void downloadQuiz(QuizAttempt attempt, int position) {
        Log.d(TAG, "⬇️ Downloading quiz: " + attempt.quizTitle);

        db.collection("quizzes"). document(attempt.quizId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Quiz quiz = documentSnapshot.toObject(Quiz.class);
                        if (quiz != null) {
                            attemptRepository.markAsDownloaded(
                                    attempt.attemptId,
                                    attempt.quizId,
                                    attempt. quizTitle,
                                    quiz.getQuestions()
                            );

                            attempt.isDownloaded = true;
                            adapter.updateItem(position, attempt);
                            Toast.makeText(HistoryActivity.this, "✅ Quiz downloaded!", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to download quiz", e);
                    Toast. makeText(HistoryActivity. this, "Failed to download", Toast.LENGTH_SHORT).show();
                });
    }

    private void toggleFavorite(QuizAttempt attempt, int position) {
        attempt.isFavorite = !attempt.isFavorite;

        Log.d(TAG, "❤️ Toggling favorite: " + attempt.isFavorite);

        attemptRepository.markAsFavorite(attempt.attemptId, attempt. isFavorite);
        adapter.updateItem(position, attempt);

        String message = attempt.isFavorite ?  "❤️ Added to favorites" : "Removed from favorites";
        Toast.makeText(this, message, Toast.LENGTH_SHORT). show();
    }

    private void showEmptyState() {
        llEmptyState.setVisibility(View.VISIBLE);
        rvHistory.setVisibility(View.GONE);
        tvEmptyMessage.setText("No quiz history yet.\nTake a quiz to get started!");
    }

    private void showContent() {
        llEmptyState. setVisibility(View.GONE);
        rvHistory.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAllHistory(); // Refresh when returning
    }

    /**
     * Compare two dates in descending order (newest first), handling nulls
     */
    private int compareDatesDescending(Date a, Date b) {
        if (a == null && b == null) return 0;
        if (a == null) return 1;
        if (b == null) return -1;
        return b.compareTo(a);
    }

    /**
     * Compare two dates in ascending order (oldest first), handling nulls
     */
    private int compareDatesAscending(Date a, Date b) {
        if (a == null && b == null) return 0;
        if (a == null) return 1;
        if (b == null) return -1;
        return a.compareTo(b);
    }
}