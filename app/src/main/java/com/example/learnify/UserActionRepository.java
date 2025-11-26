package com.example.learnify;

import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class UserActionRepository {

    private static final String TAG = "UserActionRepoDebug"; // Debug tag
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public UserActionRepository() {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    private DocumentReference getUserDocumentRef() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Log.e(TAG, "❌ Cannot get User Document: User is NULL (not logged in)");
            return null;
        }
        return db.collection("users").document(user.getUid());
    }


    public void saveHistoryEntry(String quizId, int score, int totalMarks, String title) {
        Log.d(TAG, "⚡ saveHistoryEntry called");
        Log.d(TAG, "   📝 Data: QuizID=" + quizId + ", Title='" + title + "', Score=" + score + "/" + totalMarks);

        DocumentReference userDocRef = getUserDocumentRef();
        if (userDocRef == null) return;

        // Create Map
        Map<String, Object> historyEntry = new HashMap<>();
        historyEntry.put("quizId", quizId);
        historyEntry.put("score", score);
        historyEntry.put("totalMarks", totalMarks);
        historyEntry.put("title", title);
        historyEntry.put("dateTaken", new Date());


        userDocRef.update("history", FieldValue.arrayUnion(historyEntry))
                .addOnSuccessListener(aVoid -> Log.d(TAG, "✅ SUCCESS: History entry written to Firestore!"))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ FAILURE: Could not write history", e);

                });
    }


}