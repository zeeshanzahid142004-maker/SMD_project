package com.example.learnify.helpers;

import android.content.Context;
import android.content.Intent;

import com.example.learnify.modelclass.Quiz;
import com.example.learnify.activities.QuizActivity;

public class QuizActivityStarter {
    public static void start(Context context, Quiz quiz) {
        Intent intent = new Intent(context, QuizActivity.class);
        // Pass questions as ArrayList
        //intent.putParcelableArrayListExtra("QUIZ_QUESTIONS", new ArrayList<>(quiz.getQuestions()));
        intent.putExtra("QUIZ_TITLE", quiz.getTitle());
        context.startActivity(intent);
    }
}
