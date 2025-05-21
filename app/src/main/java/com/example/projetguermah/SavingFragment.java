package com.example.projetguermah;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SavingFragment extends Fragment {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private LinearLayout rowOne, rowTwo, rowRecommendation;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_saving, container, false);

        rowOne = view.findViewById(R.id.row_one);
        rowTwo = view.findViewById(R.id.row_two);
        rowRecommendation = view.findViewById(R.id.row_recommendation); // Assure-toi que cet ID est bien dans ton XML

        rowOne.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), SavingChallengesActivity.class);
            startActivity(intent);
        });

        rowTwo.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), SavingsGoalsActivity.class);
            startActivity(intent);
        });

        rowRecommendation.setOnClickListener(v -> {
            if (mAuth.getCurrentUser() != null) {
                String userId = mAuth.getCurrentUser().getUid();
                Intent intent = new Intent(requireContext(), FinancialRecommendationActivity.class);
                intent.putExtra("userId", userId);
                String currentMonth = new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(new Date());
                intent.putExtra("month", currentMonth);
                startActivity(intent);
            }
        });

        return view;
    }
}
