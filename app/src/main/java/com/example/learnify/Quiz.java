package com.example.learnify;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.List;

public class Quiz {

    private String quizId; // The document ID
    private String title;
    private int totalMarks;
    private float totalTimeMinutes;
    private String thumbnailUrl;
    private List<QuizQuestion> questions;

    @ServerTimestamp // Automatically sets the date on the server
    private Date dateCreated;

    // Default constructor needed for Firestore
    public Quiz() {}

    public Quiz(String quizId, String title, int totalMarks, float totalTimeMinutes, String thumbnailUrl, List<QuizQuestion> questions) {
        this.quizId = quizId;
        this.title = title;
        this.totalMarks = totalMarks;
        this.totalTimeMinutes = totalTimeMinutes;
        this.thumbnailUrl = thumbnailUrl;
        this.questions = questions;
    }

    public void setQuizId(String quizId) {
        this.quizId = quizId;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setTotalMarks(int totalMarks) {
        this.totalMarks = totalMarks;
    }

    public void setTotalTimeMinutes(float totalTimeMinutes) {
        this.totalTimeMinutes = totalTimeMinutes;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public void setQuestions(List<QuizQuestion> questions) {
        this.questions = questions;
    }

    public void setDateCreated(Date dateCreated) {
        this.dateCreated = dateCreated;
    }

    // --- Getters (Needed for Firestore to read the data) ---
    public String getQuizId() { return quizId; }
    public String getTitle() { return title; }
    public int getTotalMarks() { return totalMarks; }
    public float getTotalTimeMinutes() { return totalTimeMinutes; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public List<QuizQuestion> getQuestions() { return questions; }
    public Date getDateCreated() { return dateCreated; }
}