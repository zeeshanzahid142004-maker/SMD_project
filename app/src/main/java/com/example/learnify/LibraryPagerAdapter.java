package com.example.learnify;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

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
