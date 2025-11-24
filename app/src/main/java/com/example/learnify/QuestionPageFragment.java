package com.example.learnify;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Locale;

public class QuestionPageFragment extends Fragment {

    private static final String TAG = "QuestionPageFragment";

    private QuizQuestion question;
    private int questionNumber;
    private int totalCount;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            question = (QuizQuestion) getArguments().getSerializable("QUESTION");
            totalCount = getArguments().getInt("TOTAL_COUNT");

            if (question != null) {
                questionNumber = question.id;
                Log.d(TAG, "Fragment created for question " + questionNumber + ": " + question.questionText);
            } else {
                Log.e(TAG, "Question is null!");
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView called for question " + questionNumber);
        return inflater.inflate(R.layout.item_question_page, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (question == null) {
            Log.e(TAG, "Cannot setup view - question is null!");
            return;
        }

        Log.d(TAG, "Setting up view for question " + questionNumber);

        // Find Views
        TextView tvQuestionNumber = view.findViewById(R.id.tv_question_number);
        TextView tvQuestionText = view.findViewById(R.id.tv_question_text);
        TextView tvDifficulty = view.findViewById(R.id.tv_difficulty_indicator);
        RecyclerView rvOptions = view.findViewById(R.id.rv_options);

        // Set question number
        tvQuestionNumber.setText(String.format(Locale.getDefault(), "%d / %d", questionNumber, totalCount));

        // Set question text
        tvQuestionText.setText(question.questionText);

        // Set difficulty badge
        if (question.difficulty != null) {
            tvDifficulty.setText(question.difficulty);

            // Set background based on difficulty
            switch (question.difficulty) {
                case "EASY":
                    tvDifficulty.setBackgroundResource(R.drawable.bg_difficulty_easy);
                    break;
                case "HARD":
                    tvDifficulty.setBackgroundResource(R.drawable.bg_difficulty_hard);
                    break;
                default: // NORMAL
                    tvDifficulty.setBackgroundResource(R.drawable.bg_difficulty_normal);
                    break;
            }
        } else {
            tvDifficulty.setText("NORMAL");
            tvDifficulty.setBackgroundResource(R.drawable.bg_difficulty_normal);
        }

        // Setup options RecyclerView
        if (question.type != null && question.type.equals("CODING")) {
            // This is a coding question - show different UI
            Log.d(TAG, "⚠️ This is a CODING question - marking as skippable");

            rvOptions.setVisibility(View.GONE);

            // Just add coding badge to question text
            tvQuestionText.append("\n\n💻 This is a coding exercise. You can skip it for now and practice later.");

            // Mark as answered so user can skip
            question.isAnswered = true;
            question.isCorrect = false; // Will be counted as incorrect/skipped

        } else if (question.options != null && !question.options.isEmpty()) {
            Log.d(TAG, "Setting up " + question.options.size() + " options");

            rvOptions.setVisibility(View.VISIBLE);
            QuizOptionsAdapter optionsAdapter = new QuizOptionsAdapter(question, (QuizActivity) requireActivity());
            rvOptions.setLayoutManager(new LinearLayoutManager(getContext()));
            rvOptions.setNestedScrollingEnabled(false);
            rvOptions.setAdapter(optionsAdapter);
        } else {
            Log.e(TAG, "No options available for this question!");
            rvOptions.setVisibility(View.GONE);
        }

        Log.d(TAG, "View setup complete for question " + questionNumber);
    }

    public QuizQuestion getQuestion() {
        return question;
    }

    private void openCodingExercise() {
        Log.d(TAG, "Opening coding exercise");
        Intent intent = new Intent(requireActivity(), CodingExerciseActivity.class);
        intent.putExtra("QUESTION", question);
        startActivity(intent);
    }
}