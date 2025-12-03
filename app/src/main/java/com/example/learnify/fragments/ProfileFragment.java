package com.example.learnify.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android. util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget. TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation. Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.example.learnify.helpers.CustomToast;
import com.example.learnify.helpers.DialogHelper;
import com.example.learnify.services.DriveProfileService;
import com.example.learnify.managers.FirebaseProfileManager;
import com.example.learnify.managers.GoogleDriveManager;
import com.example.learnify.managers.LanguageManager;
import com.example.learnify.R;
import com.example.learnify.activities.signin_activity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth. api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api. signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin. GoogleSignInOptions;
import com.google.android.gms. common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.material.button.MaterialButton;
import com.google.api.services.drive.DriveScopes;
import com. google.firebase.auth.FirebaseAuth;
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
    private DriveProfileService driveProfileService;
    private FirebaseProfileManager firebaseProfileManager;

    // UI Views
    private ImageView ivProfileAvatar;
    private TextView tvProfileName, tvProfileEmail;
    private MaterialButton btnSignOut;
    private ImageView btnEditProfile;
    private TextView rowLanguage, rowAppearance, rowDeleteAccount;
    private LinearLayout rowHelp;
    private LinearLayout helpExpandableView;
    private ImageView helpArrow;

    // Help section state
    private boolean isHelpExpanded = false;

    private LanguageManager languageManager;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<Intent> driveReAuthLauncher;
    private Uri selectedImageUri;
    private Uri pendingImageUri;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth. getInstance();
        db = FirebaseFirestore.getInstance();
        driveManager = new GoogleDriveManager(requireContext());
        driveProfileService = new DriveProfileService();
        firebaseProfileManager = new FirebaseProfileManager();
        languageManager = LanguageManager.getInstance(requireContext());

        // Initialize DriveProfileService with current account
        GoogleSignInAccount account = GoogleSignIn. getLastSignedInAccount(requireContext());
        if (account != null) {
            driveProfileService.initialize(account, requireContext());
        }

        // Load Language from Preferences
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String savedLangCode = prefs. getString(KEY_LANG_CODE, "en");
        String savedLangName = prefs.getString(KEY_LANG_NAME, "English");
        languageManager.setLanguage(requireContext(), savedLangName, savedLangCode);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                . requestEmail()
                . requestScopes(new Scope(DriveScopes.DRIVE_FILE))
                .build();
        mGoogleSignInClient = GoogleSignIn. getClient(requireActivity(), gso);

        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result. getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        selectedImageUri = result. getData().getData();
                        uploadProfilePhotoToDrive(selectedImageUri);
                    }
                }
        );

        // ✅ FIXED: Renamed 'account' to 'reAuthAccount' inside lambda
        driveReAuthLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result. getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        try {
                            GoogleSignInAccount reAuthAccount = GoogleSignIn. getSignedInAccountFromIntent(result. getData())
                                    .getResult(ApiException.class);
                            Log. d(TAG, "Re-authentication successful with Drive scope");
                            driveManager = new GoogleDriveManager(requireContext());
                            driveProfileService.initialize(reAuthAccount, requireContext());
                            if (pendingImageUri != null) {
                                uploadProfilePhotoToDrive(pendingImageUri);
                                pendingImageUri = null;
                            }
                        } catch (ApiException e) {
                            Log. e(TAG, "Re-authentication failed", e);
                            CustomToast.error(getContext(), "Failed to get Drive access");
                        }
                    } else {
                        CustomToast.info(getContext(), "Drive access cancelled");
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
        ivProfileAvatar = view.findViewById(R. id.iv_profile_avatar);
        tvProfileName = view. findViewById(R.id.tv_profile_name);
        tvProfileEmail = view.findViewById(R. id.tv_profile_email);
        btnEditProfile = view.findViewById(R. id.btn_edit_profile);
        btnSignOut = view.findViewById(R.id.btn_sign_out);

        rowLanguage = view.findViewById(R.id.row_language);
        rowAppearance = view.findViewById(R.id. row_appearance);
        rowHelp = view.findViewById(R.id.row_help);
        helpExpandableView = view.findViewById(R.id.help_expandable_view);
        helpArrow = view.findViewById(R.id. help_arrow);
        rowDeleteAccount = view.findViewById(R.id.row_delete_account);

        // Listeners
        btnSignOut.setOnClickListener(v -> signOut());
        btnEditProfile.setOnClickListener(v -> selectProfilePhoto());
        ivProfileAvatar.setOnClickListener(v -> selectProfilePhoto());
        rowLanguage.setOnClickListener(v -> showLanguageDialog());
        rowAppearance.setOnClickListener(v -> showThemeDialog());

        // Help Center - toggle expandable view with animation
        rowHelp.setOnClickListener(v -> toggleHelpSection());

        // Delete Account
        if (rowDeleteAccount != null) {
            rowDeleteAccount.setOnClickListener(v -> showDeleteConfirmation());
        }

        loadUserProfile();
    }

    /**
     * Toggle help section with arrow animation
     */
    private void toggleHelpSection() {
        if (isHelpExpanded) {
            // Collapse - rotate arrow back
            if (helpArrow != null) {
                helpArrow.animate().rotation(0f).setDuration(200).start();
            }
            helpExpandableView.animate()
                    .alpha(0.0f)
                    .translationY(-20)
                    .setDuration(200)
                    .withEndAction(() -> helpExpandableView.setVisibility(View.GONE))
                    . start();
        } else {
            // Expand - rotate arrow down
            if (helpArrow != null) {
                helpArrow.animate().rotation(180f). setDuration(200).start();
            }
            helpExpandableView.setVisibility(View. VISIBLE);
            helpExpandableView.setAlpha(0.0f);
            helpExpandableView.setTranslationY(-20);
            helpExpandableView.animate(). alpha(1.0f).translationY(0).setDuration(200).start();
        }
        isHelpExpanded = !isHelpExpanded;
    }

    /**
     * Show delete account confirmation dialog
     */
    private void showDeleteConfirmation() {
        DialogHelper.showConfirmation(
                requireContext(),
                "Delete Account? ",
                "This action is permanent and cannot be undone.  All your data will be lost.",
                "Delete",
                this::deleteUserAccount,
                "Cancel",
                null
        );
    }

    /**
     * Delete user account from Firebase
     */
    private void deleteUserAccount() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            CustomToast.warning(getContext(), "No user logged in");
            return;
        }

        String userId = user.getUid();

        // Show loading
        CustomToast.info(getContext(), "Deleting account...");

        // 1. Delete user data from Firestore
        db. collection("users").document(userId)
                .delete()
                . addOnSuccessListener(aVoid -> {
                    // 2. Delete Firebase Auth account
                    user.delete()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Log.d(TAG, "✅ Account deleted successfully");
                                    CustomToast.success(getContext(), "Account deleted");

                                    // 3. Sign out from Google
                                    mGoogleSignInClient.signOut(). addOnCompleteListener(signOutTask -> {
                                        // 4. Navigate to sign in
                                        Intent intent = new Intent(getActivity(), signin_activity.class);
                                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                        startActivity(intent);
                                        if (getActivity() != null) {
                                            getActivity().finish();
                                        }
                                    });
                                } else {
                                    Log.e(TAG, "❌ Failed to delete auth account", task.getException());
                                    CustomToast.error(getContext(), "Failed to delete account. Please re-login and try again.");
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    Log. e(TAG, "❌ Failed to delete Firestore data", e);
                    CustomToast.error(getContext(), "Failed to delete account data");
                });
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
        SharedPreferences prefs = requireContext(). getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String currentCode = prefs.getString(KEY_LANG_CODE, "en");

        DialogHelper.showLanguageSelectionDialog(requireContext(), languages, currentCode, (selectedLang) -> {
            updateLanguage(selectedLang. name, selectedLang.code);
        });
    }

    private void updateLanguage(String languageName, String languageCode) {
        SharedPreferences prefs = requireContext(). getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs. edit()
                .putString(KEY_LANG_CODE, languageCode)
                . putString(KEY_LANG_NAME, languageName)
                .apply();

        languageManager.setLanguage(requireContext(), languageName, languageCode);

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("language", languageName);
            updates.put("languageCode", languageCode);
            db.collection("users").document(user.getUid()).update(updates)
                    .addOnSuccessListener(aVoid ->
                            CustomToast.success(getContext(), "Language updated"));
        }

        requireActivity().recreate();
    }

    private void loadUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String name = user.getDisplayName();
        String email = user.getEmail();

        tvProfileName.setText(name != null && !name.isEmpty() ? name : "Welcome");
        tvProfileEmail.setText(email);

        // Use FirebaseProfileManager to load profile image
        firebaseProfileManager.getProfileImageUrl(user.getUid(), new FirebaseProfileManager.ProfileImageCallback() {
            @Override
            public void onSuccess(String imageUrl, String fileId) {
                loadProfilePhoto(imageUrl);
            }

            @Override
            public void onNotFound() {
                // Fallback to old field names for backward compatibility
                db.collection("users").document(user.getUid())
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                String drivePhotoUrl = documentSnapshot.getString("drivePhotoUrl");
                                if (drivePhotoUrl != null && !drivePhotoUrl.isEmpty()) {
                                    loadProfilePhoto(drivePhotoUrl);
                                } else {
                                    loadDefaultPhoto();
                                }
                            } else {
                                loadDefaultPhoto();
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "❌ Failed to load profile", e);
                            loadDefaultPhoto();
                        });
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "❌ Failed to load profile image", e);
                loadDefaultPhoto();
            }
        });
    }

    private void loadProfilePhoto(String driveUrl) {
        if (getContext() == null) return;
        Glide.with(this)
                .load(driveUrl)
                .circleCrop()
                .placeholder(R.drawable.ic_profile_inactive)
                .error(R.drawable.ic_profile_inactive)
                .into(ivProfileAvatar);
    }

    private void loadDefaultPhoto() {
        if (getContext() == null) return;
        Glide.with(this)
                .load(R.drawable. ic_profile_inactive)
                .circleCrop()
                .into(ivProfileAvatar);
    }

    private void selectProfilePhoto() {
        if (!driveProfileService.isInitialized() && !driveManager.isAvailable()) {
            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(requireContext());
            if (account != null) {
                CustomToast.info(getContext(), "Requesting Drive access...");
                triggerDriveReAuthentication();
            } else {
                CustomToast.warning(getContext(), "Please sign in with Google to upload photos");
            }
            return;
        }
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent);
    }

    private void triggerDriveReAuthentication() {
        mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
            Intent signInIntent = mGoogleSignInClient.getSignInIntent();
            driveReAuthLauncher.launch(signInIntent);
        });
    }

    private void uploadProfilePhotoToDrive(Uri imageUri) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        if (!driveProfileService.isInitialized() && !driveManager.isAvailable()) {
            pendingImageUri = imageUri;
            triggerDriveReAuthentication();
            return;
        }

        String userId = user.getUid();
        CustomToast.info(getContext(), "Uploading to Drive...");

        // Prefer DriveProfileService (uses appDataFolder) over GoogleDriveManager
        if (driveProfileService.isInitialized()) {
            driveProfileService.uploadProfilePicture(imageUri, new DriveProfileService.Callback() {
                @Override
                public void onSuccess(String imageUrl, String fileId) {
                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() -> {
                        // Save to Firestore using FirebaseProfileManager
                        firebaseProfileManager.saveProfileImageUrl(userId, imageUrl, fileId,
                                new FirebaseProfileManager.UpdateCallback() {
                                    @Override
                                    public void onSuccess() {
                                        CustomToast.success(getContext(), "Photo updated!");
                                        loadProfilePhoto(imageUrl);
                                    }

                                    @Override
                                    public void onError(Exception e) {
                                        CustomToast.error(getContext(), "Failed to save photo URL");
                                    }
                                });
                    });
                }

                @Override
                public void onError(Exception e) {
                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() ->
                            CustomToast.error(getContext(), "Upload failed: " + e.getMessage()));
                }
            });
        } else {
            // Fallback to GoogleDriveManager
            driveManager.uploadProfilePhoto(imageUri, userId, new GoogleDriveManager.UploadCallback() {
                @Override
                public void onSuccess(String driveUrl, String fileId) {
                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() -> {
                        // Save using both old and new field names for compatibility
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("drivePhotoUrl", driveUrl);
                        updates.put("driveFileId", fileId);
                        updates.put("profileImageUrl", driveUrl);
                        updates.put("profileImageFileId", fileId);
                        db.collection("users").document(userId).update(updates)
                                .addOnSuccessListener(aVoid -> {
                                    CustomToast.success(getContext(), "Photo updated!");
                                    loadProfilePhoto(driveUrl);
                                });
                    });
                }

                @Override
                public void onError(Exception e) {
                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() ->
                            CustomToast.error(getContext(), "Upload failed: " + e.getMessage()));
                }
            });
        }
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
}