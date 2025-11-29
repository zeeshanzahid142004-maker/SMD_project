package com.example.learnify;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable. ColorDrawable;
import android. os.Bundle;
import android. view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation. Nullable;
import androidx.fragment.app.DialogFragment;

import com.airbnb.lottie.LottieAnimationView;

public class QuizCompleteDialogFragment extends DialogFragment {

    public interface QuizCompleteDialogListener {
        void onViewResults();
        void onRegenerateQuiz();
        void onRetakeQuiz();
    }

    private QuizCompleteDialogListener listener;
    private String scoreText;
    private int score;
    private int totalQuestions;

    public static QuizCompleteDialogFragment newInstance(String score) {
        QuizCompleteDialogFragment fragment = new QuizCompleteDialogFragment();
        Bundle args = new Bundle();
        args.putString("SCORE_TEXT", score);
        fragment.setArguments(args);
        return fragment;
    }

    public static QuizCompleteDialogFragment newInstance(String scoreText, int score, int totalQuestions) {
        QuizCompleteDialogFragment fragment = new QuizCompleteDialogFragment();
        Bundle args = new Bundle();
        args.putString("SCORE_TEXT", scoreText);
        args.putInt("SCORE", score);
        args.putInt("TOTAL_QUESTIONS", totalQuestions);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener = (QuizCompleteDialogListener) getActivity();
        } catch (ClassCastException e) {
            throw new ClassCastException(getActivity().toString() + " must implement QuizCompleteDialogListener");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            scoreText = getArguments().getString("SCORE_TEXT");
            score = getArguments(). getInt("SCORE", 0);
            totalQuestions = getArguments().getInt("TOTAL_QUESTIONS", 1);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().requestFeature(Window. FEATURE_NO_TITLE);
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        return inflater. inflate(R.layout.fragment_quiz_complete_dialog, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(ViewGroup. LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams. WRAP_CONTENT);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Find views
        TextView tvScore = view.findViewById(R.id.dialog_score);
        Button btnViewResults = view.findViewById(R.id.btn_view_results);
        Button btnRegenerate = view.findViewById(R.id.btn_regenerate_quiz);
        Button btnRetake = view.findViewById(R.id.btn_retake_quiz);
        ImageView btnClose = view.findViewById(R.id.btn_close);
        Button btnGoHome = view.findViewById(R.id.btn_go_home);
        LottieAnimationView lottieConfetti = view.findViewById(R.id.lottie_confetti);

        // Set the score text with colored correct/wrong counts
        if (tvScore != null) {
            int wrongCount = totalQuestions - score;
            
            // Build rich text with colors
            android.text.SpannableStringBuilder sb = new android.text.SpannableStringBuilder();
            sb.append("Your score: ");
            
            // Add correct count in green
            int startCorrect = sb.length();
            sb.append(String.valueOf(score));
            sb.setSpan(new android.text.style.ForegroundColorSpan(Color.parseColor("#4CAF50")), 
                      startCorrect, sb.length(), android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 
                      startCorrect, sb.length(), android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            
            sb.append("/");
            sb.append(String.valueOf(totalQuestions));
            sb.append("\n\n");
            
            // Add correct label in green
            int startCorrectLabel = sb.length();
            sb.append("✓ Correct: " + score);
            sb.setSpan(new android.text.style.ForegroundColorSpan(Color.parseColor("#4CAF50")), 
                      startCorrectLabel, sb.length(), android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            
            sb.append("    ");
            
            // Add wrong label in red
            int startWrongLabel = sb.length();
            sb.append("✗ Wrong: " + wrongCount);
            sb.setSpan(new android.text.style.ForegroundColorSpan(Color.parseColor("#F44336")), 
                      startWrongLabel, sb.length(), android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            
            tvScore.setText(sb);
            tvScore.setTextSize(20);
        }

        // Close button (X) - navigate to home
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> {
                dismiss();
                navigateToHome();
            });
        }

        // Go Home button
        if (btnGoHome != null) {
            btnGoHome.setOnClickListener(v -> {
                dismiss();
                navigateToHome();
            });
        }

        // Show confetti animation if score is > 70%
        if (totalQuestions > 0) {
            float percentage = (float) score / totalQuestions * 100;
            if (percentage >= 70 && lottieConfetti != null) {
                lottieConfetti.setVisibility(View.VISIBLE);
                lottieConfetti.playAnimation();
            }
        }

        // Set click listeners
        if (btnViewResults != null) {
            btnViewResults.setOnClickListener(v -> {
                if (listener != null) listener.onViewResults();
                dismiss();
            });
        }

        if (btnRegenerate != null) {
            btnRegenerate.setOnClickListener(v -> {
                if (listener != null) listener.onRegenerateQuiz();
                dismiss();
            });
        }

        if (btnRetake != null) {
            btnRetake.setOnClickListener(v -> {
                if (listener != null) listener.onRetakeQuiz();
                dismiss();
            });
        }
    }

    /**
     * Navigate to MainActivity (Home)
     */
    private void navigateToHome() {
        if (getActivity() != null) {
            Intent intent = new Intent(getActivity(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            getActivity().finish();
        }
    }
}