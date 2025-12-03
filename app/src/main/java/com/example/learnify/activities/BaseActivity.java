package com.example.learnify.activities;

import android.content.Context;

import androidx.appcompat.app.AppCompatActivity;

import com.example.learnify.managers.LanguageManager;

/**
 * Base activity that applies the selected language to all activities.
 * All activities should extend this class to ensure consistent language handling.
 */
public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        // Wrap the context with the selected locale
        Context wrappedContext = LanguageManager.getInstance(newBase).wrapContext(newBase);
        super.attachBaseContext(wrappedContext);
    }
}
