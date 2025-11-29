package com.example.learnify;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends BaseActivity {

    private EditText emailInput;
    private MaterialButton sendEmailButton;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Find Views
        emailInput = findViewById(R.id.email_input);
        sendEmailButton = findViewById(R.id.send_email_button);

        sendEmailButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();

            if (isValidEmail(email)) {
                sendPasswordResetEmail(email);
            }
        });
    }

    private void sendPasswordResetEmail(String email) {
        // Disable button to prevent double clicks
        sendEmailButton.setEnabled(false);
        sendEmailButton.setText("Sending...");

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    sendEmailButton.setEnabled(true);
                    sendEmailButton.setText("Send Reset Link");

                    if (task.isSuccessful()) {
                        // Success!
                        Toast.makeText(ForgotPasswordActivity.this,
                                "Reset link sent! Check your email.", Toast.LENGTH_LONG).show();

                        // Close this screen and return to Login
                        finish();
                    } else {
                        // Error (e.g., user not found)
                        String error = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        Toast.makeText(ForgotPasswordActivity.this, "Failed: " + error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private boolean isValidEmail(String email) {
        if (email.isEmpty()) {
            emailInput.setError("Email is required");
            emailInput.requestFocus();
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Please enter a valid email");
            emailInput.requestFocus();
            return false;
        }
        return true;
    }
}