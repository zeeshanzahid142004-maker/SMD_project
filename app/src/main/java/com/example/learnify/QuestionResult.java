package com.example.learnify;

import java.io.Serializable;

// Implement Serializable so you can pass this list in an Intent
public class QuestionResult implements Serializable {
    private int questionNumber;
    private String relatedTopicId; // e.g., "for_loops", "variables"
    private String questionText;
    private String yourAnswer;
    private String correctAnswer;
    private boolean isCorrect;

    public QuestionResult(int questionNumber, String relatedTopicId, String questionText, String yourAnswer, String correctAnswer, boolean isCorrect) {
        this.questionNumber = questionNumber;
        this.relatedTopicId = relatedTopicId;
        this.questionText = questionText;
        this.yourAnswer = yourAnswer;
        this.correctAnswer = correctAnswer;
        this.isCorrect = isCorrect;
    }

    // Getters
    public int getQuestionNumber() { return questionNumber; }
    public String getRelatedTopicId() { return relatedTopicId; }
    public String getQuestionText() { return questionText; }
    public String getYourAnswer() { return yourAnswer; }
    public String getCorrectAnswer() { return correctAnswer; }
    public boolean isCorrect() { return isCorrect; }
}