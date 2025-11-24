package com.example.learnify;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class QuizNetworkService {

    private static final String TAG = "QuizNetworkService";
    private static final String BASE_URL = "https://api.groq.com/";

    private final QuizApi quizApi;
    private final String apiKey=BuildConfig.API_KEY;
    private final Gson gson;
    private final Handler mainHandler; // ADD THIS

    public interface QuizCallback {
        void onSuccess(List<QuizQuestion> quizQuestions);
        void onError(String error);
    }

    public QuizNetworkService(String apiKey) {

        this.gson = new Gson();
        this.mainHandler = new Handler(Looper.getMainLooper()); // ADD THIS

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        this.quizApi = retrofit.create(QuizApi.class);
    }

    public void generateQuiz(String inputContext, QuizCallback callback) {
        Log.d(TAG, "Generating quiz using Groq...");
        Log.d(TAG, "Callback object: " + callback);
        Log.d(TAG, "Callback hashCode: " + callback.hashCode());

        String prompt = buildPrompt(inputContext);
        GroqRequest requestBody = new GroqRequest(prompt);
        String authHeader = "Bearer " + apiKey;

        quizApi.generateQuiz(authHeader, requestBody).enqueue(new Callback<GroqResponse>() {
            @Override
            public void onResponse(@NonNull Call<GroqResponse> call, @NonNull Response<GroqResponse> response) {
                Log.d(TAG, "✅ HTTP Response received: " + response.code());
                Log.d(TAG, "Callback still valid? " + callback);
                Log.d(TAG, "Callback hashCode: " + callback.hashCode());

                if (!response.isSuccessful() || response.body() == null) {
                    String errorMsg = "Groq API Error: " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            errorMsg += " - " + response.errorBody().string();
                        }
                    } catch (Exception ignored) {}
                    Log.e(TAG, errorMsg);

                    // POST ERROR ON MAIN THREAD
                    String finalError = errorMsg;
                    Log.d(TAG, "⚠️ About to post error callback...");
                    mainHandler.post(() -> {
                        Log.d(TAG, "⚠️ EXECUTING error callback now...");
                        callback.onError(finalError);
                        Log.d(TAG, "⚠️ Error callback FINISHED");
                    });
                    return;
                }

                String quizJsonText = response.body().getContent();
                if (quizJsonText == null) {
                    Log.e(TAG, "Empty response from AI");
                    mainHandler.post(() -> {
                        Log.d(TAG, "⚠️ EXECUTING empty response callback...");
                        callback.onError("Empty response from AI");
                        Log.d(TAG, "⚠️ Empty response callback FINISHED");
                    });
                    return;
                }

                Log.d(TAG, "📩 Raw Content from AI: " + quizJsonText);

                try {
                    Log.d(TAG, "🔍 Starting Parsing...");

                    // Sanitize
                    quizJsonText = quizJsonText.replace("```json", "").replace("```", "").trim();
                    Log.d(TAG, "🧹 Sanitized JSON: " + quizJsonText);

                    // Parse wrapper object
                    JsonObject root = JsonParser.parseString(quizJsonText).getAsJsonObject();
                    Log.d(TAG, "📦 Root Object Parsed");

                    // Get questions array
                    JsonArray questionsArray = root.getAsJsonArray("questions");
                    Log.d(TAG, "🔢 Found Questions Array. Size: " + questionsArray.size());

                    // Convert to List
                    Type listType = new TypeToken<ArrayList<QuizQuestion>>(){}.getType();
                    List<QuizQuestion> questions = gson.fromJson(questionsArray, listType);

                    Log.d(TAG, "✅ Success! Parsed " + questions.size() + " questions.");
                    Log.d(TAG, "🎯 About to post SUCCESS callback...");
                    Log.d(TAG, "Callback reference: " + callback);

                    // POST SUCCESS ON MAIN THREAD
                    mainHandler.post(() -> {
                        Log.d(TAG, "🎯 INSIDE main handler post - about to call callback.onSuccess");
                        Log.d(TAG, "Callback object in handler: " + callback);
                        try {
                            callback.onSuccess(questions);
                            Log.d(TAG, "✅ Success callback COMPLETED");
                        } catch (Exception e) {
                            Log.e(TAG, "❌ EXCEPTION calling onSuccess!", e);
                        }
                    });
                    Log.d(TAG, "🎯 Handler.post() call finished");

                } catch (Exception e) {
                    Log.e(TAG, "❌ Parsing Fail: " + quizJsonText, e);

                    // POST ERROR ON MAIN THREAD
                    mainHandler.post(() -> {
                        Log.d(TAG, "⚠️ EXECUTING parsing error callback...");
                        callback.onError("Failed to parse quiz: " + e.getMessage());
                        Log.d(TAG, "⚠️ Parsing error callback FINISHED");
                    });
                }
            }

            @Override
            public void onFailure(@NonNull Call<GroqResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "❌ Network Fail", t);

                // POST ERROR ON MAIN THREAD
                Log.d(TAG, "⚠️ About to post network error callback...");
                mainHandler.post(() -> {
                    Log.d(TAG, "⚠️ EXECUTING network error callback...");
                    callback.onError("Network error: " + t.getMessage());
                    Log.d(TAG, "⚠️ Network error callback FINISHED");
                });
            }
        });

        Log.d(TAG, "📞 API call enqueued, waiting for response...");
    }

    private String buildPrompt(String input) {
        return
                "You are a strict JSON generator. OUTPUT ONLY VALID JSON.\n" +
                        "Never output explanations, notes, markdown, or text outside JSON.\n" +
                        "Rules:\n" +
                        "- Use ONLY double quotes (\")\n" +
                        "- NEVER use single quotes (')\n" +
                        "- No trailing commas\n" +
                        "- Every string must be enclosed in double quotes\n" +
                        "- The JSON MUST match the required structure exactly\n" +
                        "- The value of \"type\" must be either \"MCQ\" or \"CODING\"\n\n" +

                        "CONTENT:\n" +
                        input + "\n\n" +

                        "TASK:\n" +
                        "Generate 5 to 10 quiz questions strictly based on the content above.\n" +
                        "Rules:\n" +
                        "1. Use ONLY information present in the content.\n" +
                        "2. Do NOT invent facts.\n" +
                        "3. Mix EASY, NORMAL, and HARD difficulty.\n" +
                        "4. If the content is technical or contains code, create coding questions with \"type\": \"CODING\".\n" +
                        "5. Otherwise generate \"MCQ\" type questions.\n\n" +

                        "RESPONSE FORMAT:\n" +
                        "{\n" +
                        "  \"questions\": [\n" +
                        "    {\n" +
                        "      \"type\": \"MCQ\",\n" +
                        "      \"questionText\": \"string\",\n" +
                        "      \"options\": [\"string\", \"string\", \"string\", \"string\"],\n" +
                        "      \"correctAnswer\": \"string\",\n" +
                        "      \"difficulty\": \"EASY\"\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n\n" +

                        "For coding questions, follow this structure:\n" +
                        "{\n" +
                        "  \"type\": \"CODING\",\n" +
                        "  \"questionText\": \"string\",\n" +
                        "  \"options\": [],\n" +
                        "  \"correctAnswer\": \"string\",\n" +
                        "  \"difficulty\": \"HARD\"\n" +
                        "}\n\n" +

                        "Return ONLY the JSON object now.";
    }

}