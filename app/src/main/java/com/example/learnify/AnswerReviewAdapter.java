package com.example.learnify;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Locale;

public class AnswerReviewAdapter extends RecyclerView.Adapter<AnswerReviewAdapter.ReviewViewHolder> {

    private List<QuestionResult> results;
    private Context context;

    public AnswerReviewAdapter(List<QuestionResult> results) {
        this.results = results;
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_answer_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        QuestionResult result = results.get(position);

        holder.tvQuestionNumber.setText(String.format(Locale.getDefault(), "Question %d", result.getQuestionNumber()));
        holder.tvQuestionText.setText(result.getQuestionText());
        holder.tvYourAnswer.setText(result.getYourAnswer());

        if (result.isCorrect()) {
            holder.ivAnswerStatus.setImageResource(R.drawable.ic_correct_check);
            holder.ivAnswerStatus.setImageTintList(ContextCompat.getColorStateList(context, R.color.figma_green_correct));
            holder.tvYourAnswer.setTextColor(ContextCompat.getColor(context, R.color.text_secondary_gray));
            holder.llCorrectAnswer.setVisibility(View.GONE);
        } else {
            holder.ivAnswerStatus.setImageResource(R.drawable.ic_wrong_x);
            holder.ivAnswerStatus.setImageTintList(ContextCompat.getColorStateList(context, R.color.figma_red_error));
            holder.tvYourAnswer.setTextColor(ContextCompat.getColor(context, R.color.figma_red_error));

            // Show the correct answer section
            holder.llCorrectAnswer.setVisibility(View.VISIBLE);
            holder.tvCorrectAnswer.setText(result.getCorrectAnswer());
        }
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        TextView tvQuestionNumber, tvQuestionText, tvYourAnswer, tvCorrectAnswer;
        ImageView ivAnswerStatus;
        LinearLayout llCorrectAnswer;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            tvQuestionNumber = itemView.findViewById(R.id.tv_question_number);
            tvQuestionText = itemView.findViewById(R.id.tv_question_text);
            tvYourAnswer = itemView.findViewById(R.id.tv_your_answer);
            tvCorrectAnswer = itemView.findViewById(R.id.tv_correct_answer);
            ivAnswerStatus = itemView.findViewById(R.id.iv_answer_status);
            llCorrectAnswer = itemView.findViewById(R.id.ll_correct_answer);
        }
    }
}