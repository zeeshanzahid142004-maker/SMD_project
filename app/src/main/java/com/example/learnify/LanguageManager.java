package com.example.learnify;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Manages app-wide language settings
 */
public class LanguageManager {

    private static final String TAG = "LanguageManager";
    private static final String PREF_NAME = "LanguagePrefs";
    private static final String KEY_LANGUAGE = "selected_language";
    private static final String KEY_LANGUAGE_CODE = "language_code";

    private static LanguageManager instance;
    private SharedPreferences prefs;
    private String currentLanguage;
    private String currentLanguageCode;

    // Supported languages
    public static class Language {
        public String name;
        public String code;
        public String nativeName;

        public Language(String name, String code, String nativeName) {
            this.name = name;
            this.code = code;
            this.nativeName = nativeName;
        }
    }

    private LanguageManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        currentLanguage = prefs.getString(KEY_LANGUAGE, "English");
        currentLanguageCode = prefs.getString(KEY_LANGUAGE_CODE, "en");
        Log.d(TAG, "✅ Language Manager initialized: " + currentLanguage);
    }

    public static synchronized LanguageManager getInstance(Context context) {
        if (instance == null) {
            instance = new LanguageManager(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Get list of supported languages
     */
    public String getLanguageCode() {
        return currentLanguageCode;
    }

    public static List<Language> getSupportedLanguages() {
        List<Language> languages = new ArrayList<>();
        languages.add(new Language("English", "en", "English"));
        languages.add(new Language("Urdu", "ur", "اردو"));
        languages.add(new Language("Arabic", "ar", "العربية"));
        languages.add(new Language("Spanish", "es", "Español"));
        languages.add(new Language("French", "fr", "Français"));
        languages.add(new Language("German", "de", "Deutsch"));
        languages.add(new Language("Portuguese", "pt", "Português"));
        languages.add(new Language("Italian", "it", "Italiano"));
        languages.add(new Language("Russian", "ru", "Русский"));
        languages.add(new Language("Chinese", "zh", "中文"));
        languages.add(new Language("Japanese", "ja", "日本語"));
        languages.add(new Language("Korean", "ko", "한국어"));
        languages.add(new Language("Hindi", "hi", "हिन्दी"));
        languages.add(new Language("Turkish", "tr", "Türkçe"));
        languages.add(new Language("Dutch", "nl", "Nederlands"));
        languages.add(new Language("Polish", "pl", "Polski"));
        return languages;
    }

    /**
     * Set the app language and save to preferences
     */
    public void setLanguage(Context context, String languageName, String languageCode) {
        currentLanguage = languageName;
        currentLanguageCode = languageCode;

        // Save to SharedPreferences
        prefs.edit()
                .putString(KEY_LANGUAGE, languageName)
                .putString(KEY_LANGUAGE_CODE, languageCode)
                .commit(); // Use commit() for immediate persistence

        Log.d(TAG, "🌍 Language changed to: " + languageName + " (" + languageCode + ")");
    }

    /**
     * Set language and recreate activity to apply changes
     */
    public void setLanguageAndRecreate(Activity activity, String languageName, String languageCode) {
        setLanguage(activity, languageName, languageCode);
        activity.recreate();
    }

    /**
     * Get current language name
     */
    public String getCurrentLanguage() {
        return currentLanguage;
    }

    /**
     * Get current language code
     */
    public String getCurrentLanguageCode() {
        return currentLanguageCode;
    }

    /**
     * Wrap context with the selected locale configuration.
     * Use this in attachBaseContext() of activities.
     */
    public Context wrapContext(Context context) {
        String languageCode = prefs.getString(KEY_LANGUAGE_CODE, "en");
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);
        config.setLayoutDirection(locale);

        return context.createConfigurationContext(config);
    }

    /**
     * Update app locale (legacy method for compatibility)
     */
    public void updateLocale(Context context, String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources resources = context.getResources();
        Configuration config = new Configuration(resources.getConfiguration());
        config.setLocale(locale);
        config.setLayoutDirection(locale);

        context.createConfigurationContext(config);
        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }

    /**
     * Get language for quiz generation prompt
     */
    public String getQuizLanguageInstruction() {
        return "Generate ALL quiz questions, options, and answers in " +
                currentLanguage.toUpperCase() + " language.";
    }

    /**
     * Check if language is RTL (Right-to-Left)
     */
    public boolean isRTL() {
        return currentLanguageCode.equals("ar") ||
                currentLanguageCode.equals("he") ||
                currentLanguageCode.equals("fa") ||
                currentLanguageCode.equals("ur");
    }

    /**
     * Apply language to all app components
     */
    public void applyLanguage(Context context) {
        updateLocale(context, currentLanguageCode);
    }
}