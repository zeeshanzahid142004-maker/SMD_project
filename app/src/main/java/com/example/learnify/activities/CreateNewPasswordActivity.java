package com.example.learnify.activities; // Make sure this package name matches yours

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;

import com.example.learnify.R;
import com.example.learnify.helpers.CustomToast;
import com.google.android.material.button.MaterialButton;
// Import the new components
import com.google.android.material.textfield.TextInputEditText;

public class CreateNewPasswordActivity extends BaseActivity {

    // Use TextInputEditText for the refactored layout
    TextInputEditText oldPasswordInput, newPasswordInput, confirmPasswordInput;
    MaterialButton resetPasswordButton;
    SharedPreferences spref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_password);
        init();
    }

    private void init() {
        // Find the new component IDs
        oldPasswordInput = findViewById(R.id.old_password_input);
        newPasswordInput = findViewById(R.id.new_password_input);
        confirmPasswordInput = findViewById(R.id.confirm_password_input);
        resetPasswordButton = findViewById(R.id.reset_password_button);

        spref = getSharedPreferences("MyPref", MODE_PRIVATE);

        // --- Set Click Listeners ---
        // We no longer need listeners for the toggles, TextInputLayout does it for us!

        resetPasswordButton.setOnClickListener(v -> {
            validateAndResetPassword();
        });
    }

    private void validateAndResetPassword() {
        String oldPass = oldPasswordInput.getText().toString();
        String newPass = newPasswordInput.getText().toString();
        String confirmPass = confirmPasswordInput.getText().toString();

        String savedPassword = spref.getString("password", null);

        // Validation Checks
        if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {

            CustomToast.error(this,"Please fill all fields");
            return;
        }

        if (savedPassword == null || !oldPass.equals(savedPassword)) {

            CustomToast.error(this,"Incorrect old password");
            return;
        }

        if (newPass.equals(oldPass)) {

           CustomToast.error(this,"New password must be different from the old one");
            return;
        }

        if (!newPass.equals(confirmPass)) {
            CustomToast.error(this,"New passwords do not match");

            return;
        }

        // --- All checks passed! ---
        // Save the new password
        spref.edit().putString("password", newPass).apply();


CustomToast.success(this, "Password Reset Successful!");
        //);
        // Send user back to the Sign In page
        Intent i = new Intent(CreateNewPasswordActivity.this, signin_activity.class);
        // Clear all previous activities from the stack and start fresh at Sign In
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        finish();
    }
}