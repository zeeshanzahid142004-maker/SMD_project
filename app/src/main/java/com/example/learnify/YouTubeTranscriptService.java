package com.example.learnify;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class YouTubeTranscriptService {

    private static final String TAG = "YouTubeTranscript";
    private final OkHttpClient client;
    private final Handler mainHandler;

    public interface TranscriptCallback {
        void onSuccess(String transcript);
        void onError(String error);
    }

    public YouTubeTranscriptService() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Extract transcript from YouTube video
     * Works with videos in any language, extracts available captions
     */
    public void getTranscript(String videoUrl, TranscriptCallback callback) {
        String videoId = extractVideoId(videoUrl);
        if (videoId == null) {
            callback.onError("Invalid YouTube URL");
            return;
        }

        Log.d(TAG, "🎥 Fetching transcript for video ID: " + videoId);

        // First, fetch the video page to get caption tracks
        String videoPageUrl = "https://www.youtube.com/watch?v=" + videoId;

        Request request = new Request.Builder()
                .url(videoPageUrl)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "❌ Failed to fetch video page", e);
                mainHandler.post(() -> callback.onError("Network error: " + e.getMessage()));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    mainHandler.post(() -> callback.onError("Failed to load video page: " + response.code()));
                    return;
                }

                String html = response.body().string();
                String captionUrl = extractCaptionUrl(html);

                if (captionUrl == null) {
                    mainHandler.post(() -> callback.onError("No captions available for this video"));
                    return;
                }

                // Fetch the actual transcript
                fetchTranscriptFromUrl(captionUrl, callback);
            }
        });
    }

    /**
     * Extract caption URL from YouTube page HTML
     */
    private String extractCaptionUrl(String html) {
        try {
            // Look for captionTracks in the page source
            Pattern pattern = Pattern.compile("\"captionTracks\":\\[\\{[^\\]]+\\}\\]");
            Matcher matcher = pattern.matcher(html);

            if (matcher.find()) {
                String captionJson = matcher.group();

                // Extract the baseUrl from the first caption track
                Pattern urlPattern = Pattern.compile("\"baseUrl\":\"([^\"]+)\"");
                Matcher urlMatcher = urlPattern.matcher(captionJson);

                if (urlMatcher.find()) {
                    String url = urlMatcher.group(1);
                    // Unescape the URL
                    url = url.replace("\\u0026", "&");
                    Log.d(TAG, "✅ Found caption URL");
                    return url;
                }
            }

            // Alternative: Try to find timedtext URL
            pattern = Pattern.compile("\"timedtext\"[^}]+\"baseUrl\":\"([^\"]+)\"");
            matcher = pattern.matcher(html);

            if (matcher.find()) {
                String url = matcher.group(1);
                url = url.replace("\\u0026", "&");
                Log.d(TAG, "✅ Found timedtext URL");
                return url;
            }

            Log.w(TAG, "⚠️ No caption URL found in HTML");
            return null;

        } catch (Exception e) {
            Log.e(TAG, "❌ Error extracting caption URL", e);
            return null;
        }
    }

    /**
     * Fetch and parse the transcript from caption URL
     */
    private void fetchTranscriptFromUrl(String captionUrl, TranscriptCallback callback) {
        Request request = new Request.Builder()
                .url(captionUrl)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "❌ Failed to fetch transcript", e);
                mainHandler.post(() -> callback.onError("Failed to fetch transcript: " + e.getMessage()));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    mainHandler.post(() -> callback.onError("Failed to load transcript: " + response.code()));
                    return;
                }

                String xml = response.body().string();
                String transcript = parseTranscriptXml(xml);

                if (transcript == null || transcript.isEmpty()) {
                    mainHandler.post(() -> callback.onError("Failed to parse transcript"));
                    return;
                }

                Log.d(TAG, "✅ Transcript extracted. Length: " + transcript.length() + " characters");
                mainHandler.post(() -> callback.onSuccess(transcript));
            }
        });
    }

    /**
     * Parse XML transcript and extract text
     */
    private String parseTranscriptXml(String xml) {
        try {
            StringBuilder transcript = new StringBuilder();

            // Extract text from <text> tags
            Pattern pattern = Pattern.compile("<text[^>]*>([^<]+)</text>");
            Matcher matcher = pattern.matcher(xml);

            while (matcher.find()) {
                String text = matcher.group(1);
                // Decode HTML entities
                text = decodeHtmlEntities(text);
                transcript.append(text).append(" ");
            }

            return transcript.toString().trim();

        } catch (Exception e) {
            Log.e(TAG, "❌ Error parsing transcript XML", e);
            return null;
        }
    }

    /**
     * Decode common HTML entities
     */
    private String decodeHtmlEntities(String text) {
        return text
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&nbsp;", " ")
                .replaceAll("\\[Music\\]", "")
                .replaceAll("\\[Applause\\]", "")
                .trim();
    }

    /**
     * Extract video ID from various YouTube URL formats
     */
    private String extractVideoId(String url) {
        if (url == null || url.trim().isEmpty()) {
            return null;
        }

        // Patterns for different YouTube URL formats
        String[] patterns = {
                "(?<=watch\\?v=)[^#\\&\\?]*",
                "(?<=youtu.be/)[^#\\&\\?]*",
                "(?<=embed/)[^#\\&\\?]*",
                "(?<=v=)[^#\\&\\?]*"
        };

        for (String pattern : patterns) {
            Pattern compiledPattern = Pattern.compile(pattern);
            Matcher matcher = compiledPattern.matcher(url);
            if (matcher.find()) {
                String videoId = matcher.group();
                Log.d(TAG, "✅ Extracted video ID: " + videoId);
                return videoId;
            }
        }

        Log.e(TAG, "❌ Could not extract video ID from: " + url);
        return null;
    }
}