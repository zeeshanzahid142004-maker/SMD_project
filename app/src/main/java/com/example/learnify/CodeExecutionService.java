package com.example.learnify;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class CodeExecutionService {

    private static final String TAG = "CodeExecutionService";
    private static final String JDOODLE_URL = "https://api.jdoodle.com/v1/execute";

    private final OkHttpClient client;
    private final Gson gson;
    private final Handler mainHandler;
    private final String clientId;
    private final String clientSecret;

    public interface CodeExecutionCallback {
        void onSuccess(String output);
        void onError(String error);
    }

    public CodeExecutionService() {
        this.gson = new Gson();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.clientId = BuildConfig.JDOODLE_CLIENT_ID != null ? BuildConfig.JDOODLE_CLIENT_ID : "";
        this.clientSecret = BuildConfig.JDOODLE_CLIENT_SECRET != null ? BuildConfig.JDOODLE_CLIENT_SECRET : "";

        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public void executeCode(String code, String language, CodeExecutionCallback callback) {
        Log.d(TAG, "🚀 Executing code in " + language);

        // Check for missing credentials
        if (clientId.isEmpty() || clientSecret.isEmpty()) {
            Log.e(TAG, "❌ Missing JDoodle credentials");
            mainHandler.post(() -> callback.onError("Code execution service not configured. Please add JDoodle credentials."));
            return;
        }

        // Map language to JDoodle language code and version
        String languageCode = getLanguageCode(language);
        String versionIndex = getVersionIndex(language);

        if (languageCode == null) {
            mainHandler.post(() -> callback.onError("Unsupported language: " + language));
            return;
        }

        JsonObject requestJson = new JsonObject();
        requestJson.addProperty("clientId", clientId);
        requestJson.addProperty("clientSecret", clientSecret);
        requestJson.addProperty("script", code);
        requestJson.addProperty("language", languageCode);
        requestJson.addProperty("versionIndex", versionIndex);

        RequestBody body = RequestBody.create(
                requestJson.toString(),
                MediaType.parse("application/json")
        );

        Request request = new Request.Builder()
                .url(JDOODLE_URL)
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, "❌ Network error", e);
                mainHandler.post(() -> callback.onError("Network error: " + e.getMessage()));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String responseBody = response.body() != null ? response.body().string() : null;

                if (!response.isSuccessful()) {
                    String errorMsg = "API Error: " + response.code();
                    if (responseBody != null && !responseBody.isEmpty()) {
                        errorMsg += " - " + responseBody;
                    }
                    Log.e(TAG, errorMsg);
                    String finalError = errorMsg;
                    mainHandler.post(() -> callback.onError(finalError));
                    return;
                }

                if (responseBody == null || responseBody.isEmpty()) {
                    Log.e(TAG, "Empty response body");
                    mainHandler.post(() -> callback.onError("Empty response from server"));
                    return;
                }

                try {
                    JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();

                    // Check for error in response
                    if (jsonResponse.has("error") && !jsonResponse.get("error").isJsonNull()) {
                        String error = jsonResponse.get("error").getAsString();
                        if (error != null && !error.isEmpty()) {
                            Log.e(TAG, "Execution error: " + error);
                            mainHandler.post(() -> callback.onError(error));
                            return;
                        }
                    }

                    // Get output - handle null gracefully
                    String output = null;
                    if (jsonResponse.has("output") && !jsonResponse.get("output").isJsonNull()) {
                        output = jsonResponse.get("output").getAsString();
                    }

                    // Handle empty or null output
                    if (output == null || output.trim().isEmpty()) {
                        output = "(No output)";
                    }

                    Log.d(TAG, "✅ Execution successful");
                    String finalOutput = output;
                    mainHandler.post(() -> callback.onSuccess(finalOutput));

                } catch (Exception e) {
                    Log.e(TAG, "❌ Failed to parse response", e);
                    mainHandler.post(() -> callback.onError("Failed to parse response: " + e.getMessage()));
                }
            }
        });
    }

    private String getLanguageCode(String language) {
        if (language == null) return null;

        switch (language.toLowerCase()) {
            case "python":
            case "python3":
                return "python3";
            case "java":
                return "java";
            case "c":
                return "c";
            case "c++":
            case "cpp":
                return "cpp";
            case "javascript":
            case "js":
                return "nodejs";
            case "kotlin":
                return "kotlin";
            case "swift":
                return "swift";
            case "go":
            case "golang":
                return "go";
            case "ruby":
                return "ruby";
            case "php":
                return "php";
            default:
                return null;
        }
    }

    private String getVersionIndex(String language) {
        if (language == null) return "0";

        switch (language.toLowerCase()) {
            case "python":
            case "python3":
                return "4"; // Python 3.x
            case "java":
                return "4"; // Java 17
            case "c":
                return "5";
            case "c++":
            case "cpp":
                return "5";
            case "javascript":
            case "js":
                return "4";
            case "kotlin":
                return "3";
            case "swift":
                return "4";
            case "go":
            case "golang":
                return "4";
            case "ruby":
                return "4";
            case "php":
                return "4";
            default:
                return "0";
        }
    }
}
