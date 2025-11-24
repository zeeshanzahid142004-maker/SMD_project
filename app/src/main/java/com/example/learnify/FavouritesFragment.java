package com.example.learnify;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FavouritesFragment extends Fragment {

    private static final String TAG = "FavouritesFragment";

    RecyclerView favouritesRecyclerView;
    View emptyFavouritesView;
    HistoryAdapter historyAdapter;
    List<HistoryItem> favouritesList;

    // --- Firebase Variables ---
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // We don't need DocumentReference or User variables at the class level if they are only used in one function.

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_favourites, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // --- 1. Find Views ---
        favouritesRecyclerView = view.findViewById(R.id.favourites_recycler_view);
        emptyFavouritesView = view.findViewById(R.id.empty_favourites_view);

        // --- 2. Initialize Firebase ---
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // --- 3. Setup List and Adapter ---
        favouritesList = new ArrayList<>();
        historyAdapter = new HistoryAdapter(getContext(), favouritesList);
        favouritesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        favouritesRecyclerView.setAdapter(historyAdapter);

        // --- 4. Fetch Data ---
        fetchUserFavourites();
    }

    private void fetchUserFavourites() {
        FirebaseUser currentUser = mAuth.getCurrentUser();

        // Check if user is logged in
        if (currentUser == null) {
            showEmptyView(true);
            Log.w(TAG, "User not logged in.");
            return;
        }

        String userID = currentUser.getUid();
        DocumentReference userDocRef = db.collection("users").document(userID);

        // STEP 1: Get the user's document to find the list of favourite IDs
        userDocRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (!task.isSuccessful()) {
                    Log.e(TAG, "Failed to get user data", task.getException());
                    showEmptyView(true);
                    return;
                }

                DocumentSnapshot documentSnapshot = task.getResult();
                if (!documentSnapshot.exists()) {
                    Log.d(TAG, "User document does not exist.");
                    showEmptyView(true);
                    return;
                }

                // Safely extract the 'favorites' array as a List<String>
                // We must check if the field exists before casting.
                List<String> favouriteQuizIds = (List<String>) documentSnapshot.get("favorites");

                if (favouriteQuizIds == null || favouriteQuizIds.isEmpty()) {
                    Log.d(TAG, "Favourites array is empty for this user.");
                    showEmptyView(true);
                    return;
                }

                // STEP 2: Use the list of IDs to fetch the full Quiz documents
                fetchQuizDetails(favouriteQuizIds);
            }
        });
    }

    /**
     * Fetches details for all quizzes whose IDs are in the provided list.
     */
    private void fetchQuizDetails(List<String> quizIds) {
        // Clear the list before adding new items
        favouritesList.clear();

        // Firestore 'whereIn' limits the list size to 10. If you have more, you need batched reads.
        // For simplicity, we use 'whereIn' assuming less than 10 favorites for now.
        db.collection("quizzes")
                .whereIn("quizId", quizIds) // Assuming your quiz documents store their own ID field
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot quizDocument : task.getResult()) {
                                // Extract basic information from the Quiz document
                                String title = quizDocument.getString("title");
                                String thumbnailUrl = quizDocument.getString("thumbnailUrl");
                                long totalLong = quizDocument.getLong("totalMarks") != null ? quizDocument.getLong("totalMarks") : 0;

                                // NOTE: We don't have an "obtained score" here, so we default it to 0 or 1.
                                // We are just showing the quiz card.

                                // Create the HistoryItem (representing the quiz)
                               // HistoryItem item = new HistoryItem(title, 0, (int)totalLong, thumbnailUrl, true);
                               // favouritesList.add(item);
                            }

                            // Check if we found any quizzes
                            if (favouritesList.isEmpty()) {
                                showEmptyView(true);
                            } else {
                                historyAdapter.notifyDataSetChanged();
                                showEmptyView(false);
                            }
                        } else {
                            Log.e(TAG, "Error fetching quiz details", task.getException());
                            showEmptyView(true);
                        }
                    }
                });
    }

    // --- Utility Methods ---

    /**
     * Removes the redundant method signature and sets favorite logic directly in adapter.
     */
    private void setupFavouritesList() {
        // This method is now redundant and should be removed.
    }

    private void showEmptyView(boolean show) {
        if (emptyFavouritesView != null && favouritesRecyclerView != null) {
            if (show) {
                emptyFavouritesView.setVisibility(View.VISIBLE);
                favouritesRecyclerView.setVisibility(View.GONE);
            } else {
                emptyFavouritesView.setVisibility(View.GONE);
                favouritesRecyclerView.setVisibility(View.VISIBLE);
            }
        }
    }
}