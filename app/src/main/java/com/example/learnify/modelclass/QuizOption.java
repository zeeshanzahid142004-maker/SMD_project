package com.example.learnify.modelclass;

import java.io.Serializable;

public class QuizOption implements Serializable {
    public String text;
    public String explanation;
    public boolean isCorrect;

    public QuizOption(String text, String explanation, boolean isCorrect) {
        this.text = text;
        this.explanation = explanation;
        this.isCorrect = isCorrect;
    }

    public QuizOption(String text, boolean isCorrect) {
        this.text = text;

        this.isCorrect = isCorrect;
    }

    public QuizOption( boolean isCorrect) {

        this.isCorrect = isCorrect;
    }
    public QuizOption(){}

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public boolean isCorrect() {
        return isCorrect;
    }

    public void setCorrect(boolean correct) {
        isCorrect = correct;
    }
}