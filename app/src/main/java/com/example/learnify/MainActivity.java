package com.example.learnify;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android. material.floatingactionbutton.FloatingActionButton;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;

public class MainActivity extends AppCompatActivity implements
        HomeFragment. OnHomeFragmentInteractionListener {

    private static final String TAG = "MainActivity";
    BottomNavigationView bottomNav;
    FloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R. layout.activity_main);

        bottomNav = findViewById(R.id.bottom_navigation_view);
        fab = findViewById(R.id.fab_progress);
        PDFBoxResourceLoader.init(getApplicationContext());

        // 1. Load Home by default
        if (savedInstanceState == null) {
            loadFragment_NoBackStack(new HomeFragment());
        }

        // 2. Navigation Logic
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (id == R.id.nav_explore) {
                selectedFragment = new DownloadsFragment();
            } else if (id == R.id.nav_courses) {
                selectedFragment = new FavouritesFragment();
            } else if (id == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            }

            if (selectedFragment != null) {
                loadFragment_NoBackStack(selectedFragment);
            }
            return true;
        });

        // 3. Floating Action Button (Progress)
        fab.setOnClickListener(v -> {
            loadFragment_WithBackStack(new progress_fragment());
            bottomNav.getMenu().findItem(R.id.nav_placeholder). setChecked(true);
        });
    }

    // --- Navigation Helper Methods ---

    private void loadFragment_WithBackStack(Fragment fragment) {
        Log. d(TAG, "Loading fragment WITH backstack: " + fragment.getClass().getSimpleName());
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id. fragment_container, fragment);
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
        // ⭐ FIXED: Open HistoryActivity as an Intent (not Fragment)
        Log.d(TAG, "📜 Opening full History");
        Intent intent = new Intent(this, HistoryActivity.class);
        startActivity(intent);
    }

    @Override
    public void onUploadLinkClicked() {
        Log.d(TAG, "📤 Opening link upload");
        loadFragment_WithBackStack(new linkFragment());
    }

    @Override
    public void onGoToVideoFragment(String url) {
        Log.d(TAG, "🎬 Opening video fragment for URL: " + url);
        VideoNotesFragment videoFragment = new VideoNotesFragment();
        Bundle args = new Bundle();
        args.putString("VIDEO_URL", url);
        videoFragment.setArguments(args);
        loadFragment_WithBackStack(videoFragment);
    }

    @Override
    public void onGoToQuizFragment(String url) {
        Log.d(TAG, "🎯 Opening quiz generator for URL: " + url);
        GenerateQuizFragment generateFragment = GenerateQuizFragment.newInstance(url);
        loadFragment_WithBackStack(generateFragment);
    }
}