package com.example.learnify.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.learnify.R;
import com.example.learnify.adapters.LibraryPagerAdapter;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class LibraryFragment extends Fragment {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private FrameLayout fragmentContainer; // Reference to the container

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_library, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tabLayout = view.findViewById(R.id.library_tab_layout);
        viewPager = view.findViewById(R.id.library_view_pager);
        fragmentContainer = view.findViewById(R.id.fragment_container);

        // Setup ViewPager
        LibraryPagerAdapter pagerAdapter = new LibraryPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText(R.string.favourites);
                    tab.setIcon(R.drawable.ic_favorite);
                    break;
                case 1:
                    tab.setText(R.string.notes);
                    tab.setIcon(R.drawable.ic_notes);
                    break;
                case 2:
                    tab.setText(R.string.nav_downloads);
                    tab.setIcon(R.drawable.ic_download);
                    break;
            }
        }).attach();

        // LISTENER: Handle the "Back" button behavior
        // When the video fragment is popped off the stack, show the tabs again.
        getChildFragmentManager().addOnBackStackChangedListener(() -> {
            if (getChildFragmentManager().getBackStackEntryCount() == 0) {
                // We are back at the root (Tabs)
                showTabs();
            }
        });

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Check if there is a child fragment (VideoNotes) open
                if (getChildFragmentManager().getBackStackEntryCount() > 0) {
                    // If yes, close the video (pop the stack)
                    getChildFragmentManager().popBackStack();
                } else {
                    // If no video is open, let the system handle the back button
                    // (which effectively exits the app or goes back to previous activity)
                    setEnabled(false);
                    requireActivity().getOnBackPressedDispatcher().onBackPressed();
                }
            }
        };

        // Register the callback to the activity
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), callback);
    }

    /**
     * Called by child fragments (NotesListFragment) to open a video
     */
    public void openVideoNote(String videoUrl) {
        // 1. Hide Tabs
        tabLayout.setVisibility(View.GONE);
        viewPager.setVisibility(View.GONE);

        // 2. Show Container
        if (fragmentContainer != null) {
            fragmentContainer.setVisibility(View.VISIBLE);
        }

        // 3. Load Fragment
        VideoNotesFragment fragment = VideoNotesFragment.newInstance(videoUrl);
        getChildFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack("video_note") // Important for the back button to work
                .commit();
    }

    private void showTabs() {
        if (tabLayout != null) tabLayout.setVisibility(View.VISIBLE);
        if (viewPager != null) viewPager.setVisibility(View.VISIBLE);
        if (fragmentContainer != null) fragmentContainer.setVisibility(View.GONE);
    }
}