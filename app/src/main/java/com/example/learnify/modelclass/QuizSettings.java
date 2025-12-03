package com.example.learnify.modelclass;

import java.io.Serializable;

/**
 * Model class for quiz customization settings
 */
public class QuizSettings implements Serializable {

    public enum Difficulty {
        EASY("EASY"),
        MEDIUM("MEDIUM"),
        HARD("HARD"),
        MIX("MIX");

        private final String value;

        Difficulty(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    private int numberOfQuestions;
    private Difficulty difficulty;
    private boolean includeCodingQuestions;
    private String language;
    private String languageCode;

    public QuizSettings() {
        // Default settings
        this.numberOfQuestions = 5;
        this.difficulty = Difficulty.MIX;
        this.includeCodingQuestions = false;
        this.language = "English";
        this.languageCode = "en";
    }

    public QuizSettings(int numberOfQuestions, Difficulty difficulty, 
                        boolean includeCodingQuestions, String language, String languageCode) {
        this.numberOfQuestions = numberOfQuestions;
        this.difficulty = difficulty;
        this.includeCodingQuestions = includeCodingQuestions;
        this.language = language;
        this.languageCode = languageCode;
    }

    // Getters and Setters
    public int getNumberOfQuestions() {
        return numberOfQuestions;
    }

    public void setNumberOfQuestions(int numberOfQuestions) {
        this.numberOfQuestions = numberOfQuestions;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public boolean isIncludeCodingQuestions() {
        return includeCodingQuestions;
    }

    public void setIncludeCodingQuestions(boolean includeCodingQuestions) {
        this.includeCodingQuestions = includeCodingQuestions;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

    /**
     * Get prompt instruction for number of questions
     */
    public String getQuestionCountInstruction() {
        return "Generate exactly " + numberOfQuestions + " quiz questions.";
    }

    /**
     * Get prompt instruction for difficulty
     */
    public String getDifficultyInstruction() {
        if (difficulty == Difficulty.MIX) {
            return "Mix difficulty levels: include EASY, NORMAL, and HARD questions.";
        }
        return "ALL questions should be " + difficulty.getValue() + " difficulty.";
    }

    /**
     * Get prompt instruction for coding questions
     */
    public String getCodingInstruction() {
        if (includeCodingQuestions) {
            return "Include at least 1-2 coding questions with type 'CODING'.";
        }
        return "Do NOT include any coding questions. Use ONLY 'MCQ' type questions.";
    }

    /**
     * Get prompt instruction for language
     */
    public String getLanguageInstruction() {
        return "Generate ALL quiz content (questions, options, answers) in " + 
               language.toUpperCase() + " language.";
    }
}
