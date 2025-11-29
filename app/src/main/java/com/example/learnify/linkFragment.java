package com.example.learnify;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

public class linkFragment extends Fragment {

    private static final String TAG = "linkFragment";

    private HomeFragment.OnHomeFragmentInteractionListener mListener;

    private View enterLinkContainer;
    private View loadingContainer;
    private View confirmationContainer;
    private TextView loadingMessage;
    private MaterialButton pasteButton, rewatchButton, quizButton;
    private EditText linkInput;

    private String processedLink = "";
    private String extractedTranscript = "";
    private boolean isConfirmed = false;
    private boolean isYouTubeVideo = false;

    private YouTubeTranscriptService transcriptService;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof HomeFragment.OnHomeFragmentInteractionListener) {
            mListener = (HomeFragment.OnHomeFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnHomeFragmentInteractionListener");
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        transcriptService = new YouTubeTranscriptService();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_link, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        enterLinkContainer = view.findViewById(R.id.enter_link_container);
        loadingContainer = view.findViewById(R.id.loading_container);
        confirmationContainer = view.findViewById(R.id.confirmation_container);
        linkInput = view.findViewById(R.id.linkInput);
        pasteButton = view.findViewById(R.id.pasteButton);
        rewatchButton = view.findViewById(R.id.rewatch_button);
        quizButton = view.findViewById(R.id.quiz_button);

        // Get loading message TextView
        loadingMessage = loadingContainer.findViewById(R.id.tv_loading_subtitle);
        if (loadingMessage == null) {
            // Fallback: try to find any TextView in loading container
            loadingMessage = loadingContainer.findViewById(R.id.tv_loading_title);
        }

        updateUiState();

        // 1. Paste/Go Button
        pasteButton.setOnClickListener(v -> {
            String link = linkInput.getText().toString().trim();
            if (link.isEmpty()) {
                Toast.makeText(getContext(), "Please enter a link", Toast.LENGTH_SHORT).show();
            } else {
                processedLink = link;
                isYouTubeVideo = isYouTubeUrl(link);

                if (isYouTubeVideo) {
                    Log.d(TAG, "📹 YouTube video detected - extracting transcript");
                    showLoadingState("Extracting video content...");
                    extractTranscript(link);
                } else {
                    Log.d(TAG, "🔗 Regular link - proceeding normally");
                    showLoadingState("Analyzing...");
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        isConfirmed = true;
                        updateUiState();
                    }, 1500);
                }
            }
        });

        // 2. "Watch Video" Button
        rewatchButton.setOnClickListener(v -> {
            if (mListener != null) {
                // UPDATED: Pass both the link AND the transcript
                mListener.onGoToVideoFragment(processedLink, extractedTranscript);
            }
        });

        // 3. "Take Quiz" Button
        quizButton.setOnClickListener(v -> {
            Log.d(TAG, "🎯 Take Quiz clicked");

            if (isYouTubeVideo && !extractedTranscript.isEmpty()) {
                Log.d(TAG, "✅ Using transcript for quiz generation");
                launchGenerateQuizFragment(extractedTranscript);
            } else {
                Log.d(TAG, "📝 Using URL for quiz generation");
                launchGenerateQuizFragment(processedLink);
            }
        });
    }

    /**
     * Check if URL is a YouTube video
     */
    private boolean isYouTubeUrl(String url) {
        return url.contains("youtube.com") || url.contains("youtu.be");
    }

    /**
     * Extract transcript from YouTube video
     */
    private void extractTranscript(String videoUrl) {
        transcriptService.getTranscript(videoUrl, new YouTubeTranscriptService.TranscriptCallback() {
            @Override
            public void onSuccess(String transcript) {
                Log.d(TAG, "✅ Transcript extracted successfully. Length: " + transcript.length());
                extractedTranscript = transcript;

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        isConfirmed = true;
                        updateUiState();
                        Toast.makeText(getContext(), "✅ Video content extracted!", Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ Transcript extraction failed: " + error);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        // Fallback: proceed with URL anyway
                        Toast.makeText(getContext(),
                                "⚠️ Could not extract transcript. Using video URL instead.",
                                Toast.LENGTH_LONG).show();

                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            isConfirmed = true;
                            updateUiState();
                        }, 1000);
                    });
                }
            }
        });
    }

    private void showLoadingState(String message) {
        enterLinkContainer.setVisibility(View.GONE);
        loadingContainer.setVisibility(View.VISIBLE);
        confirmationContainer.setVisibility(View.GONE);

        if (loadingMessage != null) {
            loadingMessage.setText(message);
        }
    }

    private void updateUiState() {
        if (isConfirmed) {
            enterLinkContainer.setVisibility(View.GONE);
            loadingContainer.setVisibility(View.GONE);
            confirmationContainer.setVisibility(View.VISIBLE);

            // Update button text based on content type
            if (isYouTubeVideo) {
                rewatchButton.setText("Watch Video");
                quizButton.setText(extractedTranscript.isEmpty() ?
                        "Take Quiz (URL)" : "Take Quiz (Transcript)");
            } else {
                rewatchButton.setVisibility(View.GONE);
                quizButton.setText("Generate Quiz");
            }
        } else {
            enterLinkContainer.setVisibility(View.VISIBLE);
            loadingContainer.setVisibility(View.GONE);
            confirmationContainer.setVisibility(View.GONE);
        }
    }

    /**
     * Launch GenerateQuizFragment with content (transcript or URL)
     */
    private void launchGenerateQuizFragment(String content) {
        Log.d(TAG, "🚀 Creating GenerateQuizFragment with content length: " + content.length());

        GenerateQuizFragment fragment = GenerateQuizFragment.newInstance(content);

        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();

        Log.d(TAG, "✅ GenerateQuizFragment transaction committed");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
}
