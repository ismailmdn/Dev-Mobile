package com.example.projetguermah.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.projetguermah.R;
import com.example.projetguermah.model.SavingChallenge;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.Calendar;
import java.util.Date;

public class ChallengeMonitorService extends Service {
    private FirebaseFirestore db;
    private String uid;
    private ListenerRegistration transactionListener;

    @Override
    public void onCreate() {
        super.onCreate();
        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        createNotificationChannel();
        startForeground(1, createNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        monitorActiveChallenges();
        return START_STICKY;
    }

    private void monitorActiveChallenges() {
        db.collection("finance")
                .document(uid)
                .collection("activeChallenges")
                .whereEqualTo("challengeType", "NO_SPEND")
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) return;

                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.ADDED ||
                                dc.getType() == DocumentChange.Type.MODIFIED) {
                            SavingChallenge challenge = dc.getDocument().toObject(SavingChallenge.class);
                            monitorTransactionsForChallenge(challenge);
                        }
                    }
                });
    }

    private void monitorTransactionsForChallenge(SavingChallenge challenge) {
        // Stop previous listener if exists
        if (transactionListener != null) {
            transactionListener.remove();
        }

        transactionListener = db.collection("transaction")
                .document(uid)
                .collection("transactions")
                .whereGreaterThanOrEqualTo("date", challenge.getStartDate())
                .whereLessThanOrEqualTo("date", challenge.getEndDate())
                .addSnapshotListener((transactions, error) -> {
                    if (error != null) return;

                    if (!transactions.isEmpty()) {
                        // Transaction detected during challenge period
                        handleChallengeFailure(challenge);
                    }
                });
    }

    private void handleChallengeFailure(SavingChallenge challenge) {
        // Move to failed challenges
        db.collection("finance")
                .document(uid)
                .collection("failedChallenges")
                .add(challenge)
                .addOnSuccessListener(documentReference -> {
                    // Remove from active challenges
                    db.collection("finance")
                            .document(uid)
                            .collection("activeChallenges")
                            .document(challenge.getId())
                            .delete();

                    // Show notification
                    showFailureNotification(challenge);
                });
    }

    private void showFailureNotification(SavingChallenge challenge) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification notification = new NotificationCompat.Builder(this, "CHALLENGE_CHANNEL")
                .setContentTitle("Challenge Failed!")
                .setContentText("You spent money during your No Spend Weekend challenge")
                .setSmallIcon(R.drawable.ic_warning)
                .setAutoCancel(true)
                .build();

        manager.notify((int) System.currentTimeMillis(), notification);
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                "CHALLENGE_CHANNEL",
                "Challenge Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.createNotificationChannel(channel);
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, "CHALLENGE_CHANNEL")
                .setContentTitle("Challenge Monitor Running")
                .setContentText("Monitoring your No Spend challenges")
                .setSmallIcon(R.drawable.ic_savings)
                .build();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}