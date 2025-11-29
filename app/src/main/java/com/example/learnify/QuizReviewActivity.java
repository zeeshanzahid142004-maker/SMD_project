package com.example.learnify;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Activity for reviewing completed quiz attempts.
 * Shows all questions with user's answers, correct answers, and explanations.
 */
public class QuizReviewActivity extends BaseActivity {

    private static final String TAG = "QuizReviewActivity";

    private MaterialToolbar toolbar;
    private TextView tvQuizTitle;
    private TextView tvScoreSummary;
    private RecyclerView rvQuestions;
    private MaterialButton btnRetake;
    private View loadingView;

    private String quizId;
    private String quizTitle;
    private int score;
    private int totalQuestions;
    private String attemptId;
    private List<QuizQuestion> questions;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_review);

        Log.d(TAG, "📖 QuizReviewActivity opened");

        db = FirebaseFirestore.getInstance();

        // Initialize views
        toolbar = findViewById(R.id.toolbar);
        tvQuizTitle = findViewById(R.id.tv_quiz_title);
        tvScoreSummary = findViewById(R.id.tv_score_summary);
        rvQuestions = findViewById(R.id.rv_questions);
        btnRetake = findViewById(R.id.btn_retake);
        loadingView = findViewById(R.id.loading_view);

        // Setup toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // Get data from intent
        extractIntentData();

        // Setup UI
        setupUI();

        // Load quiz questions if not passed
        if (questions == null || questions.isEmpty()) {
            loadQuizQuestions();
        } else {
            setupQuestionsRecyclerView();
        }
    }

    private void extractIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            quizId = intent.getStringExtra("QUIZ_ID");
            quizTitle = intent.getStringExtra("QUIZ_TITLE");
            score = intent.getIntExtra("SCORE", 0);
            totalQuestions = intent.getIntExtra("TOTAL_QUESTIONS", 0);
            attemptId = intent.getStringExtra("ATTEMPT_ID");
            
            Serializable questionsData = intent.getSerializableExtra("QUESTIONS");
            if (questionsData instanceof ArrayList) {
                questions = (ArrayList<QuizQuestion>) questionsData;
            }

            Log.d(TAG, "📊 Quiz: " + quizTitle + " | Score: " + score + "/" + totalQuestions);
        }
    }

    private void setupUI() {
        // Set quiz title
        if (quizTitle != null && !quizTitle.isEmpty()) {
            tvQuizTitle.setText(quizTitle);
        } else {
            tvQuizTitle.setText("Quiz Review");
        }

        // Set score summary
        int percentage = totalQuestions > 0 ? (score * 100 / totalQuestions) : 0;
        String scoreSummary = "Score: " + score + "/" + totalQuestions + " (" + percentage + "%)";
        tvScoreSummary.setText(scoreSummary);

        // Color code score
        if (percentage >= 70) {
            tvScoreSummary.setTextColor(getColor(android.R.color.holo_green_dark));
        } else if (percentage >= 50) {
            tvScoreSummary.setTextColor(getColor(android.R.color.holo_orange_dark));
        } else {
            tvScoreSummary.setTextColor(getColor(android.R.color.holo_red_dark));
        }

        // Setup retake button
        btnRetake.setOnClickListener(v -> retakeQuiz());
    }

    private void loadQuizQuestions() {
        if (quizId == null || quizId.isEmpty()) {
            Toast.makeText(this, "Quiz ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        if (loadingView != null) {
            loadingView.setVisibility(View.VISIBLE);
        }

        db.collection("quizzes").document(quizId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (loadingView != null) {
                        loadingView.setVisibility(View.GONE);
                    }

                    if (documentSnapshot.exists()) {
                        Quiz quiz = documentSnapshot.toObject(Quiz.class);
                        if (quiz != null && quiz.getQuestions() != null) {
                            questions = quiz.getQuestions();
                            setupQuestionsRecyclerView();
                        } else {
                            Toast.makeText(this, "No questions found", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Quiz not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    if (loadingView != null) {
                        loadingView.setVisibility(View.GONE);
                    }
                    Log.e(TAG, "Failed to load quiz", e);
                    Toast.makeText(this, "Failed to load quiz questions", Toast.LENGTH_SHORT).show();
                });
    }

    private void setupQuestionsRecyclerView() {
        if (questions == null || questions.isEmpty()) {
            Toast.makeText(this, "No questions to display", Toast.LENGTH_SHORT).show();
            return;
        }

        rvQuestions.setLayoutManager(new LinearLayoutManager(this));
        QuizReviewAdapter adapter = new QuizReviewAdapter(questions);
        rvQuestions.setAdapter(adapter);

        Log.d(TAG, "✅ Displaying " + questions.size() + " questions for review");
    }

    private void retakeQuiz() {
        if (quizId == null || quizId.isEmpty()) {
            Toast.makeText(this, "Cannot retake - Quiz ID missing", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "🔄 Retaking quiz: " + quizId);

        // Load quiz and start QuizActivity
        db.collection("quizzes").document(quizId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Quiz quiz = documentSnapshot.toObject(Quiz.class);
                        if (quiz != null && quiz.getQuestions() != null) {
                            // Reset question states
                            List<QuizQuestion> freshQuestions = quiz.getQuestions();
                            for (QuizQuestion q : freshQuestions) {
                                q.isAnswered = false;
                                q.isCorrect = false;
                                q.selectedOptionIndex = -1;
                            }

                            Intent intent = new Intent(this, QuizActivity.class);
                            intent.putExtra("QUIZ_ID", quizId);
                            intent.putExtra("QUIZ_TITLE", quizTitle != null ? quizTitle : quiz.getTitle());
                            intent.putExtra("QUIZ_DATA", (Serializable) freshQuestions);
                            startActivity(intent);
                            finish();
                        }
                    } else {
                        Toast.makeText(this, "Quiz not found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load quiz for retake", e);
                    Toast.makeText(this, "Failed to load quiz", Toast.LENGTH_SHORT).show();
                });
    }
}
