package com.example.learnify.fragments;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.learnify.BuildConfig;
import com.example.learnify.helpers.CustomToast;
import com.example.learnify.helpers.DialogHelper;
import com.example.learnify.modelclass.Quiz;
import com.example.learnify.activities.QuizActivity;
import com.example.learnify.services.QuizNetworkService;
import com.example.learnify.services.YouTubeFallbackService; // ✅ NEW
import com.example.learnify.services.YouTubeTranscriptService; // ✅ NEW
import com.example.learnify.modelclass.QuizQuestion;
import com.example.learnify.R;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class GenerateQuizFragment extends Fragment {

    private static final String TAG = "GenerateQuizFragment";
    private static final String ARG_EXTRACTED_TEXT = "EXTRACTED_TEXT";

    private String inputContent = "";
    private QuizNetworkService quizService;
    private YouTubeTranscriptService transcriptService;
    private YouTubeFallbackService fallbackService;
    private Dialog loadingDialog;

    // Regex to detect YouTube URLs
    private static final Pattern YOUTUBE_URL_PATTERN = Pattern.compile(
            "^(https?://)?(www\\.)?(youtube\\.com|youtu\\.?be)/.+$"
    );

    private final QuizNetworkService.QuizCallback quizCallback = new QuizNetworkService.QuizCallback() {
        @Override
        public void onSuccess(List<QuizQuestion> questions, String aiTopic) {
            Log.d(TAG, "✅ Quiz generated: " + questions.size() + " questions");

            if (!isAdded() || isDetached() || getActivity() == null) {
                Log.e(TAG, "❌ Fragment not attached");
                return;
            }

            getActivity().runOnUiThread(() -> {
                try {
                    dismissLoadingDialog();

                    int totalMarks = questions.size();
                    float totalTime = 0;
                    for (QuizQuestion q : questions) {
                        if (q.timeLimit <= 0) q.timeLimit = 1.0f;
                        totalTime += q.timeLimit;
                    }

                    String autoTitle = (aiTopic != null && !aiTopic.isEmpty()) ? aiTopic : generateQuizTitle(questions);
                    Log.d(TAG, "📌 Using quiz title: " + autoTitle);
                    saveQuizToFirestore(autoTitle, totalMarks, totalTime, questions);

                } catch (Exception e) {
                    Log.e(TAG, "❌ Error in success handler", e);
                    CustomToast.error(getContext(), "Error: " + e.getMessage());
                }
            });
        }

        @Override
        public void onError(String error) {
            Log.e(TAG, "❌ AI Generation Error: " + error);
            handleError("AI Generation Failed: " + error);
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

        // Initialize the YouTube services
        transcriptService = new YouTubeTranscriptService(); // ✅ NEW
        fallbackService = new YouTubeFallbackService();     // ✅ NEW
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.blank_layout, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (inputContent != null && !inputContent.isEmpty()) {
            startProcess();
        }
    }

    private void startProcess() {
        Log.d(TAG, "🚀 Starting process...");

        if (getContext() != null) {
            loadingDialog = DialogHelper.createAnimatedLoadingDialog(
                    getContext(),
                    "Analyzing Content...",
                    "Please wait while we process your request.");
            loadingDialog.show();
        }

        // 1. Check if input is a YouTube URL
        if (isYouTubeUrl(inputContent)) {
            Log.d(TAG, "🔗 YouTube URL detected. Attempting to fetch content...");
            fetchYouTubeContent(inputContent);
        } else {
            // 2. It's just text, send directly to AI
            Log.d(TAG, "📝 Raw text detected. Sending to AI...");
            quizService.generateQuiz(inputContent, quizCallback);
        }
    }

    private boolean isYouTubeUrl(String text) {
        return YOUTUBE_URL_PATTERN.matcher(text).matches();
    }

    // ✅ STEP 1: Try to get Transcript
    private void fetchYouTubeContent(String videoUrl) {
        transcriptService.getTranscript(videoUrl, new YouTubeTranscriptService.TranscriptCallback() {
            @Override
            public void onSuccess(String transcript) {
                Log.d(TAG, "✅ Transcript fetched (" + transcript.length() + " chars). Generating quiz...");
                // Send transcript to AI
                quizService.generateQuiz(transcript, quizCallback);
            }

            @Override
            public void onError(String error) {
                Log.w(TAG, "⚠️ Transcript failed: " + error + ". Trying fallback metadata...");
                // Transcript failed, try Fallback
                fetchFallbackMetadata(videoUrl);
            }
        });
    }

    // ✅ STEP 2: Try to get Metadata (Description/Tags)
    private void fetchFallbackMetadata(String videoUrl) {
        fallbackService.getVideoMetadata(videoUrl, new YouTubeFallbackService.MetadataCallback() {
            @Override
            public void onSuccess(String metadata) {
                Log.d(TAG, "✅ Metadata fetched. Generating quiz from description...");
                // Send description/tags to AI
                quizService.generateQuiz(metadata, quizCallback);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ Fallback failed: " + error);
                handleError("Could not read video content. Ensure the video is public.");
            }
        });
    }

    private void handleError(String message) {
        if (!isAdded() || getActivity() == null) return;

        getActivity().runOnUiThread(() -> {
            dismissLoadingDialog();
            CustomToast.error(getContext(), message);
            if (getActivity() != null) {
                getActivity().finish();
            }
        });
    }

    private void dismissLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            try {
                loadingDialog.dismiss();
            } catch (Exception e) {
                Log.e(TAG, "Error dismissing dialog", e);
            }
        }
    }

    // ... (generateQuizTitle and saveQuizToFirestore methods remain exactly the same as your code) ...

    private String generateQuizTitle(List<QuizQuestion> questions) {
        if (questions == null || questions.isEmpty()) {
            return "Quiz " + System.currentTimeMillis();
        }

        String firstQuestion = questions.get(0).questionText;

        if (firstQuestion.toLowerCase().contains("code") ||
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
            return titleBuilder.toString().trim() + " Quiz";
        } else {
            int length = Math.min(30, firstQuestion.length());
            return firstQuestion.substring(0, length) + "... Quiz";
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
                    CustomToast.success(getContext(), "Quiz Saved!");

                    Intent intent = new Intent(getActivity(), QuizActivity.class);
                    intent.putExtra("QUIZ_DATA", (ArrayList) questions);
                    intent.putExtra("QUIZ_ID", quizId);
                    intent.putExtra("QUIZ_TITLE", title);
                    intent.putExtra("QUIZ_SOURCE", inputContent);

                    startActivity(intent);

                    if (getActivity() != null) getActivity().finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Firestore save failed", e);
                    CustomToast.error(getContext(), "Save failed: " + e.getMessage());
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        dismissLoadingDialog();
    }
}