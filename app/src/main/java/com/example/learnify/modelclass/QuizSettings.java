package com.example.learnify.modelclass;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Model class for quiz customization settings
 */
public class QuizSettings {

    private static final String PREFS_NAME = "QuizSettingsPrefs";
    private static final String KEY_NUM_QUESTIONS = "num_questions";
    private static final String KEY_DIFFICULTY = "difficulty";
    private static final String KEY_INCLUDE_CODING = "include_coding";

    // Number of questions options
    public static final int QUESTIONS_3 = 3;
    public static final int QUESTIONS_5 = 5;
    public static final int QUESTIONS_10 = 10;
    public static final int QUESTIONS_15 = 15;

    // Difficulty options
    public static final String DIFFICULTY_EASY = "Easy";
    public static final String DIFFICULTY_MEDIUM = "Medium";
    public static final String DIFFICULTY_HARD = "Hard";
    public static final String DIFFICULTY_MIX = "Mix";

    private int numQuestions;
    private String difficulty;
    private boolean includeCoding;

    /**
     * Create QuizSettings with default values
     */
    public QuizSettings() {
        this.numQuestions = QUESTIONS_5;
        this.difficulty = DIFFICULTY_MIX;
        this.includeCoding = true;
    }

    /**
     * Create QuizSettings with custom values
     */
    public QuizSettings(int numQuestions, String difficulty, boolean includeCoding) {
        this.numQuestions = numQuestions;
        this.difficulty = difficulty;
        this.includeCoding = includeCoding;
    }

    // Getters
    public int getNumQuestions() {
        return numQuestions;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public boolean isIncludeCoding() {
        return includeCoding;
    }

    // Setters
    public void setNumQuestions(int numQuestions) {
        this.numQuestions = numQuestions;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public void setIncludeCoding(boolean includeCoding) {
        this.includeCoding = includeCoding;
    }

    /**
     * Save settings to SharedPreferences
     */
    public void save(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putInt(KEY_NUM_QUESTIONS, numQuestions)
                .putString(KEY_DIFFICULTY, difficulty)
                .putBoolean(KEY_INCLUDE_CODING, includeCoding)
                .apply();
    }

    /**
     * Load settings from SharedPreferences
     */
    public static QuizSettings load(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        QuizSettings settings = new QuizSettings();
        settings.numQuestions = prefs.getInt(KEY_NUM_QUESTIONS, QUESTIONS_5);
        settings.difficulty = prefs.getString(KEY_DIFFICULTY, DIFFICULTY_MIX);
        settings.includeCoding = prefs.getBoolean(KEY_INCLUDE_CODING, true);
        return settings;
    }

    /**
     * Get array of available question count options
     */
    public static int[] getQuestionCountOptions() {
        return new int[]{QUESTIONS_3, QUESTIONS_5, QUESTIONS_10, QUESTIONS_15};
    }

    /**
     * Get array of available difficulty options
     */
    public static String[] getDifficultyOptions() {
        return new String[]{DIFFICULTY_EASY, DIFFICULTY_MEDIUM, DIFFICULTY_HARD, DIFFICULTY_MIX};
    }

    @Override
    public String toString() {
        return "QuizSettings{" +
                "numQuestions=" + numQuestions +
                ", difficulty='" + difficulty + '\'' +
                ", includeCoding=" + includeCoding +
                '}';
    }
}
