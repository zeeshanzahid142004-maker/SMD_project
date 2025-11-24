package com.example.learnify;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

public class OnboardingActivity extends AppCompatActivity {

    private View dot1, dot2, dot3;
    private MaterialButton nextButton;
    private FrameLayout backButton;
    private TextView titleText, bodyText;

    private int currentPage = 0;
    private int longDotWidth;
    private int shortDotWidth;

    // --- Content for the 3 pages ---
    private final String[] titles = {
            "Learning at Home Anything",
            "Learn from Anywhere",
            "Get Certified"
    };

    private final String[] bodies = {
            "We are Providing the best\nonline courses for you.",
            "Access our courses on your phone, tablet, or computer.",
            "Finish a course to earn a certificate of completion."
    };

    // You'll also want to change the illustration
    // private final int[] illustrations = {
    //         R.drawable.illustration_onboarding_1,
    //         R.drawable.illustration_onboarding_2,
    //         R.drawable.illustration_onboarding_3
    // };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.onboarding);

        // Find all the views
        dot1 = findViewById(R.id.dot_1);
        dot2 = findViewById(R.id.dot_2);
        dot3 = findViewById(R.id.dot_3);
        nextButton = findViewById(R.id.next_button);
        backButton = findViewById(R.id.back_button_container);
        titleText = findViewById(R.id.title_text);
        bodyText = findViewById(R.id.body_text);
        // illustrationView = findViewById(R.id.onboarding_illustration);

        // Calculate DP values in pixels
        // Your XML uses 29dp and 15dp
        shortDotWidth = dpToPx(15);
        longDotWidth = dpToPx(29);

        // Set the click listener for the 'Next' button
        nextButton.setOnClickListener(v -> {
            currentPage++;
            if (currentPage > 2) {
                // User finished onboarding, go to Welcome page
                goToWelcomeActivity();
            } else {
                updateOnboardingState();
            }
        });

        // Set the click listener for the 'Back' button
        backButton.setOnClickListener(v -> {
            if (currentPage == 0) {
                // If on the first page, finish or go to previous screen
                finish();
            } else {
                currentPage--;
                updateOnboardingState();
            }
        });

        // Set initial state (Page 0)
        updateOnboardingState();
    }

    private void updateOnboardingState() {
        // Update text
        titleText.setText(titles[currentPage]);
        bodyText.setText(bodies[currentPage]);
        // illustrationView.setImageResource(illustrations[currentPage]);

        // Update pagination dots
        switch (currentPage) {
            case 0: // L S S
                animateDot(dot1, longDotWidth, R.drawable.dot_active);
                animateDot(dot2, shortDotWidth, R.drawable.dot_inactive);
                animateDot(dot3, shortDotWidth, R.drawable.dot_inactive);
                break;
            case 1: // S L S
                animateDot(dot1, shortDotWidth, R.drawable.dot_inactive);
                animateDot(dot2, longDotWidth, R.drawable.dot_active);
                animateDot(dot3, shortDotWidth, R.drawable.dot_inactive);
                break;
            case 2: // S S L
                animateDot(dot1, shortDotWidth, R.drawable.dot_inactive);
                animateDot(dot2, shortDotWidth, R.drawable.dot_inactive);
                animateDot(dot3, longDotWidth, R.drawable.dot_active);
                break;
        }
    }

    /**
     * Animates a dot's width and background.
     */
    private void animateDot(View dot, int targetWidth, int backgroundResId) {
        // Get current layout params
        ViewGroup.LayoutParams params = dot.getLayoutParams();

        // Animate width
        ValueAnimator widthAnimator = ValueAnimator.ofInt(params.width, targetWidth);
        widthAnimator.addUpdateListener(animation -> {
            dot.getLayoutParams().width = (Integer) animation.getAnimatedValue();
            dot.requestLayout();
        });

        // Set background (this happens instantly)
        dot.setBackground(ContextCompat.getDrawable(this, backgroundResId));

        // Run the animation
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.play(widthAnimator);
        animatorSet.setDuration(300); // 300ms animation
        animatorSet.start();
    }

    /**
     * Converts a DP value to its pixel equivalent.
     */
    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        );
    }

    private void goToWelcomeActivity() {
        // Navigate to your Welcome_Page_Activity
        Intent intent = new Intent(OnboardingActivity.this, Welcome_Page_Activity.class);
        startActivity(intent);
        finish(); // Finish this activity so user can't "back" into it
    }
}