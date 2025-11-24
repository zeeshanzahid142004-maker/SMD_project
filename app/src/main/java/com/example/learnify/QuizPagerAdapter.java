package com.example.learnify;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.List;

public class QuizPagerAdapter extends FragmentStateAdapter {

    public interface OnOptionSelectedListener {
        void onOptionSelected(int questionIndex, boolean isCorrect);
    }

    private final List<QuizQuestion> questions;
    private final OnOptionSelectedListener listener;

    public QuizPagerAdapter(
            @NonNull FragmentActivity activity,
            List<QuizQuestion> questions,
            OnOptionSelectedListener listener
    ) {
        super(activity);
        this.questions = questions;
        this.listener = listener;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        QuestionPageFragment fragment = new QuestionPageFragment();

        Bundle b = new Bundle();
        b.putSerializable("QUESTION", questions.get(position));
        b.putInt("POSITION", position);
        b.putInt("TOTAL_COUNT", questions.size());
        fragment.setArguments(b);

        return fragment;
    }

    @Override
    public int getItemCount() {
        return questions.size();
    }
}
