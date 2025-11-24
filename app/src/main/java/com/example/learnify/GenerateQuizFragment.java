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
import androidx.annotation.Nullable;
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
    private Dialog loadingDialog; // Changed from ProgressDialog

    // Store callback as a field to prevent garbage collection
    private final QuizNetworkService.QuizCallback quizCallback = new QuizNetworkService.QuizCallback() {
        @Override
        public void onSuccess(List<QuizQuestion> questions) {
            Log.d(TAG, "✅✅✅ CALLBACK REACHED: onSuccess called with " + questions.size() + " questions");
            Log.d(TAG, "🔍 Fragment added? " + isAdded());
            Log.d(TAG, "🔍 Fragment detached? " + isDetached());
            Log.d(TAG, "🔍 Activity null? " + (getActivity() == null));
            Log.d(TAG, "🔍 Context null? " + (getContext() == null));

            // Check if fragment is still attached
            if (!isAdded() || isDetached() || getActivity() == null) {
                Log.e(TAG, "❌ Fragment not attached anymore! Cannot update UI.");
                return;
            }

            getActivity().runOnUiThread(() -> {
                Log.d(TAG, "🎯 Running on UI thread now");

                try {
                    // DISMISS LOADING
                    dismissLoadingDialog();
                    Log.d(TAG, "📱 Dialog dismissed");

                    // Calculate totals
                    int totalMarks = questions.size();
                    float totalTime = 0;
                    for (QuizQuestion q : questions) {
                        if (q.timeLimit <= 0) q.timeLimit = 1.0f;
                        totalTime += q.timeLimit;
                    }

                    Log.d(TAG, "📊 Total marks: " + totalMarks + ", Total time: " + totalTime);

                    // Auto-generate title based on content
                    Log.d(TAG, "🔍 About to generate title...");
                    String autoTitle = generateQuizTitle(questions);
                    Log.d(TAG, "🏷️ Auto-generated title: " + autoTitle);

                    // Save directly without asking user
                    Log.d(TAG, "🔍 About to save to Firestore...");
                    saveQuizToFirestore(autoTitle, totalMarks, totalTime, questions);

                } catch (Exception e) {
                    Log.e(TAG, "❌ ERROR in success handler", e);
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        }

        @Override
        public void onError(String error) {
            Log.e(TAG, "❌❌❌ CALLBACK REACHED: onError called - " + error);
            Log.d(TAG, "🔍 Fragment added? " + isAdded());
            Log.d(TAG, "🔍 Activity null? " + (getActivity() == null));

            if (!isAdded() || getActivity() == null) {
                Log.e(TAG, "❌ Fragment not attached anymore! Cannot show error.");
                return;
            }

            getActivity().runOnUiThread(() -> {
                Log.d(TAG, "🎯 Error handler running on UI thread");

                // DISMISS LOADING
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
        Log.d(TAG, "🚀 Starting quiz generation...");
        Log.d(TAG, "🔍 Fragment added? " + isAdded());
        Log.d(TAG, "🔍 Fragment detached? " + isDetached());

        // SHOW LOADING DIALOG
        if (getContext() != null) {
            loadingDialog = DialogHelper.createLoadingDialog(getContext(), "Generating Quiz...");
            loadingDialog.show();
            Log.d(TAG, "📱 Loading dialog shown");
        }

        // Use the stored callback field instead of anonymous inner class
        Log.d(TAG, "📞 Calling generateQuiz with stored callback...");
        quizService.generateQuiz(inputContent, quizCallback);

        Log.d(TAG, "📞 Quiz service called, waiting for callback...");
    }

    private void dismissLoadingDialog() {
        Log.d(TAG, "🔄 dismissLoadingDialog called");
        if (loadingDialog != null) {
            Log.d(TAG, "📱 Dialog exists, showing? " + loadingDialog.isShowing());
            if (loadingDialog.isShowing()) {
                try {
                    loadingDialog.dismiss();
                    Log.d(TAG, "✅ Dialog dismissed successfully");
                } catch (Exception e) {
                    Log.e(TAG, "❌ Error dismissing dialog", e);
                }
            }
        } else {
            Log.d(TAG, "⚠️ Loading dialog is null!");
        }
    }

    /**
     * Generate a smart title based on the quiz questions
     */


    private String generateQuizTitle(List<QuizQuestion> questions) {
        Log.d(TAG, "🎨 Generating title from " + questions.size() + " questions");

        if (questions == null || questions.isEmpty()) {
            return "Quiz " + System.currentTimeMillis();
        }

        // Extract key topics from first few questions
        String firstQuestion = questions.get(0).questionText;
        Log.d(TAG, "First question: " + firstQuestion);

        // Simple title generation - you can make this smarter
        if (firstQuestion.toLowerCase().contains("code") ||
                firstQuestion.toLowerCase().contains("program") ||
                firstQuestion.toLowerCase().contains("function")) {
            return "Coding Practice Quiz";
        } else if (firstQuestion.length() > 50) {
            // Use first few words
            String[] words = firstQuestion.split(" ");
            int wordCount = Math.min(4, words.length);
            StringBuilder titleBuilder = new StringBuilder();
            for (int i = 0; i < wordCount; i++) {
                titleBuilder.append(words[i]).append(" ");
            }
            return titleBuilder.toString().trim() + " Quiz";
        } else {
            int length = Math.min(30, firstQuestion.length());
            return firstQuestion.substring(0, length) + "... Quiz";
        }
    }

    private void saveQuizToFirestore(String title, int totalMarks, float totalTime, List<QuizQuestion> questions) {
        Log.d(TAG, "💾 Saving to Firestore: " + title);

        // Show saving progress
        if (getContext() != null) {
            // Ensure you have your loading dialog logic here
            // loadingDialog = DialogHelper.createLoadingDialog(getContext(), "Saving Quiz...");
            // loadingDialog.show();
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference newQuizRef = db.collection("quizzes").document();
        String quizId = newQuizRef.getId();

        Quiz newQuiz = new Quiz(quizId, title, totalMarks, totalTime, "", questions);

        Log.d(TAG, "🔥 Starting Firestore save...");

        newQuizRef.set(newQuiz)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "✅ Firestore save successful");
                    // dismissLoadingDialog();
                    Toast.makeText(getContext(), "Quiz Saved!", Toast.LENGTH_SHORT).show();

                    // --- THIS IS THE UPDATE ---
                    Intent intent = new Intent(getActivity(), QuizActivity.class);
                    intent.putExtra("QUIZ_DATA", (ArrayList) questions);

                    // 1. PASS THE ID
                    intent.putExtra("QUIZ_ID", quizId);
                    // 2. PASS THE TITLE
                    intent.putExtra("QUIZ_TITLE", title);

                    startActivity(intent);

                    if (getActivity() != null) getActivity().finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Firestore save failed", e);
                    // dismissLoadingDialog();
                    Toast.makeText(getContext(), "Save failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        dismissLoadingDialog();
        Log.d(TAG, "🧹 Fragment view destroyed");
    }
}