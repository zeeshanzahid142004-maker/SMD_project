package com.example.learnify;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java. util.regex.Matcher;
import java. util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class YouTubeTranscriptService {

    private static final String TAG = "YouTubeTranscript";
    private static final OkHttpClient client = new OkHttpClient();

    public interface TranscriptCallback {
        void onSuccess(String transcript);
        void onError(String error);
    }

    public void getTranscript(String videoUrl, TranscriptCallback callback) {
        getTranscript(videoUrl, null, callback);
    }

    public void getTranscript(String videoUrl, String targetLanguage, TranscriptCallback callback) {
        String videoId = extractVideoId(videoUrl);
        if (videoId == null) {
            callback.onError("Invalid YouTube URL");
            return;
        }

        Log.d(TAG, "Fetching transcript for: " + videoId);
        fetchFromVideoPage(videoId, targetLanguage, callback);
    }

    private void fetchFromVideoPage(String videoId, String targetLang, TranscriptCallback callback) {
        String url = "https://www.youtube.com/watch?v=" + videoId;

        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Accept-Language", "en-US,en;q=0.9")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to fetch video page", e);
                callback.onError("Network error: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onError("Failed to load video");
                    return;
                }

                String html = response.body().string();
                String captionUrl = extractCaptionUrl(html);
                String sourceLang = extractLanguage(html);

                if (captionUrl != null) {
                    fetchCaptions(captionUrl, sourceLang, targetLang, callback);
                } else {
                    callback.onError("No captions available");
                }
            }
        });
    }

    private String extractCaptionUrl(String html) {
        try {
            Pattern pattern = Pattern.compile("\"captionTracks\":\\s*\\[(.*?)\\]");
            Matcher matcher = pattern.matcher(html);

            if (matcher.find()) {
                String json = "[" + matcher.group(1) + "]";
                JSONArray tracks = new JSONArray(json);

                for (int i = 0; i < tracks.length(); i++) {
                    JSONObject track = tracks.getJSONObject(i);
                    String baseUrl = track.optString("baseUrl");
                    if (baseUrl != null && ! baseUrl.isEmpty()) {
                        return URLDecoder.decode(baseUrl, "UTF-8");
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error extracting caption URL", e);
        }
        return null;
    }

    private String extractLanguage(String html) {
        try {
            Pattern pattern = Pattern.compile("\"languageCode\":\\s*\"([a-z]{2})\"");
            Matcher matcher = pattern.matcher(html);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error extracting language", e);
        }
        return "en";
    }

    private void fetchCaptions(String captionUrl, String sourceLang, String targetLang, TranscriptCallback callback) {
        Request request = new Request.Builder()
                .url(captionUrl)
                .header("User-Agent", "Mozilla/5.0")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Failed to fetch captions");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body().string();
                String transcript = parseXmlCaptions(body);

                if (transcript != null && !transcript.isEmpty()) {
                    // Translate if needed
                    if (targetLang != null && ! targetLang.equals(sourceLang)) {
                        translateText(transcript, sourceLang, targetLang, callback);
                    } else {
                        callback.onSuccess(transcript);
                    }
                } else {
                    callback.onError("Could not parse captions");
                }
            }
        });
    }

    private String parseXmlCaptions(String xml) {
        StringBuilder sb = new StringBuilder();
        try {
            Pattern pattern = Pattern.compile("<text[^>]*>([^<]*)</text>");
            Matcher matcher = pattern.matcher(xml);

            while (matcher.find()) {
                String text = matcher.group(1)
                        .replace("&amp;", "&")
                        .replace("&lt;", "<")
                        .replace("&gt;", ">")
                        .replace("&quot;", "\"")
                        .replace("&#39;", "'")
                        .replace("&nbsp;", " ");
                sb.append(text). append(" ");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing XML", e);
        }

        return sb.toString()
                .replaceAll("\\s+", " ")
                .replaceAll("\\[.*?\\]", "")
                .trim();
    }

    private void translateText(String text, String from, String to, TranscriptCallback callback) {
        try {
            // Limit text length for API
            String limitedText = text.length() > 4000 ? text.substring(0, 4000) : text;
            String encoded = URLEncoder.encode(limitedText, "UTF-8");
            String url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl="
                    + from + "&tl=" + to + "&dt=t&q=" + encoded;

            Request request = new Request.Builder()
                    . url(url)
                    . header("User-Agent", "Mozilla/5.0")
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    // Return original on failure
                    callback.onSuccess(text);
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String body = response.body().string();
                        JSONArray arr = new JSONArray(body);
                        JSONArray translations = arr.getJSONArray(0);

                        StringBuilder result = new StringBuilder();
                        for (int i = 0; i < translations.length(); i++) {
                            result.append(translations.getJSONArray(i).getString(0));
                        }
                        callback.onSuccess(result.toString());
                    } catch (Exception e) {
                        callback.onSuccess(text); // Return original on error
                    }
                }
            });
        } catch (Exception e) {
            callback.onSuccess(text);
        }
    }

    public String extractVideoId(String url) {
        if (url == null || url.isEmpty()) return null;

        String videoId = null;
        url = url.trim();

        try {
            if (url.contains("youtu.be/")) {
                videoId = url.split("youtu.be/")[1];
            } else if (url.contains("/shorts/")) {
                videoId = url.split("/shorts/")[1];
            } else if (url.contains("v=")) {
                videoId = url.split("v=")[1];
            } else if (url.contains("/embed/")) {
                videoId = url.split("/embed/")[1];
            }

            if (videoId != null) {
                if (videoId.contains("&")) videoId = videoId.split("&")[0];
                if (videoId.contains("?")) videoId = videoId.split("\\?")[0];
                if (videoId.contains("#")) videoId = videoId.split("#")[0];
                if (videoId.contains("/")) videoId = videoId.split("/")[0];
                videoId = videoId.trim();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error extracting video ID", e);
        }

        return videoId;
    }
}