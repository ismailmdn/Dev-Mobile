package com.example.projetguermah;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AddGoalActivity extends AppCompatActivity {

    private EditText goalNameInput, goalAmountInput, goalDateInput;
    private Button saveButton, cancelButton;
    private FirebaseFirestore db;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_goal);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        goalNameInput = findViewById(R.id.goalNameInput);
        goalAmountInput = findViewById(R.id.goalAmountInput);
        goalDateInput = findViewById(R.id.goalDateInput);
        saveButton = findViewById(R.id.saveButton);
        cancelButton = findViewById(R.id.cancelButton);

        saveButton.setOnClickListener(v -> saveGoal());
        cancelButton.setOnClickListener(v -> finish());
    }

    private void saveGoal() {
        String name = goalNameInput.getText().toString().trim();
        String amountStr = goalAmountInput.getText().toString().trim();
        String date = goalDateInput.getText().toString().trim();

        if (name.isEmpty() || amountStr.isEmpty() || date.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double amount = Double.parseDouble(amountStr);

            Map<String, Object> goal = new HashMap<>();
            goal.put("name", name);
            goal.put("targetAmount", amount);
            goal.put("currentAmount", 0);
            goal.put("targetDate", date);
            goal.put("isActive", true);

            db.collection("finance")
                    .document(uid)
                    .collection("savingsGoals")
                    .add(goal)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(this, "Goal saved successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to save goal", Toast.LENGTH_SHORT).show();
                    });

        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
        }
    }
}