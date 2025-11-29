package com.example.learnify;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
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
    private static final String YOUTUBE_URL = "https://www.youtube.com";
    private static final String[] SUPPORTED_LANGUAGES = {"en", "hi", "es", "ar", "fr", "de", "pt", "ru", "ja", "ko", "zh"};

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    public interface TranscriptCallback {
        void onSuccess(String transcript);
        void onError(String error);
    }

    public void getTranscript(String videoUrl, TranscriptCallback callback) {
        getTranscript(videoUrl, null, callback);
    }

    public void getTranscript(String videoUrl, String targetLang, TranscriptCallback callback) {
        String videoId = extractVideoId(videoUrl);
        if (videoId == null || videoId.isEmpty()) {
            callback.onError("Invalid YouTube URL");
            return;
        }
        Log.d(TAG, "Fetching transcript for: " + videoId);
        fetchPage(videoId, targetLang, callback);
    }

    private void fetchPage(String videoId, String targetLang, TranscriptCallback callback) {
        String url = YOUTUBE_URL + "/watch?v=" + videoId;

        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0")
                .header("Accept-Language", "en-US,en;q=0.9")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError("Network error");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onError("HTTP " + response.code());
                    return;
                }
                String html = response.body().string();
                String captionUrl = findCaptionUrl(html);
                String lang = findLang(html);

                if (captionUrl != null) {
                    getCaptions(captionUrl, lang, targetLang, callback);
                } else {
                    tryApi(videoId, targetLang, callback);
                }
            }
        });
    }

    private void tryApi(String videoId, String targetLang, TranscriptCallback callback) {
        // Try manual captions first, then fallback to auto-generated
        tryLangManual(videoId, SUPPORTED_LANGUAGES, 0, targetLang, callback);
    }

    private void tryLangManual(String videoId, String[] langs, int i, String targetLang, TranscriptCallback cb) {
        if (i >= langs.length) {
            // Manual captions exhausted, try auto-generated captions
            Log.d(TAG, "Manual captions not found, trying auto-generated captions");
            tryLangAsr(videoId, langs, 0, targetLang, cb);
            return;
        }

        String url = YOUTUBE_URL + "/api/timedtext?v=" + videoId + "&lang=" + langs[i] + "&fmt=json3";

        Request req = new Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build();

        client.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call c, IOException e) {
                tryLangManual(videoId, langs, i + 1, targetLang, cb);
            }

            @Override
            public void onResponse(Call c, Response r) throws IOException {
                if (r.isSuccessful()) {
                    String body = r.body().string();
                    if (body.contains("events")) {
                        String text = parseJson(body);
                        if (text.length() > 50) {
                            finish(text, langs[i], targetLang, cb);
                            return;
                        }
                    }
                }
                tryLangManual(videoId, langs, i + 1, targetLang, cb);
            }
        });
    }

    private void tryLangAsr(String videoId, String[] langs, int i, String targetLang, TranscriptCallback cb) {
        if (i >= langs.length) {
            cb.onError("No captions available");
            return;
        }

        // Add kind=asr parameter for auto-generated captions
        String url = YOUTUBE_URL + "/api/timedtext?v=" + videoId + "&lang=" + langs[i] + "&kind=asr&fmt=json3";

        Request req = new Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build();

        client.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call c, IOException e) {
                tryLangAsr(videoId, langs, i + 1, targetLang, cb);
            }

            @Override
            public void onResponse(Call c, Response r) throws IOException {
                if (r.isSuccessful()) {
                    String body = r.body().string();
                    if (body.contains("events")) {
                        String text = parseJson(body);
                        if (text.length() > 50) {
                            Log.d(TAG, "Found auto-generated captions in: " + langs[i]);
                            finish(text, langs[i], targetLang, cb);
                            return;
                        }
                    }
                }
                tryLangAsr(videoId, langs, i + 1, targetLang, cb);
            }
        });
    }

    private String findCaptionUrl(String html) {
        try {
            Pattern p = Pattern.compile("\"captionTracks\":\\s*\\[\\{.*?\"baseUrl\":\\s*\"([^\"]+)\"", Pattern.DOTALL);
            Matcher m = p.matcher(html);
            if (m.find()) {
                return m.group(1).replace("\\u0026", "&").replace("\\/", "/");
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String findLang(String html) {
        try {
            Pattern p = Pattern.compile("\"languageCode\":\\s*\"([a-z]{2})\"");
            Matcher m = p.matcher(html);
            if (m.find()) return m.group(1);
        } catch (Exception ignored) {}
        return "en";
    }

    private void getCaptions(String capUrl, String srcLang, String tgtLang, TranscriptCallback cb) {
        String url = capUrl + (capUrl.contains("?") ? "&" : "?") + "fmt=json3";

        Request req = new Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build();

        client.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call c, IOException e) {
                cb.onError("Failed to fetch captions");
            }

            @Override
            public void onResponse(Call c, Response r) throws IOException {
                String body = r.body().string();
                String text = body.startsWith("{") ? parseJson(body) : parseXml(body);
                if (text != null && !text.isEmpty()) {
                    finish(text, srcLang, tgtLang, cb);
                } else {
                    cb.onError("Parse error");
                }
            }
        });
    }

    private String parseJson(String json) {
        StringBuilder sb = new StringBuilder();
        try {
            JSONObject root = new JSONObject(json);
            JSONArray events = root.optJSONArray("events");
            if (events != null) {
                for (int i = 0; i < events.length(); i++) {
                    JSONArray segs = events.getJSONObject(i).optJSONArray("segs");
                    if (segs != null) {
                        for (int j = 0; j < segs.length(); j++) {
                            String t = segs.getJSONObject(j).optString("utf8", "");
                            if (!t.isEmpty() && !t.equals("\n"))
                                sb.append(t);
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return clean(sb.toString());
    }

    private String parseXml(String xml) {
        StringBuilder sb = new StringBuilder();
        Pattern p = Pattern.compile("<text[^>]*>([^<]*)</text>");
        Matcher m = p.matcher(xml);
        while (m.find()) {
            sb.append(m.group(1)
                            .replace("&amp;", "&")
                            .replace("&#39;", "'"))
                    .append(" ");
        }
        return clean(sb.toString());
    }

    private String clean(String s) {
        return s == null ? "" : s.replaceAll("\\s+", " ")
                .replaceAll("\\[.*?]", "")
                .trim();
    }

    private void finish(String text, String src, String tgt, TranscriptCallback cb) {
        if (tgt != null && !tgt.isEmpty() && !tgt.equals(src)) {
            translate(text, src, tgt, cb);
        } else {
            cb.onSuccess(text);
        }
    }

    private void translate(String text, String from, String to, TranscriptCallback cb) {
        try {
            String enc = URLEncoder.encode(text.length() > 4000 ? text.substring(0, 4000) : text, "UTF-8");
            String url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl="
                    + from + "&tl=" + to + "&dt=t&q=" + enc;

            client.newCall(new Request.Builder().url(url).build()).enqueue(new Callback() {
                @Override
                public void onFailure(Call c, IOException e) {
                    cb.onSuccess(text);
                }

                @Override
                public void onResponse(Call c, Response r) throws IOException {
                    try {
                        JSONArray arr = new JSONArray(r.body().string());
                        StringBuilder sb = new StringBuilder();
                        JSONArray t = arr.getJSONArray(0);
                        for (int i = 0; i < t.length(); i++) {
                            sb.append(t.getJSONArray(i).getString(0));
                        }
                        cb.onSuccess(sb.length() > 0 ? sb.toString() : text);
                    } catch (Exception e) {
                        cb.onSuccess(text);
                    }
                }
            });
        } catch (Exception e) {
            cb.onSuccess(text);
        }
    }

    public String extractVideoId(String url) {
        if (url == null) return null;
        url = url.trim();
        String id = null;
        try {
            if (url.contains("youtu.be/"))
                id = url.split("youtu.be/")[1];
            else if (url.contains("/shorts/"))
                id = url.split("/shorts/")[1];
            else if (url.contains("v="))
                id = url.split("v=")[1];
            else if (url.contains("/embed/"))
                id = url.split("/embed/")[1];

            if (id != null) {
                if (id.contains("&")) id = id.split("&")[0];
                if (id.contains("?")) id = id.split("\\?")[0];
                if (id.contains("/")) id = id.split("/")[0];
            }
        } catch (Exception ignored) {}

        return id;
    }
}
