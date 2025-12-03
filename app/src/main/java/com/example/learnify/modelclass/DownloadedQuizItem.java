package com.example.learnify.modelclass;

import java.io. Serializable;
import java.util.Date;
import java.util.List;

public class DownloadedQuizItem implements Serializable {
    public String quizId;
    public String quizTitle;
    public int totalQuestions;
    public int lastScore; // From last attempt
    public Date downloadedAt;
    public Date lastAttemptedAt;
    public boolean isFavorite;
    public List<QuizQuestion> fullQuizData;
    public int attemptCount; // How many times user attempted this quiz

    // Default constructor for Firestore
    public DownloadedQuizItem() {}

    public DownloadedQuizItem(String quizId, String quizTitle, int totalQuestions,
                              Date downloadedAt, List<QuizQuestion> fullQuizData) {
        this.quizId = quizId;
        this.quizTitle = quizTitle;
        this.totalQuestions = totalQuestions;
        this.downloadedAt = downloadedAt;
        this.fullQuizData = fullQuizData;
        this.isFavorite = false;
        this.lastScore = 0;
        this.lastAttemptedAt = null;
        this.attemptCount = 0;
    }

    // Getters and Setters
    public String getQuizId() { return quizId; }
    public String getQuizTitle() { return quizTitle; }
    public int getTotalQuestions() { return totalQuestions; }
    public int getLastScore() { return lastScore; }
    public Date getDownloadedAt() { return downloadedAt; }
    public Date getLastAttemptedAt() { return lastAttemptedAt; }
    public boolean isFavorite() { return isFavorite; }
    public List<QuizQuestion> getFullQuizData() { return fullQuizData; }
    public int getAttemptCount() { return attemptCount; }

    public void setLastScore(int score) { this. lastScore = score; }
    public void setLastAttemptedAt(Date date) { this. lastAttemptedAt = date; }
    public void setFavorite(boolean favorite) { this.isFavorite = favorite; }
    public void setAttemptCount(int count) { this.attemptCount = count; }
}