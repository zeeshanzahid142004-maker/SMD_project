package com.example.learnify;

import java.io.Serializable;
import java.util.List;

public class QuizQuestion implements Serializable {
    public String questionText;
    public String difficulty;
    public float timeLimit;
    public String type; // "MCQ" or "CODING"

    // --- CHANGED: Simple Strings for AI compatibility ---
    public List<String> options;
    public String correctAnswer; // We will just match the string

    // --- Coding fields ---
    public String codingPrompt;
    public String starterCode;
    public String expectedOutput;

    // --- State ---
    public int selectedOptionIndex = -1;
    public boolean isAnswered = false;
    public boolean isCorrect = false;
    public int id;
public String explanation;
    public QuizQuestion() {}

    public boolean isCodingQuestion() {
        return "CODING".equalsIgnoreCase(type);
    }
}