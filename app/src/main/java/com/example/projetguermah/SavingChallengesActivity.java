package com.example.projetguermah;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projetguermah.model.SavingChallenge;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class SavingChallengesActivity extends AppCompatActivity {

    private RecyclerView challengesRecyclerView;
    private Button joinChallengeButton;
    private List<SavingChallenge> savingChallenges = new ArrayList<>();
    private ChallengesAdapter challengesAdapter;
    private FirebaseFirestore db;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saving_challenges);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        challengesRecyclerView = findViewById(R.id.challengesRecyclerView);
        joinChallengeButton = findViewById(R.id.joinChallengeButton);

        challengesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        challengesAdapter = new ChallengesAdapter(savingChallenges);
        challengesRecyclerView.setAdapter(challengesAdapter);

        loadChallenges();

        joinChallengeButton.setOnClickListener(v -> {
            // Implement challenge joining logic
            Toast.makeText(this, "Challenge joined!", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadChallenges() {
        // Predefined challenges - could also load from Firestore
        savingChallenges.clear();
        savingChallenges.add(new SavingChallenge(
                "No Spend Weekend",
                "Avoid unnecessary spending for an entire weekend",
                0,
                "2 days"));
        savingChallenges.add(new SavingChallenge(
                "Weekly Savings",
                "Save €100 this week",
                100,
                "1 week"));
        savingChallenges.add(new SavingChallenge(
                "Monthly Challenge",
                "Save €500 this month",
                500,
                "1 month"));

        challengesAdapter.notifyDataSetChanged();
    }

    private class ChallengesAdapter extends RecyclerView.Adapter<ChallengesAdapter.ChallengeViewHolder> {
        private List<SavingChallenge> challenges;

        public ChallengesAdapter(List<SavingChallenge> challenges) {
            this.challenges = challenges;
        }

        @Override
        public ChallengeViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_saving_challenge, parent, false);
            return new ChallengeViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ChallengeViewHolder holder, int position) {
            SavingChallenge challenge = challenges.get(position);
            holder.challengeName.setText(challenge.getName());
            holder.challengeDescription.setText(challenge.getDescription());
            holder.challengeTarget.setText(String.format("Target: %.2f in %s",
                    challenge.getTargetAmount(), challenge.getDuration()));

            holder.itemView.setOnClickListener(v -> {
                // Handle challenge click
                Toast.makeText(SavingChallengesActivity.this,
                        "Joining: " + challenge.getName(), Toast.LENGTH_SHORT).show();
            });
        }

        @Override
        public int getItemCount() {
            return challenges.size();
        }

        class ChallengeViewHolder extends RecyclerView.ViewHolder {
            TextView challengeName, challengeDescription, challengeTarget;

            public ChallengeViewHolder(View itemView) {
                super(itemView);
                challengeName = itemView.findViewById(R.id.challengeName);
                challengeDescription = itemView.findViewById(R.id.challengeDescription);
                challengeTarget = itemView.findViewById(R.id.challengeTarget);
            }
        }
    }
}