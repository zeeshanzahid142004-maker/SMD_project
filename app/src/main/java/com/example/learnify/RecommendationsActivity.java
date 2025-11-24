package com.example.learnify;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

public class RecommendationsActivity extends AppCompatActivity {

    private static final String TAG = "RecommendationsActivity";

    private TextView tvHeader;
    private RecyclerView rvRecommendations;
    private MaterialToolbar toolbar;

    private int totalQuestions;
    private int correctCount;
    private List<QuizQuestion> incorrectQuestions;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recommendations);

        // Get data from intent
        totalQuestions = getIntent().getIntExtra("TOTAL_QUESTIONS", 0);
        correctCount = getIntent().getIntExtra("CORRECT_COUNT", 0);
        incorrectQuestions = (List<QuizQuestion>) getIntent().getSerializableExtra("INCORRECT_QUESTIONS");

        if (incorrectQuestions == null) {
            incorrectQuestions = new ArrayList<>();
        }

        Log.d(TAG, "Score: " + correctCount + "/" + totalQuestions);
        Log.d(TAG, "Incorrect questions: " + incorrectQuestions.size());

        // Initialize views
        toolbar = findViewById(R.id.toolbar);
        tvHeader = findViewById(R.id.tv_recommendation_header);
        rvRecommendations = findViewById(R.id.rv_recommendations);

        // Setup toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // Setup header
        setupHeader();

        // Setup recommendations list
        setupRecommendations();
    }

    private void setupHeader() {
        int percentage = totalQuestions > 0 ? (correctCount * 100 / totalQuestions) : 0;
        StringBuilder headerText = new StringBuilder();

        // Emoji based on score
        String emoji;
        if (percentage >= 90) {
            emoji = "🎉 Outstanding!";
        } else if (percentage >= 70) {
            emoji = "👏 Great Job!";
        } else if (percentage >= 50) {
            emoji = "💪 Good Effort!";
        } else {
            emoji = "📚 Keep Learning!";
        }

        headerText.append(emoji).append("\n\n");
        headerText.append("You scored ").append(correctCount).append("/").append(totalQuestions);
        headerText.append(" (").append(percentage).append("%)");

        if (incorrectQuestions.isEmpty()) {
            headerText.append("\n\n✨ Perfect score! No recommendations needed.");
        } else {
            headerText.append("\n\n📋 Here are ").append(incorrectQuestions.size());
            headerText.append(" topic").append(incorrectQuestions.size() > 1 ? "s" : "");
            headerText.append(" to review:");
        }

        tvHeader.setText(headerText.toString());
    }

    private void setupRecommendations() {
        if (incorrectQuestions.isEmpty()) {
            // No recommendations needed - hide RecyclerView
            rvRecommendations.setVisibility(View.GONE);
            return;
        }

        List<RecommendationItem> recommendations = new ArrayList<>();

        for (QuizQuestion q : incorrectQuestions) {
            // Check if it's a coding question
            boolean isCoding = q.type != null && q.type.equals("CODING");

            String description = "Review this topic";
            if (isCoding) {
                description = "Practice this coding exercise";
            }

            RecommendationItem item = new RecommendationItem(
                    q.questionText,
                    description,
                    isCoding ? "CODING" : "CONCEPT",
                    q
            );
            recommendations.add(item);
        }

        // Setup adapter with click handling
        RecommendationAdapter adapter = new RecommendationAdapter(recommendations, item -> {
            // Handle click - open coding exercise or show review
            if (item.type.equals("CODING")) {
                openCodingExercise(item.question);
            } else {
                showReview(item.question);
            }
        });

        rvRecommendations.setAdapter(adapter);
        rvRecommendations.setVisibility(View.VISIBLE);
    }

    private void openCodingExercise(QuizQuestion question) {
        Log.d(TAG, "Opening coding exercise for: " + question.questionText);

        Intent intent = new Intent(this, CodingExerciseActivity.class);
        intent.putExtra("QUESTION", question);
        startActivity(intent);
    }

    private void showReview(QuizQuestion question) {
        Log.d(TAG, "Showing review for: " + question.questionText);

        // Create beautiful review dialog
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_review, null);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        // Make dialog background transparent
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        // Setup dialog content
        TextView tvQuestion = dialogView.findViewById(R.id.tv_review_question);
        TextView tvYourAnswer = dialogView.findViewById(R.id.tv_review_your_answer);
        TextView tvCorrectAnswer = dialogView.findViewById(R.id.tv_review_correct_answer);
        TextView tvExplanation = dialogView.findViewById(R.id.tv_review_explanation);
        com.google.android.material.button.MaterialButton btnGotIt = dialogView.findViewById(R.id.btn_review_close);

        tvQuestion.setText(question.questionText);

        // Show user's answer if available
        if (question.selectedOptionIndex >= 0 && question.selectedOptionIndex < question.options.size()) {
            tvYourAnswer.setText("Your Answer: " + question.options.get(question.selectedOptionIndex));
            tvYourAnswer.setVisibility(View.VISIBLE);
        } else {
            tvYourAnswer.setVisibility(View.GONE);
        }

        tvCorrectAnswer.setText("Correct Answer: " + question.correctAnswer);

        // Show explanation if available
        if (question.explanation != null && !question.explanation.isEmpty()) {
            tvExplanation.setText(question.explanation);
            tvExplanation.setVisibility(View.VISIBLE);
        } else {
            tvExplanation.setVisibility(View.GONE);
        }

        btnGotIt.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}