package com.example.learnify.fragments;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.learnify.helpers.CustomToast;
import com.example.learnify.managers.GoogleDriveManager;
import com.example.learnify.R;
import com.example.learnify.repository.VideoNotesRepository;
import com.example.learnify.services.YouTubeTranscriptService;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VideoNotesFragment extends Fragment {

    private static final String TAG = "VideoNotesFragment";

    private EditText notesEditText;
    private ImageView buttonBold, buttonItalic, buttonBullet, buttonUnderline,
            buttonH1, buttonH2, buttonH3, buttonHighlight,
            buttonUndo, buttonRedo, buttonSave, buttonScreenshot, buttonEmbedScreenshot;
    private YouTubePlayerView youTubePlayerView;
    private YouTubePlayer youTubePlayer;
    private String videoUrl;
    private String passedTranscript;
    private View loadingOverlay;
    private FrameLayout videoPlayerContainer;

    // Undo/Redo stacks
    private final Stack<CharSequence> undoStack = new Stack<>();
    private final Stack<CharSequence> redoStack = new Stack<>();
    private boolean isUndoRedoOperation = false;

    // Formatting state
    private boolean isBoldModeActive = false;
    private boolean isItalicModeActive = false;
    private boolean isUnderlineModeActive = false;
    private boolean isHighlightModeActive = false;

    // Anti-spam
    private long lastSaveClickTime = 0;
    private long lastScreenshotClickTime = 0;
    private long lastEmbedScreenshotClickTime = 0;
    private static final long CLICK_DELAY = 1000; // 1 second

    // Repository & services
    private VideoNotesRepository notesRepository;
    private YouTubeTranscriptService transcriptService;
    private GoogleDriveManager driveManager;

    // Permission launcher for screenshot
    private ActivityResultLauncher<String> requestPermissionLauncher;

    // --------------------- FACTORY METHODS -----------------------------------

    public static VideoNotesFragment newInstance(String videoUrl) {
        return newInstance(videoUrl, null);
    }

    public static VideoNotesFragment newInstance(String videoUrl, String transcript) {
        Log.d(TAG, "newInstance() videoUrl=" + videoUrl
                + ", transcript=" + (transcript == null ? "null" : "len=" + transcript.length()));
        VideoNotesFragment fragment = new VideoNotesFragment();
        Bundle args = new Bundle();
        args.putString("VIDEO_URL", videoUrl);
        if (transcript != null) {
            args.putString("TRANSCRIPT_DATA", transcript);
        }
        fragment.setArguments(args);
        return fragment;
    }

    // --------------------- LIFECYCLE ----------------------------------------

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate()");

        if (getArguments() != null) {
            videoUrl = getArguments().getString("VIDEO_URL");
            passedTranscript = getArguments().getString("TRANSCRIPT_DATA");
        }

        Log.d(TAG, "onCreate(): VIDEO_URL=" + videoUrl
                + ", TRANSCRIPT_DATA=" + (passedTranscript == null ? "null" : "len=" + passedTranscript.length()));

        notesRepository = new VideoNotesRepository();
        transcriptService = new YouTubeTranscriptService();
        driveManager = new GoogleDriveManager(requireContext());

        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    Log.d(TAG, "WRITE_EXTERNAL_STORAGE permission result=" + isGranted);
                    if (isGranted) {
                        takeScreenshot();
                    } else {
                        CustomToast.warning(getContext(), "Storage permission required for screenshot");
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView()");
        return inflater.inflate(R.layout.fragment_video_notes, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated()");

        // Bind views
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
        buttonScreenshot = view.findViewById(R.id.button_screenshot);
        loadingOverlay = view.findViewById(R.id.video_loading_overlay);
        videoPlayerContainer = view.findViewById(R.id.video_player_container);
        youTubePlayerView = view.findViewById(R.id.youtube_player_view);

        getLifecycle().addObserver(youTubePlayerView);

        setupVideoPlayer();
        setupFormattingButtons();
        setupTextWatcher();
        loadSavedNotes();
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, "onDestroyView()");
        super.onDestroyView();
        if (youTubePlayerView != null) {
            youTubePlayerView.release();
        }
    }

    // --------------------- VIDEO PLAYER --------------------------------------

    private void setupVideoPlayer() {
        Log.d(TAG, "setupVideoPlayer() videoUrl=" + videoUrl);

        if (videoUrl == null || videoUrl.trim().isEmpty()) {
            Log.e(TAG, "setupVideoPlayer: videoUrl is null or empty");
            if (getContext() != null) {
                CustomToast.error(getContext(), "No video URL provided");
            }
            if (loadingOverlay != null) loadingOverlay.setVisibility(View.GONE);
            return;
        }

        String videoId = getVideoIdFromUrl(videoUrl);
        Log.d(TAG, "setupVideoPlayer: extracted videoId=" + videoId);

        if (videoId != null && !videoId.isEmpty() && isValidVideoId(videoId)) {
            Log.d(TAG, "setupVideoPlayer: valid videoId=" + videoId);

            // ✅ DEFINITION: The variable is created here
            final String sanitizedVideoId = videoId.replaceAll("[^a-zA-Z0-9_-]", "");
            Log.d(TAG, "setupVideoPlayer: sanitizedVideoId=" + sanitizedVideoId);

            youTubePlayerView.addYouTubePlayerListener(new AbstractYouTubePlayerListener() {
                @Override
                public void onReady(@NonNull YouTubePlayer player) {
                    Log.d(TAG, "YouTubePlayer onReady(), loading video");
                    youTubePlayer = player;
                    // ✅ USAGE: Now it is accessible here
                    player.loadVideo(sanitizedVideoId, 0f);
                    if (loadingOverlay != null) loadingOverlay.setVisibility(View.GONE);
                }

                @Override
                public void onError(@NonNull YouTubePlayer player, @NonNull PlayerConstants.PlayerError error) {
                    // Handle Error 15 (Restricted Content)
                    if (error == PlayerConstants.PlayerError.VIDEO_NOT_PLAYABLE_IN_EMBEDDED_PLAYER) {
                        if (getContext() != null) {
                            CustomToast.error(getContext(), "Video restricted! Opening in YouTube App...");

                            try {
                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl));
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            } catch (Exception e) {
                                Log.e(TAG, "Could not open YouTube app", e);
                            }
                        }
                    } else {
                        Log.e(TAG, "YouTube Player Error: " + error.name());
                    }
                }
            });
        } else {
            Log.e(TAG, "setupVideoPlayer: invalid videoId from url=" + videoUrl + " -> " + videoId);
            if (getContext() != null) {
                CustomToast.error(getContext(), "Invalid Video URL");
            }
            if (loadingOverlay != null) loadingOverlay.setVisibility(View.GONE);
        }
    }

    private boolean isValidVideoId(String videoId) {
        if (videoId == null) return false;
        boolean result = videoId.matches("^[a-zA-Z0-9_-]{6,64}$");
        Log.d(TAG, "isValidVideoId(" + videoId + ")=" + result);
        return result;
    }

    // --------------------- NOTES / TRANSCRIPT LOADING ------------------------

    private void loadSavedNotes() {
        Log.d(TAG, "loadSavedNotes() start. passedTranscript="
                + (passedTranscript == null ? "null" : "len=" + passedTranscript.length())
                + ", videoUrl=" + videoUrl);

        // 1) Use passed transcript if available
        if (passedTranscript != null && !passedTranscript.isEmpty()) {
            Log.d(TAG, "Using passedTranscript");
            if (notesEditText.getText().toString().trim().isEmpty()) {
                notesEditText.setText(passedTranscript);
                CustomToast.success(getContext(), "Notes auto-filled from transcript!");
            }
            return;
        }

        // 2) Try repository
        if (videoUrl != null && !videoUrl.isEmpty()) {
            Log.d(TAG, "Loading notes from repository for videoUrl=" + videoUrl);
            notesRepository.loadNotes(videoUrl, new VideoNotesRepository.OnNotesLoadedListener() {
                @Override
                public void onNotesLoaded(String content, Date updatedAt) {
                    Log.d(TAG, "onNotesLoaded: content="
                            + (content == null ? "null" : "len=" + content.length())
                            + ", updatedAt=" + updatedAt);

                    if (getActivity() != null && content != null && !content.isEmpty()) {
                        getActivity().runOnUiThread(() -> {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                notesEditText.setText(Html.fromHtml(content, Html.FROM_HTML_MODE_LEGACY));
                            } else {
                                notesEditText.setText(Html.fromHtml(content));
                            }
                            Log.d(TAG, "Notes loaded from repository into EditText");
                        });
                    } else if (notesEditText.getText().toString().trim().isEmpty()) {
                        Log.d(TAG, "Repository empty; will fetch transcript");
                        fetchTranscriptBackground();
                    }
                }

                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "loadNotes error", e);
                    if (notesEditText.getText().toString().trim().isEmpty()) {
                        Log.d(TAG, "Notes empty after error; will fetch transcript");
                        fetchTranscriptBackground();
                    }
                }
            });
        } else {
            // 3) No videoUrl, still try transcript as a last fallback
            Log.d(TAG, "No videoUrl, checking if we should still fetch transcript");
            if (notesEditText.getText().toString().trim().isEmpty()) {
                fetchTranscriptBackground();
            }
        }
    }

    private void fetchTranscriptBackground() {
        Log.d(TAG, "fetchTranscriptBackground() videoUrl=" + videoUrl);
        if (videoUrl == null || videoUrl.isEmpty()) {
            Log.e(TAG, "fetchTranscriptBackground: videoUrl is null/empty, aborting");
            return;
        }

        transcriptService.getTranscript(videoUrl, new YouTubeTranscriptService.TranscriptCallback() {
            @Override
            public void onSuccess(String transcript) {
                Log.d(TAG, "Transcript success, len=" + transcript.length());
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (notesEditText.getText().toString().trim().isEmpty()) {
                            notesEditText.setText(transcript);
                            CustomToast.success(getContext(), "Transcript loaded automatically");
                        } else {
                            Log.d(TAG, "Notes already filled; not overwriting with transcript");
                        }
                    });
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Transcript error: " + error);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (notesEditText.getText().toString().trim().isEmpty()) {
                            notesEditText.setText("Transcript not available for this video.");
                            CustomToast.warning(getContext(), "Transcript not available");
                        }
                    });
                }
            }
        });
    }

    // --------------------- FORMATTING LOGIC ---------------------------------

    private void setupFormattingButtons() {
        Log.d(TAG, "setupFormattingButtons()");
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

        if (buttonScreenshot != null) {
            buttonScreenshot.setOnClickListener(v -> captureAndEmbedScreenshot());
            buttonScreenshot.setOnLongClickListener(v -> {
                requestScreenshotPermission();
                return true;
            });
        }
    }

    private void setupTextWatcher() {
        Log.d(TAG, "setupTextWatcher()");
        notesEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (count > 0 && count > before) {
                    int newTextStart = start;
                    int newTextEnd = start + count;

                    if (newTextEnd <= s.length()) {
                        Spannable spannable = notesEditText.getText();

                        if (isBoldModeActive) {
                            spannable.setSpan(new StyleSpan(Typeface.BOLD),
                                    newTextStart, newTextEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                        if (isItalicModeActive) {
                            spannable.setSpan(new StyleSpan(Typeface.ITALIC),
                                    newTextStart, newTextEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                        if (isUnderlineModeActive) {
                            spannable.setSpan(new UnderlineSpan(),
                                    newTextStart, newTextEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                        if (isHighlightModeActive && getContext() != null) {
                            spannable.setSpan(
                                    new BackgroundColorSpan(ContextCompat.getColor(requireContext(), R.color.figma_purple_main)),
                                    newTextStart, newTextEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        }
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!isUndoRedoOperation) {
                    saveStateForUndo();
                }
            }
        });
    }

    private void toggleStyle(int style, ImageView button) {
        Log.d(TAG, "toggleStyle() style=" + style);
        int start = notesEditText.getSelectionStart();
        int end = notesEditText.getSelectionEnd();

        if (start == end) {
            if (style == Typeface.BOLD) {
                isBoldModeActive = !isBoldModeActive;
                updateButtonTint(button, isBoldModeActive);
            } else if (style == Typeface.ITALIC) {
                isItalicModeActive = !isItalicModeActive;
                updateButtonTint(button, isItalicModeActive);
            }
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

        updateButtonTint(button, !styleExists);
    }

    private void toggleUnderline(ImageView button) {
        Log.d(TAG, "toggleUnderline()");
        int start = notesEditText.getSelectionStart();
        int end = notesEditText.getSelectionEnd();

        if (start == end) {
            isUnderlineModeActive = !isUnderlineModeActive;
            updateButtonTint(button, isUnderlineModeActive);
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
        Log.d(TAG, "toggleHighlight()");
        int start = notesEditText.getSelectionStart();
        int end = notesEditText.getSelectionEnd();

        if (start == end) {
            isHighlightModeActive = !isHighlightModeActive;
            updateButtonTint(button, isHighlightModeActive);
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
            spannable.setSpan(
                    new BackgroundColorSpan(ContextCompat.getColor(requireContext(), R.color.figma_purple_main)),
                    start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
        updateButtonTint(button, !exists);
    }

    private void updateButtonTint(ImageView button, boolean isActive) {
        if (isActive) {
            button.setBackgroundResource(R.drawable.formatting_button_active);
            button.setColorFilter(ContextCompat.getColor(requireContext(), R.color.white));
        } else {
            button.setBackground(null);
            button.clearColorFilter();
        }
    }

    private void applyBullet() {
        Log.d(TAG, "applyBullet()");
        Editable text = notesEditText.getText();
        int start = notesEditText.getSelectionStart();
        int lineStart = start;
        while (lineStart > 0 && text.charAt(lineStart - 1) != '\n') {
            lineStart--;
        }
        if (lineStart < text.length() && text.charAt(lineStart) == '•') {
            return;
        }
        text.insert(lineStart, "• ");
    }

    private void applyHeading(int level) {
        Log.d(TAG, "applyHeading() level=" + level);
        int start = notesEditText.getSelectionStart();
        int end = notesEditText.getSelectionEnd();
        if (start == end) {
            CustomToast.info(getContext(), "Please select text first");
            return;
        }
        Spannable spannable = notesEditText.getText();
        float size = (level == 1) ? 2.0f : (level == 2) ? 1.5f : 1.25f;
        spannable.setSpan(new RelativeSizeSpan(size), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    // --------------------- UNDO / REDO --------------------------------------

    private void saveStateForUndo() {
        CharSequence currentText = new SpannableStringBuilder(notesEditText.getText());
        undoStack.push(currentText);
        redoStack.clear();
    }

    private void undo() {
        Log.d(TAG, "undo()");
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
        Log.d(TAG, "redo()");
        if (redoStack.isEmpty()) return;
        isUndoRedoOperation = true;
        CharSequence currentText = new SpannableStringBuilder(notesEditText.getText());
        undoStack.push(currentText);
        CharSequence nextText = redoStack.pop();
        notesEditText.setText(nextText);
        notesEditText.setSelection(nextText.length());
        isUndoRedoOperation = false;
    }

    // --------------------- SAVING NOTES -------------------------------------

    private void saveNotes() {
        Log.d(TAG, "saveNotes() clicked");
        long now = System.currentTimeMillis();
        if (now - lastSaveClickTime < CLICK_DELAY) {
            Log.d(TAG, "saveNotes() ignored due to debounce");
            return;
        }
        lastSaveClickTime = now;

        Editable editable = notesEditText.getText();
        if (editable.toString().trim().isEmpty()) {
            CustomToast.warning(getContext(), "Notes are empty");
            return;
        }

        String htmlContent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            htmlContent = Html.toHtml(editable, Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE);
        } else {
            htmlContent = Html.toHtml(editable);
        }

        Log.d(TAG, "saveNotes() saving html length=" + htmlContent.length()
                + " for videoUrl=" + videoUrl);

        notesRepository.saveNotes(videoUrl, htmlContent, new VideoNotesRepository.OnNoteSavedListener() {
            @Override
            public void onNoteSaved() {
                Log.d(TAG, "saveNotes() success");
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                            CustomToast.success(getContext(), "Notes saved!"));
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "saveNotes() error", e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() ->
                            CustomToast.error(getContext(), "Failed: " + e.getMessage()));
                }
            }
        });
    }

    // --------------------- SCREENSHOT / EMBED -------------------------------

    private void requestScreenshotPermission() {
        Log.d(TAG, "requestScreenshotPermission()");
        long now = System.currentTimeMillis();
        if (now - lastScreenshotClickTime < CLICK_DELAY) {
            Log.d(TAG, "requestScreenshotPermission ignored due to debounce");
            return;
        }
        lastScreenshotClickTime = now;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            takeScreenshot();
        } else if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            takeScreenshot();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
    }

    private void takeScreenshot() {
        Log.d(TAG, "takeScreenshot()");
        try {
            View notesCard = getView() != null ? getView().findViewById(R.id.notes_card) : null;
            if (notesCard == null) {
                Log.e(TAG, "takeScreenshot(): notes_card not found");
                CustomToast.error(getContext(), "Cannot capture screenshot");
                return;
            }

            Bitmap bitmap = Bitmap.createBitmap(notesCard.getWidth(), notesCard.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            notesCard.draw(canvas);

            String fileName = "Learnify_Notes_" + System.currentTimeMillis() + ".png";

            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Learnify");
            }

            Uri imageUri = requireContext().getContentResolver().insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            if (imageUri != null) {
                try (OutputStream outputStream = requireContext().getContentResolver().openOutputStream(imageUri)) {
                    if (outputStream != null) {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                        Log.d(TAG, "Screenshot saved to " + imageUri);
                        CustomToast.success(getContext(), "Screenshot saved to gallery!");
                    }
                }
            } else {
                Log.e(TAG, "takeScreenshot(): imageUri null");
                CustomToast.error(getContext(), "Failed to save screenshot");
            }
        } catch (Exception e) {
            Log.e(TAG, "Screenshot failed", e);
            CustomToast.error(getContext(), "Screenshot failed: " + e.getMessage());
        }
    }

    private void captureAndEmbedScreenshot() {
        Log.d(TAG, "captureAndEmbedScreenshot()");
        long now = System.currentTimeMillis();
        if (now - lastEmbedScreenshotClickTime < CLICK_DELAY) {
            Log.d(TAG, "captureAndEmbedScreenshot ignored due to debounce");
            return;
        }
        lastEmbedScreenshotClickTime = now;

        try {
            if (youTubePlayerView == null) {
                Log.e(TAG, "captureAndEmbedScreenshot(): youTubePlayerView null");
                CustomToast.warning(getContext(), "Video not available");
                return;
            }

            Bitmap bitmap = Bitmap.createBitmap(youTubePlayerView.getWidth(), youTubePlayerView.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            youTubePlayerView.draw(canvas);

            CustomToast.info(getContext(), "Capturing screenshot...");

            if (driveManager != null && driveManager.isAvailable()) {
                Log.d(TAG, "Uploading screenshot to Google Drive");
                driveManager.uploadNoteImage(bitmap, videoUrl, new GoogleDriveManager.UploadCallback() {
                    @Override
                    public void onSuccess(String driveUrl, String fileId) {
                        Log.d(TAG, "Drive upload success: " + driveUrl + ", fileId=" + fileId);
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                insertImageIntoNotes(driveUrl);
                                CustomToast.success(getContext(), "Screenshot embedded!");
                            });
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        Log.w(TAG, "Drive upload failed, using base64 fallback", e);
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> insertImageAsBase64(bitmap));
                        }
                    }
                });
            } else {
                Log.d(TAG, "Google Drive not available, using base64 fallback");
                insertImageAsBase64(bitmap);
            }
        } catch (Exception e) {
            Log.e(TAG, "Embed screenshot failed", e);
            CustomToast.error(getContext(), "Failed to capture: " + e.getMessage());
        }
    }

    private void insertImageIntoNotes(String imageUrl) {
        Log.d(TAG, "insertImageIntoNotes() imageUrl=" + imageUrl);
        try {
            int cursorPosition = notesEditText.getSelectionStart();
            Editable editable = notesEditText.getText();

            String imageTag = "\n[📸 Screenshot]\n";
            editable.insert(cursorPosition, imageTag);

            notesEditText.setSelection(cursorPosition + imageTag.length());
            Log.d(TAG, "Image tag inserted at " + cursorPosition);
        } catch (Exception e) {
            Log.e(TAG, "Failed to insert image tag", e);
        }
    }

    private void insertImageAsBase64(Bitmap bitmap) {
        Log.d(TAG, "insertImageAsBase64()");
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(
                    bitmap,
                    Math.min(bitmap.getWidth(), 400),
                    Math.min(bitmap.getHeight(), 300),
                    true
            );
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            byte[] imageBytes = baos.toByteArray();
            String base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);
            Log.d(TAG, "Base64 image size=" + imageBytes.length);

            int cursorPosition = notesEditText.getSelectionStart();
            Editable editable = notesEditText.getText();
            String imageTag = "\n[📸 Screenshot embedded]\n";
            editable.insert(cursorPosition, imageTag);
            notesEditText.setSelection(cursorPosition + imageTag.length());

            CustomToast.success(getContext(), "Screenshot embedded (offline mode)");
        } catch (Exception e) {
            Log.e(TAG, "Failed to embed base64 image", e);
            CustomToast.error(getContext(), "Failed to embed screenshot");
        }
    }

    // --------------------- VIDEO ID EXTRACTION -------------------------------

    private String getVideoIdFromUrl(String url) {
        Log.d(TAG, "getVideoIdFromUrl() url=" + url);
        if (url == null || url.trim().isEmpty()) return null;
        url = url.trim();
        String videoId = null;

        try {
            if (url.contains("youtu.be/")) {
                String[] split = url.split("youtu.be/");
                if (split.length > 1) videoId = split[1];
            } else if (url.contains("/shorts/")) {
                String[] split = url.split("/shorts/");
                if (split.length > 1) videoId = split[1];
            } else if (url.contains("v=")) {
                String[] split = url.split("v=");
                if (split.length > 1) videoId = split[1];
            } else if (url.contains("/embed/")) {
                String[] split = url.split("/embed/");
                if (split.length > 1) videoId = split[1];
            } else {
                Pattern pattern = Pattern.compile("(?<=watch\\?v=|/videos/|embed/|youtu.be/|/v/|/e/|watch\\?v%3D|watch\\?feature=player_embedded&v=|%2Fvideos%2F|embed%2F|youtu.be%2F|%2Fv%2F)[^#&?\\n]*");
                Matcher matcher = pattern.matcher(url);
                if (matcher.find()) {
                    videoId = matcher.group();
                }
            }

            if (videoId != null) {
                int ampIndex = videoId.indexOf('&');
                if (ampIndex != -1) videoId = videoId.substring(0, ampIndex);

                int qIndex = videoId.indexOf('?');
                if (qIndex != -1) videoId = videoId.substring(0, qIndex);

                int hashIndex = videoId.indexOf('#');
                if (hashIndex != -1) videoId = videoId.substring(0, hashIndex);

                if (videoId.contains("/")) {
                    videoId = videoId.split("/")[0];
                }

                videoId = videoId.trim();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error extracting videoId from url=" + url, e);
        }

        Log.d(TAG, "getVideoIdFromUrl() -> " + videoId);
        return videoId;
    }
}