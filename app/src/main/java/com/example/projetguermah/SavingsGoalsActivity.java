package com.example.projetguermah;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projetguermah.model.SavingsGoal;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class SavingsGoalsActivity extends AppCompatActivity {

    private RecyclerView goalsRecyclerView;
    private Button addGoalButton, backButton;
    private List<SavingsGoal> savingsGoals = new ArrayList<>();
    private GoalsAdapter goalsAdapter;
    private FirebaseFirestore db;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_savings_goals);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        goalsRecyclerView = findViewById(R.id.goalsRecyclerView);
        addGoalButton = findViewById(R.id.addGoalButton);
        backButton = findViewById(R.id.backButton);

        goalsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        goalsAdapter = new GoalsAdapter(savingsGoals);
        goalsRecyclerView.setAdapter(goalsAdapter);

        loadSavingsGoals();

        addGoalButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddGoalActivity.class);
            startActivity(intent);
        });

        backButton.setOnClickListener(v -> {
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSavingsGoals();
    }

    private void loadSavingsGoals() {
        db.collection("finance")
                .document(uid)
                .collection("savingsGoals")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    savingsGoals.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        SavingsGoal goal = document.toObject(SavingsGoal.class);
                        goal.setId(document.getId());
                        savingsGoals.add(goal);
                    }
                    goalsAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load goals", Toast.LENGTH_SHORT).show();
                });
    }

    private class GoalsAdapter extends RecyclerView.Adapter<GoalsAdapter.GoalViewHolder> {
        private List<SavingsGoal> goals;

        public GoalsAdapter(List<SavingsGoal> goals) {
            this.goals = goals;
        }

        @Override
        public GoalViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_savings_goal, parent, false);
            return new GoalViewHolder(view);
        }

        @Override
        public void onBindViewHolder(GoalViewHolder holder, int position) {
            SavingsGoal goal = goals.get(position);
            holder.goalName.setText(goal.getName());
            holder.goalProgress.setText(String.format("%.2f€/%.2f€ (%.0f%%)",
                    goal.getCurrentAmount(),
                    goal.getTargetAmount(),
                    (goal.getCurrentAmount() / goal.getTargetAmount()) * 100));
            holder.targetDate.setText("Target date: " + goal.getTargetDate());

            int progress = (int) ((goal.getCurrentAmount() / goal.getTargetAmount()) * 100);
            holder.progressBar.setProgress(progress);

            holder.addAmountButton.setOnClickListener(v -> {
                showAddAmountDialog(goal);
            });
        }

        @Override
        public int getItemCount() {
            return goals.size();
        }

        private void showAddAmountDialog(SavingsGoal goal) {
            AlertDialog.Builder builder = new AlertDialog.Builder(SavingsGoalsActivity.this);
            View dialogView = LayoutInflater.from(SavingsGoalsActivity.this)
                    .inflate(R.layout.dialog_add_amount, null);

            EditText amountInput = dialogView.findViewById(R.id.amountInput);
            Button cancelButton = dialogView.findViewById(R.id.cancelButton);
            Button saveButton = dialogView.findViewById(R.id.saveButton);

            builder.setView(dialogView);
            AlertDialog dialog = builder.create();

            cancelButton.setOnClickListener(v -> dialog.dismiss());

            saveButton.setOnClickListener(v -> {
                String amountStr = amountInput.getText().toString().trim();
                if (amountStr.isEmpty()) {
                    amountInput.setError("Please enter an amount");
                    return;
                }

                try {
                    double amount = Double.parseDouble(amountStr);
                    double newAmount = goal.getCurrentAmount() + amount;

                    if (newAmount > goal.getTargetAmount()) {
                        Toast.makeText(SavingsGoalsActivity.this,
                                "Amount exceeds goal target", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Update in Firestore
                    db.collection("finance")
                            .document(uid)
                            .collection("savingsGoals")
                            .document(goal.getId())
                            .update("currentAmount", newAmount)
                            .addOnSuccessListener(aVoid -> {
                                goal.setCurrentAmount(newAmount);
                                notifyDataSetChanged();
                                dialog.dismiss();
                                Toast.makeText(SavingsGoalsActivity.this,
                                        "Savings added successfully", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(SavingsGoalsActivity.this,
                                        "Failed to update savings", Toast.LENGTH_SHORT).show();
                            });

                } catch (NumberFormatException e) {
                    amountInput.setError("Invalid amount");
                }
            });

            dialog.show();
        }

        class GoalViewHolder extends RecyclerView.ViewHolder {
            TextView goalName, goalProgress, targetDate;
            com.google.android.material.progressindicator.LinearProgressIndicator progressBar;
            Button addAmountButton;

            public GoalViewHolder(View itemView) {
                super(itemView);
                goalName = itemView.findViewById(R.id.goalName);
                goalProgress = itemView.findViewById(R.id.goalProgress);
                targetDate = itemView.findViewById(R.id.targetDate);
                progressBar = itemView.findViewById(R.id.progressBar);
                addAmountButton = itemView.findViewById(R.id.addAmountButton);
            }
        }
    }
}