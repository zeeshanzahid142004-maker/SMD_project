package com.example.learnify;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.material.button.MaterialButton;
import com.google.api.services.drive.DriveScopes;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";
    private static final String PREFS_NAME = "LearnifyPrefs";
    private static final String KEY_THEME = "theme_mode";
    private static final String KEY_LANG_CODE = "language_code";
    private static final String KEY_LANG_NAME = "language_name";

    // Firebase & Google
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;
    private GoogleDriveManager driveManager;

    // UI Views
    private ImageView ivProfileAvatar;
    private TextView tvProfileName, tvProfileEmail;
    private MaterialButton btnSignOut;
    private ImageView btnEditProfile;
    private TextView rowLanguage, rowAppearance, rowHelp;
    private LinearLayout helpExpandableView;

    private LanguageManager languageManager;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<Intent> driveReAuthLauncher;
    private Uri selectedImageUri;
    private Uri pendingImageUri; // Store URI when re-auth is needed

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        driveManager = new GoogleDriveManager(requireContext());
        languageManager = LanguageManager.getInstance(requireContext());

        // --- 1. Load Language from Preferences immediately ---
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String savedLangCode = prefs.getString(KEY_LANG_CODE, "en"); // Default English
        String savedLangName = prefs.getString(KEY_LANG_NAME, "English");
        languageManager.setLanguage(requireContext(), savedLangName, savedLangCode);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .requestScopes(new Scope(DriveScopes.DRIVE_FILE))
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(requireActivity(), gso);

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        uploadProfilePhotoToDrive(selectedImageUri);
                    }
                }
        );

        // Re-authentication launcher for Drive scope
        driveReAuthLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        try {
                            GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(result.getData())
                                    .getResult(ApiException.class);
                            Log.d(TAG, "Re-authentication successful with Drive scope");
                            // Reinitialize Drive manager with new credentials
                            driveManager = new GoogleDriveManager(requireContext());
                            // Now try to upload the pending image
                            if (pendingImageUri != null) {
                                uploadProfilePhotoToDrive(pendingImageUri);
                                pendingImageUri = null;
                            }
                        } catch (ApiException e) {
                            Log.e(TAG, "Re-authentication failed", e);
                            Toast.makeText(getContext(), "Failed to get Drive access", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "Drive access cancelled", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Find views
        ivProfileAvatar = view.findViewById(R.id.iv_profile_avatar);
        tvProfileName = view.findViewById(R.id.tv_profile_name);
        tvProfileEmail = view.findViewById(R.id.tv_profile_email);
        btnEditProfile = view.findViewById(R.id.btn_edit_profile);
        btnSignOut = view.findViewById(R.id.btn_sign_out);

        rowLanguage = view.findViewById(R.id.row_language);
        rowAppearance = view.findViewById(R.id.row_appearance);
        rowHelp = view.findViewById(R.id.row_help);
        helpExpandableView = view.findViewById(R.id.help_expandable_view);

        // Listeners
        btnSignOut.setOnClickListener(v -> signOut());
        btnEditProfile.setOnClickListener(v -> selectProfilePhoto());
        ivProfileAvatar.setOnClickListener(v -> selectProfilePhoto());
        rowLanguage.setOnClickListener(v -> showLanguageDialog());

        rowAppearance.setOnClickListener(v -> showThemeDialog());

        // Help Center - toggle expandable view
        rowHelp.setOnClickListener(v -> {
            if (helpExpandableView.getVisibility() == View.GONE) {
                helpExpandableView.setVisibility(View.VISIBLE);
            } else {
                helpExpandableView.setVisibility(View.GONE);
            }
        });

        loadUserProfile();
    }

    private void showThemeDialog() {
        int currentMode = AppCompatDelegate.getDefaultNightMode();

        DialogHelper.showThemeSelectionDialog(requireContext(), currentMode, (selectedMode) -> {
            AppCompatDelegate.setDefaultNightMode(selectedMode);
            saveThemePreference(selectedMode);
        });
    }

    private void saveThemePreference(int mode) {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_THEME, mode);
        editor.apply();
    }

    /**
     * Show styled language selection dialog
     */
    private void showLanguageDialog() {
        List<LanguageManager.Language> languages = LanguageManager.getSupportedLanguages();

        // Get current language code from Preferences (source of truth)
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String currentCode = prefs.getString(KEY_LANG_CODE, "en");

        DialogHelper.showLanguageSelectionDialog(requireContext(), languages, currentCode, (selectedLang) -> {
            updateLanguage(selectedLang.name, selectedLang.code);
        });
    }

    /**
     * Update app language - Saves to BOTH Prefs and Firestore
     */
    private void updateLanguage(String languageName, String languageCode) {
        // 1. Save to SharedPreferences (Local Persistence)
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(KEY_LANG_CODE, languageCode);
        editor.putString(KEY_LANG_NAME, languageName);
        editor.apply();

        // 2. Apply immediately
        languageManager.setLanguage(requireContext(), languageName, languageCode);

        // 3. Save to Firestore (Cloud Sync)
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            Map<String, Object> updates = new HashMap<>();
            updates.put("language", languageName);
            updates.put("languageCode", languageCode);

            db.collection("users").document(userId).update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "✅ Language updated", Toast.LENGTH_SHORT).show();
                    });
        }

        // 4. Restart Activity to apply changes
        requireActivity().recreate();
    }

    private void loadUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String name = user.getDisplayName();
        String email = user.getEmail();

        if (name != null && !name.isEmpty()) {
            tvProfileName.setText(name);
        } else {
            tvProfileName.setText("Welcome");
        }
        tvProfileEmail.setText(email);

        String userId = user.getUid();
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String drivePhotoUrl = documentSnapshot.getString("drivePhotoUrl");
                        if (drivePhotoUrl != null && !drivePhotoUrl.isEmpty()) {
                            loadProfilePhoto(drivePhotoUrl);
                        } else {
                            loadDefaultPhoto();
                        }

                        // Note: We rely on local prefs for language primarily for speed,
                        // but you could sync here if needed.
                    } else {
                        loadDefaultPhoto();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Failed to load profile", e);
                    loadDefaultPhoto();
                });
    }

    // ... (loadProfilePhoto, loadDefaultPhoto, selectProfilePhoto, uploadProfilePhotoToDrive, signOut, showToast remain unchanged) ...
    // Keep them exactly as they were in your previous code.

    private void loadProfilePhoto(String driveUrl) {
        if (getContext() == null) return;
        Glide.with(this).load(driveUrl).circleCrop().placeholder(R.drawable.ic_profile_inactive).error(R.drawable.ic_profile_inactive).into(ivProfileAvatar);
    }

    private void loadDefaultPhoto() {
        if (getContext() == null) return;
        Glide.with(this).load(R.drawable.ic_profile_inactive).circleCrop().into(ivProfileAvatar);
    }

    private void selectProfilePhoto() {
        if (!driveManager.isAvailable()) {
            // Check if user needs to re-authenticate with Drive scope
            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(requireContext());
            if (account != null) {
                // User is signed in but doesn't have Drive scope, trigger re-auth
                Toast.makeText(getContext(), "📂 Requesting Drive access...", Toast.LENGTH_SHORT).show();
                triggerDriveReAuthentication();
            } else {
                Toast.makeText(getContext(), "⚠️ Please sign in with Google to upload photos", Toast.LENGTH_LONG).show();
            }
            return;
        }
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void triggerDriveReAuthentication() {
        // First, sign out to clear cached credentials
        mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
            // Now sign in again with Drive scope
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            driveReAuthLauncher.launch(signInIntent);
        });
    }

    private void uploadProfilePhotoToDrive(Uri imageUri) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        
        // Check if Drive manager is available
        if (!driveManager.isAvailable()) {
            // Store the image URI and trigger re-auth
            pendingImageUri = imageUri;
            triggerDriveReAuthentication();
            return;
        }
        
        String userId = user.getUid();
        Toast.makeText(getContext(), "📤 Uploading to Drive...", Toast.LENGTH_SHORT).show();
        driveManager.uploadProfilePhoto(imageUri, userId, new GoogleDriveManager.UploadCallback() {
            @Override
            public void onSuccess(String driveUrl, String fileId) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("drivePhotoUrl", driveUrl);
                    updates.put("driveFileId", fileId);
                    db.collection("users").document(userId).update(updates).addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "✅ Photo updated!", Toast.LENGTH_SHORT).show();
                        loadProfilePhoto(driveUrl);
                    });
                });
            }
            @Override
            public void onError(Exception e) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "❌ Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void signOut() {
        mAuth.signOut();
        mGoogleSignInClient.signOut().addOnCompleteListener(requireActivity(), task -> {
            Intent intent = new Intent(getActivity(), signin_activity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            if (getActivity() != null) getActivity().finish();
        });
    }

    private void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }
}