package com.example.learnify;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.OnBackPressedDispatcher;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class QuizActivity extends AppCompatActivity implements QuizCompleteDialogFragment.QuizCompleteDialogListener {

    private static final String TAG = "QuizActivity";
    private static final int REQUEST_CODE_CODING_EXERCISE = 100;
    private static final long BACK_PRESS_DELAY = 2000;

    // Views
    private ViewPager2 viewPager;
    private ProgressBar progressBar;
    private TextView tvTimer;
    private MaterialButton btnNext;
    private MaterialButton btnStartCoding; // New button for coding questions

    // Data
    private List<QuizQuestion> questions;
    private String currentQuizId;    // To store the ID
    private String currentQuizTitle; // To store the Title

    // State
    private int correctCount = 0;
    private int currentQuestionIndex = 0;
    private CountDownTimer quizTimer;
    private long totalTimeInMillis;
    private boolean isSubmitted = false;
    private long backPressedTime = 0;
    private Toast exitToast;

    // Repository
    private UserActionRepository actionRepository;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        Log.d(TAG, "QuizActivity started");
        actionRepository = new UserActionRepository();

        // Initialize Views
        viewPager = findViewById(R.id.view_pager_questions);
        progressBar = findViewById(R.id.quiz_progress_bar);
        tvTimer = findViewById(R.id.tv_timer);
        btnNext = findViewById(R.id.btn_next_question);
        btnStartCoding = findViewById(R.id.btn_start_coding); // Ensure this ID exists in XML

        // --- 1. Retrieve Intent Data ---
        if (getIntent().hasExtra("QUIZ_ID")) {
            currentQuizId = getIntent().getStringExtra("QUIZ_ID");
        }
        if (getIntent().hasExtra("QUIZ_TITLE")) {
            currentQuizTitle = getIntent().getStringExtra("QUIZ_TITLE");
        } else {
            currentQuizTitle = "Generated Quiz"; // Fallback title
        }

        if (getIntent().hasExtra("QUIZ_DATA")) {
            questions = (List<QuizQuestion>) getIntent().getSerializableExtra("QUIZ_DATA");
            Log.d(TAG, "✅ Received " + (questions != null ? questions.size() : 0) + " questions from intent");

            if (questions == null || questions.isEmpty()) {
                Toast.makeText(this, "No questions found.", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // Assign IDs if missing
            for (int i = 0; i < questions.size(); i++) {
                questions.get(i).id = i + 1;
                if (questions.get(i).difficulty == null || questions.get(i).difficulty.isEmpty()) {
                    questions.get(i).difficulty = "NORMAL";
                }
            }

            setupQuiz();

        } else if (getIntent().hasExtra("QUIZ_ID")) {
            // Fallback: Load from Firestore if only ID is passed
            Log.d(TAG, "📥 Loading quiz from Firestore: " + currentQuizId);
            fetchQuizFromFirestore(currentQuizId);
        } else {
            Log.e(TAG, "❌ No QUIZ_DATA or QUIZ_ID provided!");
            Toast.makeText(this, "No quiz data provided", Toast.LENGTH_SHORT).show();
            finish();
        }

        // --- 2. Setup Back Press Handling ---
        setupBackPressHandler();
    }

    private void setupQuiz() {
        Log.d(TAG, "📋 Setting up quiz with " + questions.size() + " questions");

        // Setup ViewPager
        QuizPagerAdapter adapter = new QuizPagerAdapter(this, questions);
        viewPager.setAdapter(adapter);
        viewPager.setUserInputEnabled(false); // Disable swipe

        // Setup progress bar
        progressBar.setMax(questions.size());
        progressBar.setProgress(0);

        // Setup timer
        calculateTotalTime();
        startTimer();

        // Setup buttons
        btnNext.setEnabled(false);
        btnNext.setOnClickListener(v -> goToNextQuestion());
        btnStartCoding.setOnClickListener(v -> startCodingExercise());

        // ViewPager Listener
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                currentQuestionIndex = position;
                progressBar.setProgress(position + 1);

                // Update button state based on the NEW page
                updateButtonState(position);
            }
        });

        // --- CRITICAL FIX: Initialize buttons for the FIRST question ---
        updateButtonState(0);

        Log.d(TAG, "✅ Quiz setup complete");
    }

    /**
     * Updates visibility of "Next" vs "Skip/Start Coding" buttons
     */
    private void updateButtonState(int position) {
        QuizQuestion currentQuestion = questions.get(position);
        boolean isCodingQuestion = currentQuestion.type != null && currentQuestion.type.equals("CODING");

        if (isCodingQuestion) {
            // Show Coding Button
            btnStartCoding.setVisibility(View.VISIBLE);

            // Enable "Next" but rename it to "Skip"
            btnNext.setText("Skip For Now");
            btnNext.setEnabled(true);
            Log.d(TAG, "📝 Coding question at pos " + position + " - Skip enabled");
        } else {
            // Hide Coding Button
            btnStartCoding.setVisibility(View.GONE);

            // Only enable "Next" if answered
            btnNext.setEnabled(currentQuestion.isAnswered);

            if (position == questions.size() - 1) {
                btnNext.setText("Finish Quiz");
            } else {
                btnNext.setText("Next Question");
            }
        }
    }

    private void calculateTotalTime() {
        totalTimeInMillis = 0;
        for (QuizQuestion q : questions) {
            if (q.timeLimit <= 0) q.timeLimit = 1.0f;
            totalTimeInMillis += (long) (q.timeLimit * 60 * 1000);
        }
        if (totalTimeInMillis == 0) {
            totalTimeInMillis = 10 * 60 * 1000; // Default 10 min
        }
    }

    private void startTimer() {
        quizTimer = new CountDownTimer(totalTimeInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished);
                long seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) -
                        TimeUnit.MINUTES.toSeconds(minutes);
                tvTimer.setText(String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds));
            }

            @Override
            public void onFinish() {
                tvTimer.setText("00:00");
                Toast.makeText(QuizActivity.this, "Time's up!", Toast.LENGTH_SHORT).show();
                finishQuiz(); // Auto-submit
            }
        }.start();
    }

    // Called by QuizOptionsAdapter when an option is selected
    public void onOptionSelected(int selectedOptionIndex, boolean isCorrect) {
        Log.d(TAG, "Question " + currentQuestionIndex + " answered. Correct: " + isCorrect);

        QuizQuestion currentQuestion = questions.get(currentQuestionIndex);
        currentQuestion.isAnswered = true;
        currentQuestion.isCorrect = isCorrect;
        currentQuestion.selectedOptionIndex = selectedOptionIndex;

        if (isCorrect) {
            correctCount++;
        }

        // Enable next button since it's answered
        btnNext.setEnabled(true);
    }

    private void startCodingExercise() {
        QuizQuestion currentQuestion = questions.get(currentQuestionIndex);
        Log.d(TAG, "Starting coding exercise for question " + currentQuestionIndex);

        Intent intent = new Intent(this, CodingExerciseActivity.class);
        // Pass data to coding activity
        intent.putExtra("PROMPT", currentQuestion.codingPrompt);
        intent.putExtra("STARTER_CODE", currentQuestion.starterCode);
        intent.putExtra("EXPECTED_OUTPUT", currentQuestion.expectedOutput);

        startActivityForResult(intent, REQUEST_CODE_CODING_EXERCISE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_CODING_EXERCISE) {
            if (resultCode == RESULT_OK) {
                Log.d(TAG, "✅ Coding exercise completed successfully");

                QuizQuestion currentQuestion = questions.get(currentQuestionIndex);
                currentQuestion.isAnswered = true;
                currentQuestion.isCorrect = true;
                correctCount++;

                Toast.makeText(this, "Great job! Code passed.", Toast.LENGTH_SHORT).show();
                goToNextQuestion(); // Auto-advance
            } else {
                Log.d(TAG, "Coding exercise cancelled");
            }
        }
    }

    private void goToNextQuestion() {
        QuizQuestion currentQuestion = questions.get(currentQuestionIndex);
        boolean isCodingQuestion = currentQuestion.type != null && currentQuestion.type.equals("CODING");

        // If user clicked "Skip" on a coding question
        if (isCodingQuestion && !currentQuestion.isCorrect) {
            Log.d(TAG, "Skipping coding question " + currentQuestionIndex);
            currentQuestion.isAnswered = true;
            currentQuestion.isCorrect = false;
        }

        if (currentQuestionIndex < questions.size() - 1) {
            viewPager.setCurrentItem(currentQuestionIndex + 1, true);
        } else {
            finishQuiz();
        }
    }

    private void finishQuiz() {
        if (isSubmitted) return;
        isSubmitted = true;

        if (quizTimer != null) {
            quizTimer.cancel();
        }

        // --- SAVE TO HISTORY ---
        if (currentQuizId != null && actionRepository != null) {
            Log.d(TAG, "💾 Saving history for: " + currentQuizTitle);
            actionRepository.saveHistoryEntry(
                    currentQuizId,
                    correctCount,
                    questions.size(),
                    currentQuizTitle
            );
        } else {
            Log.e(TAG, "❌ Cannot save history. Quiz ID is null.");
        }

        showQuizCompleteDialog();
    }

    private void showQuizCompleteDialog() {
        int total = questions.size();
        String scoreMessage = "Your score: " + correctCount + "/" + total;

        QuizCompleteDialogFragment dialog = QuizCompleteDialogFragment.newInstance(scoreMessage);
        dialog.setCancelable(false);
        dialog.show(getSupportFragmentManager(), "QuizCompleteDialogFragment");
    }

    private void fetchQuizFromFirestore(String quizId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("quizzes").document(quizId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Quiz quiz = documentSnapshot.toObject(Quiz.class);
                        if (quiz != null) {
                            questions = quiz.getQuestions();
                            currentQuizTitle = quiz.getTitle(); // Get title from DB
                            if (questions != null && !questions.isEmpty()) {
                                setupQuiz();
                            } else {
                                Toast.makeText(this, "Error: Quiz is empty.", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        }
                    } else {
                        Toast.makeText(this, "Quiz not found.", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load quiz", e);
                    finish();
                });
    }

    // --- Interface Methods for Dialog ---
    @Override
    public void onViewResults() {
        ArrayList<QuizQuestion> incorrectQuestions = new ArrayList<>();
        for (QuizQuestion q : questions) {
            if (q.isAnswered && !q.isCorrect) {
                incorrectQuestions.add(q);
            }
        }

        Intent intent = new Intent(this, RecommendationsActivity.class);
        intent.putExtra("TOTAL_QUESTIONS", questions.size());
        intent.putExtra("CORRECT_COUNT", correctCount);
        intent.putExtra("INCORRECT_QUESTIONS", incorrectQuestions);
        startActivity(intent);
        finish();
    }

    @Override
    public void onRegenerateQuiz() {
        Toast.makeText(this, "Returning to Home...", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onRetakeQuiz() {
        Log.d(TAG, "Retaking quiz...");

        // Reset State
        correctCount = 0;
        isSubmitted = false;

        for (QuizQuestion q : questions) {
            q.isAnswered = false;
            q.isCorrect = false;
            q.selectedOptionIndex = -1;
        }

        viewPager.setCurrentItem(0, false);
        currentQuestionIndex = 0;
        progressBar.setProgress(0);

        // Re-initialize first button state
        updateButtonState(0);

        if (quizTimer != null) quizTimer.cancel();
        calculateTotalTime();
        startTimer();

        if (viewPager.getAdapter() != null) {
            viewPager.getAdapter().notifyDataSetChanged();
        }
    }

    // --- Focus Control (Double Back Press) ---
    private void setupBackPressHandler() {
        OnBackPressedDispatcher dispatcher = getOnBackPressedDispatcher();
        dispatcher.addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isSubmitted) {
                    finish();
                    return;
                }
                if (backPressedTime + BACK_PRESS_DELAY > System.currentTimeMillis()) {
                    if (exitToast != null) exitToast.cancel();
                    if (quizTimer != null) quizTimer.cancel();
                    finish();
                } else {
                    backPressedTime = System.currentTimeMillis();
                    exitToast = Toast.makeText(QuizActivity.this, "Press BACK again to quit quiz.", Toast.LENGTH_SHORT);
                    exitToast.show();
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (quizTimer != null) quizTimer.cancel();
    }

    // --- ViewPager Adapter Class ---
    private static class QuizPagerAdapter extends FragmentStateAdapter {
        private final List<QuizQuestion> questions;

        public QuizPagerAdapter(FragmentActivity activity, List<QuizQuestion> questions) {
            super(activity);
            this.questions = questions;
        }

        @Override
        public Fragment createFragment(int position) {
            QuestionPageFragment fragment = new QuestionPageFragment();
            Bundle args = new Bundle();
            args.putSerializable("QUESTION", questions.get(position));
            args.putInt("TOTAL_COUNT", questions.size());
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public int getItemCount() {
            return questions.size();
        }
    }
}