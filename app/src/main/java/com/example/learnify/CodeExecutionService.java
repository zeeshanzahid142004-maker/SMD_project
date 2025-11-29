package com.example.learnify;

import android.util.Log;
import org.json.JSONObject;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.*;

public class CodeExecutionService {
    private static final String TAG = "CodeExecutionService";
    private static final String JDOODLE_URL = "https://api.jdoodle.com/v1/execute";
    
    // Free tier credentials - users can replace with their own
    private static final String CLIENT_ID = ""; // User adds their own
    private static final String CLIENT_SECRET = ""; // User adds their own
    
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    public interface ExecutionCallback {
        void onSuccess(String output);
        void onError(String error);
    }

    public void executeCode(String code, String language, ExecutionCallback callback) {
        // Check if API credentials are configured
        if (CLIENT_ID.isEmpty() || CLIENT_SECRET.isEmpty()) {
            // Use offline validation if no API credentials
            callback.onError("API not configured. Using offline validation.");
            return;
        }
        
        // Map language names to JDoodle language codes
        String langCode = mapLanguage(language);
        String versionIndex = getVersionIndex(langCode);
        
        try {
            JSONObject json = new JSONObject();
            json.put("clientId", CLIENT_ID);
            json.put("clientSecret", CLIENT_SECRET);
            json.put("script", code);
            json.put("language", langCode);
            json.put("versionIndex", versionIndex);
            
            RequestBody body = RequestBody.create(
                json.toString(),
                MediaType.parse("application/json")
            );
            
            Request request = new Request.Builder()
                    .url(JDOODLE_URL)
                    .post(body)
                    .build();
            
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "Network error: " + e.getMessage());
                    callback.onError("Network error: " + e.getMessage());
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String responseBody = response.body().string();
                        JSONObject result = new JSONObject(responseBody);
                        
                        if (result.has("error")) {
                            callback.onError(result.getString("error"));
                        } else {
                            String output = result.optString("output", "");
                            callback.onSuccess(output);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Parse error: " + e.getMessage());
                        callback.onError("Parse error: " + e.getMessage());
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error: " + e.getMessage());
            callback.onError("Error: " + e.getMessage());
        }
    }
    
    private String mapLanguage(String language) {
        if (language == null) return "python3";
        switch (language.toLowerCase()) {
            case "python":
            case "python3": return "python3";
            case "java": return "java";
            case "c":
            case "c++":
            case "cpp": return "cpp17";
            case "javascript":
            case "js": return "nodejs";
            default: return "python3";
        }
    }
    
    private String getVersionIndex(String langCode) {
        switch (langCode) {
            case "python3": return "4";
            case "java": return "4";
            case "cpp17": return "0";
            case "nodejs": return "4";
            default: return "0";
        }
    }
}
