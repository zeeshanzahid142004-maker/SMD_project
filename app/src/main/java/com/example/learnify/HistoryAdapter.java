package com.example.learnify;

import android.view. LayoutInflater;
import android. view.View;
import android. view.ViewGroup;
import android.widget.ImageView;
import android.widget. TextView;

import androidx.annotation.NonNull;
import androidx. cardview.widget.CardView;
import androidx. recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private final List<QuizAttempt> attempts;
    private final OnHistoryActionListener listener;

    public interface OnHistoryActionListener {
        void onDownload(QuizAttempt attempt, int position);
        void onToggleFavorite(QuizAttempt attempt, int position);
    }

    // ⭐ CORRECT CONSTRUCTOR - Takes List and Listener
    public HistoryAdapter(List<QuizAttempt> attempts, OnHistoryActionListener listener) {
        this.attempts = attempts;
        this.listener = listener; // Can be null if you don't need callbacks
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        QuizAttempt attempt = attempts.get(position);

        // Quiz title
        holder.tvQuizTitle.setText(attempt.quizTitle);

        // Score
        holder.tvScore.setText(attempt.score + "/" + attempt.totalQuestions);

        // Percentage with color coding
        holder.tvPercentage.setText(attempt.percentage + "%");
        int color;
        if (attempt.percentage >= 80) {
            color = holder.itemView.getContext().getColor(android.R.color.holo_green_dark);
        } else if (attempt.percentage >= 60) {
            color = holder.itemView. getContext().getColor(android. R.color.holo_orange_dark);
        } else {
            color = holder.itemView.getContext().getColor(android.R.color.holo_red_dark);
        }
        holder.tvPercentage. setTextColor(color);

        // Date
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault());
        holder.tvDate.setText(sdf.format(attempt.attemptedAt));

        // Favorite icon
        updateFavoriteIcon(holder, attempt.isFavorite);

        // Download button state
        if (attempt.isDownloaded) {
            holder.btnDownload.setText("✅ Downloaded");
            holder. btnDownload.setEnabled(false);
        } else {
            holder.btnDownload. setText("⬇️ Download");
            holder. btnDownload.setEnabled(true);
        }

        // Favorite button click
        holder.ivFavorite.setOnClickListener(v -> {
            if (listener != null) {
                listener.onToggleFavorite(attempt, position);
            }
        });

        // Download button click
        holder.btnDownload.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDownload(attempt, position);
            }
        });
    }

    private void updateFavoriteIcon(ViewHolder holder, boolean isFavorite) {
        if (isFavorite) {
            holder.ivFavorite. setImageResource(R.drawable. ic_favorite_filled);
        } else {
            holder.ivFavorite.setImageResource(R.drawable.ic_favorite_border);
        }
    }

    public void updateItem(int position, QuizAttempt updatedAttempt) {
        attempts.set(position, updatedAttempt);
        notifyItemChanged(position);
    }

    @Override
    public int getItemCount() {
        return attempts.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvQuizTitle;
        TextView tvScore;
        TextView tvPercentage;
        TextView tvDate;
        MaterialButton btnDownload;
        ImageView ivFavorite;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvQuizTitle = itemView. findViewById(R.id.tv_quiz_title);
            tvScore = itemView.findViewById(R.id.tv_score);
            tvPercentage = itemView.findViewById(R.id.tv_percentage);
            tvDate = itemView.findViewById(R.id.tv_date);
            btnDownload = itemView.findViewById(R.id.btn_download);
            ivFavorite = itemView.findViewById(R.id. iv_favorite);
        }
    }
}