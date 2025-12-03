package com.example.learnify.groqapihelpers;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface QuizApi {
    @POST("openai/v1/chat/completions")
    Call<GroqResponse> generateQuiz(
            @Header("Authorization") String authHeader, // "Bearer gsk_..."
            @Body GroqRequest request
    );
}