package com.example.learnify.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
// Removed standard Toast

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.learnify.helpers.CustomToast; // ✅ Import CustomToast
import com.example.learnify.adapters.NotesAdapter;
import com.example.learnify.R;
import com.example.learnify.repository.VideoNotesRepository;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;

public class NotesListFragment extends Fragment {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyView;
    private View emptyStateLayout;
    private Chip filterFavorites;
    private SearchView searchView;
    private VideoNotesRepository repository;
    private NotesAdapter adapter;
    private List<VideoNotesRepository.VideoNote> allNotes = new ArrayList<>();
    private boolean showingFavoritesOnly = false;
    private String currentSearchQuery = "";

    // Debounce handler for search
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private static final long SEARCH_DEBOUNCE_DELAY = 300; // 300ms

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = new VideoNotesRepository();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notes_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.notes_recycler_view);
        progressBar = view.findViewById(R.id.loading_progress);
        emptyView = view.findViewById(R.id.empty_view);
        emptyStateLayout = view.findViewById(R.id.ll_empty_state);
        filterFavorites = view.findViewById(R.id.filter_favorites);
        searchView = view.findViewById(R.id.notes_search_view);

        recyclerView.setLayoutManager(new GridLayoutManager(getContext(),2));

        // Setup filter chip if it exists
        if (filterFavorites != null) {
            filterFavorites.setOnCheckedChangeListener((buttonView, isChecked) -> {
                showingFavoritesOnly = isChecked;
                applyFilter();
            });
        }

        // Setup search functionality
        setupSearch();

        loadNotes();
    }

    private void setupSearch() {
        if (searchView != null) {
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    currentSearchQuery = query;
                    applyFilter();
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    // Debounce search input
                    if (searchRunnable != null) {
                        searchHandler.removeCallbacks(searchRunnable);
                    }
                    searchRunnable = () -> {
                        currentSearchQuery = newText;
                        applyFilter();
                    };
                    searchHandler.postDelayed(searchRunnable, SEARCH_DEBOUNCE_DELAY);
                    return true;
                }
            });

            // Handle close button
            searchView.setOnCloseListener(() -> {
                currentSearchQuery = "";
                applyFilter();
                return false;
            });
        }
    }

    private void loadNotes() {
        progressBar.setVisibility(View.VISIBLE);
        repository.getAllNotes(new VideoNotesRepository.OnAllNotesLoadedListener() {
            @Override
            public void onAllNotesLoaded(List<VideoNotesRepository.VideoNote> notes) {
                if (getActivity() == null) return;

                progressBar.setVisibility(View.GONE);
                allNotes.clear();
                allNotes.addAll(notes);

                applyFilter();
            }

            @Override
            public void onError(Exception e) {
                if (getActivity() == null) return;
                progressBar.setVisibility(View.GONE);
                // ✅ Replaced Toast
                CustomToast.error(getContext(), "Failed to load notes");
            }
        });
    }

    private void applyFilter() {
        List<VideoNotesRepository.VideoNote> filteredNotes = new ArrayList<>();

        for (VideoNotesRepository.VideoNote note : allNotes) {
            boolean matchesFavorite = !showingFavoritesOnly || note.isFavorite;
            boolean matchesSearch = true;

            // Apply search filter
            if (currentSearchQuery != null && !currentSearchQuery.isEmpty()) {
                String searchLower = currentSearchQuery.toLowerCase();
                String contentText = note.content != null ?
                        Html.fromHtml(note.content, Html.FROM_HTML_MODE_LEGACY).toString().toLowerCase() : "";
                String urlText = note.videoUrl != null ? note.videoUrl.toLowerCase() : "";

                matchesSearch = contentText.contains(searchLower) || urlText.contains(searchLower);
            }

            if (matchesFavorite && matchesSearch) {
                filteredNotes.add(note);
            }
        }

        if (filteredNotes.isEmpty()) {
            if (emptyStateLayout != null) {
                emptyStateLayout.setVisibility(View.VISIBLE);
            }
            recyclerView.setVisibility(View.GONE);
            if (!currentSearchQuery.isEmpty()) {
                emptyView.setText("No results found for \"" + currentSearchQuery + "\"");
            } else if (showingFavoritesOnly) {
                emptyView.setText("No favorite notes yet");
            } else {
                emptyView.setText("No notes yet");
            }
        } else {
            if (emptyStateLayout != null) {
                emptyStateLayout.setVisibility(View.GONE);
            }
            recyclerView.setVisibility(View.VISIBLE);
            setupAdapter(filteredNotes);
        }
    }

    private void setupAdapter(List<VideoNotesRepository.VideoNote> notes) {
        adapter = new NotesAdapter(
                notes,
                note -> {
                    // FIX: Don't do the transaction here. Ask the parent (LibraryFragment) to do it.
                    Fragment parentFragment = getParentFragment();
                    if (parentFragment instanceof LibraryFragment) {
                        ((LibraryFragment) parentFragment).openVideoNote(note.videoUrl);
                    } else {
                        // Fallback (or Error Log)
                        android.util.Log.e("NotesListFragment", "Parent is not LibraryFragment!");
                    }
                },
                (note, position) -> {
                    // Delete with confirmation
                    showDeleteConfirmationDialog(note, position);
                },
                (note, position) -> {
                    // Toggle favorite
                    toggleFavorite(note, position);
                }
        );
        recyclerView.setAdapter(adapter);
    }

    private void showDeleteConfirmationDialog(VideoNotesRepository.VideoNote note, int position) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Note")
                .setMessage("Are you sure you want to delete this note? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    deleteNote(note, position);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteNote(VideoNotesRepository.VideoNote note, int position) {
        repository.deleteNotes(note.videoUrl, new VideoNotesRepository.OnNoteDeletedListener() {
            @Override
            public void onNoteDeleted() {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    // Remove from allNotes
                    allNotes.removeIf(n -> n.videoUrl.equals(note.videoUrl));

                    // Update adapter
                    if (adapter != null) {
                        adapter.removeItem(position);
                    }

                    // Check if list is now empty
                    if (allNotes.isEmpty() || (showingFavoritesOnly && !hasFavorites())) {
                        applyFilter();
                    }

                    // ✅ Replaced Toast
                    CustomToast.success(getContext(), "✅ Note deleted");
                });
            }

            @Override
            public void onError(Exception e) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    // ✅ Replaced Toast
                    CustomToast.error(getContext(), "❌ Failed to delete note");
                });
            }
        });
    }

    private void toggleFavorite(VideoNotesRepository.VideoNote note, int position) {
        boolean newFavoriteState = !note.isFavorite;

        repository.toggleFavorite(note.videoUrl, newFavoriteState, new VideoNotesRepository.OnFavoriteToggledListener() {
            @Override
            public void onFavoriteToggled(boolean isFavorite) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    // Update the note in allNotes
                    for (VideoNotesRepository.VideoNote n : allNotes) {
                        if (n.videoUrl.equals(note.videoUrl)) {
                            n.isFavorite = isFavorite;
                            break;
                        }
                    }

                    // If showing favorites only and unfavorited, reapply filter
                    if (showingFavoritesOnly && !isFavorite) {
                        applyFilter();
                    } else if (adapter != null) {
                        note.isFavorite = isFavorite;
                        adapter.updateItem(position, note);
                    }

                    String message = isFavorite ? "❤️ Added to favorites" : "Removed from favorites";
                    // ✅ Replaced Toast
                    CustomToast.success(getContext(), message);
                });
            }

            @Override
            public void onError(Exception e) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    // ✅ Replaced Toast
                    CustomToast.error(getContext(), "❌ Failed to update favorite");
                });
            }
        });
    }

    private boolean hasFavorites() {
        for (VideoNotesRepository.VideoNote note : allNotes) {
            if (note.isFavorite) return true;
        }
        return false;
    }
}