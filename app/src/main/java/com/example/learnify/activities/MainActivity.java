package com.example.learnify.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.OnBackPressedCallback;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.learnify.R;
import com.example.learnify.helpers.CustomToast;
import com.example.learnify.fragments.GenerateQuizFragment;
import com.example.learnify.fragments.HomeFragment;
import com.example.learnify.fragments.LibraryFragment;
import com.example.learnify.fragments.ProfileFragment;
import com.example.learnify.fragments.VideoNotesFragment;
import com.example.learnify.fragments.linkFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;

public class MainActivity extends BaseActivity implements HomeFragment.OnHomeFragmentInteractionListener {

    private static final String TAG = "MainActivity";
    private static final long BACK_PRESS_DELAY = 2000;
    
    private BottomNavigationView bottomNavigationView;
    private long backPressedTime = 0;

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
                // Clear back stack and go to home
                clearBackStackAndLoadHome();
                return true;
            } else if (itemId == R.id.nav_library) {
                selectedFragment = new LibraryFragment();
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                loadFragment_NoBackStack(selectedFragment);
            }
            return true;
        });

        // Setup back press handler
        setupBackPressHandler();
    }

    /**
     * Setup proper back press handling for fragments
     */
    private void setupBackPressHandler() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                FragmentManager fm = getSupportFragmentManager();
                
                // Check if there are fragments in the back stack
                if (fm.getBackStackEntryCount() > 0) {
                    // Pop the back stack to go to previous fragment
                    fm.popBackStack();
                    return;
                }

                // Get current fragment
                Fragment currentFragment = fm.findFragmentById(R.id.fragment_container);

                // If not on HomeFragment, go to HomeFragment
                if (!(currentFragment instanceof HomeFragment)) {
                    clearBackStackAndLoadHome();
                    bottomNavigationView.setSelectedItemId(R.id.nav_home);
                    return;
                }

                // On HomeFragment - double tap to exit
                if (backPressedTime + BACK_PRESS_DELAY > System.currentTimeMillis()) {
                    setEnabled(false);
                    finish();
                } else {
                    backPressedTime = System.currentTimeMillis();
                    CustomToast.info(MainActivity.this, getString(R.string.press_back_to_exit));
                }
            }
        });
    }

    /**
     * Clear back stack and load HomeFragment
     */
    private void clearBackStackAndLoadHome() {
        FragmentManager fm = getSupportFragmentManager();
        // Clear all back stack entries
        fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        // Load HomeFragment
        loadFragment_NoBackStack(new HomeFragment());
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
        VideoNotesFragment fragment = VideoNotesFragment.newInstance(videoUrl, transcript);

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