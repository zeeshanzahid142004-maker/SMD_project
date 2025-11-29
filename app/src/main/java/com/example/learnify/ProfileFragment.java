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
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.button.MaterialButton;
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
    private TextView rowLanguage, rowNotifications, rowAppearance, rowHelp, rowDeleteAccount;
    private View helpExpandableView;
    private boolean isHelpExpanded = false;

    private LanguageManager languageManager;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private Uri selectedImageUri;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        driveManager = new GoogleDriveManager(requireContext());
        languageManager = LanguageManager.getInstance(requireContext());

        // 1. Load Saved Language
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String savedLangCode = prefs.getString(KEY_LANG_CODE, "en");
        String savedLangName = prefs.getString(KEY_LANG_NAME, "English");
        languageManager.setLanguage(requireContext(), savedLangName, savedLangCode);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .requestScopes(new com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/drive.file"))
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
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Init Views
        ivProfileAvatar = view.findViewById(R.id.iv_profile_avatar);
        tvProfileName = view.findViewById(R.id.tv_profile_name);
        tvProfileEmail = view.findViewById(R.id.tv_profile_email);
        btnEditProfile = view.findViewById(R.id.btn_edit_profile);
        btnSignOut = view.findViewById(R.id.btn_sign_out);

        rowLanguage = view.findViewById(R.id.row_language);
        rowNotifications = view.findViewById(R.id.row_notifications);
        rowAppearance = view.findViewById(R.id.row_appearance);
        rowHelp = view.findViewById(R.id.row_help);
        rowDeleteAccount = view.findViewById(R.id.row_delete_account);
        helpExpandableView = view.findViewById(R.id.help_expandable_view);

        // Set Listeners
        btnSignOut.setOnClickListener(v -> signOut());
        btnEditProfile.setOnClickListener(v -> selectProfilePhoto());
        ivProfileAvatar.setOnClickListener(v -> selectProfilePhoto());

        rowLanguage.setOnClickListener(v -> showLanguageDialog());
        rowAppearance.setOnClickListener(v -> showThemeDialog());
        rowNotifications.setOnClickListener(v -> showToast("Notifications clicked"));

        // Help Toggle Logic
        rowHelp.setOnClickListener(v -> toggleHelpSection());

        // Delete Account Logic
        rowDeleteAccount.setOnClickListener(v -> showDeleteConfirmation());

        loadUserProfile();
    }

    private void toggleHelpSection() {
        if (isHelpExpanded) {
            helpExpandableView.animate()
                    .alpha(0.0f)
                    .translationY(-20)
                    .setDuration(200)
                    .withEndAction(() -> helpExpandableView.setVisibility(View.GONE))
                    .start();
        } else {
            helpExpandableView.setVisibility(View.VISIBLE);
            helpExpandableView.setAlpha(0.0f);
            helpExpandableView.setTranslationY(-20);
            helpExpandableView.animate().alpha(1.0f).translationY(0).setDuration(200).start();
        }
        isHelpExpanded = !isHelpExpanded;
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
        prefs.edit().putInt(KEY_THEME, mode).apply();
    }

    private void showLanguageDialog() {
        List<LanguageManager.Language> languages = LanguageManager.getSupportedLanguages();
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String currentCode = prefs.getString(KEY_LANG_CODE, "en");

        DialogHelper.showLanguageSelectionDialog(requireContext(), languages, currentCode, (selectedLang) -> {
            updateLanguage(selectedLang.name, selectedLang.code);
        });
    }

    private void updateLanguage(String languageName, String languageCode) {
        // 1. Save Local
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_LANG_CODE, languageCode)
                .putString(KEY_LANG_NAME, languageName)
                .apply();

        // 2. Apply
        languageManager.setLanguage(requireContext(), languageName, languageCode);

        // 3. Sync Cloud
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("language", languageName);
            updates.put("languageCode", languageCode);
            db.collection("users").document(user.getUid()).update(updates);
        }

        requireActivity().recreate();
    }

    private void showDeleteConfirmation() {
        DialogHelper.showConfirmation(requireContext(),
                "Delete Account?",
                "This action is permanent.",
                "Delete", this::deleteUserAccount,
                "Cancel", null);
    }

    private void deleteUserAccount() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            db.collection("users").document(user.getUid()).delete();
            user.delete().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Intent intent = new Intent(getActivity(), signin_activity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }
            });
        }
    }

    private void loadUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String name = user.getDisplayName();
        String email = user.getEmail();

        tvProfileName.setText(name != null && !name.isEmpty() ? name : "Welcome");
        tvProfileEmail.setText(email);

        db.collection("users").document(user.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String drivePhotoUrl = documentSnapshot.getString("drivePhotoUrl");
                        if (drivePhotoUrl != null) loadProfilePhoto(drivePhotoUrl);
                        else loadDefaultPhoto();
                    } else {
                        loadDefaultPhoto();
                    }
                })
                .addOnFailureListener(e -> loadDefaultPhoto());
    }

    private void loadProfilePhoto(String driveUrl) {
        if (getContext() == null) return;
        Glide.with(this).load(driveUrl).circleCrop().placeholder(R.drawable.ic_profile_inactive).into(ivProfileAvatar);
    }

    private void loadDefaultPhoto() {
        if (getContext() == null) return;
        Glide.with(this).load(R.drawable.ic_profile_inactive).circleCrop().into(ivProfileAvatar);
    }

    private void selectProfilePhoto() {
        if (!driveManager.isAvailable()) {
            Toast.makeText(getContext(), "Sign in with Google to upload photos", Toast.LENGTH_LONG).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void uploadProfilePhotoToDrive(Uri imageUri) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;
        Toast.makeText(getContext(), "Uploading...", Toast.LENGTH_SHORT).show();

        driveManager.uploadProfilePhoto(imageUri, user.getUid(), new GoogleDriveManager.UploadCallback() {
            @Override
            public void onSuccess(String driveUrl, String fileId) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("drivePhotoUrl", driveUrl);
                    updates.put("driveFileId", fileId);
                    db.collection("users").document(user.getUid()).update(updates)
                            .addOnSuccessListener(a -> loadProfilePhoto(driveUrl));
                });
            }
            @Override
            public void onError(Exception e) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Upload failed", Toast.LENGTH_SHORT).show());
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