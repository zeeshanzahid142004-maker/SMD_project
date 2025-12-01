package com.example.learnify;

import android.util.Log;
import org.json.JSONObject;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.*;

public class CodeExecutionService {
    private static final String TAG = "CodeExecutionService";
    private static final String JDOODLE_URL = "https://api.jdoodle.com/v1/execute";
    
    // API credentials - should be configured via BuildConfig or local.properties
    // To enable JDoodle API:
    // 1. Sign up at https://www.jdoodle.com/compiler-api
    // 2. Add to local.properties: JDOODLE_CLIENT_ID=your_client_id
    //                            JDOODLE_CLIENT_SECRET=your_client_secret
    // 3. Update build.gradle.kts to include these as BuildConfig fields
    private final String clientId;
    private final String clientSecret;
    
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();

    public interface ExecutionCallback {
        void onSuccess(String output);
        void onError(String error);
    }
    
    public CodeExecutionService() {
        // Try to load from BuildConfig, default to empty if not configured
        this.clientId = getConfigValue("JDOODLE_CLIENT_ID", "");
        this.clientSecret = getConfigValue("JDOODLE_CLIENT_SECRET", "");
    }
    
    private String getConfigValue(String key, String defaultValue) {
        // BuildConfig approach - credentials should be added to build.gradle.kts
        // For now, return default as credentials are not configured
        return defaultValue;
    }

    public void executeCode(String code, String language, ExecutionCallback callback) {
        // Check if API credentials are configured
        if (clientId == null || clientId.isEmpty() || clientSecret == null || clientSecret.isEmpty()) {
            // Use offline validation if no API credentials
            callback.onError("API not configured. Using offline validation.");
            return;
        }
        
        // Map language names to JDoodle language codes
        String langCode = mapLanguage(language);
        String versionIndex = getVersionIndex(langCode);
        
        try {
            JSONObject json = new JSONObject();
            json.put("clientId", clientId);
            json.put("clientSecret", clientSecret);
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
                        // Check HTTP status code first
                        if (!response.isSuccessful()) {
                            callback.onError("Server error: HTTP " + response.code());
                            return;
                        }
                        
                        ResponseBody responseBody = response.body();
                        if (responseBody == null) {
                            callback.onError("Empty response from server");
                            return;
                        }
                        
                        String bodyString = responseBody.string();
                        JSONObject result = new JSONObject(bodyString);
                        
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
    
    /**
     * Get the version index for a language. JDoodle uses version indices to specify
     * which compiler/interpreter version to use. Higher numbers generally mean newer versions.
     * @param langCode The JDoodle language code
     * @return Version index string for the JDoodle API
     */
    private String getVersionIndex(String langCode) {
        switch (langCode) {
            case "python3": return "4";  // Python 3.x (latest stable)
            case "java": return "4";     // JDK 17.x
            case "cpp17": return "0";    // GCC with C++17 support
            case "nodejs": return "4";   // Node.js 18.x
            default: return "0";
        }
    }
}
