package com.example.learnify.activities;

import static java.security.AccessController.getContext;

import android. os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android. util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget. TextView;
import android.widget. Toast;

import androidx.annotation. Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.learnify.helpers.CustomToast;
import com.example.learnify.helpers.DialogHelper;
import com.example.learnify.modelclass.Quiz;
import com.example.learnify.modelclass.QuizAttempt;
import com.example.learnify.repository.QuizAttemptRepository;
import com.example.learnify.R;
import com.example.learnify.adapters.HistoryAdapter;
import com.google.android.material. appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.facebook.shimmer.ShimmerFrameLayout;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
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
    private ImageView btnSortOptions;
    private ImageView btnSortDirection;
    private String currentSortOption = "Newest First";
    private boolean isAscending = false; // false = descending (newest first), true = ascending (oldest first)
    private int currentFilterChipId = -1;

    // Shimmer loading
    private ShimmerFrameLayout shimmerContainer;

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
        btnSortOptions = findViewById(R.id.btn_sort_options);
        btnSortDirection = findViewById(R.id.btn_sort_direction);
        
        // Shimmer loading
        shimmerContainer = findViewById(R.id.shimmer_container);

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
        if(btnSortOptions != null) {
            btnSortOptions.setOnClickListener(v -> openSortDialog());
        }
        // Setup search functionality
        setupSearch();
        
        // Setup filter chips
        setupFilterChips();
        
        // Setup sort spinner and direction toggle

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


    // 1. Keep track of current sort (Member variable)
    private DialogHelper.SortOption currentSort = DialogHelper.SortOption.NEWEST_FIRST;

    // 2. Call this when the sort icon is clicked
    // Corrected openSortDialog method
    private void openSortDialog() {
        // Change getContext() to this
        DialogHelper.showSortSelectionDialog(this, currentSort, selectedOption -> {
            currentSort = selectedOption;

            switch (selectedOption) {
                case NEWEST_FIRST:
                    sortListByDateDesc();
                    break;
                case OLDEST_FIRST:
                    sortListByDateAsc();
                    break;
                case HIGHEST_SCORE:
                    sortListByScoreDesc();
                    break;
                case LOWEST_SCORE:
                    sortListByScoreAsc();
                    break;
            }
        });
    }



    /**
     * Helper to configure sort for Newest First (Date Descending)
     */
    private void sortListByDateDesc() {
        currentSortOption = "Newest First"; // Matches your switch case string
        isAscending = false;                // Descending = Newest First
        applyFiltersAndSort();              // Re-run the main logic
    }

    /**
     * Helper to configure sort for Oldest First (Date Ascending)
     */
    private void sortListByDateAsc() {
        currentSortOption = "Oldest First"; // Matches your switch case string
        isAscending = true;                 // Ascending = Oldest First
        applyFiltersAndSort();
    }

    /**
     * Helper to configure sort for Highest Score First (Score Descending)
     */
    private void sortListByScoreDesc() {
        currentSortOption = "Highest Score"; // Matches your switch case string
        isAscending = false;                 // Descending = 100 -> 0
        applyFiltersAndSort();
    }

    /**
     * Helper to configure sort for Lowest Score First (Score Ascending)
     */
    private void sortListByScoreAsc() {
        currentSortOption = "Lowest Score";  // Matches your switch case string
        isAscending = true;                  // Ascending = 0 -> 100
        applyFiltersAndSort();
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

    // Add this import at the very top of HistoryActivity.java


// ... inside the class ...

    private void setupSearch() {
        if (searchView != null) {
            // 1. Get the internal Text View (The typing area)
            android.widget.EditText searchEditText = searchView.findViewById(androidx.appcompat.R.id.search_src_text);

            if (searchEditText != null) {
                // ✅ FIX 1: Use ContextCompat to solve "Deprecated" error
                // ✅ FIX 2: Set text to WHITE so it shows on the black background
                searchEditText.setTextColor(ContextCompat.getColor(this, R.color.white));

                // Set hint text to Gray so it's readable but distinct
                searchEditText.setHintTextColor(ContextCompat.getColor(this, R.color.text_secondary_gray));
            }

            // 2. Color the Search Icon (Magnifying glass) to White/Gray
            android.widget.ImageView searchIcon = searchView.findViewById(androidx.appcompat.R.id.search_mag_icon);
            if (searchIcon != null) {
                searchIcon.setColorFilter(ContextCompat.getColor(this, R.color.text_secondary_gray));
            }

            // 3. Color the Close Icon (X button) to White/Gray
            android.widget.ImageView closeIcon = searchView.findViewById(androidx.appcompat.R.id.search_close_btn);
            if (closeIcon != null) {
                closeIcon.setColorFilter(ContextCompat.getColor(this, R.color.text_secondary_gray));
            }

            // ... Your existing listeners ...
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

        // Show shimmer before loading
        showShimmer();

        attemptRepository.getAllAttempts(new QuizAttemptRepository.OnAttemptsLoadedListener() {
            @Override
            public void onAttemptsLoaded(List<QuizAttempt> loadedAttempts) {
                // Hide shimmer after data loads
                hideShimmer();
                
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
                // Hide shimmer on error
                hideShimmer();
                
                Log.e(TAG, "❌ Failed to load history", e);

                CustomToast.error(HistoryActivity.this, "Failed to load history");
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

                            CustomToast.success(HistoryActivity.this, "✅ Quiz downloaded!");
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to download quiz", e);

                    CustomToast.error(HistoryActivity.this, "Failed to download");
                });
    }

    private void toggleFavorite(QuizAttempt attempt, int position) {
        attempt.isFavorite = !attempt.isFavorite;

        Log.d(TAG, "❤️ Toggling favorite: " + attempt.isFavorite);

        attemptRepository.markAsFavorite(attempt.attemptId, attempt. isFavorite);
        adapter.updateItem(position, attempt);

        String message = attempt.isFavorite ?  "❤️ Added to favorites" : "Removed from favorites";

        CustomToast.info(this, message);
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

    private void showShimmer() {
        if (shimmerContainer != null) {
            shimmerContainer.setVisibility(View.VISIBLE);
            shimmerContainer.startShimmer();
            rvHistory.setVisibility(View.GONE);
            llEmptyState.setVisibility(View.GONE);
        }
    }

    private void hideShimmer() {
        if (shimmerContainer != null) {
            shimmerContainer.stopShimmer();
            shimmerContainer.setVisibility(View.GONE);
        }
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