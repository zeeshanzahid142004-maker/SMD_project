package com.example.learnify;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

public class CodingExerciseActivity extends BaseActivity {

    private static final String TAG = "CodingExerciseActivity";

    private MaterialToolbar toolbar;
    private TextView tvExercisePrompt;
    private EditText etCodeInput;
    private MaterialButton btnSubmit;
    private MaterialButton btnSkip; // ⭐ ADD SKIP BUTTON

    // Validation error views
    private LinearLayout llValidationError;
    private TextView tvValidationError;

    private QuizQuestion question;

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

        // Initialize views
        toolbar = findViewById(R.id.toolbar);
        tvExercisePrompt = findViewById(R.id.tv_exercise_prompt);
        etCodeInput = findViewById(R.id.et_code_input);
        btnSubmit = findViewById(R.id.btn_submit_exercise);
        btnSkip = findViewById(R.id.btn_skip_coding);

        // Initialize validation error views
        llValidationError = findViewById(R.id.ll_validation_error);
        tvValidationError = findViewById(R.id.tv_validation_error);

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
            hideValidationError();
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

    /**
     * Shows validation error with orange text and yellow status dot
     */
    private void showValidationError(String errorMessage) {
        if (llValidationError != null && tvValidationError != null) {
            tvValidationError.setText(errorMessage);
            llValidationError.setVisibility(View.VISIBLE);
            Log.d(TAG, "⚠️ Validation error shown: " + errorMessage);
        }
    }

    /**
     * Hides the validation error display
     */
    private void hideValidationError() {
        if (llValidationError != null) {
            llValidationError.setVisibility(View.GONE);
        }
    }

    private void submitCode() {
        String code = etCodeInput.getText().toString().trim();

        Log.d(TAG, "📝 User submitted code: " + (code.isEmpty() ? "(empty)" : code.substring(0, Math.min(50, code.length()))));

        // Use CodeValidator for anti-cheating validation (includes empty check)
        String questionText = question != null ? question.questionText : "";
        CodeValidator.ValidationResult validationResult = CodeValidator.validateCode(code, questionText);

        if (!validationResult.isValid) {
            // Show validation error in orange with yellow status dot
            showValidationError(validationResult.errorMessage);
            Log.d(TAG, "❌ Code validation failed: " + validationResult.errorMessage);
            return;
        }

        // Additional basic structure validation
        boolean hasRelevantCode = validateCode(code);

        if (hasRelevantCode) {
            showSuccessDialog(code);
        } else {
            showHintDialog();
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