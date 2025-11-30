package com.example.learnify;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class LibraryFragment extends Fragment {

    private static final String TAG = "LibraryFragment";

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private LibraryPagerAdapter pagerAdapter;

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

        if (tabLayout == null || viewPager == null) {
            return;
        }

        // Setup ViewPager2 with adapter
        pagerAdapter = new LibraryPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        // Link TabLayout with ViewPager2
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
                default:
                    tab.setText(R.string.favourites);
                    tab.setIcon(R.drawable.ic_favorite);

            }
        }).attach();
    }
}
