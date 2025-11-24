package com.example.learnify;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class HistoryFragment extends Fragment {

    private static final String TAG = "HistoryFragDebug"; // Debug tag

    private RecyclerView recyclerView;
    private TextView emptyView;
    private HistoryAdapter adapter;
    private List<HistoryItem> historyList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "🖼️ onViewCreated");

        recyclerView = view.findViewById(R.id.history_recycler_view);
        emptyView = view.findViewById(R.id.empty_history_view); // Check this ID matches your XML!

        historyList = new ArrayList<>();
        adapter = new HistoryAdapter(getContext(), historyList);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        fetchHistory();
    }

    private void fetchHistory() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) {
            Log.e(TAG, "❌ User not logged in, cannot fetch history");
            return;
        }

        Log.d(TAG, "⚡ Fetching history for User ID: " + uid);

        FirebaseFirestore.getInstance().collection("users").document(uid)
                .get()
                .addOnSuccessListener(document -> {
                    Log.d(TAG, "✅ Document snapshot retrieved");

                    if (!document.exists()) {
                        Log.w(TAG, "⚠️ User document does NOT exist in Firestore");
                        toggleEmptyView(true);
                        return;
                    }

                    if (document.contains("history")) {
                        Object historyObj = document.get("history");
                        Log.d(TAG, "📦 'history' field type: " + (historyObj != null ? historyObj.getClass().getSimpleName() : "null"));

                        List<Map<String, Object>> rawHistory = (List<Map<String, Object>>) historyObj;

                        if (rawHistory != null && !rawHistory.isEmpty()) {
                            Log.d(TAG, "🔢 Found " + rawHistory.size() + " items. Parsing...");
                            historyList.clear();

                            for (Map<String, Object> map : rawHistory) {
                                try {
                                    String title = (String) map.get("title");
                                    String quizId = (String) map.get("quizId");
                                    // Number safety (Firestore numbers can be Long or Double)
                                    Number scoreNum = (Number) map.get("score");
                                    Number totalNum = (Number) map.get("totalMarks");
                                    int score = scoreNum != null ? scoreNum.intValue() : 0;
                                    int total = totalNum != null ? totalNum.intValue() : 0;

                                    Date date = null;
                                    Object dateObj = map.get("dateTaken");
                                    if (dateObj instanceof Timestamp) {
                                        date = ((Timestamp) dateObj).toDate();
                                    }

                                    Log.d(TAG, "   👉 Item: " + title + " (" + score + "/" + total + ")");
                                    historyList.add(new HistoryItem(quizId, title != null ? title : "Quiz", score, total, date));
                                } catch (Exception e) {
                                    Log.e(TAG, "   ⚠️ Error parsing an item", e);
                                }
                            }

                            Collections.reverse(historyList); // Newest first
                            adapter.notifyDataSetChanged();
                            toggleEmptyView(false);
                        } else {
                            Log.d(TAG, "⚠️ History array is empty");
                            toggleEmptyView(true);
                        }
                    } else {
                        Log.w(TAG, "⚠️ Document exists but has NO 'history' field");
                        toggleEmptyView(true);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Firestore fetch failed", e);
                    toggleEmptyView(true);
                });
    }

    private void toggleEmptyView(boolean isEmpty) {
        if (isEmpty) {
            recyclerView.setVisibility(View.GONE);
            if(emptyView != null) emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            if(emptyView != null) emptyView.setVisibility(View.GONE);
        }
    }
}