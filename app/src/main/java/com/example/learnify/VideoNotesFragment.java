package com.example.learnify;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spannable;
import android.text.style.BulletSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VideoNotesFragment extends Fragment {

    private EditText notesEditText;
    private ImageView buttonBold, buttonItalic, buttonBullet, buttonUnderline,
            buttonH1, buttonH2, buttonH3, buttonHighlight,
            buttonUndo, buttonRedo, buttonScreenshot;
    private YouTubePlayerView youTubePlayerView;
    private String videoUrl;
    private View loadingOverlay;

    // Undo/Redo stack
    private final Stack<Editable> undoStack = new Stack<>();
    private final Stack<Editable> redoStack = new Stack<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            videoUrl = getArguments().getString("VIDEO_URL");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_video_notes, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Find all views
        notesEditText = view.findViewById(R.id.notes_edittext);
        buttonBold = view.findViewById(R.id.button_bold);
        buttonItalic = view.findViewById(R.id.button_italic);
        buttonBullet = view.findViewById(R.id.button_bullet);
        buttonUnderline = view.findViewById(R.id.button_underline);
        buttonH1 = view.findViewById(R.id.button_h1);
        buttonH2 = view.findViewById(R.id.button_h2);
        buttonH3 = view.findViewById(R.id.button_h3);
        buttonHighlight = view.findViewById(R.id.button_highlight);
        buttonUndo = view.findViewById(R.id.button_undo);
        buttonRedo = view.findViewById(R.id.button_redo);
        buttonScreenshot = view.findViewById(R.id.button_screenshot);

        loadingOverlay = view.findViewById(R.id.video_loading_overlay);

        // --- VIDEO PLAYER INIT ---
        youTubePlayerView = view.findViewById(R.id.youtube_player_view);
        getLifecycle().addObserver(youTubePlayerView);

        youTubePlayerView.addYouTubePlayerListener(new AbstractYouTubePlayerListener() {
            @Override
            public void onReady(@NonNull YouTubePlayer youTubePlayer) {
                // Hide loading overlay
                loadingOverlay.animate()
                        .alpha(0f)
                        .setDuration(300)
                        .withEndAction(() -> loadingOverlay.setVisibility(View.GONE))
                        .start();

                String videoId = getVideoIdFromUrl(videoUrl);
                if (videoId != null) {
                    youTubePlayer.loadVideo(videoId, 0);
                } else {
                    Toast.makeText(getContext(), "Error: Invalid YouTube URL", Toast.LENGTH_LONG).show();
                }
            }
        });

        // --- STYLE BUTTONS ---
        buttonBold.setOnClickListener(v -> applyStyle(Typeface.BOLD));
        buttonItalic.setOnClickListener(v -> applyStyle(Typeface.ITALIC));
        buttonUnderline.setOnClickListener(v -> applyUnderline());
        buttonBullet.setOnClickListener(v -> applyBullet());
        buttonH1.setOnClickListener(v -> applyHeading(1));
        buttonH2.setOnClickListener(v -> applyHeading(2));
        buttonH3.setOnClickListener(v -> applyHeading(3));
        buttonHighlight.setOnClickListener(v -> applyHighlight());

        // --- UNDO / REDO ---
        buttonUndo.setOnClickListener(v -> undo());
        buttonRedo.setOnClickListener(v -> redo());

        // --- SCREENSHOT STUB ---
        buttonScreenshot.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Screenshot button clicked", Toast.LENGTH_SHORT).show();
        });

        // Save initial state
        saveStateForUndo();
    }

    // ----------------- TEXT STYLING -----------------
    private void applyStyle(int style) {
        int start = notesEditText.getSelectionStart();
        int end = notesEditText.getSelectionEnd();
        if (start == end) return;

        Spannable spannable = notesEditText.getText();
        spannable.setSpan(new StyleSpan(style), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        saveStateForUndo();
    }

    private void applyUnderline() {
        int start = notesEditText.getSelectionStart();
        int end = notesEditText.getSelectionEnd();
        if (start == end) return;

        Spannable spannable = notesEditText.getText();
        spannable.setSpan(new android.text.style.UnderlineSpan(),
                start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        saveStateForUndo();
    }

    private void applyBullet() {
        Editable text = notesEditText.getText();
        int start = notesEditText.getSelectionStart();

        int lineStart = start;
        while (lineStart > 0 && text.charAt(lineStart - 1) != '\n') lineStart--;

        text.insert(lineStart, "• ");
        text.setSpan(new BulletSpan(40), lineStart, lineStart + 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        saveStateForUndo();
    }

    private void applyHeading(int level) {
        int start = notesEditText.getSelectionStart();
        int end = notesEditText.getSelectionEnd();
        if (start == end) return;

        Spannable spannable = notesEditText.getText();
        float size;
        switch (level) {
            case 1: size = 32f; break;
            case 2: size = 24f; break;
            case 3: size = 18f; break;
            default: size = 14f; break;
        }
        spannable.setSpan(new android.text.style.RelativeSizeSpan(size/14f),
                start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        saveStateForUndo();
    }

    private void applyHighlight() {
        int start = notesEditText.getSelectionStart();
        int end = notesEditText.getSelectionEnd();
        if (start == end) return;

        Spannable spannable = notesEditText.getText();
        spannable.setSpan(new android.text.style.BackgroundColorSpan(
                        getResources().getColor(R.color.figma_purple_main)),
                start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        saveStateForUndo();
    }

    // ----------------- UNDO / REDO -----------------
    private void saveStateForUndo() {
        undoStack.push(new android.text.SpannableStringBuilder(notesEditText.getText()));
        redoStack.clear();
    }

    private void undo() {
        if (!undoStack.isEmpty()) {
            redoStack.push(new android.text.SpannableStringBuilder(notesEditText.getText()));
            Editable prev = undoStack.pop();
            notesEditText.setText(prev);
        }
    }

    private void redo() {
        if (!redoStack.isEmpty()) {
            undoStack.push(new android.text.SpannableStringBuilder(notesEditText.getText()));
            Editable next = redoStack.pop();
            notesEditText.setText(next);
        }
    }

    // ----------------- VIDEO HELPERS -----------------

    private String getVideoIdFromUrl(String url) {
        if (url == null || url.trim().isEmpty()) return null;

        // This regex covers standard URLs, short URLs (youtu.be), and embedded URLs
        String pattern = "(?<=watch\\?v=|/videos/|embed/|youtu.be/|/v/|/e/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%\u200C\u200B2F|youtu.be%2F|%2Fv%2F)[^#\\&\\?\\n]*";

        java.util.regex.Pattern compiledPattern = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher matcher = compiledPattern.matcher(url);

        if (matcher.find()) {
            return matcher.group();
        }
        return null; // Could not find ID
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (youTubePlayerView != null) {
            youTubePlayerView.release();
        }
    }


    // In VideoNotesFragment.java

    public void onReady(@NonNull YouTubePlayer youTubePlayer) {
        // Use the helper to extract the ID
        String videoId = getVideoIdFromUrl(videoUrl);

        if (videoId != null) {
            youTubePlayer.loadVideo(videoId, 0);
        } else {
            // Fallback: Try loading it as is, or show error
            Log.e("VideoNotes", "Could not extract video ID from: " + videoUrl);
            Toast.makeText(getContext(), "Invalid YouTube URL", Toast.LENGTH_SHORT).show();
        }
    }
}
