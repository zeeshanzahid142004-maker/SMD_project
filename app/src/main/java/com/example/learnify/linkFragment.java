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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class linkFragment extends Fragment {

    private static final String TAG = "linkFragment";

    private HomeFragment.OnHomeFragmentInteractionListener mListener;

    private View enterLinkContainer;
    private View loadingContainer;
    private View confirmationContainer;
    private MaterialButton pasteButton, rewatchButton, quizButton;
    private EditText linkInput;

    private String processedLink = "";
    private boolean isConfirmed = false;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof HomeFragment.OnHomeFragmentInteractionListener) {
            mListener = (HomeFragment.OnHomeFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnHomeFragmentInteractionListener");
        }
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

        updateUiState();

        // 1. Paste/Go Button
        pasteButton.setOnClickListener(v -> {
            String link = linkInput.getText().toString().trim();
            if (link.isEmpty()) {
                Toast.makeText(getContext(), "Please enter a link", Toast.LENGTH_SHORT).show();
            } else {
                processedLink = link;
                showLoadingState();
            }
        });

        // 2. "Watch Video" Button
        rewatchButton.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onGoToVideoFragment(processedLink);
            }
        });

        // 3. "Take Quiz" Button - Navigate to GenerateQuizFragment
        quizButton.setOnClickListener(v -> {
            Log.d(TAG, "Take Quiz clicked - launching GenerateQuizFragment with URL: " + processedLink);
            launchGenerateQuizFragment(processedLink);
        });
    }

    private void showLoadingState() {
        enterLinkContainer.setVisibility(View.GONE);
        loadingContainer.setVisibility(View.VISIBLE);
        confirmationContainer.setVisibility(View.GONE);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            isConfirmed = true;
            updateUiState();
        }, 1500);
    }

    private void updateUiState() {
        if (isConfirmed) {
            enterLinkContainer.setVisibility(View.GONE);
            loadingContainer.setVisibility(View.GONE);
            confirmationContainer.setVisibility(View.VISIBLE);
        } else {
            enterLinkContainer.setVisibility(View.VISIBLE);
            loadingContainer.setVisibility(View.GONE);
            confirmationContainer.setVisibility(View.GONE);
        }
    }

    /**
     * Launch GenerateQuizFragment with the URL/text
     * This fragment will handle the AI generation, saving, and launching QuizActivity
     */
    private void launchGenerateQuizFragment(String urlOrText) {
        Log.d(TAG, "Creating GenerateQuizFragment with content: " + urlOrText.substring(0, Math.min(50, urlOrText.length())));

        GenerateQuizFragment fragment = GenerateQuizFragment.newInstance(urlOrText);

        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();

        Log.d(TAG, "GenerateQuizFragment transaction committed");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
}