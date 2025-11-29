package com.example.learnify;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

import jp.wasabeef.richeditor.RichEditor;

public class VideoNotesFragment extends Fragment {

    private static final String TAG = "VideoNotesFragment";
    private static final String ARG_VIDEO_URL = "video_url";
    private static final String ARG_TRANSCRIPT = "transcript";

    private String videoUrl;
    private String passedTranscript;

    private RichEditor richEditor;
    private YouTubePlayerView youTubePlayerView;
    private View loadingOverlay;

    private ImageView buttonBold, buttonItalic, buttonUnderline, buttonBullet,
            buttonH1, buttonH2, buttonH3, buttonHighlight,
            buttonUndo, buttonRedo, buttonSave;

    private VideoNotesRepository notesRepository;
    private YouTubeTranscriptService transcriptService;

    public static VideoNotesFragment newInstance(String videoUrl, String transcript) {
        VideoNotesFragment fragment = new VideoNotesFragment();
        Bundle args = new Bundle();
        args.putString(ARG_VIDEO_URL, videoUrl);
        args.putString(ARG_TRANSCRIPT, transcript);
        fragment.setArguments(args);
        return fragment;
    }
    public static VideoNotesFragment newInstance(String videoUrl) {
        VideoNotesFragment fragment = new VideoNotesFragment();
        Bundle args = new Bundle();
        args.putString(ARG_VIDEO_URL, videoUrl);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            videoUrl = getArguments().getString(ARG_VIDEO_URL);
            passedTranscript = getArguments().getString(ARG_TRANSCRIPT);
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

        richEditor = view.findViewById(R.id.rich_editor);
        youTubePlayerView = view.findViewById(R.id.youtube_player_view);
        loadingOverlay = view.findViewById(R.id.video_loading_overlay);

        buttonBold = view.findViewById(R.id.button_bold);
        buttonItalic = view.findViewById(R.id.button_italic);
        buttonUnderline = view.findViewById(R.id.button_underline);
        buttonBullet = view.findViewById(R.id.button_bullet);
        buttonH1 = view.findViewById(R.id.button_h1);
        buttonH2 = view.findViewById(R.id.button_h2);
        buttonH3 = view.findViewById(R.id.button_h3);
        buttonHighlight = view.findViewById(R.id.button_highlight);
        buttonUndo = view.findViewById(R.id.button_undo);
        buttonRedo = view.findViewById(R.id.button_redo);
        buttonSave = view.findViewById(R.id.button_save);

        loadingOverlay.setClickable(true);
        loadingOverlay.setOnClickListener(v -> {
            String vid = extractVideoId(videoUrl);
            if (vid != null) openInYouTubeAppOrBrowser(vid);
            else Toast.makeText(getContext(), "Invalid video link", Toast.LENGTH_SHORT).show();
        });

        setupRichEditorToolbar();
        loadTranscriptOrFetch();
        setupYouTubePlayer();
    }

    private void setupRichEditorToolbar() {
        buttonBold.setOnClickListener(v -> richEditor.setBold());
        buttonItalic.setOnClickListener(v -> richEditor.setItalic());
        buttonUnderline.setOnClickListener(v -> richEditor.setUnderline());
        buttonBullet.setOnClickListener(v -> richEditor.setBullets());
        buttonH1.setOnClickListener(v -> richEditor.setHeading(1));
        buttonH2.setOnClickListener(v -> richEditor.setHeading(2));
        buttonH3.setOnClickListener(v -> richEditor.setHeading(3));
        buttonHighlight.setOnClickListener(v -> richEditor.setTextBackgroundColor(0xFFFFFF00)); // yellow
        buttonSave.setOnClickListener(v -> saveNotes());
        // Undo/Redo can be implemented with RichEditor commands if library supports
    }

    private void loadTranscriptOrFetch() {
        if (passedTranscript != null && !passedTranscript.isEmpty()) {
            richEditor.setHtml(passedTranscript);
        } else {
            fetchTranscriptBackground();
        }
    }

    private void fetchTranscriptBackground() {
        if (videoUrl == null || videoUrl.isEmpty()) return;
        String vid = extractVideoId(videoUrl);
        if (vid == null) return;

        transcriptService.getTranscript(videoUrl, new YouTubeTranscriptService.TranscriptCallback() {
            @Override
            public void onSuccess(String rawTranscript) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    String parsed = parseTranscript(rawTranscript);
                    if (parsed != null && !parsed.trim().isEmpty()) {
                        richEditor.setHtml(parsed);
                    }
                });
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Transcript fetch failed: " + error);
            }
        });
    }

    private void setupYouTubePlayer() {
        getLifecycle().addObserver(youTubePlayerView);

        youTubePlayerView.addYouTubePlayerListener(new AbstractYouTubePlayerListener() {
            @Override
            public void onReady(@NonNull YouTubePlayer youTubePlayer) {
                String videoId = extractVideoId(videoUrl);
                if (videoId != null) {
                    try {
                        youTubePlayer.cueVideo(videoId, 0f);
                        loadingOverlay.setVisibility(View.GONE);
                    } catch (Exception e) {
                        Log.e(TAG, "Embed failed, fallback to YouTube intent", e);
                        loadingOverlay.setVisibility(View.VISIBLE);
                        Toast.makeText(getContext(), "Playback blocked. Tap to open YouTube.", Toast.LENGTH_LONG).show();
                    }
                }
            }

            @Override
            public void onError(@NonNull YouTubePlayer player,
                                @NonNull com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants.PlayerError error) {
                loadingOverlay.setVisibility(View.VISIBLE);
                Toast.makeText(getContext(),
                        "Video cannot be played (" + error.name() + "). Tap to open YouTube.",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void saveNotes() {
        String notes = richEditor.getHtml();
        if (notes == null || notes.trim().isEmpty()) {
            Toast.makeText(getContext(), "Notes are empty", Toast.LENGTH_SHORT).show();
            return;
        }
        notesRepository.saveNotes(videoUrl, notes, new VideoNotesRepository.OnNoteSavedListener() {
            @Override
            public void onNoteSaved() {
                if (getActivity() != null)
                    getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "✅ Notes saved!", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onError(Exception e) {
                if (getActivity() != null)
                    getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "❌ Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }

    private String extractVideoId(String url) {
        if (url == null) return null;
        url = url.trim();
        if (url.matches("^[A-Za-z0-9_-]{11}$")) return url;
        String pattern = "(?:v=|v%3D|youtu\\.be/|/embed/|/shorts/)([A-Za-z0-9_-]{11})";
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(pattern).matcher(url);
        if (matcher.find()) return matcher.group(1);
        return null;
    }

    private void openInYouTubeAppOrBrowser(String videoId) {
        if (videoId == null) return;
        Intent appIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + videoId));
        Intent webIntent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://www.youtube.com/watch?v=" + videoId));
        try {
            startActivity(appIntent);
        } catch (ActivityNotFoundException ex) {
            startActivity(webIntent);
        }
    }

    private String parseTranscript(String raw) {
        if (raw == null) return null;
        return raw.replaceAll("<[^>]+>", "").trim(); // simple HTML/XML cleanup
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (youTubePlayerView != null) youTubePlayerView.release();
    }
}
