package com.example.learnify.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.learnify.fragments.DownloadsFragment;
import com.example.learnify.fragments.FavouritesFragment;

public class HistoryViewPagerAdapter extends FragmentStateAdapter {

    public HistoryViewPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // This is the "smart" logic for the tabs
        // Position 0 = Favourites, Position 1 = Downloads
        switch (position) {
            case 0:
                return new FavouritesFragment();
            case 1:
                return new DownloadsFragment();
            default:
                return new FavouritesFragment(); // Default to Favourites
        }
    }

    @Override
    public int getItemCount() {
        return 2; // We have 2 tabs
    }
}