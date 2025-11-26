package com.example.learnify;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget. TextView;
import android.widget.Toast;

import androidx.annotation. Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class RecommendationsActivity extends AppCompatActivity {

    private static final String TAG = "RecommendationsActivity";

    private TextView tvHeader;
    private RecyclerView rvRecommendations;
    private MaterialToolbar toolbar;
    private LinearLayout llYoutubeSection;
    private MaterialButton btnDownloadQuiz;
    private MaterialButton btnMarkFavorite;

    private int totalQuestions;
    private int correctCount;
    private List<QuizQuestion> incorrectQuestions;
    private String quizId;
    private String quizTitle;

    private QuizHistoryRepository historyRepository;
    private RecommendationService recommendationService;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recommendations);

        Log.d(TAG, "=== RecommendationsActivity started ===");

        historyRepository = new QuizHistoryRepository();
        recommendationService = new RecommendationService();

        // Get data from intent
        totalQuestions = getIntent().getIntExtra("TOTAL_QUESTIONS", 0);
        correctCount = getIntent().getIntExtra("CORRECT_COUNT", 0);
        incorrectQuestions = (List<QuizQuestion>) getIntent().getSerializableExtra("INCORRECT_QUESTIONS");
        quizId = getIntent().getStringExtra("QUIZ_ID");
        quizTitle = getIntent(). getStringExtra("QUIZ_TITLE");

        if (incorrectQuestions == null) {
            incorrectQuestions = new ArrayList<>();
        }

        // Initialize views
        toolbar = findViewById(R.id.toolbar);
        tvHeader = findViewById(R.id.tv_recommendation_header);
        rvRecommendations = findViewById(R. id.rv_recommendations);
        llYoutubeSection = findViewById(R.id.ll_youtube_section);
        btnDownloadQuiz = findViewById(R.id.btn_download_quiz);
        btnMarkFavorite = findViewById(R.id.btn_mark_favorite);

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

        // Setup YouTube recommendations
        setupYoutubeRecommendations();

        // Setup download and favorite buttons
        setupActionButtons();

        Log.d(TAG, "=== RecommendationsActivity setup complete ===");
    }

    private void setupHeader() {
        int percentage = totalQuestions > 0 ? (correctCount * 100 / totalQuestions) : 0;
        StringBuilder headerText = new StringBuilder();

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

        headerText.append(emoji). append("\n\n");
        headerText.append("You scored ").append(correctCount).append("/").append(totalQuestions);
        headerText.append(" (").append(percentage).append("%)");

        if (incorrectQuestions.isEmpty()) {
            headerText.append("\n\n✨ Perfect score! No recommendations needed.");
        } else {
            headerText.append("\n\n📋 Here are ").append(incorrectQuestions. size());
            headerText.append(" topic"). append(incorrectQuestions.size() > 1 ? "s" : "");
            headerText.append(" to review:");
        }

        tvHeader. setText(headerText.toString());
    }

    private void setupRecommendations() {
        if (incorrectQuestions.isEmpty()) {
            rvRecommendations.setVisibility(View.GONE);
            return;
        }

        List<RecommendationItem> recommendations = new ArrayList<>();

        for (QuizQuestion q : incorrectQuestions) {
            boolean isCoding = q.type != null && q.type.equals("CODING");
            String description = isCoding ? "Practice this coding exercise" : "Review this topic";

            RecommendationItem item = new RecommendationItem(
                    q.questionText,
                    description,
                    isCoding ?  "CODING" : "CONCEPT",
                    q
            );
            recommendations.add(item);
        }

        RecommendationAdapter adapter = new RecommendationAdapter(recommendations, item -> {
            if (item.type.equals("CODING")) {
                openCodingExercise(item. question);
            } else {
                showReview(item.question);
            }
        });

        rvRecommendations.setAdapter(adapter);
        rvRecommendations.setVisibility(View.VISIBLE);
    }

    /**
     * ⭐ NEW: Setup YouTube recommendations from AI
     */
    private void setupYoutubeRecommendations() {
        if (incorrectQuestions.isEmpty() || llYoutubeSection == null) {
            if (llYoutubeSection != null) llYoutubeSection.setVisibility(View.GONE);
            return;
        }

        // Clear previous videos
        llYoutubeSection.removeAllViews();

        // Add header
        TextView tvYoutubeHeader = new TextView(this);
        tvYoutubeHeader.setText("📺 Recommended Videos");
        tvYoutubeHeader.setTextSize(16);
        tvYoutubeHeader.setTypeface(null, android.graphics. Typeface.BOLD);
        tvYoutubeHeader.setPadding(16, 16, 16, 8);
        llYoutubeSection.addView(tvYoutubeHeader);

        // Fetch recommendations for each incorrect question
        for (QuizQuestion q : incorrectQuestions) {
            String topic = extractTopicFromQuestion(q.questionText);

            recommendationService.getYoutubeRecommendation(topic, videoUrl -> {
                if (videoUrl != null && !videoUrl.isEmpty()) {
                    addVideoItem(llYoutubeSection, topic, videoUrl);
                }
            });
        }

        llYoutubeSection.setVisibility(View.VISIBLE);
    }

    /**
     * Add a YouTube video item to the recommendations
     */
    private void addVideoItem(LinearLayout parent, String topic, String videoUrl) {
        MaterialButton btnVideo = new MaterialButton(this);
        btnVideo.setText("▶ " + topic);
        btnVideo.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams. MATCH_PARENT,
                LinearLayout.LayoutParams. WRAP_CONTENT
        ));
        ((LinearLayout.LayoutParams) btnVideo.getLayoutParams()).setMargins(16, 8, 16, 8);

        btnVideo.setOnClickListener(v -> openYoutubeVideo(videoUrl));
        parent.addView(btnVideo);
    }

    /**
     * Extract main topic from question text
     */
    private String extractTopicFromQuestion(String questionText) {
        // Simple extraction - take first few words
        String[] words = questionText.split(" ");
        StringBuilder topic = new StringBuilder();
        int limit = Math.min(4, words.length);
        for (int i = 0; i < limit; i++) {
            topic.append(words[i]). append(" ");
        }
        return topic.toString().trim();
    }

    /**
     * Setup Download and Favorite buttons
     */
    private void setupActionButtons() {
        if (btnDownloadQuiz != null) {
            btnDownloadQuiz.setOnClickListener(v -> downloadQuiz());
        }
        if (btnMarkFavorite != null) {
            btnMarkFavorite.setOnClickListener(v -> markAsFavorite());
        }
    }

    private void downloadQuiz() {
        if (quizId == null || incorrectQuestions == null) {
            Toast.makeText(this, "Cannot download quiz", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "⬇️ Downloading quiz.. .", Toast.LENGTH_SHORT).show();

        // Note: Pass full questions list from original quiz
        // For now, we'll use incorrectQuestions - in production, pass all questions
        historyRepository.markAsDownloaded(quizId, quizTitle != null ? quizTitle : "Downloaded Quiz",
                incorrectQuestions);

        btnDownloadQuiz.setEnabled(false);
        btnDownloadQuiz.setText("✅ Downloaded");
    }

    private void markAsFavorite() {
        if (quizId == null) {
            Toast.makeText(this, "Cannot mark as favorite", Toast.LENGTH_SHORT).show();
            return;
        }

        historyRepository.markAsFavorite(quizId, true);
        Toast.makeText(this, "❤️ Added to favorites!", Toast.LENGTH_SHORT).show();
        btnMarkFavorite.setEnabled(false);
        btnMarkFavorite.setText("❤️ Favorited");
    }

    private void openYoutubeVideo(String videoUrl) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(android.net.Uri.parse(videoUrl));
        startActivity(intent);
    }

    private void openCodingExercise(QuizQuestion question) {
        Log.d(TAG, "Opening coding exercise for: " + question.questionText);

        Intent intent = new Intent(this, CodingExerciseActivity. class);
        intent.putExtra("QUESTION", question);
        startActivity(intent);
    }

    private void showReview(QuizQuestion question) {
        Log.d(TAG, "Showing review for: " + question.questionText);

        View dialogView = getLayoutInflater().inflate(R. layout.dialog_review, null);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        TextView tvQuestion = dialogView.findViewById(R. id.tv_review_question);
        TextView tvYourAnswer = dialogView.findViewById(R.id.tv_review_your_answer);
        TextView tvCorrectAnswer = dialogView.findViewById(R.id.tv_review_correct_answer);
        TextView tvExplanation = dialogView.findViewById(R.id.tv_review_explanation);
        MaterialButton btnGotIt = dialogView.findViewById(R.id.btn_review_close);

        tvQuestion.setText(question.questionText);

        if (question.selectedOptionIndex >= 0 && question.selectedOptionIndex < question.options.size()) {
            tvYourAnswer.setText("Your Answer: " + question.options.get(question.selectedOptionIndex));
            tvYourAnswer.setVisibility(View.VISIBLE);
        } else {
            tvYourAnswer.setVisibility(View. GONE);
        }

        tvCorrectAnswer.setText("Correct Answer: " + question.correctAnswer);

        if (question.explanation != null && !question.explanation.isEmpty()) {
            tvExplanation.setText(question.explanation);
            tvExplanation.setVisibility(View.VISIBLE);
        } else {
            tvExplanation.setVisibility(View. GONE);
        }

        btnGotIt.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }
}