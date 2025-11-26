package com.example.learnify;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {

    private final List<VideoNotesRepository.VideoNote> notes;
    private final OnNoteClickListener listener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());

    public interface OnNoteClickListener {
        void onNoteClick(VideoNotesRepository.VideoNote note);
    }

    public NotesAdapter(List<VideoNotesRepository.VideoNote> notes, OnNoteClickListener listener) {
        this.notes = notes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note_card, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        VideoNotesRepository.VideoNote note = notes.get(position);
        holder.bind(note);
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView title, preview, date;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.note_title);
            preview = itemView.findViewById(R.id.note_preview);
            date = itemView.findViewById(R.id.note_date);
        }

        void bind(VideoNotesRepository.VideoNote note) {
            // Use part of video URL as title for now, or "Video Note"
            title.setText("Video Notes");
            preview.setText(note.content);
            if (note.updatedAt != null) {
                date.setText(dateFormat.format(note.updatedAt));
            }

            itemView.setOnClickListener(v -> listener.onNoteClick(note));
        }
    }
}