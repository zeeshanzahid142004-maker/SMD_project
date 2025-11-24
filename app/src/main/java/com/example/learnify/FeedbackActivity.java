package com.example.learnify;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;
import java.util.ArrayList;
import java.util.Locale;

public class FeedbackActivity extends AppCompatActivity {

    // You will pass this list from your QuizActivity
    private ArrayList<QuestionResult> questionResults;
    private double scorePercent = 0.0;
    private String firstWrongTopicId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        // --- Setup Toolbar ---
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // --- 1. Get data from Intent (or create dummy data) ---
        // In your real app, you'd get this from the intent
        // questionResults = (ArrayList<QuestionResult>) getIntent().getSerializableExtra("QUIZ_RESULTS");

        // Using dummy data for this example
        createDummyData();

        // --- 2. Calculate Stats ---
        int correctCount = 0;
        for (QuestionResult result : questionResults) {
            if (result.isCorrect()) {
                correctCount++;
            } else if (firstWrongTopicId == null) {
                // Store the topic of the *first* wrong answer
                firstWrongTopicId = result.getRelatedTopicId();
            }
        }
        int totalQuestions = questionResults.size();
        if (totalQuestions > 0) {
            scorePercent = (double) correctCount / totalQuestions;
        }

        // --- 3. Find & Populate Views ---
        TextView tvScoreMessage = findViewById(R.id.tv_score_message);
        TextView tvScore = findViewById(R.id.tv_score);
        TextView tvScorePercentView = findViewById(R.id.tv_score_percent);

        tvScore.setText(String.format(Locale.getDefault(), "%d/%d", correctCount, totalQuestions));
        tvScorePercentView.setText(String.format(Locale.getDefault(), "%.0f%%", scorePercent * 100));

        if (scorePercent > 0.8) tvScoreMessage.setText("Great Job!");
        else if (scorePercent > 0.5) tvScoreMessage.setText("Good Effort!");
        else tvScoreMessage.setText("Keep Practicing!");

        // --- 4. Set up RecyclerView ---
        RecyclerView rvAnswerReview = findViewById(R.id.rv_answer_review);
        AnswerReviewAdapter adapter = new AnswerReviewAdapter(questionResults);
        rvAnswerReview.setLayoutManager(new LinearLayoutManager(this));
        rvAnswerReview.setAdapter(adapter);

        // --- 5. Set up Button Click (Conditional Logic) ---
        Button btnSeeRecommendations = findViewById(R.id.btn_see_recommendations);
        btnSeeRecommendations.setOnClickListener(v -> {

            // *** CONDITIONAL LOGIC ***
            // If score is less than 80% AND we have a topic to work on
            if (scorePercent < 0.8 && firstWrongTopicId != null) {
                // Go to the Coding Exercise Screen
                Intent intent = new Intent(FeedbackActivity.this, CodingExerciseActivity.class);
                intent.putExtra("TOPIC_ID", firstWrongTopicId);
                startActivity(intent);
            } else {
                // Go directly to Recommendations
                Intent intent = new Intent(FeedbackActivity.this, RecommendationsActivity.class);
                intent.putExtra("TOPIC_ID", "general"); // Pass a general tag
                startActivity(intent);
            }
        });
    }

    private void createDummyData() {
        questionResults = new ArrayList<>();
        // Wrong answer example
        questionResults.add(new QuestionResult(
                1,
                "for_loops", // This is the topic ID
                "What keyword starts a 'for' loop?",
                "while",
                "for",
                false
        ));
        // Correct answer example
        questionResults.add(new QuestionResult(
                2,
                "variables",
                "Which type stores a whole number?",
                "int",
                "int",
                true
        ));
    }
}