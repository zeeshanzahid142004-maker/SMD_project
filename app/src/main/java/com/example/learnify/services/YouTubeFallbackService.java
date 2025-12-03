package com.example.learnify.services;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YouTubeFallbackService {

    private static final String TAG = "YouTubeFallback";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface MetadataCallback {
        void onSuccess(String content); // Returns combined Title + Desc + Tags
        void onError(String error);
    }

    public void getVideoMetadata(String videoUrl, MetadataCallback callback) {
        executor.execute(() -> {
            try {
                // 1. Prepare the connection
                URL url = new URL(videoUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                // IMPORTANT: Use a Desktop User-Agent to ensure we get the full HTML page
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                // 2. Read the page content
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder html = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    html.append(line);
                }
                reader.close();

                String fullHtml = html.toString();

                // 3. Extract Data using Regex (Robust for Meta tags)
                String title = extractMetaTag(fullHtml, "title");
                String description = extractMetaTag(fullHtml, "description");
                String keywords = extractMetaTag(fullHtml, "keywords");

                // 4. Validate
                if (description.isEmpty() && title.isEmpty()) {
                    notifyError(callback, "Could not extract metadata");
                    return;
                }

                // 5. Format the result for the AI
                StringBuilder result = new StringBuilder();
                result.append("Video Title: ").append(title).append("\n\n");
                result.append("Description:\n").append(description).append("\n\n");
                if (!keywords.isEmpty()) {
                    result.append("Keywords/Tags: ").append(keywords);
                }

                Log.d(TAG, "Fallback extraction successful. Length: " + result.length());
                notifySuccess(callback, result.toString());

            } catch (Exception e) {
                Log.e(TAG, "Fallback failed", e);
                notifyError(callback, e.getMessage());
            }
        });
    }

    // Helper to find <meta name="..." content="...">
    private String extractMetaTag(String html, String name) {
        try {
            // Looks for <meta name="description" content="THIS TEXT">
            Pattern pattern = Pattern.compile("<meta name=\"" + name + "\" content=\"(.*?)\"");
            Matcher matcher = pattern.matcher(html);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not extract " + name);
        }
        return "";
    }

    private void notifySuccess(MetadataCallback callback, String content) {
        mainHandler.post(() -> callback.onSuccess(content));
    }

    private void notifyError(MetadataCallback callback, String error) {
        mainHandler.post(() -> callback.onError(error));
    }
}