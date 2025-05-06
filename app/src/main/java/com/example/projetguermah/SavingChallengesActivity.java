package com.example.projetguermah;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.projetguermah.model.SavingChallenge;
import com.example.projetguermah.utils.DateUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class SavingChallengesActivity extends AppCompatActivity {

    private RecyclerView challengesRecyclerView;
    private Button backButton, historyButton;
    private List<SavingChallenge> allChallenges = new ArrayList<>();
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
        backButton = findViewById(R.id.backButton);
        historyButton = findViewById(R.id.historyButton);

        challengesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        challengesAdapter = new ChallengesAdapter(allChallenges);
        challengesRecyclerView.setAdapter(challengesAdapter);

        loadChallenges();

        backButton.setOnClickListener(v -> finish());
        historyButton.setOnClickListener(v -> {
            Toast.makeText(this, "Challenge history coming soon!", Toast.LENGTH_SHORT).show();
        });
    }

    private void loadChallenges() {
        List<SavingChallenge> predefinedChallenges = new ArrayList<>();
        predefinedChallenges.add(new SavingChallenge(
                "No Spend Weekend",
                "Avoid all non-essential spending from Friday evening to Sunday night",
                0,
                "2 days",
                "NO_SPEND"));

        predefinedChallenges.add(new SavingChallenge(
                "Weekly Savings",
                "Save €100 this week by cutting unnecessary expenses",
                100,
                "1 week",
                "SAVINGS_TARGET"));

        db.collection("finance")
                .document(uid)
                .collection("activeChallenges")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allChallenges.clear();
                    allChallenges.addAll(predefinedChallenges);

                    for (DocumentChange dc : queryDocumentSnapshots.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.ADDED) {
                            SavingChallenge challenge = dc.getDocument().toObject(SavingChallenge.class);
                            challenge.setId(dc.getDocument().getId());
                            challenge.setType("active");
                            allChallenges.add(challenge);
                        }
                    }

                    challengesAdapter.notifyDataSetChanged();
                });
    }

    private class ChallengesAdapter extends RecyclerView.Adapter<ChallengesAdapter.ChallengeViewHolder> {
        private final List<SavingChallenge> challenges;

        public ChallengesAdapter(List<SavingChallenge> challenges) {
            this.challenges = challenges;
        }

        @Override
        public ChallengeViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_saving_challenge, parent, false);
            return new ChallengeViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ChallengeViewHolder holder, int position) {
            SavingChallenge challenge = challenges.get(position);
            holder.challengeName.setText(challenge.getName());
            holder.challengeDescription.setText(challenge.getDescription());

            if (challenge.getType().equals("predefined")) {
                setupPredefinedChallengeUI(holder, challenge);
            } else {
                setupActiveChallengeUI(holder, challenge);
            }
        }

        private void setupPredefinedChallengeUI(ChallengeViewHolder holder, SavingChallenge challenge) {
            holder.challengeTarget.setText("Duration: " + challenge.getDuration());
            holder.progressBar.setVisibility(View.GONE);
            holder.actionButton.setText("JOIN CHALLENGE");
            holder.actionButton.setBackgroundTintList(getResources().getColorStateList(R.color.colorPrimary));
            holder.actionButton.setOnClickListener(v -> joinChallenge(challenge));
        }

        private void setupActiveChallengeUI(ChallengeViewHolder holder, SavingChallenge challenge) {
            if (challenge.getChallengeType().equals("NO_SPEND")) {
                String timeRange = DateUtils.formatDateTime(challenge.getStartDate()) + " to " +
                        DateUtils.formatDateTime(challenge.getEndDate());
                holder.challengeTarget.setText("Active: " + timeRange);
                holder.progressBar.setVisibility(View.GONE);
                holder.actionButton.setText("MONITORING");
                holder.actionButton.setEnabled(false);
            } else {
                holder.challengeTarget.setText(String.format("Progress: %.2f€/%.2f€ (until %s)",
                        challenge.getCurrentAmount(),
                        challenge.getTargetAmount(),
                        DateUtils.formatDateShort(challenge.getEndDate())));
                holder.progressBar.setVisibility(View.VISIBLE);
                int progress = (int) ((challenge.getCurrentAmount() / challenge.getTargetAmount()) * 100);
                holder.progressBar.setProgress(progress);
                holder.actionButton.setText("ADD SAVINGS");
                holder.actionButton.setOnClickListener(v -> showAddSavingsDialog(challenge));
            }

            if (challenge.isCompleted()) {
                holder.actionButton.setText("COMPLETED");
                holder.actionButton.setBackgroundTintList(getResources().getColorStateList(R.color.colorSuccess));
                holder.actionButton.setEnabled(false);
            }
        }

        private void joinChallenge(SavingChallenge challenge) {
            if (challenge.getChallengeType().equals("NO_SPEND")) {
                showCalendarWeekendDialog(challenge);
            } else {
                // Default weekly challenge
                Calendar calendar = Calendar.getInstance();
                Date startDate = calendar.getTime();
                calendar.add(Calendar.WEEK_OF_YEAR, 1);
                Date endDate = calendar.getTime();

                SavingChallenge activeChallenge = new SavingChallenge(
                        challenge.getName(),
                        challenge.getDescription(),
                        challenge.getTargetAmount(),
                        challenge.getDuration(),
                        startDate,
                        endDate
                );
                activeChallenge.setChallengeType(challenge.getChallengeType());

                saveChallengeToFirestore(activeChallenge);
            }
        }

        private void showCalendarWeekendDialog(SavingChallenge challenge) {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(SavingChallengesActivity.this);
            View dialogView = LayoutInflater.from(SavingChallengesActivity.this)
                    .inflate(R.layout.dialog_calendar_weekend, null);

            Button btnSelectStart = dialogView.findViewById(R.id.btnSelectStart);
            Button btnSelectEnd = dialogView.findViewById(R.id.btnSelectEnd);
            TextView tvStartDate = dialogView.findViewById(R.id.tvStartDate);
            TextView tvEndDate = dialogView.findViewById(R.id.tvEndDate);

            // Default to next weekend (Friday 18:00 to Sunday 23:59)
            Calendar startCal = Calendar.getInstance();
            startCal.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
            startCal.set(Calendar.HOUR_OF_DAY, 18);
            startCal.set(Calendar.MINUTE, 0);
            if (startCal.before(Calendar.getInstance())) {
                startCal.add(Calendar.WEEK_OF_YEAR, 1);
            }

            Calendar endCal = (Calendar) startCal.clone();
            endCal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
            endCal.set(Calendar.HOUR_OF_DAY, 23);
            endCal.set(Calendar.MINUTE, 59);

            updateDateTextViews(tvStartDate, tvEndDate, startCal, endCal);

            btnSelectStart.setOnClickListener(v -> showDateTimePicker(startCal, true, () -> {
                if (startCal.after(endCal)) {
                    endCal.setTimeInMillis(startCal.getTimeInMillis());
                    endCal.add(Calendar.HOUR, 2); // Default 2 hour duration if needed
                }
                updateDateTextViews(tvStartDate, tvEndDate, startCal, endCal);
            }));

            btnSelectEnd.setOnClickListener(v -> showDateTimePicker(endCal, false, () -> {
                if (endCal.before(startCal)) {
                    startCal.setTimeInMillis(endCal.getTimeInMillis());
                    startCal.add(Calendar.HOUR, -2); // Default 2 hour duration if needed
                }
                updateDateTextViews(tvStartDate, tvEndDate, startCal, endCal);
            }));

            builder.setView(dialogView);
            android.app.AlertDialog dialog = builder.create();

            dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());

            dialogView.findViewById(R.id.btnSave).setOnClickListener(v -> {
                SavingChallenge activeChallenge = new SavingChallenge(
                        challenge.getName(),
                        challenge.getDescription(),
                        challenge.getTargetAmount(),
                        challenge.getDuration(),
                        startCal.getTime(),
                        endCal.getTime()
                );
                activeChallenge.setChallengeType(challenge.getChallengeType());

                saveChallengeToFirestore(activeChallenge);
                dialog.dismiss();
            });

            dialog.show();
        }

        private void showDateTimePicker(Calendar calendar, boolean isStart, Runnable callback) {
            // First show date picker
            DatePickerDialog datePicker = new DatePickerDialog(
                    SavingChallengesActivity.this,
                    (view, year, month, day) -> {
                        calendar.set(year, month, day);
                        // Then show time picker
                        TimePickerDialog timePicker = new TimePickerDialog(
                                SavingChallengesActivity.this,
                                (view1, hour, minute) -> {
                                    calendar.set(Calendar.HOUR_OF_DAY, hour);
                                    calendar.set(Calendar.MINUTE, minute);
                                    callback.run();
                                },
                                calendar.get(Calendar.HOUR_OF_DAY),
                                calendar.get(Calendar.MINUTE),
                                true
                        );
                        timePicker.setTitle(isStart ? "Select Start Time" : "Select End Time");
                        timePicker.show();
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            );
            datePicker.setTitle(isStart ? "Select Start Date" : "Select End Date");
            datePicker.show();
        }

        private void updateDateTextViews(TextView tvStart, TextView tvEnd, Calendar start, Calendar end) {
            tvStart.setText("Start: " + DateUtils.formatDateTime(start.getTime()));
            tvEnd.setText("End: " + DateUtils.formatDateTime(end.getTime()));
        }

        private void saveChallengeToFirestore(SavingChallenge challenge) {
            db.collection("finance")
                    .document(uid)
                    .collection("activeChallenges")
                    .add(challenge)
                    .addOnSuccessListener(documentReference -> {
                        challenge.setId(documentReference.getId());
                        challenges.add(challenge);
                        notifyDataSetChanged();
                        Toast.makeText(SavingChallengesActivity.this,
                                "Challenge joined!", Toast.LENGTH_SHORT).show();
                    });
        }

        private void showAddSavingsDialog(SavingChallenge challenge) {
            // Keep existing implementation
            // ...
        }

        @Override
        public int getItemCount() {
            return challenges.size();
        }

        class ChallengeViewHolder extends RecyclerView.ViewHolder {
            TextView challengeName, challengeDescription, challengeTarget;
            com.google.android.material.progressindicator.LinearProgressIndicator progressBar;
            Button actionButton;

            public ChallengeViewHolder(View itemView) {
                super(itemView);
                challengeName = itemView.findViewById(R.id.challengeName);
                challengeDescription = itemView.findViewById(R.id.challengeDescription);
                challengeTarget = itemView.findViewById(R.id.challengeTarget);
                progressBar = itemView.findViewById(R.id.progressBar);
                actionButton = itemView.findViewById(R.id.actionButton);
            }
        }
    }
}