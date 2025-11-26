package com.example.learnify;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class NotesListFragment extends Fragment {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private TextView emptyView;
    private VideoNotesRepository repository;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = new VideoNotesRepository();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Reusing a simple layout structure programmatically to avoid creating another file if possible,
        // but for standard practice, let's assume a simple frame layout with recyclerview.
        // Using fragment_history.xml structure as a base or creating a new one.
        return inflater.inflate(R.layout.fragment_notes_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.notes_recycler_view);
        progressBar = view.findViewById(R.id.loading_progress);
        emptyView = view.findViewById(R.id.empty_view);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        loadNotes();
    }

    private void loadNotes() {
        progressBar.setVisibility(View.VISIBLE);
        repository.getAllNotes(new VideoNotesRepository.OnAllNotesLoadedListener() {
            @Override
            public void onAllNotesLoaded(List<VideoNotesRepository.VideoNote> notes) {
                if (getActivity() == null) return;

                progressBar.setVisibility(View.GONE);
                if (notes.isEmpty()) {
                    emptyView.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    emptyView.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    NotesAdapter adapter = new NotesAdapter(notes, note -> {
                        // Open VideoNotesFragment with the specific URL
                        VideoNotesFragment fragment = VideoNotesFragment.newInstance(note.videoUrl);
                        getParentFragmentManager().beginTransaction()
                                .replace(R.id.fragment_container, fragment)
                                .addToBackStack(null)
                                .commit();
                    });
                    recyclerView.setAdapter(adapter);
                }
            }

            @Override
            public void onError(Exception e) {
                if (getActivity() == null) return;
                progressBar.setVisibility(View.GONE);
            }
        });
    }
}