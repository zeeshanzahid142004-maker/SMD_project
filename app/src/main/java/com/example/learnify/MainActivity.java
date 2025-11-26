package com.example.learnify;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;

public class MainActivity extends AppCompatActivity implements HomeFragment.OnHomeFragmentInteractionListener {

    private static final String TAG = "MainActivity";
    private BottomNavigationView bottomNavigationView;

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

            if (itemId == R.id.navigation_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.navigation_downloads) {
                // Matches the ID in bottom_app_bar_menu.xml
                selectedFragment = new DownloadsFragment();
            } else if (itemId == R.id.navigation_notes) {
                // Matches the ID in bottom_app_bar_menu.xml
                selectedFragment = new NotesListFragment();
            } else if (itemId == R.id.navigation_favourites) {
                // Matches the ID in bottom_app_bar_menu.xml
                selectedFragment = new FavouritesFragment();
            }

            if (selectedFragment != null) {
                loadFragment_NoBackStack(selectedFragment);
            }
            return true;
        });
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
        Log.d(TAG, "🎬 Opening video fragment for URL: " + url);
        VideoNotesFragment fragment = VideoNotesFragment.newInstance(url);
        loadFragment_WithBackStack(fragment);
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