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
import java.util. Locale;

public class DownloadedQuizAdapter extends RecyclerView.Adapter<DownloadedQuizAdapter.ViewHolder> {

    private final List<DownloadedQuizItem> quizzes;
    private final OnQuizActionListener listener;

    public interface OnQuizActionListener {
        void onRetakeQuiz(DownloadedQuizItem quiz);
        void onReviewQuiz(DownloadedQuizItem quiz);
        void onToggleFavorite(DownloadedQuizItem quiz, int position);
        void onDeleteDownload(DownloadedQuizItem quiz, int position);
    }

    public DownloadedQuizAdapter(List<DownloadedQuizItem> quizzes, OnQuizActionListener listener) {
        this.quizzes = quizzes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_downloaded_quiz, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DownloadedQuizItem quiz = quizzes.get(position);

        // Set title
        holder.tvTitle.setText(quiz.quizTitle);

        // Set questions count
        holder.tvQuestionsCount.setText(quiz.totalQuestions + " Questions");

        // Set last score if available
        if (quiz.lastScore > 0) {
            holder.tvLastScore.setText("Last Score: " + quiz.lastScore + "/" + quiz.totalQuestions);
            holder.tvLastScore.setVisibility(View.VISIBLE);
        } else {
            holder. tvLastScore.setVisibility(View.GONE);
        }

        // Set attempt count
        if (quiz.attemptCount > 0) {
            holder.tvAttempts.setText("Attempts: " + quiz.attemptCount);
            holder.tvAttempts.setVisibility(View.VISIBLE);
        } else {
            holder.tvAttempts.setVisibility(View.GONE);
        }

        // Set downloaded date
        if (quiz.downloadedAt != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale. getDefault());
            holder.tvDownloadedDate.setText("Downloaded: " + sdf.format(quiz.downloadedAt));
        }

        // Set favorite icon
        updateFavoriteIcon(holder, quiz.isFavorite);

        // Review button - opens QuizReviewActivity to review answers
        holder.btnReview.setOnClickListener(v -> {
            if (listener != null) {
                listener.onReviewQuiz(quiz);
            }
        });

        // Retake button
        holder.btnRetake.setOnClickListener(v -> {
            if (listener != null) {
                listener. onRetakeQuiz(quiz);
            }
        });

        // Favorite button
        holder.ivFavorite.setOnClickListener(v -> {
            if (listener != null) {
                listener.onToggleFavorite(quiz, position);
            }
        });

        // Delete button
        holder.ivDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteDownload(quiz, position);
            }
        });
    }

    private void updateFavoriteIcon(ViewHolder holder, boolean isFavorite) {
        if (isFavorite) {
            holder.ivFavorite. setImageResource(R.drawable.ic_favorite_filled);
            holder.ivFavorite.setContentDescription("Remove from favorites");
        } else {
            holder.ivFavorite.setImageResource(R.drawable.ic_favorite_border);
            holder.ivFavorite.setContentDescription("Add to favorites");
        }
    }

    public void updateItem(int position, DownloadedQuizItem updatedQuiz) {
        quizzes.set(position, updatedQuiz);
        notifyItemChanged(position);
    }

    public void removeItem(int position) {
        quizzes.remove(position);
        notifyItemRemoved(position);
    }

    @Override
    public int getItemCount() {
        return quizzes.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle;
        TextView tvQuestionsCount;
        TextView tvLastScore;
        TextView tvAttempts;
        TextView tvDownloadedDate;
        MaterialButton btnReview;
        MaterialButton btnRetake;
        ImageView ivFavorite;
        ImageView ivDelete;
        CardView cardView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (CardView) itemView;
            tvTitle = itemView. findViewById(R.id.tv_quiz_title);
            tvQuestionsCount = itemView.findViewById(R.id.tv_questions_count);
            tvLastScore = itemView.findViewById(R.id.tv_last_score);
            tvAttempts = itemView.findViewById(R.id.tv_attempts);
            tvDownloadedDate = itemView.findViewById(R.id.tv_downloaded_date);
            btnReview = itemView.findViewById(R.id.btn_review);
            btnRetake = itemView.findViewById(R.id.btn_retake);
            ivFavorite = itemView. findViewById(R.id.iv_favorite);
            ivDelete = itemView.findViewById(R.id. iv_delete);
        }
    }
}