package com.example.learnify;

import android.content.Context;
import android.content.Intent;
import java.util.ArrayList;

public class QuizActivityStarter {
    public static void start(Context context, Quiz quiz) {
        Intent intent = new Intent(context, QuizActivity.class);
        // Pass questions as ArrayList
        //intent.putParcelableArrayListExtra("QUIZ_QUESTIONS", new ArrayList<>(quiz.getQuestions()));
        intent.putExtra("QUIZ_TITLE", quiz.getTitle());
        context.startActivity(intent);
    }
}
