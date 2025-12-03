package com.example.learnify.adapters;

import android.content.res.Configuration;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.learnify.modelclass.QuizQuestion;
import com.example.learnify.R;
import com.example.learnify.activities.QuizActivity;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class QuizOptionsAdapter extends RecyclerView.Adapter<QuizOptionsAdapter.OptionViewHolder> {

    private static final String TAG = "QuizOptionsAdapter";

    private final QuizQuestion question;
    private final List<String> options;
    private final QuizActivity activity;

    // Dynamic Colors (Not final anymore)
    private int COLOR_DEFAULT_BG;
    private int COLOR_TEXT_DEFAULT;

    // Constant Colors (Feedback colors look good in both modes)
    private final int COLOR_CORRECT_BG = Color.parseColor("#E8F5E9"); // Light Green
    private final int COLOR_WRONG_BG = Color.parseColor("#FFEBEE");   // Light Red
    private final int COLOR_TEXT_FEEDBACK = Color.BLACK; // Always Black on light feedback cards

    public QuizOptionsAdapter(QuizQuestion question, QuizActivity activity) {
        this.question = question;
        this.options = question.options;
        this.activity = activity;

        // ✅ DETECT THEME (Light vs Dark)
        int nightModeFlags = activity.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        boolean isDarkMode = nightModeFlags == Configuration.UI_MODE_NIGHT_YES;

        if (isDarkMode) {
            // Dark Mode Colors
            COLOR_DEFAULT_BG = Color.parseColor("#2D2D2D"); // Dark Grey Card
            COLOR_TEXT_DEFAULT = Color.WHITE;               // White Text
        } else {
            // Light Mode Colors
            COLOR_DEFAULT_BG = Color.WHITE;                 // White Card
            COLOR_TEXT_DEFAULT = Color.parseColor("#1F1F1F"); // Dark Grey Text
        }
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

        // Set option letter (A, B, C, D)
        holder.tvOptionLetter.setText(String.valueOf((char) ('A' + position)));

        // Set option text
        holder.tvOptionText.setText(optionText);

        // Handle click
        holder.cardOption.setOnClickListener(v -> {
            if (!question.isAnswered) {
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
                // Correct Answer (Green)
                setupOptionStyle(holder, COLOR_CORRECT_BG, COLOR_TEXT_FEEDBACK,
                        Color.parseColor("#4CAF50"), View.VISIBLE);

                holder.ivOptionStatus.setImageResource(R.drawable.ic_correct_check);
                holder.tvExplanation.setVisibility(View.VISIBLE);

                String explanation = (question.explanation != null && !question.explanation.isEmpty())
                        ? "✓ " + question.explanation
                        : "✓ Correct answer!";
                holder.tvExplanation.setText(explanation);
                holder.tvExplanation.setTextColor(Color.parseColor("#4CAF50"));

                if (isSelected) animateFeedbackIcon(holder.ivOptionStatus);

            } else if (isSelected) {
                // Wrong Selection (Red)
                setupOptionStyle(holder, COLOR_WRONG_BG, COLOR_TEXT_FEEDBACK,
                        Color.parseColor("#F44336"), View.VISIBLE);

                holder.ivOptionStatus.setImageResource(R.drawable.ic_wrong_x);
                holder.tvExplanation.setVisibility(View.VISIBLE);

                String explanation = (question.explanation != null && !question.explanation.isEmpty())
                        ? "✗ Incorrect. " + question.explanation
                        : "✗ Incorrect. The correct answer is: " + question.correctAnswer;
                holder.tvExplanation.setText(explanation);
                holder.tvExplanation.setTextColor(Color.parseColor("#F44336"));

                animateFeedbackIcon(holder.ivOptionStatus);

            } else {
                // Unselected (Fade out)
                setupOptionStyle(holder, COLOR_DEFAULT_BG, COLOR_TEXT_DEFAULT,
                        Color.parseColor("#E0E0E0"), View.GONE);
                holder.tvExplanation.setVisibility(View.GONE);
                holder.cardOption.setAlpha(0.5f);
            }

        } else {
            // Default State (Ready to answer)
            // Uses the dynamic colors set in the constructor
            setupOptionStyle(holder, COLOR_DEFAULT_BG, COLOR_TEXT_DEFAULT,
                    Color.parseColor("#9C27B0"), View.INVISIBLE);
            holder.tvExplanation.setVisibility(View.GONE);
            holder.cardOption.setClickable(true);
            holder.cardOption.setAlpha(1.0f);
        }
    }

    // Helper to reduce code duplication and ensure text colors are correct
    private void setupOptionStyle(OptionViewHolder holder, int bgColor, int textColor, int strokeColor, int iconVisibility) {
        holder.cardOption.setCardBackgroundColor(bgColor);
        holder.tvOptionText.setTextColor(textColor);
        holder.cardOption.setStrokeColor(strokeColor);
        holder.ivOptionStatus.setVisibility(iconVisibility);
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