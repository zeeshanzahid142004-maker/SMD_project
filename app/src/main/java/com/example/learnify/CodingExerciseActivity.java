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

public class CodingExerciseActivity extends BaseActivity {

    private static final String TAG = "CodingExerciseActivity";

    private MaterialToolbar toolbar;
    private TextView tvExercisePrompt;
    private EditText etCodeInput;
    private MaterialButton btnSubmit;
    private MaterialButton btnSkip;
    
    // New UI components for code execution
    private Spinner spinnerLanguage;
    private TextView tvOutput;
    private ProgressBar progressBar;
    private CodeExecutionService codeService;

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
        
        // Initialize new UI components
        spinnerLanguage = findViewById(R.id.spinner_language);
        tvOutput = findViewById(R.id.tv_output);
        progressBar = findViewById(R.id.progress_execution);
        codeService = new CodeExecutionService();
        
        // Setup language spinner
        String[] languages = {"Python", "Java", "C++", "JavaScript"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, languages);
        spinnerLanguage.setAdapter(adapter);

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

    private void submitCode() {
        String code = etCodeInput.getText().toString().trim();

        if (code.isEmpty()) {
            Toast.makeText(this, "Please write some code first!", Toast.LENGTH_SHORT).show();
            return;
        }

        String language = spinnerLanguage.getSelectedItem().toString();
        Log.d(TAG, "📝 User submitted code in " + language + ": " + code.substring(0, Math.min(50, code.length())));

        // Show loading state
        progressBar.setVisibility(View.VISIBLE);
        btnSubmit.setEnabled(false);
        tvOutput.setText("Running code...");

        // Try API execution first
        codeService.executeCode(code, language, new CodeExecutionService.ExecutionCallback() {
            @Override
            public void onSuccess(String output) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnSubmit.setEnabled(true);
                    tvOutput.setText("Output:\n" + output);

                    // Check if output matches expected (if we have expected output)
                    if (question.correctAnswer != null && output.trim().contains(question.correctAnswer.trim())) {
                        showSuccessDialog(code);
                    } else if (question.expectedOutput != null && output.trim().contains(question.expectedOutput.trim())) {
                        showSuccessDialog(code);
                    } else {
                        // If no expected output match, use offline validation
                        if (validateCodeOffline(code, language)) {
                            showSuccessDialog(code);
                        }
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnSubmit.setEnabled(true);
                    
                    // If API not configured or failed, use offline validation
                    if (error.contains("API not configured") || error.contains("Network error")) {
                        tvOutput.setText("Offline mode: Validating code structure...");
                        if (validateCodeOffline(code, language)) {
                            tvOutput.setText("✅ Code structure looks good!");
                            showSuccessDialog(code);
                        } else {
                            tvOutput.setText("❌ " + error + "\nTry adding more code structure.");
                            showHintDialog();
                        }
                    } else {
                        tvOutput.setText("Error:\n" + error);
                    }
                });
            }
        });
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

    /**
     * Offline validation when JDoodle API is not available.
     * Validates code structure based on question type and language.
     */
    private boolean validateCodeOffline(String code, String language) {
        String lowerCode = code.toLowerCase();
        String questionLower = question.questionText != null ? question.questionText.toLowerCase() : "";

        // Check for loops
        if (questionLower.contains("loop") || questionLower.contains("iterate") || questionLower.contains("repeat")) {
            return lowerCode.contains("for") || lowerCode.contains("while");
        }

        // Check for conditionals
        if (questionLower.contains("if") || questionLower.contains("condition") || questionLower.contains("check")) {
            return lowerCode.contains("if");
        }

        // Check for functions
        if (questionLower.contains("function") || questionLower.contains("method") || questionLower.contains("define")) {
            return lowerCode.contains("def ") || lowerCode.contains("function ") ||
                    lowerCode.contains("void ") || lowerCode.contains("public ") ||
                    lowerCode.contains("private ") || lowerCode.contains("=>") ||
                    lowerCode.contains("int ") || lowerCode.contains("string ");
        }

        // Check for print statements
        if (questionLower.contains("print") || questionLower.contains("output") || questionLower.contains("display")) {
            return lowerCode.contains("print") || lowerCode.contains("console.log") ||
                    lowerCode.contains("system.out") || lowerCode.contains("cout");
        }

        // Check for array/list operations
        if (questionLower.contains("array") || questionLower.contains("list")) {
            return lowerCode.contains("[") || lowerCode.contains("array") || 
                    lowerCode.contains("list") || lowerCode.contains("vector");
        }

        // Language-specific basic structure validation
        switch (language.toLowerCase()) {
            case "python":
                return lowerCode.contains("def ") || lowerCode.contains("print") || 
                        lowerCode.contains("for") || lowerCode.contains("while") ||
                        lowerCode.contains("if") || lowerCode.contains("class");
            case "java":
                return lowerCode.contains("public") || lowerCode.contains("class") ||
                        lowerCode.contains("void") || lowerCode.contains("int") ||
                        lowerCode.contains("system.out");
            case "c++":
                return lowerCode.contains("cout") || lowerCode.contains("cin") ||
                        lowerCode.contains("#include") || lowerCode.contains("int main");
            case "javascript":
                return lowerCode.contains("function") || lowerCode.contains("const") ||
                        lowerCode.contains("let") || lowerCode.contains("var") ||
                        lowerCode.contains("console.log") || lowerCode.contains("=>");
        }

        // Default: check minimum length and basic structure
        return code.length() >= 20 && (
                lowerCode.contains("for") ||
                lowerCode.contains("while") ||
                lowerCode.contains("if") ||
                lowerCode.contains("function") ||
                lowerCode.contains("def") ||
                lowerCode.contains("return"));
    }
}