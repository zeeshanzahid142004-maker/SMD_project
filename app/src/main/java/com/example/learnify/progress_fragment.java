package com.example.learnify; // Use your package name

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

public class progress_fragment extends Fragment {

    SharedPreferences spref;
    TextView profileName;
    MaterialButton startLearningButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_progress, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Get SharedPreferences
        spref = getContext().getSharedPreferences("MyPref", Context.MODE_PRIVATE);

        // Find views
        profileName = view.findViewById(R.id.profile_name);
        startLearningButton = view.findViewById(R.id.start_learning_button);

        // --- Set User Name from SharedPreferences ---
        // This is the logic you asked for
        String username = spref.getString("username", "User");
        profileName.setText(username);

        // --- Set Click Listeners ---
        startLearningButton.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Start Learning Clicked!", Toast.LENGTH_SHORT).show();
            // TODO: Add navigation logic, perhaps back to HomeFragment?
            // Or to the 'MyCoursesFragment'?
        });
    }
}