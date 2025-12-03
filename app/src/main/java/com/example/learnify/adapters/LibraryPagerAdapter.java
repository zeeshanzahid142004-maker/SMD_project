package com.example.learnify.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.learnify.fragments.DownloadsFragment;
import com.example.learnify.fragments.FavouritesFragment;
import com.example.learnify.fragments.NotesListFragment;

public class LibraryPagerAdapter extends FragmentStateAdapter {

    public LibraryPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // 4 sub-tabs: History, Favourites, Notes, Downloads
        switch (position) {
            case 0:
                return new FavouritesFragment();
            case 1:
                return new NotesListFragment();
            case 2:
                return new DownloadsFragment();

            default:
                return new FavouritesFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 3; // 4 tabs: History, Favourites, Notes, Downloads
    }
}
