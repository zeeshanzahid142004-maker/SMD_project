package com.example.learnify;

import android.text.Html;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {

    private List<VideoNotesRepository.VideoNote> notes;
    private final OnNoteClickListener clickListener;
    private final OnNoteDeleteListener deleteListener;
    private final OnNoteFavoriteListener favoriteListener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());

    // Anti-spam protection
    private long lastDeleteClickTime = 0;
    private long lastFavoriteClickTime = 0;
    private static final long CLICK_DELAY = 1000; // 1 second

    public interface OnNoteClickListener {
        void onNoteClick(VideoNotesRepository.VideoNote note);
    }

    public interface OnNoteDeleteListener {
        void onNoteDelete(VideoNotesRepository.VideoNote note, int position);
    }

    public interface OnNoteFavoriteListener {
        void onNoteFavoriteToggle(VideoNotesRepository.VideoNote note, int position);
    }

    public NotesAdapter(List<VideoNotesRepository.VideoNote> notes, OnNoteClickListener clickListener) {
        this(notes, clickListener, null, null);
    }

    public NotesAdapter(List<VideoNotesRepository.VideoNote> notes, 
                       OnNoteClickListener clickListener,
                       OnNoteDeleteListener deleteListener,
                       OnNoteFavoriteListener favoriteListener) {
        this.notes = notes;
        this.clickListener = clickListener;
        this.deleteListener = deleteListener;
        this.favoriteListener = favoriteListener;
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
        holder.bind(note, position);
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    public void updateNotes(List<VideoNotesRepository.VideoNote> newNotes) {
        this.notes = newNotes;
        notifyDataSetChanged();
    }

    public void removeItem(int position) {
        if (position >= 0 && position < notes.size()) {
            notes.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, notes.size());
        }
    }

    public void updateItem(int position, VideoNotesRepository.VideoNote note) {
        if (position >= 0 && position < notes.size()) {
            notes.set(position, note);
            notifyItemChanged(position);
        }
    }

    class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView title, preview, date;
        ImageView favoriteButton, deleteButton;

        public NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.note_title);
            preview = itemView.findViewById(R.id.note_preview);
            date = itemView.findViewById(R.id.note_date);
            favoriteButton = itemView.findViewById(R.id.note_favorite);
            deleteButton = itemView.findViewById(R.id.note_delete);
        }

        void bind(VideoNotesRepository.VideoNote note, int position) {
            // Use part of video URL as title for now, or "Video Note"
            title.setText("Video Notes");
            
            // Strip HTML tags for preview
            String contentPreview = note.content;
            if (contentPreview != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    contentPreview = Html.fromHtml(contentPreview, Html.FROM_HTML_MODE_LEGACY).toString();
                } else {
                    contentPreview = Html.fromHtml(contentPreview).toString();
                }
            }
            preview.setText(contentPreview);
            
            if (note.updatedAt != null) {
                date.setText(dateFormat.format(note.updatedAt));
            }

            // Update favorite icon
            updateFavoriteIcon(note.isFavorite);

            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onNoteClick(note);
                }
            });

            // Favorite button with anti-spam
            if (favoriteButton != null) {
                favoriteButton.setOnClickListener(v -> {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastFavoriteClickTime < CLICK_DELAY) {
                        return;
                    }
                    lastFavoriteClickTime = currentTime;

                    if (favoriteListener != null) {
                        favoriteListener.onNoteFavoriteToggle(note, position);
                    }
                });
            }

            // Delete button with anti-spam
            if (deleteButton != null) {
                deleteButton.setOnClickListener(v -> {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastDeleteClickTime < CLICK_DELAY) {
                        return;
                    }
                    lastDeleteClickTime = currentTime;

                    if (deleteListener != null) {
                        deleteListener.onNoteDelete(note, position);
                    }
                });
            }
        }

        private void updateFavoriteIcon(boolean isFavorite) {
            if (favoriteButton != null) {
                if (isFavorite) {
                    favoriteButton.setImageResource(R.drawable.ic_favorite_filled);
                } else {
                    favoriteButton.setImageResource(R.drawable.ic_favorite_border);
                }
            }
        }
    }
}