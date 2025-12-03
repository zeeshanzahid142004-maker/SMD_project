package com.example.learnify.modelclass;

import java.io.Serializable;

public class RecommendationItem implements Serializable {

    public String title;        // e.g., "Loops", "Variables", "Pointers"
    public String description;  // "Review this topic"
    public String type;         // "CONCEPT" or "CODING"
    public QuizQuestion question;

    public RecommendationItem() {}

    public RecommendationItem(String title, String description, String type, QuizQuestion question) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.question = question;
    }
}
