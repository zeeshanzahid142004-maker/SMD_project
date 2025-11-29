package com.example.learnify; // Or your package name

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class QuizCompleteDialogFragment extends DialogFragment {

    public interface QuizCompleteDialogListener {
        void onViewResults();
        void onRegenerateQuiz();
        void onRetakeQuiz();
    }

    private QuizCompleteDialogListener listener;
    private String scoreText;

    public static QuizCompleteDialogFragment newInstance(String score) {
        QuizCompleteDialogFragment fragment = new QuizCompleteDialogFragment();
        Bundle args = new Bundle();
        args.putString("SCORE_TEXT", score);
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
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Style the window *before* inflating
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        return inflater.inflate(R.layout.fragment_quiz_complete_dialog, container, false);
    }

    // --- ADD THIS METHOD ---
    @Override
    public void onStart() {
        super.onStart();
        // This makes the dialog window match the screen width,
        // allowing your CardView's margins to work correctly.
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
    // --- END OF NEW METHOD ---

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

        // Set the score text
        tvScore.setText(scoreText);

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

        // Set click listeners
        btnViewResults.setOnClickListener(v -> {
            listener.onViewResults();
            dismiss();
        });

        btnRegenerate.setOnClickListener(v -> {
            listener.onRegenerateQuiz();
            dismiss();
        });

        btnRetake.setOnClickListener(v -> {
            listener.onRetakeQuiz();
            dismiss();
        });
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