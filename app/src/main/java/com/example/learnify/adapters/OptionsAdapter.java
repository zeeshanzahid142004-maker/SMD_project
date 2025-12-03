package com.example.learnify.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.learnify.modelclass.QuizOption;
import com.example.learnify.modelclass.QuizQuestion;
import com.example.learnify.R;

import java.util.List;

public class OptionsAdapter extends RecyclerView.Adapter<OptionsAdapter.OptionViewHolder> {

    private final List<QuizOption> options;
    private final QuizQuestion question;
    private final int questionPosition;
    private final QuizPagerAdapter.OnOptionSelectedListener listener;

    public OptionsAdapter(List<QuizOption> options, QuizQuestion question, int questionPosition, QuizPagerAdapter.OnOptionSelectedListener listener) {
        this.options = options;
        this.question = question;
        this.questionPosition = questionPosition;
        this.listener = listener;
    }

    @NonNull
    @Override
    public OptionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_quiz_option, parent, false);
        return new OptionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OptionViewHolder holder, int position) {
        holder.bind(options.get(position), position);
    }

    @Override
    public int getItemCount() {
        return options.size();
    }

    class OptionViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvLetter, tvText, tvExplanation;
        private final ImageView ivStatus;

        public OptionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLetter = itemView.findViewById(R.id.tv_option_letter);
            tvText = itemView.findViewById(R.id.tv_option_text);
            tvExplanation = itemView.findViewById(R.id.tv_explanation);
            ivStatus = itemView.findViewById(R.id.iv_option_status);
        }

        public void bind(QuizOption option, int position) {
            tvLetter.setText(Character.toString((char) ('A' + position)));
            tvText.setText(option.text);

            // Reset
            ivStatus.setVisibility(View.INVISIBLE);
            tvExplanation.setVisibility(View.GONE);
            itemView.setClickable(!question.isAnswered);

            itemView.setOnClickListener(v -> {
                if (question.isAnswered) return;

                question.selectedOptionIndex = position;
                question.isAnswered = true;
                question.isCorrect = option.isCorrect;

                // Show status
                ivStatus.setVisibility(View.VISIBLE);
                if (option.isCorrect) {
                    ivStatus.setImageResource(R.drawable.ic_correct_check);
                } else {
                    ivStatus.setImageResource(R.drawable.ic_wrong_x);
                }

                tvExplanation.setVisibility(option.explanation != null ? View.VISIBLE : View.GONE);
                if (option.explanation != null) tvExplanation.setText(option.explanation);

                listener.onOptionSelected(questionPosition, option.isCorrect);

                notifyDataSetChanged();
            });

            // Show pre-selected if already answered
            if (question.isAnswered) {
                ivStatus.setVisibility(View.VISIBLE);
                if (position == question.selectedOptionIndex) {
                    ivStatus.setImageResource(option.isCorrect ? R.drawable.ic_correct_check : R.drawable.ic_wrong_x);
                } else if (option.isCorrect) {
                    ivStatus.setImageResource(R.drawable.ic_correct_check);
                    ivStatus.setVisibility(View.VISIBLE);
                }
                if (option.explanation != null) {
                    tvExplanation.setVisibility(View.VISIBLE);
                    tvExplanation.setText(option.explanation);
                }
            }
        }
    }
}
