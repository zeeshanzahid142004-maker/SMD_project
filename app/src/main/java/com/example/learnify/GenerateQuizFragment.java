package com.example.learnify;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx. annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class GenerateQuizFragment extends Fragment {

    private static final String TAG = "GenerateQuizFragment";
    private static final String ARG_EXTRACTED_TEXT = "EXTRACTED_TEXT";

    private String inputContent = "";
    private QuizNetworkService quizService;
    private Dialog loadingDialog;

    private final QuizNetworkService.QuizCallback quizCallback = new QuizNetworkService.QuizCallback() {
        @Override
        public void onSuccess(List<QuizQuestion> questions, String aiTopic) {
            Log.d(TAG, "✅ Quiz generated: " + questions.size() + " questions");

            if (! isAdded() || isDetached() || getActivity() == null) {
                Log.e(TAG, "❌ Fragment not attached");
                return;
            }

            getActivity().runOnUiThread(() -> {
                try {
                    dismissLoadingDialog();

                    int totalMarks = questions.size();
                    float totalTime = 0;
                    for (QuizQuestion q : questions) {
                        if (q.timeLimit <= 0) q.timeLimit = 1.F;
                        totalTime += q.timeLimit;
                    }

                    // Use AI-generated topic if available, otherwise fallback to generated title
                    String autoTitle = (aiTopic != null && !aiTopic.isEmpty()) ? aiTopic : generateQuizTitle(questions);
                    Log.d(TAG, "📌 Using quiz title: " + autoTitle);
                    saveQuizToFirestore(autoTitle, totalMarks, totalTime, questions);

                } catch (Exception e) {
                    Log.e(TAG, "❌ Error in success handler", e);
                    Toast. makeText(getContext(), "Error: " + e.getMessage(), Toast. LENGTH_LONG).show();
                }
            });
        }

        @Override
        public void onError(String error) {
            Log.e(TAG, "❌ Error: " + error);

            if (!isAdded() || getActivity() == null) {
                Log.e(TAG, "❌ Fragment not attached");
                return;
            }

            getActivity().runOnUiThread(() -> {
                dismissLoadingDialog();
                Toast.makeText(getContext(), "Failed: " + error, Toast.LENGTH_LONG).show();

                if (getActivity() != null) {
                    getActivity().finish();
                }
            });
        }
    };

    public static GenerateQuizFragment newInstance(String textOrUrl) {
        GenerateQuizFragment fragment = new GenerateQuizFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EXTRACTED_TEXT, textOrUrl);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            inputContent = getArguments().getString(ARG_EXTRACTED_TEXT);
        }

        String apiKey = BuildConfig.API_KEY;
        quizService = new QuizNetworkService(apiKey);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_generate_quiz, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (inputContent != null && !inputContent.isEmpty()) {
            startQuizGeneration();
        }
    }

    private void startQuizGeneration() {
        Log.d(TAG, "🚀 Starting quiz generation.. .");

        if (getContext() != null) {
            loadingDialog = DialogHelper.createLoadingDialog(getContext(), "Generating Quiz...");
            loadingDialog. show();
        }

        quizService.generateQuiz(inputContent, quizCallback);
    }

    private void dismissLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            try {
                loadingDialog.dismiss();
                Log.d(TAG, "✅ Dialog dismissed");
            } catch (Exception e) {
                Log.e(TAG, "Error dismissing dialog", e);
            }
        }
    }

    private String generateQuizTitle(List<QuizQuestion> questions) {
        if (questions == null || questions.isEmpty()) {
            return "Quiz " + System.currentTimeMillis();
        }

        String firstQuestion = questions.get(0). questionText;

        if (firstQuestion. toLowerCase().contains("code") ||
                firstQuestion.toLowerCase().contains("program") ||
                firstQuestion.toLowerCase().contains("function")) {
            return "Coding Practice Quiz";
        } else if (firstQuestion.length() > 50) {
            String[] words = firstQuestion.split(" ");
            int wordCount = Math.min(4, words.length);
            StringBuilder titleBuilder = new StringBuilder();
            for (int i = 0; i < wordCount; i++) {
                titleBuilder.append(words[i]).append(" ");
            }
            return titleBuilder.toString(). trim() + " Quiz";
        } else {
            int length = Math.min(30, firstQuestion. length());
            return firstQuestion. substring(0, length) + "... Quiz";
        }
    }

    private void saveQuizToFirestore(String title, int totalMarks, float totalTime, List<QuizQuestion> questions) {
        Log.d(TAG, "💾 Saving to Firestore: " + title);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference newQuizRef = db.collection("quizzes").document();
        String quizId = newQuizRef.getId();

        Quiz newQuiz = new Quiz(quizId, title, totalMarks, totalTime, "", questions);

        newQuizRef.set(newQuiz)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Quiz saved to Firestore");
                    Toast.makeText(getContext(), "Quiz Saved!", Toast.LENGTH_SHORT). show();

                    Intent intent = new Intent(getActivity(), QuizActivity.class);
                    intent.putExtra("QUIZ_DATA", (ArrayList) questions);
                    intent.putExtra("QUIZ_ID", quizId);
                    intent.putExtra("QUIZ_TITLE", title);
                    // ⭐ PASS SOURCE CONTENT FOR REGENERATE
                    intent. putExtra("QUIZ_SOURCE", inputContent);

                    startActivity(intent);

                    if (getActivity() != null) getActivity().finish();
                })
                .addOnFailureListener(e -> {
                    Log. e(TAG, "❌ Firestore save failed", e);
                    Toast.makeText(getContext(), "Save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        dismissLoadingDialog();
    }
}