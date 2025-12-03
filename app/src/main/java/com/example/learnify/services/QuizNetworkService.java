package com.example.learnify.services;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;

import com.example.learnify.BuildConfig;
import com.example.learnify.groqapihelpers.GroqRequest;
import com.example.learnify.groqapihelpers.GroqResponse;
import com.example.learnify.managers.LanguageManager;
import com.example.learnify.modelclass.QuizSettings;
import com.example.learnify.groqapihelpers.QuizApi;
import com.example.learnify.modelclass.QuizQuestion;
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
    private final String apiKey = BuildConfig.API_KEY;
    private final Gson gson;
    private final Handler mainHandler;
    private final Context context;

    public interface QuizCallback {
        void onSuccess(List<QuizQuestion> quizQuestions, String topic);
        void onError(String error);
    }

    public QuizNetworkService(String apiKey) {
        this(apiKey, null);
    }

    public QuizNetworkService(String apiKey, Context context) {
        this.context = context;
        this.gson = new Gson();
        this.mainHandler = new Handler(Looper.getMainLooper());

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

    /**
     * Generate quiz with default settings (backwards compatible)
     */
    public void generateQuiz(String inputContext, QuizCallback callback) {
        generateQuizWithSettings(inputContext, null, callback);
    }

    /**
     * Generate quiz with custom settings
     */
    public void generateQuizWithSettings(String inputContext, QuizSettings settings, QuizCallback callback) {
        Log.d(TAG, "🎯 Generating quiz from content...");

        String prompt = buildPromptWithSettings(inputContext, settings);
        GroqRequest requestBody = new GroqRequest(prompt);
        String authHeader = "Bearer " + apiKey;

        quizApi.generateQuiz(authHeader, requestBody).enqueue(new Callback<GroqResponse>() {
            @Override
            public void onResponse(@NonNull Call<GroqResponse> call, @NonNull Response<GroqResponse> response) {
                Log.d(TAG, "✅ HTTP Response received: " + response.code());

                if (!response.isSuccessful() || response.body() == null) {
                    String errorMsg = "Groq API Error: " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            errorMsg += " - " + response.errorBody().string();
                        }
                    } catch (Exception ignored) {}
                    Log.e(TAG, errorMsg);

                    String finalError = errorMsg;
                    mainHandler.post(() -> callback.onError(finalError));
                    return;
                }

                String quizJsonText = response.body().getContent();
                if (quizJsonText == null) {
                    Log.e(TAG, "Empty response from AI");
                    mainHandler.post(() -> callback.onError("Empty response from AI"));
                    return;
                }

                Log.d(TAG, "📩 Raw Content from AI");

                try {
                    // Sanitize
                    quizJsonText = quizJsonText.replace("```json", "").replace("```", "").trim();

                    // Parse wrapper object
                    JsonObject root = JsonParser.parseString(quizJsonText).getAsJsonObject();

                    // Get topic from AI response (if available)
                    String topic = null;
                    if (root.has("topic") && !root.get("topic").isJsonNull()) {
                        topic = root.get("topic").getAsString();
                        Log.d(TAG, "📌 AI generated topic: " + topic);
                    }

                    // Get questions array
                    JsonArray questionsArray = root.getAsJsonArray("questions");
                    Log.d(TAG, "🔢 Found Questions Array. Size: " + questionsArray.size());

                    // Convert to List
                    Type listType = new TypeToken<ArrayList<QuizQuestion>>(){}.getType();
                    List<QuizQuestion> questions = gson.fromJson(questionsArray, listType);

                    String language = context != null ?
                            LanguageManager.getInstance(context).getCurrentLanguage() : "English";
                    Log.d(TAG, "✅ Success! Parsed " + questions.size() + " questions in " + language);

                    String finalTopic = topic;
                    mainHandler.post(() -> callback.onSuccess(questions, finalTopic));

                } catch (Exception e) {
                    Log.e(TAG, "❌ Parsing Fail", e);
                    mainHandler.post(() -> callback.onError("Failed to parse quiz: " + e.getMessage()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<GroqResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "❌ Network Fail", t);
                mainHandler.post(() -> callback.onError("Network error: " + t.getMessage()));
            }
        });
    }

    private String buildPromptWithSettings(String input, QuizSettings settings) {
        // Get user's preferred language
        String targetLanguage = "ENGLISH";
        int questionCount = 5;
        String difficultyInstruction = "Mix difficulty levels: include EASY, NORMAL, and HARD questions.";
        String codingInstruction = "Include at least 1-2 coding questions with type 'CODING' if the content is technical.";

        if (context != null) {
            LanguageManager langManager = LanguageManager.getInstance(context);
            targetLanguage = langManager.getCurrentLanguage().toUpperCase();
        }

        // Apply custom settings if provided
        if (settings != null) {
            targetLanguage = settings.getLanguage().toUpperCase();
            questionCount = settings.getNumberOfQuestions();
            difficultyInstruction = settings.getDifficultyInstruction();
            codingInstruction = settings.getCodingInstruction();
        }

        return
                "You are a strict JSON generator that creates educational quizzes. OUTPUT ONLY VALID JSON.\n" +
                        "Never output explanations, notes, markdown, or text outside JSON.\n\n" +

                        "JSON FORMATTING RULES:\n" +
                        "- Use ONLY double quotes (\")\n" +
                        "- NEVER use single quotes (')\n" +
                        "- No trailing commas\n" +
                        "- Every string must be enclosed in double quotes\n" +
                        "- The JSON MUST match the required structure exactly\n" +
                        "- The value of \"type\" must be either \"MCQ\" or \"CODING\"\n\n" +

                        "🏷️ QUIZ TITLE RULES (IMPORTANT):\n" +
                        "1. Generate a SHORT topic name (2-4 words max)\n" +
                        "2. DO NOT include the word 'Quiz' in the topic\n" +
                        "3. Use general terms like 'Loops' instead of specific syntax like 'For Loop'\n" +
                        "4. Good examples: 'Newton Laws', 'Python Functions', 'Cell Biology', 'Data Structures'\n" +
                        "5. Bad examples: 'For Loop Quiz', 'Quiz About Functions', 'The Quiz'\n\n" +

                        "🌍 LANGUAGE REQUIREMENT:\n" +
                        "CRITICAL: ALL quiz content MUST be in " + targetLanguage + " ONLY.\n" +
                        "- Even if the input content is in a different language\n" +
                        "- ALL questions, options, and answers MUST be generated in " + targetLanguage + "\n" +
                        "- Translate concepts, ideas, and facts to " + targetLanguage + "\n" +
                        "- Maintain the original meaning while using " + targetLanguage + " language\n\n" +

                        "📊 QUIZ SETTINGS:\n" +
                        "- Number of questions: Generate EXACTLY " + questionCount + " questions\n" +
                        "- Difficulty: " + difficultyInstruction + "\n" +
                        "- Coding questions: " + codingInstruction + "\n\n" +

                        "CONTENT TO ANALYZE:\n" +
                        input + "\n\n" +

                        "TASK:\n" +
                        "Generate exactly " + questionCount + " quiz questions based on the content above.\n" +
                        "Rules:\n" +
                        "1. Extract key information from the content (regardless of its language)\n" +
                        "2. Create questions that test understanding of the material\n" +
                        "3. Do NOT invent facts not present in the content\n" +
                        "4. Follow the difficulty setting: " + difficultyInstruction + "\n" +
                        "5. Follow the coding setting: " + codingInstruction + "\n" +
                        "6. 🌍 ALL TEXT IN " + targetLanguage + " - translate if source is in another language\n\n" +

                        "RESPONSE FORMAT:\n" +
                        "{\n" +
                        "  \"topic\": \"Short Topic Name (2-4 words, NO 'Quiz' word)\",\n" +
                        "  \"questions\": [\n" +
                        "    {\n" +
                        "      \"type\": \"MCQ\",\n" +
                        "      \"questionText\": \"Question in " + targetLanguage + "\",\n" +
                        "      \"options\": [\"Option A\", \"Option B\", \"Option C\", \"Option D\"],\n" +
                        "      \"correctAnswer\": \"Correct option\",\n" +
                        "      \"difficulty\": \"EASY\"\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n\n" +

                        "For coding questions:\n" +
                        "{\n" +
                        "  \"type\": \"CODING\",\n" +
                        "  \"questionText\": \"Coding task in " + targetLanguage + "\",\n" +
                        "  \"options\": [],\n" +
                        "  \"correctAnswer\": \"Sample solution\",\n" +
                        "  \"difficulty\": \"HARD\"\n" +
                        "}\n\n" +

                        "🎯 REMEMBER: Output ONLY the JSON object. ALL text in " + targetLanguage + ". Include 'topic' field! Generate EXACTLY " + questionCount + " questions!";
    }
}