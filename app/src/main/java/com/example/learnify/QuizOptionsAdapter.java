package com.example.learnify;

import android.animation.ValueAnimator;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class QuizOptionsAdapter extends RecyclerView.Adapter<QuizOptionsAdapter.OptionViewHolder> {

    private static final String TAG = "QuizOptionsAdapter";

    private final QuizQuestion question;
    private final List<String> options;
    private final QuizActivity activity;

    public QuizOptionsAdapter(QuizQuestion question, QuizActivity activity) {
        this.question = question;
        this.options = question.options;
        this.activity = activity;

        Log.d(TAG, "Adapter created with " + options.size() + " options");
    }

    @NonNull
    @Override
    public OptionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_quiz_option, parent, false);
        return new OptionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OptionViewHolder holder, int position) {
        String optionText = options.get(position);

        Log.d(TAG, "Binding option " + position + ": " + optionText);

        // Set option letter (A, B, C, D)
        holder.tvOptionLetter.setText(String.valueOf((char) ('A' + position)));

        // Set option text
        holder.tvOptionText.setText(optionText);

        // Handle click
        holder.cardOption.setOnClickListener(v -> {
            if (!question.isAnswered) {
                Log.d(TAG, "Option " + position + " clicked");

                question.selectedOptionIndex = position;
                boolean isCorrect = optionText.equals(question.correctAnswer);
                question.isCorrect = isCorrect;
                question.isAnswered = true;

                // Notify activity
                activity.onOptionSelected(position, isCorrect);

                // Refresh all options to show feedback
                notifyDataSetChanged();
            }
        });

        // Update UI based on answer state
        if (question.isAnswered) {
            holder.cardOption.setClickable(false);

            boolean isThisCorrect = optionText.equals(question.correctAnswer);
            boolean isSelected = position == question.selectedOptionIndex;

            if (isThisCorrect) {
                // This is the correct answer - always show green
                holder.ivOptionStatus.setVisibility(View.VISIBLE);
                holder.ivOptionStatus.setImageResource(R.drawable.ic_correct_check);

                holder.tvExplanation.setVisibility(View.VISIBLE);

                // Show explanation if available, otherwise default message
                if (question.explanation != null && !question.explanation.isEmpty()) {
                    holder.tvExplanation.setText("✓ " + question.explanation);
                } else {
                    holder.tvExplanation.setText("✓ Correct answer!");
                }
                holder.tvExplanation.setTextColor(Color.parseColor("#4CAF50"));

                holder.cardOption.setCardBackgroundColor(Color.parseColor("#E8F5E9"));
                holder.cardOption.setStrokeColor(Color.parseColor("#4CAF50"));

                if (isSelected) {
                    animateFeedbackIcon(holder.ivOptionStatus);
                }

            } else if (isSelected) {
                // User selected this wrong answer - show red
                holder.ivOptionStatus.setVisibility(View.VISIBLE);
                holder.ivOptionStatus.setImageResource(R.drawable.ic_wrong_x);

                holder.tvExplanation.setVisibility(View.VISIBLE);

                // Show explanation why it's wrong, or default message
                if (question.explanation != null && !question.explanation.isEmpty()) {
                    holder.tvExplanation.setText("✗ Incorrect. " + question.explanation);
                } else {
                    holder.tvExplanation.setText("✗ Incorrect. The correct answer is: " + question.correctAnswer);
                }
                holder.tvExplanation.setTextColor(Color.parseColor("#F44336"));

                holder.cardOption.setCardBackgroundColor(Color.parseColor("#FFEBEE"));
                holder.cardOption.setStrokeColor(Color.parseColor("#F44336"));

                animateFeedbackIcon(holder.ivOptionStatus);

            } else {
                // Unselected wrong options - fade out
                holder.ivOptionStatus.setVisibility(View.GONE);
                holder.tvExplanation.setVisibility(View.GONE);
                holder.cardOption.setCardBackgroundColor(Color.WHITE);
                holder.cardOption.setStrokeColor(Color.parseColor("#E0E0E0"));
                holder.cardOption.setAlpha(0.5f);
            }

        } else {
            // Reset to default state
            holder.cardOption.setClickable(true);
            holder.cardOption.setCardBackgroundColor(Color.WHITE);
            holder.cardOption.setStrokeColor(Color.parseColor("#9C27B0")); // Purple
            holder.cardOption.setAlpha(1.0f);

            holder.ivOptionStatus.setVisibility(View.INVISIBLE);
            holder.tvExplanation.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return options.size();
    }

    private void animateFeedbackIcon(View icon) {
        icon.setAlpha(0f);
        icon.setScaleX(0.5f);
        icon.setScaleY(0.5f);

        icon.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .setInterpolator(new OvershootInterpolator())
                .start();
    }

    public static class OptionViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardOption;
        TextView tvOptionLetter, tvOptionText, tvExplanation;
        ImageView ivOptionStatus;

        public OptionViewHolder(@NonNull View itemView) {
            super(itemView);
            cardOption = itemView.findViewById(R.id.card_option);
            tvOptionLetter = itemView.findViewById(R.id.tv_option_letter);
            tvOptionText = itemView.findViewById(R.id.tv_option_text);
            ivOptionStatus = itemView.findViewById(R.id.iv_option_status);
            tvExplanation = itemView.findViewById(R.id.tv_explanation);
        }
    }
}