package com.example.learnify;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Adapter for displaying quiz questions in review mode.
 * Shows question text, user's answer, correct answer, and explanation.
 */
public class QuizReviewAdapter extends RecyclerView.Adapter<QuizReviewAdapter.ViewHolder> {

    private final List<QuizQuestion> questions;

    public QuizReviewAdapter(List<QuizQuestion> questions) {
        this.questions = questions;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_review_question, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        QuizQuestion question = questions.get(position);
        holder.bind(question, position + 1);
    }

    @Override
    public int getItemCount() {
        return questions != null ? questions.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvQuestionNumber;
        TextView tvQuestionText;
        TextView tvUserAnswerLabel;
        TextView tvUserAnswer;
        TextView tvCorrectAnswerLabel;
        TextView tvCorrectAnswer;
        TextView tvExplanationLabel;
        TextView tvExplanation;
        LinearLayout layoutExplanation;
        View statusIndicator;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvQuestionNumber = itemView.findViewById(R.id.tv_question_number);
            tvQuestionText = itemView.findViewById(R.id.tv_question_text);
            tvUserAnswerLabel = itemView.findViewById(R.id.tv_user_answer_label);
            tvUserAnswer = itemView.findViewById(R.id.tv_user_answer);
            tvCorrectAnswerLabel = itemView.findViewById(R.id.tv_correct_answer_label);
            tvCorrectAnswer = itemView.findViewById(R.id.tv_correct_answer);
            tvExplanationLabel = itemView.findViewById(R.id.tv_explanation_label);
            tvExplanation = itemView.findViewById(R.id.tv_explanation);
            layoutExplanation = itemView.findViewById(R.id.layout_explanation);
            statusIndicator = itemView.findViewById(R.id.status_indicator);
        }

        void bind(QuizQuestion question, int questionNumber) {
            // Question number
            tvQuestionNumber.setText("Question " + questionNumber);

            // Question text
            if (question.questionText != null && !question.questionText.isEmpty()) {
                tvQuestionText.setText(question.questionText);
            } else if (question.codingPrompt != null && !question.codingPrompt.isEmpty()) {
                tvQuestionText.setText(question.codingPrompt);
            } else {
                tvQuestionText.setText("Question text not available");
            }

            // User's answer
            String userAnswer = "Not answered";
            if (question.isAnswered) {
                if (question.selectedOptionIndex >= 0 && question.options != null 
                        && question.selectedOptionIndex < question.options.size()) {
                    userAnswer = question.options.get(question.selectedOptionIndex);
                } else if (question.isCodingQuestion()) {
                    userAnswer = question.isCorrect ? "Code submitted successfully" : "Code not submitted";
                }
            }
            tvUserAnswer.setText(userAnswer);

            // Color code user's answer based on correctness
            if (question.isAnswered) {
                if (question.isCorrect) {
                    tvUserAnswer.setTextColor(ContextCompat.getColor(itemView.getContext(), 
                            android.R.color.holo_green_dark));
                    statusIndicator.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), 
                            android.R.color.holo_green_dark));
                } else {
                    tvUserAnswer.setTextColor(ContextCompat.getColor(itemView.getContext(), 
                            android.R.color.holo_red_dark));
                    statusIndicator.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), 
                            android.R.color.holo_red_dark));
                }
            } else {
                tvUserAnswer.setTextColor(ContextCompat.getColor(itemView.getContext(), 
                        R.color.text_secondary_gray));
                statusIndicator.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), 
                        R.color.gray));
            }

            // Correct answer
            String correctAnswer = "Not available";
            if (question.correctAnswer != null && !question.correctAnswer.isEmpty()) {
                correctAnswer = question.correctAnswer;
            } else if (question.isCodingQuestion() && question.expectedOutput != null) {
                correctAnswer = "Expected output: " + question.expectedOutput;
            }
            tvCorrectAnswer.setText(correctAnswer);
            tvCorrectAnswer.setTextColor(ContextCompat.getColor(itemView.getContext(), 
                    android.R.color.holo_green_dark));

            // Explanation (if available)
            if (question.explanation != null && !question.explanation.isEmpty()) {
                layoutExplanation.setVisibility(View.VISIBLE);
                tvExplanation.setText(question.explanation);
            } else {
                layoutExplanation.setVisibility(View.GONE);
            }
        }
    }
}
