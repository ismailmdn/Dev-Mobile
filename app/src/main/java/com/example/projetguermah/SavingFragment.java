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

public class SavingFragment extends Fragment {

    LinearLayout rowOne, rowTwo;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_saving, container, false);

        rowOne = view.findViewById(R.id.row_one);
        rowTwo = view.findViewById(R.id.row_two);

        rowOne.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(requireContext(), SavingChallengesActivity.class);
                startActivity(intent);
            }
        });

        rowTwo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(requireContext(), SavingsGoalsActivity.class);
                startActivity(intent);
            }
        });

        return view;
    }
}
