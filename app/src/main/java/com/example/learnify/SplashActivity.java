package com.example.learnify;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen);


        mAuth = FirebaseAuth.getInstance();
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Check if user is signed in (non-null)
        FirebaseUser currentUser = mAuth.getCurrentUser();


        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            if (currentUser != null) {
                // User IS already signed in, go directly to Main App
                Log.d("SplashActivity", "User is signed in: " + currentUser.getUid());
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(intent);
            } else {
                // User is NOT signed in, go to Welcome Page
                Log.d("SplashActivity", "User is null, going to Welcome Page");
                Intent intent = new Intent(SplashActivity.this, OnboardingActivity.class);
                startActivity(intent);
            }

            // Finish this SplashActivity so the user can't press "back" to it
            finish();

        }, 2000);
    }
}