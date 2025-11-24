package com.example.learnify;

import java.util.List;

public class GroqResponse {
    List<Choice> choices;

    public String getContent() {
        if (choices != null && !choices.isEmpty()) {
            return choices.get(0).message.content;
        }
        return null;
    }

    static class Choice {
        Message message;
    }

    static class Message {
        String content;
    }
}