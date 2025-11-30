package com.example.learnify;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.api.services.drive.DriveScopes;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class signin_activity extends BaseActivity {

    private static final String TAG = "SignInActivity";

    // Views
    EditText emailInput, passwordInput;
    ImageView passwordVisibilityToggle;
    MaterialButton logInButton;
    TextView forgotPasswordLink, signUpPromptButton;

    // Firebase and Google
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private MaterialButton googleSignInButton;

    // ActivityResultLauncher for Google Sign-In
    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                        try {
                            GoogleSignInAccount account = task.getResult(ApiException.class);
                            Log.d(TAG, "firebaseAuthWithGoogle:" + account.getId());
                            firebaseAuthWithGoogle(account.getIdToken());
                        } catch (ApiException e) {
                            Log.w(TAG, "Google sign in failed", e);
                            Toast.makeText(signin_activity.this, "Google Sign-In Failed", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(signin_activity.this, "Sign-In cancelled", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_page); // Make sure this layout name is correct

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Configure Google Sign-In with Drive appData scope for profile photo upload
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .requestScopes(new Scope(DriveScopes.DRIVE_APPDATA)) // App-specific hidden folder
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // Initialize all views
        init();
    }

    void init() {
        // Find views
        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        passwordVisibilityToggle = findViewById(R.id.password_visibility_toggle);
        logInButton = findViewById(R.id.log_in_button);
        forgotPasswordLink = findViewById(R.id.forgot_password_link);
        signUpPromptButton = findViewById(R.id.sign_up_prompt_button);
        googleSignInButton = findViewById(R.id.google_sign_in_button);

        // --- Listeners ---

        // Google Sign-In Button
        googleSignInButton.setOnClickListener(v -> {
            Log.d(TAG, "Google Sign-In button clicked");
            googleSignIn();
        });

        // Password Visibility Toggle
        passwordVisibilityToggle.setOnClickListener(v -> togglePasswordVisibility());

        // --- Log In Button Logic (MODIFIED) ---
        logInButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (!validateInput(email, password)) {
                return; // Validation failed
            }

            // --- This is the NEW Firebase Email/Password Sign-In logic ---
            // Show a progress bar here
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        // Hide progress bar here
                        if (task.isSuccessful()) {
                            // Sign in success
                            Log.d(TAG, "signInWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);
                        } else {
                            // If sign in fails
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            Toast.makeText(signin_activity.this, "Authentication failed. Check credentials.",
                                    Toast.LENGTH_SHORT).show();
                            updateUI(null);
                        }
                    });
        });

        // Forgot Password Logic
        forgotPasswordLink.setOnClickListener(v -> {
            Intent i = new Intent(signin_activity.this, ForgotPasswordActivity.class);
            startActivity(i);
        });

        // "Sign up" Text Logic
        signUpPromptButton.setOnClickListener(v -> {
            Intent i = new Intent(signin_activity.this, signup_activity.class);
            startActivity(i);
        });
    }

    private boolean validateInput(String email, String password) {
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
        if (password.isEmpty()) {
            passwordInput.setError("Password is required");
            passwordInput.requestFocus();
            return false;
        }
        return true;
    }

    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            passwordInput.setTransformationMethod(PasswordTransformationMethod.getInstance());
            passwordVisibilityToggle.setImageResource(R.drawable.ic_eye_invisible);
            isPasswordVisible = false;
        } else {
            passwordInput.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            passwordVisibilityToggle.setImageResource(R.drawable.ic_eye_visible);
            isPasswordVisible = true;
        }
        passwordInput.setSelection(passwordInput.length());
    }

    // --- Google Sign-In Methods ---

    private void googleSignIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    private void firebaseAuthWithGoogle(String idToken) {
        // Show a progress bar here
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    // Hide progress bar here
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithCredential:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        updateUI(user);
                    } else {
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                        Toast.makeText(signin_activity.this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                        updateUI(null);
                    }
                });
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            // User is signed in, navigate to MainActivity
            Toast.makeText(this, "Signed in as: " + (user.getDisplayName() != null ? user.getDisplayName() : user.getEmail()), Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(signin_activity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }
}