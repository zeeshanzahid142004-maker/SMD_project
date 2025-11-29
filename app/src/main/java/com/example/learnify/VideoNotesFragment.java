package com.example.learnify;

import android.Manifest;
import android.content.ContentValues;
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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

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
            buttonUndo, buttonRedo, buttonSave, buttonScreenshot;
    private WebView youTubeWebView;
    private String videoUrl;
    private String passedTranscript;
    private View loadingOverlay;
    private FrameLayout videoPlayerContainer;

    // Undo/Redo stacks
    private final Stack<CharSequence> undoStack = new Stack<>();
    private final Stack<CharSequence> redoStack = new Stack<>();
    private boolean isUndoRedoOperation = false;

    // Formatting State Toggles
    private boolean isBoldActive = false;
    private boolean isItalicActive = false;
    private boolean isUnderlineActive = false;
    private boolean isHighlightActive = false;

    // Anti-spam protection
    private long lastSaveClickTime = 0;
    private long lastScreenshotClickTime = 0;
    private static final long CLICK_DELAY = 1000; // 1 second

    // Repository & Service
    private VideoNotesRepository notesRepository;
    private YouTubeTranscriptService transcriptService;

    // Permission launcher for screenshot
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            videoUrl = getArguments().getString("VIDEO_URL");
            passedTranscript = getArguments().getString("TRANSCRIPT_DATA");
        }

        notesRepository = new VideoNotesRepository();
        transcriptService = new YouTubeTranscriptService();

        // Initialize permission launcher for screenshot
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        takeScreenshot();
                    } else {
                        Toast.makeText(getContext(), "Storage permission required for screenshot", Toast.LENGTH_SHORT).show();
                    }
                }
        );
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
        buttonScreenshot = view.findViewById(R.id.button_screenshot);
        loadingOverlay = view.findViewById(R.id.video_loading_overlay);
        videoPlayerContainer = view.findViewById(R.id.video_player_container);
        youTubeWebView = view.findViewById(R.id.youtube_webview);

        setupVideoPlayer();
        setupFormattingButtons();
        setupTextWatcher();
        loadSavedNotes();
    }

    private void setupVideoPlayer() {
        // Configure WebView for YouTube video playback
        WebSettings webSettings = youTubeWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);

        youTubeWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (loadingOverlay != null) {
                    loadingOverlay.setVisibility(View.GONE);
                }
            }
        });

        youTubeWebView.setWebChromeClient(new WebChromeClient() {
            private View customView;
            private WebChromeClient.CustomViewCallback customViewCallback;

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                customView = view;
                customViewCallback = callback;
                if (videoPlayerContainer != null) {
                    videoPlayerContainer.addView(view);
                }
            }

            @Override
            public void onHideCustomView() {
                if (customView != null && videoPlayerContainer != null) {
                    videoPlayerContainer.removeView(customView);
                    customView = null;
                }
                if (customViewCallback != null) {
                    customViewCallback.onCustomViewHidden();
                }
            }
        });

        // Load the video
        String videoId = getVideoIdFromUrl(videoUrl);
        if (videoId != null && !videoId.isEmpty()) {
            Log.d(TAG, "✅ Loading Video ID: " + videoId);
            String embedUrl = "https://www.youtube.com/embed/" + videoId + "?autoplay=1&playsinline=1&rel=0";
            String html = "<html><body style='margin:0;padding:0;background:#000;'>" +
                    "<iframe width='100%' height='100%' src='" + embedUrl + "' " +
                    "frameborder='0' allow='accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture' " +
                    "allowfullscreen></iframe></body></html>";
            youTubeWebView.loadData(html, "text/html", "utf-8");
        } else {
            if (getContext() != null) {
                Toast.makeText(getContext(), "Invalid Video URL", Toast.LENGTH_SHORT).show();
            }
            if (loadingOverlay != null) {
                loadingOverlay.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (youTubeWebView != null) {
            youTubeWebView.loadUrl("about:blank");
            youTubeWebView.destroy();
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
        // Try loading from repository
        else if (videoUrl != null && !videoUrl.isEmpty()) {
            notesRepository.loadNotes(videoUrl, new VideoNotesRepository.OnNotesLoadedListener() {
                @Override
                public void onNotesLoaded(String content, Date updatedAt) {
                    if (getActivity() != null && content != null && !content.isEmpty()) {
                        getActivity().runOnUiThread(() -> {
                            // Load HTML content back as Spannable
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                notesEditText.setText(Html.fromHtml(content, Html.FROM_HTML_MODE_LEGACY));
                            } else {
                                notesEditText.setText(Html.fromHtml(content));
                            }
                        });
                    } else if (notesEditText.getText().toString().trim().isEmpty()) {
                        fetchTranscriptBackground();
                    }
                }

                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Failed to load notes", e);
                    if (notesEditText.getText().toString().trim().isEmpty()) {
                        fetchTranscriptBackground();
                    }
                }
            });
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
        
        if (buttonScreenshot != null) {
            buttonScreenshot.setOnClickListener(v -> requestScreenshotPermission());
        }
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
        // Anti-spam protection
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSaveClickTime < CLICK_DELAY) {
            return;
        }
        lastSaveClickTime = currentTime;

        Editable editable = notesEditText.getText();
        if (editable.toString().trim().isEmpty()) {
            Toast.makeText(getContext(), "Notes are empty", Toast.LENGTH_SHORT).show();
            return;
        }

        // Convert Spannable to HTML to preserve formatting
        String htmlContent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            htmlContent = Html.toHtml(editable, Html.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE);
        } else {
            htmlContent = Html.toHtml(editable);
        }

        notesRepository.saveNotes(videoUrl, htmlContent, new VideoNotesRepository.OnNoteSavedListener() {
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

    // ==================== SCREENSHOT ====================

    private void requestScreenshotPermission() {
        // Anti-spam protection
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastScreenshotClickTime < CLICK_DELAY) {
            return;
        }
        lastScreenshotClickTime = currentTime;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ doesn't need permission for MediaStore
            takeScreenshot();
        } else if (ContextCompat.checkSelfPermission(requireContext(), 
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            takeScreenshot();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
    }

    private void takeScreenshot() {
        try {
            View notesCard = getView() != null ? getView().findViewById(R.id.notes_card) : null;
            if (notesCard == null) {
                Toast.makeText(getContext(), "Cannot capture screenshot", Toast.LENGTH_SHORT).show();
                return;
            }

            // Create bitmap from the notes card
            Bitmap bitmap = Bitmap.createBitmap(notesCard.getWidth(), notesCard.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            notesCard.draw(canvas);

            // Save to gallery
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
                        Toast.makeText(getContext(), "📸 Screenshot saved to gallery!", Toast.LENGTH_SHORT).show();
                    }
                }
            } else {
                Toast.makeText(getContext(), "Failed to save screenshot", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Screenshot failed", e);
            Toast.makeText(getContext(), "Screenshot failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
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
