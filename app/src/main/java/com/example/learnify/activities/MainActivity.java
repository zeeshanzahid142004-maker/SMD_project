package com.example.learnify.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.learnify.R;
import com.example.learnify.fragments.GenerateQuizFragment;
import com.example.learnify.fragments.HomeFragment;
import com.example.learnify.fragments.LibraryFragment;
import com.example.learnify.fragments.ProfileFragment;
import com.example.learnify.fragments.VideoNotesFragment;
import com.example.learnify.fragments.linkFragment;
import com.example.learnify.helpers.CustomToast;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;

public class MainActivity extends BaseActivity implements HomeFragment.OnHomeFragmentInteractionListener {

    private static final String TAG = "MainActivity";
    private static final long BACK_PRESS_DELAY = 2000;
    
    private BottomNavigationView bottomNavigationView;
    private long backPressedTime = 0;
    private Toast exitToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize PDFBox
        try {
            PDFBoxResourceLoader.init(getApplicationContext());
        } catch (Exception e) {
            Log.e(TAG, "Could not initialize PDFBox", e);
        }

        // Make sure this ID matches your activity_main.xml
        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        // 1. Load Home by default
        if (savedInstanceState == null) {
            loadFragment_NoBackStack(new HomeFragment());
        }

        // 2. Navigation Logic
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            // Debug log to verify clicks
            Log.d(TAG, "Nav item clicked: " + itemId);

            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_library) {
                selectedFragment = new LibraryFragment();
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                // Clear back stack and load fresh fragment
                clearBackStack();
                loadFragment_NoBackStack(selectedFragment);
            }
            return true;
        });

        // Setup back press handler
        setupBackPressHandler();
    }

    /**
     * Setup proper back press handling
     */
    private void setupBackPressHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                FragmentManager fragmentManager = getSupportFragmentManager();
                
                // If there are fragments in the back stack, pop them
                if (fragmentManager.getBackStackEntryCount() > 0) {
                    fragmentManager.popBackStack();
                    
                    // Update bottom nav if needed
                    Fragment currentFragment = fragmentManager.findFragmentById(R.id.fragment_container);
                    updateBottomNavForFragment(currentFragment);
                } else {
                    // No back stack - double tap to exit
                    if (backPressedTime + BACK_PRESS_DELAY > System.currentTimeMillis()) {
                        if (exitToast != null) exitToast.cancel();
                        finish();
                    } else {
                        backPressedTime = System.currentTimeMillis();
                        exitToast = CustomToast.info(MainActivity.this, "Press BACK again to exit");
                    }
                }
            }
        });
    }

    /**
     * Update bottom navigation selection based on current fragment
     */
    private void updateBottomNavForFragment(Fragment fragment) {
        if (fragment instanceof HomeFragment) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        } else if (fragment instanceof LibraryFragment) {
            bottomNavigationView.setSelectedItemId(R.id.nav_library);
        } else if (fragment instanceof ProfileFragment) {
            bottomNavigationView.setSelectedItemId(R.id.nav_profile);
        }
    }

    /**
     * Clear the fragment back stack
     */
    private void clearBackStack() {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    }

    // --- Navigation Helper Methods ---

    private void loadFragment_WithBackStack(Fragment fragment) {
        Log.d(TAG, "Loading fragment WITH backstack: " + fragment.getClass().getSimpleName());
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void loadFragment_NoBackStack(Fragment fragment) {
        Log.d(TAG, "Loading fragment WITHOUT backstack: " + fragment.getClass().getSimpleName());
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }

    // --- Interface Implementations ---

    @Override
    public void onViewMoreHistoryClicked() {
        Log.d(TAG, "📜 Opening full History");
        Intent intent = new Intent(this, HistoryActivity.class);
        startActivity(intent);
    }

    @Override
    public void onGoToVideoFragment(String url) {

    }

    @Override
    public void onGoToVideoFragment(String videoUrl, String transcript) {
        // Use the new instance method that accepts transcript
        VideoNotesFragment fragment = VideoNotesFragment.newInstance(videoUrl,transcript);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }


    @Override
    public void onGoToQuizFragment(String url) {
        Log.d(TAG, "🎯 Opening quiz generator for URL: " + url);
        GenerateQuizFragment fragment = GenerateQuizFragment.newInstance(url);
        loadFragment_WithBackStack(fragment);
    }

    @Override
    public void onUploadLinkClicked() {
        Log.d(TAG, "📤 Opening link upload");
        loadFragment_WithBackStack(new linkFragment());
    }
}