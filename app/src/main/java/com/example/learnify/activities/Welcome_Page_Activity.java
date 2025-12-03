package com.example.learnify.activities; // Make sure this package name matches yours

import android.content.Intent;
import android.os.Bundle;

import com.example.learnify.R;
import com.google.android.material.button.MaterialButton;

public class Welcome_Page_Activity extends BaseActivity {

    MaterialButton signInBtn;
    MaterialButton signUpBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.welcome_page);

        // The SharedPreferences check is REMOVED from here.
        // This activity's only job is to show the buttons.

        init();
    }

    void init(){
        signInBtn = findViewById(R.id.sign_in_button);
        signUpBtn = findViewById(R.id.sign_up_button);

        signInBtn.setOnClickListener(v -> {
            // Corrected: Use your 'signin_activity.class' name
            Intent i = new Intent(Welcome_Page_Activity.this, signin_activity.class);
            startActivity(i);
            finish(); // Finish this activity
        });

        signUpBtn.setOnClickListener(v -> {
            // Corrected: Use your 'signup_activity.class' name
            Intent i = new Intent(Welcome_Page_Activity.this, signup_activity.class);
            startActivity(i);
            finish(); // Finish this activity
        });
    }
}