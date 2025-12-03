package com.example.learnify.groqapihelpers;

import java.util.ArrayList;
import java.util.List;

public class GroqRequest {
    // Use the model we know works
    final String model = "llama-3.1-8b-instant";

    final List<Message> messages;

    // --- NEW FIELD: Force JSON Mode ---
    final ResponseFormat response_format;

    public GroqRequest(String prompt) {
        this.messages = new ArrayList<>();
        this.messages.add(new Message("user", prompt));

        // Initialize the format to force JSON object
        this.response_format = new ResponseFormat("json_object");
    }

    static class Message {
        String role;
        String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }

    // Helper class for the format
    static class ResponseFormat {
        String type;
        public ResponseFormat(String type) {
            this.type = type;
        }
    }
}