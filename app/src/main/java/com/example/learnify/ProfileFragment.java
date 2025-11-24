package com.example.learnify;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";

    // Firebase & Google
    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    // UI Views
    private ImageView ivProfileAvatar;
    private TextView tvProfileName, tvProfileEmail;
    private MaterialButton btnSignOut;
    private ImageView btnEditProfile;
    private TextView rowNotifications, rowAppearance, rowHelp;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Configure Google Sign-In to be able to sign out
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(requireActivity(), gso);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
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

        rowNotifications = view.findViewById(R.id.row_notifications);
        rowAppearance = view.findViewById(R.id.row_appearance);
        rowHelp = view.findViewById(R.id.row_help);

        // Set click listeners
        btnSignOut.setOnClickListener(v -> signOut());

        // Placeholder click listeners for other options
        btnEditProfile.setOnClickListener(v -> showToast("Edit Profile clicked"));
        rowNotifications.setOnClickListener(v -> showToast("Notifications clicked"));
        rowAppearance.setOnClickListener(v -> showToast("Appearance clicked"));
        rowHelp.setOnClickListener(v -> showToast("Help Center clicked"));

        // Load user data into views
        loadUserProfile();
    }

    private void loadUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String name = user.getDisplayName();
            String email = user.getEmail();
            Uri photoUrl = user.getPhotoUrl();

            // Set Name
            if (name != null && !name.isEmpty()) {
                tvProfileName.setText(name);
            } else {
                tvProfileName.setText("Welcome"); // Fallback text
            }

            // Set Email
            tvProfileEmail.setText(email);

            // Set Profile Picture using Glide
            if (photoUrl != null) {
                Glide.with(this)
                        .load(photoUrl)
                        .circleCrop() // Make it circular
                        .placeholder(R.drawable.ic_profile_inactive)
                        .into(ivProfileAvatar);
            } else {
                // Load placeholder if no photo URL
                Glide.with(this)
                        .load(R.drawable.ic_profile_inactive)
                        .circleCrop()
                        .into(ivProfileAvatar);
            }
        }
    }

    private void signOut() {
        Log.d(TAG, "Sign-out button clicked");

        // 1. Sign out from Firebase
        mAuth.signOut();

        // 2. Sign out from Google
        // This clears the account from the Google Sign-In client
        mGoogleSignInClient.signOut().addOnCompleteListener(requireActivity(), task -> {
            Log.d(TAG, "Signed out from Google");

            // 3. Navigate back to the sign-in screen
            // We also clear the activity stack to prevent user from pressing "back"
            Intent intent = new Intent(getActivity(), signin_activity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);

            // Finish the host activity (MainActivity)
            if (getActivity() != null) {
                getActivity().finish();
            }
        });
    }

    private void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }
}