package com.example.learnify;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class NotesListFragment extends Fragment {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyView;
    private Chip filterFavorites;
    private VideoNotesRepository repository;
    private NotesAdapter adapter;
    private List<VideoNotesRepository.VideoNote> allNotes = new ArrayList<>();
    private boolean showingFavoritesOnly = false;

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
        filterFavorites = view.findViewById(R.id.filter_favorites);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Setup filter chip if it exists
        if (filterFavorites != null) {
            filterFavorites.setOnCheckedChangeListener((buttonView, isChecked) -> {
                showingFavoritesOnly = isChecked;
                applyFilter();
            });
        }

        loadNotes();
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
                Toast.makeText(getContext(), "Failed to load notes", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyFilter() {
        List<VideoNotesRepository.VideoNote> filteredNotes;
        
        if (showingFavoritesOnly) {
            filteredNotes = new ArrayList<>();
            for (VideoNotesRepository.VideoNote note : allNotes) {
                if (note.isFavorite) {
                    filteredNotes.add(note);
                }
            }
        } else {
            filteredNotes = new ArrayList<>(allNotes);
        }

        if (filteredNotes.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            if (showingFavoritesOnly) {
                emptyView.setText("No favorite notes yet");
            } else {
                emptyView.setText("No notes yet");
            }
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            setupAdapter(filteredNotes);
        }
    }

    private void setupAdapter(List<VideoNotesRepository.VideoNote> notes) {
        adapter = new NotesAdapter(
                notes,
                note -> {
                    // Open VideoNotesFragment with the specific URL
                    VideoNotesFragment fragment = VideoNotesFragment.newInstance(note.videoUrl);
                    getParentFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, fragment)
                            .addToBackStack(null)
                            .commit();
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
                    
                    Toast.makeText(getContext(), "✅ Note deleted", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(Exception e) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "❌ Failed to delete note", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(Exception e) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "❌ Failed to update favorite", Toast.LENGTH_SHORT).show();
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