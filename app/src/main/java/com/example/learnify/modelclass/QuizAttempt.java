package com.example.learnify.modelclass;

import java.io.Serializable;
import java.util.Date;

public class QuizAttempt implements Serializable {
    public String attemptId;
    public String quizId;
    public String quizTitle;
    public int score;
    public int totalQuestions;
    public int percentage;
    public Date attemptedAt;
    public boolean isDownloaded;
    public boolean isFavorite;
    public int attemptNumber = 1; // Track retake number (1 = first attempt)

    // Default constructor for Firestore
    public QuizAttempt() {}

    public QuizAttempt(String attemptId, String quizId, String quizTitle, int score,
                       int totalQuestions, Date attemptedAt) {
        this.attemptId = attemptId;
        this.quizId = quizId;
        this.quizTitle = quizTitle;
        this.score = score;
        this.totalQuestions = totalQuestions;
        this.percentage = totalQuestions > 0 ? (score * 100 / totalQuestions) : 0;
        this.attemptedAt = attemptedAt;
        this.isDownloaded = false;
        this.isFavorite = false;
        this.attemptNumber = 1;
    }

    // Getters
    public String getAttemptId() { return attemptId; }
    public String getQuizId() { return quizId; }
    public String getQuizTitle() { return quizTitle; }
    public int getScore() { return score; }
    public int getTotalQuestions() { return totalQuestions; }
    public int getPercentage() { return percentage; }
    public Date getAttemptedAt() { return attemptedAt; }
    public boolean isDownloaded() { return isDownloaded; }
    public boolean isFavorite() { return isFavorite; }
    public int getAttemptNumber() { return attemptNumber; }

    // Setters
    public void setDownloaded(boolean downloaded) { this.isDownloaded = downloaded; }
    public void setFavorite(boolean favorite) { this.isFavorite = favorite; }
    public void setAttemptNumber(int attemptNumber) { this.attemptNumber = attemptNumber; }
}