package com.example. learnify;

import android. os.Bundle;
import android. util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget. TextView;
import android.widget. Toast;

import androidx.annotation. Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material. appbar.MaterialToolbar;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private static final String TAG = "HistoryActivity";

    private RecyclerView rvHistory;
    private LinearLayout llEmptyState;
    private TextView tvEmptyMessage;
    private MaterialToolbar toolbar;
    private HistoryAdapter adapter;
    private List<QuizAttempt> attempts = new ArrayList<>();

    private QuizAttemptRepository attemptRepository;
    private FirebaseFirestore db;

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

        // Setup toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // Setup RecyclerView
        rvHistory.setLayoutManager(new LinearLayoutManager(this));

        // ⭐ CREATE ADAPTER CORRECTLY
        adapter = new HistoryAdapter(attempts, new HistoryAdapter.OnHistoryActionListener() {
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

        // Load all history
        loadAllHistory();
    }

    private void loadAllHistory() {
        Log.d(TAG, "📥 Loading all history...");

        attemptRepository.getAllAttempts(new QuizAttemptRepository.OnAttemptsLoadedListener() {
            @Override
            public void onAttemptsLoaded(List<QuizAttempt> loadedAttempts) {
                attempts.clear();
                attempts. addAll(loadedAttempts);

                if (attempts.isEmpty()) {
                    showEmptyState();
                } else {
                    showContent();
                    adapter.notifyDataSetChanged();
                    Log.d(TAG, "✅ Loaded " + attempts.size() + " history items");
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
}