package com.example.learnify;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class CodingExerciseActivity extends BaseActivity {

    private static final String TAG = "CodingExerciseActivity";

    private MaterialToolbar toolbar;
    private TextView tvExercisePrompt;
    private EditText etCodeInput;
    private MaterialButton btnSubmit;
    private MaterialButton btnSkip;
    private Spinner spinnerLanguage;

    // Output views
    private MaterialCardView cardOutput;
    private View viewStatusDot;
    private TextView tvOutputLabel;
    private TextView tvOutput;
    private ProgressBar progressExecution;

    private QuizQuestion question;
    private CodeExecutionService codeExecutionService;

    private static final String[] LANGUAGES = {
            "Python", "Java", "C", "C++", "JavaScript", "Kotlin", "Go", "Ruby", "PHP"
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_coding_exercise);

        Log.d(TAG, "🎯 CodingExerciseActivity started");

        if (getIntent().hasExtra("QUESTION")) {
            question = (QuizQuestion) getIntent().getSerializableExtra("QUESTION");
            Log.d(TAG, "✅ Received coding question: " + question.questionText);
        } else {
            Log.e(TAG, "❌ No question provided!");
            CustomToast.warning(this, "No exercise provided");
            finish();
            return;
        }

        // Initialize code execution service
        codeExecutionService = new CodeExecutionService();

        // Initialize views
        toolbar = findViewById(R.id.toolbar);
        tvExercisePrompt = findViewById(R.id.tv_exercise_prompt);
        etCodeInput = findViewById(R.id.et_code_input);
        btnSubmit = findViewById(R.id.btn_submit_exercise);
        btnSkip = findViewById(R.id.btn_skip_coding);
        spinnerLanguage = findViewById(R.id.spinner_language);

        // Output views
        cardOutput = findViewById(R.id.card_output);
        viewStatusDot = findViewById(R.id.view_status_dot);
        tvOutputLabel = findViewById(R.id.tv_output_label);
        tvOutput = findViewById(R.id.tv_output);
        progressExecution = findViewById(R.id.progress_execution);

        // Setup language spinner
        setupLanguageSpinner();

        // Setup toolbar with back button to skip
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        toolbar.setNavigationOnClickListener(v -> {
            Log.d(TAG, "⏭️ Back button tapped - skipping exercise");
            setResult(RESULT_CANCELED);
            finish();
        });

        // Setup prompt
        String promptText = "💻 Coding Exercise\n\n" + question.questionText;
        if (question.correctAnswer != null && !question.correctAnswer.isEmpty()) {
            promptText += "\n\n💡 Hint: Look for keywords like loops, conditions, and functions";
        }
        tvExercisePrompt.setText(promptText);

        // ⭐ SUBMIT BUTTON
        btnSubmit.setOnClickListener(v -> {
            Log.d(TAG, "📤 Submit button clicked");
            submitCode();
        });

        // ⭐ SKIP BUTTON (explicit)
        if (btnSkip != null) {
            btnSkip.setOnClickListener(v -> {
                Log.d(TAG, "⏭️ Skip button clicked");
                setResult(RESULT_CANCELED);
                finish();
            });
        }
    }

    private void setupLanguageSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.item_spinner_language,
                R.id.tv_language_name,
                LANGUAGES
        );
        adapter.setDropDownViewResource(R.layout.item_spinner_language);
        spinnerLanguage.setAdapter(adapter);
    }

    private void submitCode() {
        String code = etCodeInput.getText().toString().trim();

        if (code.isEmpty()) {
            Toast.makeText(this, "Please write some code first!", Toast.LENGTH_SHORT).show();
            return;
        }

        String selectedLanguage = (String) spinnerLanguage.getSelectedItem();
        Log.d(TAG, "📝 User submitted code in " + selectedLanguage + ": " + code.substring(0, Math.min(50, code.length())));

        // Show loading state
        showLoading(true);

        // Execute code via JDoodle API
        codeExecutionService.executeCode(code, selectedLanguage, new CodeExecutionService.CodeExecutionCallback() {
            @Override
            public void onSuccess(String output) {
                showLoading(false);
                showOutput(output, true);

                // Also validate locally
                boolean hasRelevantCode = validateCode(code);
                if (hasRelevantCode) {
                    showSuccessDialog(code);
                }
            }

            @Override
            public void onError(String error) {
                showLoading(false);
                showOutput(error, false);
            }
        });
    }

    private void showLoading(boolean isLoading) {
        cardOutput.setVisibility(View.VISIBLE);
        progressExecution.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        tvOutput.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        viewStatusDot.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        btnSubmit.setEnabled(!isLoading);

        if (isLoading) {
            tvOutputLabel.setText("RUNNING...");
        }
    }

    private void showOutput(String output, boolean isSuccess) {
        cardOutput.setVisibility(View.VISIBLE);
        tvOutputLabel.setText("OUTPUT");

        // Output is already handled by CodeExecutionService (null -> "(No output)")
        tvOutput.setText(output);

        // Set status dot color
        if (isSuccess) {
            viewStatusDot.setBackgroundResource(R.drawable.bg_dot_green);
        } else {
            viewStatusDot.setBackgroundResource(R.drawable.bg_dot_red);
        }
    }

    private void showSuccessDialog(String code) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_success, null);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        TextView tvMessage = dialogView.findViewById(R.id.tv_dialog_message);
        MaterialButton btnDone = dialogView.findViewById(R.id.btn_dialog_positive);

        tvTitle.setText("Great Job! 🎉");
        tvMessage.setText("Your code looks good! You've completed this exercise.");
        btnDone.setText("Continue");
        btnDone.setOnClickListener(v -> {
            dialog.dismiss();
            Log.d(TAG, "✅ Code accepted - returning OK");
            setResult(RESULT_OK);
            finish();
        });

        dialog.show();
    }

    private void showHintDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_hint, null);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        TextView tvTitle = dialogView.findViewById(R.id.tv_dialog_title);
        TextView tvMessage = dialogView.findViewById(R.id.tv_dialog_message);
        MaterialButton btnTryAgain = dialogView.findViewById(R.id.btn_dialog_positive);
        MaterialButton btnShowSolution = dialogView.findViewById(R.id.btn_dialog_negative);

        tvTitle.setText("Need a hint? 💡");
        tvMessage.setText("Your code might be missing something. Would you like to see the expected solution?");

        btnTryAgain.setText("Try Again");
        btnTryAgain.setOnClickListener(v -> dialog.dismiss());

        btnShowSolution.setText("Show Solution");
        btnShowSolution.setOnClickListener(v -> {
            dialog.dismiss();
            etCodeInput.setText(question.correctAnswer);
            Toast.makeText(this, "Solution shown - try to understand it!", Toast.LENGTH_LONG).show();
        });

        dialog.show();
    }

    private boolean validateCode(String code) {
        String lowerCode = code.toLowerCase();

        return lowerCode.contains("for") ||
                lowerCode.contains("while") ||
                lowerCode.contains("if") ||
                lowerCode.contains("function") ||
                lowerCode.contains("def") ||
                lowerCode.contains("return") ||
                code.length() > 20;
    }
}