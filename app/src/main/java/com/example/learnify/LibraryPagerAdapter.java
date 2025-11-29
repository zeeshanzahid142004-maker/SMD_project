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
                return new RecentHistoryFragment();
            case 1:
                return new FavouritesFragment();
            case 2:
                return new NotesListFragment();
            case 3:
                return new DownloadsFragment();
            default:
                return new RecentHistoryFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 4; // 4 tabs: History, Favourites, Notes, Downloads
    }
}
