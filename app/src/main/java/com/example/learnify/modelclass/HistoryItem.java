package com.example.learnify.modelclass;

import java.util.Date;

public class HistoryItem {
    private String quizId;
    private String title;
    private int score;
    private int totalMarks;
    private Date dateTaken;

    public HistoryItem() { } // Required for Firestore

    public HistoryItem(String quizId, String title, int score, int totalMarks, Date dateTaken) {
        this.quizId = quizId;
        this.title = title;
        this.score = score;
        this.totalMarks = totalMarks;
        this.dateTaken = dateTaken;
    }

    public String getQuizId() { return quizId; }
    public String getTitle() { return title; }
    public int getScore() { return score; }
    public int getTotalMarks() { return totalMarks; }
    public Date getDateTaken() { return dateTaken; }
}