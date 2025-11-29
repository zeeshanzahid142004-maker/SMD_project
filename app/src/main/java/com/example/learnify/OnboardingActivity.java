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
import android.widget.ImageView; // <--- 1. ADD THIS IMPORT
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

public class OnboardingActivity extends BaseActivity {

    private View dot1, dot2, dot3;
    private MaterialButton nextButton;
    private FrameLayout backButton;
    private TextView titleText, bodyText;
    private ImageView illustrationView;

    private int currentPage = 0;
    private int longDotWidth;
    private int shortDotWidth;

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


    private final int[] illustrations = {
            R.drawable.illustration_onboarding_1,
            R.drawable.illustration_onboarding_2,
            R.drawable.illustration_onboarding_3
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.onboarding);


        dot1 = findViewById(R.id.dot_1);
        dot2 = findViewById(R.id.dot_2);
        dot3 = findViewById(R.id.dot_3);
        nextButton = findViewById(R.id.next_button);
        backButton = findViewById(R.id.back_button_container);
        titleText = findViewById(R.id.title_text);
        bodyText = findViewById(R.id.body_text);


        illustrationView = findViewById(R.id.onboarding_illustration);

        shortDotWidth = dpToPx(15);
        longDotWidth = dpToPx(29);

        nextButton.setOnClickListener(v -> {
            currentPage++;
            if (currentPage > 2) {
                goToWelcomeActivity();
            } else {
                updateOnboardingState();
            }
        });

        backButton.setOnClickListener(v -> {
            if (currentPage == 0) {
                finish();
            } else {
                currentPage--;
                updateOnboardingState();
            }
        });

        updateOnboardingState();
    }

    private void updateOnboardingState() {
        titleText.setText(titles[currentPage]);
        bodyText.setText(bodies[currentPage]);


        if (illustrationView != null) {

             illustrationView.setImageResource(illustrations[currentPage]);
        }

        switch (currentPage) {
            case 0:
                animateDot(dot1, longDotWidth, R.drawable.dot_active);
                animateDot(dot2, shortDotWidth, R.drawable.dot_inactive);
                animateDot(dot3, shortDotWidth, R.drawable.dot_inactive);
                break;
            case 1:
                animateDot(dot1, shortDotWidth, R.drawable.dot_inactive);
                animateDot(dot2, longDotWidth, R.drawable.dot_active);
                animateDot(dot3, shortDotWidth, R.drawable.dot_inactive);
                break;
            case 2:
                animateDot(dot1, shortDotWidth, R.drawable.dot_inactive);
                animateDot(dot2, shortDotWidth, R.drawable.dot_inactive);
                animateDot(dot3, longDotWidth, R.drawable.dot_active);
                break;
        }
    }

    private void animateDot(View dot, int targetWidth, int backgroundResId) {
        ViewGroup.LayoutParams params = dot.getLayoutParams();
        ValueAnimator widthAnimator = ValueAnimator.ofInt(params.width, targetWidth);
        widthAnimator.addUpdateListener(animation -> {
            dot.getLayoutParams().width = (Integer) animation.getAnimatedValue();
            dot.requestLayout();
        });

        dot.setBackground(ContextCompat.getDrawable(this, backgroundResId));

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.play(widthAnimator);
        animatorSet.setDuration(300);
        animatorSet.start();
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                getResources().getDisplayMetrics()
        );
    }

    private void goToWelcomeActivity() {
        Intent intent = new Intent(OnboardingActivity.this, Welcome_Page_Activity.class);
        startActivity(intent);
        finish();
    }
}