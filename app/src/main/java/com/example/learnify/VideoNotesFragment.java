package com.example.learnify;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VideoNotesFragment extends Fragment {

    private static final String TAG = "VideoNotesFragment";

    private EditText notesEditText;
    private ImageView buttonBold, buttonItalic, buttonBullet, buttonUnderline,
            buttonH1, buttonH2, buttonH3, buttonHighlight,
            buttonUndo, buttonRedo, buttonSave;
    private YouTubePlayerView youTubePlayerView;
    private String videoUrl;
    private String passedTranscript;
    private View loadingOverlay;

    // Undo/Redo stacks
    private final Stack<CharSequence> undoStack = new Stack<>();
    private final Stack<CharSequence> redoStack = new Stack<>();
    private boolean isUndoRedoOperation = false;

    // Formatting State Toggles
    private boolean isBoldActive = false;
    private boolean isItalicActive = false;
    private boolean isUnderlineActive = false;
    private boolean isHighlightActive = false;

    // Repository & Service
    private VideoNotesRepository notesRepository;
    private YouTubeTranscriptService transcriptService;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            videoUrl = getArguments().getString("VIDEO_URL");
            passedTranscript = getArguments().getString("TRANSCRIPT_DATA");
        }

        notesRepository = new VideoNotesRepository();
        transcriptService = new YouTubeTranscriptService();
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

        // Initialize views
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
        buttonSave = view.findViewById(R.id.button_save);
        loadingOverlay = view.findViewById(R.id.video_loading_overlay);
        youTubePlayerView = view.findViewById(R.id.youtube_player_view);

        setupVideoPlayer();
        setupFormattingButtons();
        setupTextWatcher();
        loadSavedNotes();
    }

    private void setupVideoPlayer() {
        getLifecycle().addObserver(youTubePlayerView);

        youTubePlayerView.addYouTubePlayerListener(new AbstractYouTubePlayerListener() {
            @Override
            public void onReady(@NonNull YouTubePlayer youTubePlayer) {
                if (loadingOverlay != null) {
                    loadingOverlay.setVisibility(View.GONE);
                }

                String videoId = getVideoIdFromUrl(videoUrl);

                if (videoId != null && !videoId.isEmpty()) {
                    Log.d(TAG, "✅ Loading Video ID: " + videoId);
                    youTubePlayer.loadVideo(videoId, 0);
                } else {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Invalid Video URL", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onError(@NonNull YouTubePlayer youTubePlayer, @NonNull com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants.PlayerError error) {
                super.onError(youTubePlayer, error);
                Log.e(TAG, "❌ Player Error: " + error.name());
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (youTubePlayerView != null) {
            youTubePlayerView.release();
        }
    }

    private void loadSavedNotes() {
        // Prioritize passed transcript (new session)
        if (passedTranscript != null && !passedTranscript.isEmpty()) {
            if (notesEditText.getText().toString().trim().isEmpty()) {
                notesEditText.setText(passedTranscript);
                Toast.makeText(getContext(), "✅ Notes auto-filled from transcript!", Toast.LENGTH_SHORT).show();
            }
        }
        // If empty, try fetching in background
        else if (notesEditText.getText().toString().trim().isEmpty()) {
            fetchTranscriptBackground();
        }
    }

    private void fetchTranscriptBackground() {
        if (videoUrl == null || videoUrl.isEmpty()) return;
        transcriptService.getTranscript(videoUrl, new YouTubeTranscriptService.TranscriptCallback() {
            @Override
            public void onSuccess(String transcript) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (notesEditText.getText().toString().trim().isEmpty()) {
                            notesEditText.setText(transcript);
                            Toast.makeText(getContext(), "✅ Transcript loaded automatically", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Background fetch failed: " + error);
            }
        });
    }

    private void setupFormattingButtons() {
        buttonBold.setOnClickListener(v -> toggleStyle(Typeface.BOLD, buttonBold));
        buttonItalic.setOnClickListener(v -> toggleStyle(Typeface.ITALIC, buttonItalic));
        buttonUnderline.setOnClickListener(v -> toggleUnderline(buttonUnderline));

        buttonBullet.setOnClickListener(v -> applyBullet());

        buttonH1.setOnClickListener(v -> applyHeading(1));
        buttonH2.setOnClickListener(v -> applyHeading(2));
        buttonH3.setOnClickListener(v -> applyHeading(3));

        buttonHighlight.setOnClickListener(v -> toggleHighlight(buttonHighlight));

        buttonUndo.setOnClickListener(v -> undo());
        buttonRedo.setOnClickListener(v -> redo());
        buttonSave.setOnClickListener(v -> saveNotes());
    }

    private void setupTextWatcher() {
        notesEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (!isUndoRedoOperation) {
                    saveStateForUndo();
                }
            }
        });
    }

    // --- FIX: Added 1-argument Overload for NotesListFragment ---
    public static VideoNotesFragment newInstance(String videoUrl) {
        return newInstance(videoUrl, null);
    }

    // --- Existing 2-argument method for linkFragment ---
    public static VideoNotesFragment newInstance(String videoUrl, String transcript) {
        VideoNotesFragment fragment = new VideoNotesFragment();
        Bundle args = new Bundle();
        args.putString("VIDEO_URL", videoUrl);
        if (transcript != null) {
            args.putString("TRANSCRIPT_DATA", transcript);
        }
        fragment.setArguments(args);
        return fragment;
    }

    // ==================== FORMATTING LOGIC ====================

    private void toggleStyle(int style, ImageView button) {
        int start = notesEditText.getSelectionStart();
        int end = notesEditText.getSelectionEnd();

        if (start == end) {
            Toast.makeText(getContext(), "Please select text first", Toast.LENGTH_SHORT).show();
            return;
        }

        Spannable spannable = notesEditText.getText();
        StyleSpan[] existingSpans = spannable.getSpans(start, end, StyleSpan.class);
        boolean styleExists = false;

        for (StyleSpan span : existingSpans) {
            if (span.getStyle() == style) {
                spannable.removeSpan(span);
                styleExists = true;
            }
        }

        if (!styleExists) {
            spannable.setSpan(new StyleSpan(style), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        if (style == Typeface.BOLD) isBoldActive = !isBoldActive;
        if (style == Typeface.ITALIC) isItalicActive = !isItalicActive;

        updateButtonTint(button, !styleExists);
    }

    private void toggleUnderline(ImageView button) {
        int start = notesEditText.getSelectionStart();
        int end = notesEditText.getSelectionEnd();

        if (start == end) {
            Toast.makeText(getContext(), "Please select text first", Toast.LENGTH_SHORT).show();
            return;
        }

        Spannable spannable = notesEditText.getText();
        UnderlineSpan[] existingSpans = spannable.getSpans(start, end, UnderlineSpan.class);
        boolean exists = false;

        for (UnderlineSpan span : existingSpans) {
            spannable.removeSpan(span);
            exists = true;
        }

        if (!exists) {
            spannable.setSpan(new UnderlineSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        updateButtonTint(button, !exists);
    }

    private void toggleHighlight(ImageView button) {
        int start = notesEditText.getSelectionStart();
        int end = notesEditText.getSelectionEnd();

        if (start == end) {
            Toast.makeText(getContext(), "Please select text first", Toast.LENGTH_SHORT).show();
            return;
        }

        Spannable spannable = notesEditText.getText();
        BackgroundColorSpan[] existingSpans = spannable.getSpans(start, end, BackgroundColorSpan.class);
        boolean exists = false;

        for (BackgroundColorSpan span : existingSpans) {
            spannable.removeSpan(span);
            exists = true;
        }

        if (!exists) {
            // --- RESTORED CUT-OFF CODE HERE ---
            spannable.setSpan(
                    new BackgroundColorSpan(ContextCompat.getColor(requireContext(), R.color.figma_purple_main)),
                    start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
        updateButtonTint(button, !exists);
    }

    private void updateButtonTint(ImageView button, boolean isActive) {
        if (isActive) {
            button.setColorFilter(ContextCompat.getColor(requireContext(), R.color.figma_purple_main));
        } else {
            button.clearColorFilter();
        }
    }

    private void applyBullet() {
        Editable text = notesEditText.getText();
        int start = notesEditText.getSelectionStart();
        int lineStart = start;
        while (lineStart > 0 && text.charAt(lineStart - 1) != '\n') {
            lineStart--;
        }
        if (lineStart < text.length() && text.charAt(lineStart) == '•') {
            return; // Already has bullet
        }
        text.insert(lineStart, "• ");
    }

    private void applyHeading(int level) {
        int start = notesEditText.getSelectionStart();
        int end = notesEditText.getSelectionEnd();
        if (start == end) {
            Toast.makeText(getContext(), "Please select text first", Toast.LENGTH_SHORT).show();
            return;
        }
        Spannable spannable = notesEditText.getText();
        float size = (level == 1) ? 2.0f : (level == 2) ? 1.5f : 1.25f;
        spannable.setSpan(new RelativeSizeSpan(size), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    // ==================== UNDO/REDO ====================

    private void saveStateForUndo() {
        CharSequence currentText = new SpannableStringBuilder(notesEditText.getText());
        undoStack.push(currentText);
        redoStack.clear();
    }

    private void undo() {
        if (undoStack.isEmpty()) return;
        isUndoRedoOperation = true;
        CharSequence currentText = new SpannableStringBuilder(notesEditText.getText());
        redoStack.push(currentText);
        CharSequence previousText = undoStack.pop();
        notesEditText.setText(previousText);
        notesEditText.setSelection(previousText.length());
        isUndoRedoOperation = false;
    }

    private void redo() {
        if (redoStack.isEmpty()) return;
        isUndoRedoOperation = true;
        CharSequence currentText = new SpannableStringBuilder(notesEditText.getText());
        undoStack.push(currentText);
        CharSequence nextText = redoStack.pop();
        notesEditText.setText(nextText);
        notesEditText.setSelection(nextText.length());
        isUndoRedoOperation = false;
    }

    // ==================== SAVING ====================

    private void saveNotes() {
        String notes = notesEditText.getText().toString();
        if (notes.trim().isEmpty()) {
            Toast.makeText(getContext(), "Notes are empty", Toast.LENGTH_SHORT).show();
            return;
        }
        notesRepository.saveNotes(videoUrl, notes, new VideoNotesRepository.OnNoteSavedListener() {
            @Override
            public void onNoteSaved() {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "✅ Notes saved!", Toast.LENGTH_SHORT).show());
                }
            }
            @Override
            public void onError(Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "❌ Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    // ==================== ID EXTRACTION ====================

    private String getVideoIdFromUrl(String url) {
        if (url == null || url.trim().isEmpty()) return null;
        url = url.trim();
        String videoId = null;

        try {
            if (url.contains("youtu.be/")) {
                String[] split = url.split("youtu.be/");
                if (split.length > 1) videoId = split[1];
            }
            else if (url.contains("/shorts/")) {
                String[] split = url.split("/shorts/");
                if (split.length > 1) videoId = split[1];
            }
            else if (url.contains("v=")) {
                // Handles youtube.com/watch?v=ID and m.youtube.com/watch?v=ID
                String[] split = url.split("v=");
                if (split.length > 1) videoId = split[1];
            }
            else if (url.contains("/embed/")) {
                String[] split = url.split("/embed/");
                if (split.length > 1) videoId = split[1];
            }
            // Fallback regex for standard patterns if string split fails
            else {
                Pattern pattern = Pattern.compile("(?<=watch\\?v=|/videos/|embed\\/|youtu.be\\/|/v\\/|/e\\/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%\u200C\u200B2F|youtu.be%2F|%2Fv%2F)[^#\\&\\?\\n]*");
                Matcher matcher = pattern.matcher(url);
                if (matcher.find()) {
                    videoId = matcher.group();
                }
            }

            // Clean up parameters like &t=, ?feature=, etc.
            if (videoId != null) {
                int ampIndex = videoId.indexOf('&');
                if (ampIndex != -1) videoId = videoId.substring(0, ampIndex);

                int qIndex = videoId.indexOf('?');
                if (qIndex != -1) videoId = videoId.substring(0, qIndex);

                int hashIndex = videoId.indexOf('#');
                if (hashIndex != -1) videoId = videoId.substring(0, hashIndex);

                // Ensure we don't have slashes (some URLs have trailing slashes)
                if (videoId.contains("/")) {
                    videoId = videoId.split("/")[0];
                }

                return videoId.trim();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error extracting ID", e);
        }
        return null;
    }
}
