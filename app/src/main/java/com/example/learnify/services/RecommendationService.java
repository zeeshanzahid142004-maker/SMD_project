package com.example.learnify.services;

import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;

import java. util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecommendationService {

    private static final String TAG = "RecommendationService";
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * Get YouTube recommendation for a topic using AI-generated suggestions
     */
    public void getYoutubeRecommendation(String topic, OnVideoFetchedListener listener) {
        Log.d(TAG, "Fetching YouTube recommendation for: " + topic);

        // For now, use a hardcoded mapping of topics to YouTube videos
        // In production, you can integrate with YouTube API or call your AI backend

        String videoUrl = getVideoForTopic(topic);

        if (videoUrl != null) {
            listener.onVideoFetched(videoUrl);
        } else {
            // Fallback: Create a YouTube search URL
            String searchUrl = "https://www.youtube.com/results?search_query=" + topic. replace(" ", "+");
            listener.onVideoFetched(searchUrl);
        }
    }

    /**
     * Get learning resources/topics for improvement
     */
    public void getRecommendedTopics(String questionTopic, OnTopicsFetchedListener listener) {
        Log.d(TAG, "Fetching recommended topics for: " + questionTopic);

        List<String> topics = extractRecommendedTopics(questionTopic);
        listener.onTopicsFetched(topics);
    }

    /**
     * Map topics to relevant YouTube videos
     */
    private String getVideoForTopic(String topic) {
        topic = topic.toLowerCase();

        Map<String, String> topicVideoMap = new HashMap<>();

        // Programming Topics
        topicVideoMap.put("array", "https://www.youtube. com/watch?v=ZoS3oM_5gJI");
        topicVideoMap. put("linked list", "https://www.youtube. com/watch?v=WwfhLC2THlo");
        topicVideoMap. put("recursion", "https://www.youtube.com/watch? v=IJDJ0kBx2LM");
        topicVideoMap. put("function", "https://www.youtube. com/watch?v=NIWwBMt6gX8");
        topicVideoMap. put("loop", "https://www.youtube. com/watch?v=4rPQZf1lVnU");
        topicVideoMap.put("oop", "https://www.youtube.com/watch?v=pTB0EiLXUC8");
        topicVideoMap. put("class", "https://www.youtube. com/watch?v=pTB0EiLXUC8");
        topicVideoMap.put("inheritance", "https://www.youtube. com/watch?v=ovaJVW2OY3s");
        topicVideoMap. put("polymorphism", "https://www.youtube.com/watch?v=Z5K5fI8_bL8");
        topicVideoMap.put("encapsulation", "https://www. youtube.com/watch?v=a_3J7DkW7w0");
        topicVideoMap.put("database", "https://www.youtube. com/watch?v=4cWkiBq_eUE");
        topicVideoMap.put("sql", "https://www.youtube. com/watch?v=HXV3zeQKqGY");
        topicVideoMap. put("api", "https://www.youtube. com/watch?v=fgTGADljAe4");
        topicVideoMap.put("json", "https://www.youtube. com/watch?v=iiADOW_2gIA");
        topicVideoMap. put("sorting", "https://www.youtube.com/watch?v=kgBjnD0Ks_0");
        topicVideoMap. put("searching", "https://www.youtube. com/watch?v=HoTkJlHXbPE");

        // Find matching video
        for (Map.Entry<String, String> entry : topicVideoMap. entrySet()) {
            if (topic.contains(entry.getKey()) || entry.getKey().contains(topic)) {
                return entry.getValue();
            }
        }

        return null; // Will fallback to search
    }

    /**
     * Extract recommended learning topics from question
     */
    private List<String> extractRecommendedTopics(String questionTopic) {
        List<String> topics = new ArrayList<>();

        String lower = questionTopic.toLowerCase();

        if (lower.contains("array")) {
            topics.add("Array operations and indexing");
            topics.add("Multi-dimensional arrays");
            topics. add("Array algorithms");
        } else if (lower.contains("loop")) {
            topics.add("Loop control statements");
            topics.add("Nested loops");
            topics.add("Loop optimization");
        } else if (lower.contains("recursion")) {
            topics.add("Base cases and recursive calls");
            topics.add("Recursion stack trace");
            topics.add("Tail recursion");
        } else if (lower.contains("class") || lower.contains("oop")) {
            topics.add("Class structure and methods");
            topics.add("Constructors and destructors");
            topics.add("Static vs instance members");
        } else if (lower.contains("inheritance")) {
            topics.add("Parent and child classes");
            topics.add("Method overriding");
            topics. add("Super keyword");
        } else if (lower.contains("polymorphism")) {
            topics.add("Method overloading");
            topics.add("Method overriding");
            topics. add("Interface implementation");
        } else {
            // Generic recommendations
            topics.add("Review the concept thoroughly");
            topics.add("Practice more examples");
            topics.add("Watch supplementary videos");
        }

        return topics;
    }

    public interface OnVideoFetchedListener {
        void onVideoFetched(String videoUrl);
    }

    public interface OnTopicsFetchedListener {
        void onTopicsFetched(List<String> topics);
    }
}