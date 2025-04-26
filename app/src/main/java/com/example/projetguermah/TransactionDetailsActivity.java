package com.example.projetguermah;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class TransactionDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_TRANSACTION_ID = "transaction_id";
    public static final String EXTRA_USER_ID = "user_id";
    
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    
    private TextView transactionTitle;
    private TextView transactionAmount;
    private TextView transactionCategory;
    private TextView transactionType;
    private TextView transactionDate;
    private Button deleteButton;
    
    private String transactionId;
    private String userId;
    private Transaction transaction;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_details);
        
        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        
        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbar.setNavigationOnClickListener(v -> finish());
        
        // Initialize views
        transactionTitle = findViewById(R.id.transaction_title);
        transactionAmount = findViewById(R.id.transaction_amount);
        transactionCategory = findViewById(R.id.transaction_category);
        transactionType = findViewById(R.id.transaction_type);
        transactionDate = findViewById(R.id.transaction_date);
        deleteButton = findViewById(R.id.delete_transaction_btn);
        
        // Get transaction data from intent
        Intent intent = getIntent();
        if (intent != null) {
            transactionId = intent.getStringExtra(EXTRA_TRANSACTION_ID);
            userId = intent.getStringExtra(EXTRA_USER_ID);
            
            if (transactionId != null && userId != null) {
                loadTransactionDetails();
            } else {
                showError("Transaction details not available");
                finish();
            }
        }
        
        // Setup delete button
        deleteButton.setOnClickListener(v -> confirmDelete());
    }
    
    private void loadTransactionDetails() {
        db.collection("transaction")
            .document(userId)
            .collection("transactions")
            .document(transactionId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    transaction = documentSnapshot.toObject(Transaction.class);
                    transaction.setId(documentSnapshot.getId());
                    displayTransactionDetails();
                } else {
                    showError("Transaction not found");
                    finish();
                }
            })
            .addOnFailureListener(e -> {
                showError("Failed to load transaction: " + e.getMessage());
                finish();
            });
    }
    
    private void displayTransactionDetails() {
        // Set transaction title
        transactionTitle.setText(transaction.getTitle());
        
        // Format and set amount with + or - sign
        String amountText = (transaction.getType().equals("income") ? "+" : "-") + 
                           String.format(Locale.getDefault(), "%.2f", transaction.getAmount());
        transactionAmount.setText(amountText);
        
        // Set amount color based on type
        int color = getResources().getColor(
                transaction.getType().equals("income") ? 
                android.R.color.holo_green_dark : 
                android.R.color.holo_red_dark);
        transactionAmount.setTextColor(color);
        
        // Set other details
        transactionCategory.setText(transaction.getCategory());
        transactionType.setText(transaction.getType());
        
        // Format and set date
        if (transaction.getDate() != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy HH:mm", Locale.getDefault());
            transactionDate.setText(dateFormat.format(transaction.getDate()));
        }
    }
    
    private void confirmDelete() {
        new AlertDialog.Builder(this)
            .setTitle("Delete Transaction")
            .setMessage("Are you sure you want to delete this transaction?")
            .setPositiveButton("Delete", (dialog, which) -> deleteTransaction())
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void deleteTransaction() {
        db.collection("transaction")
            .document(userId)
            .collection("transactions")
            .document(transactionId)
            .delete()
            .addOnSuccessListener(aVoid -> {
                showToast("Transaction deleted successfully");
                setResult(RESULT_OK);
                finish();
            })
            .addOnFailureListener(e -> {
                showError("Failed to delete transaction: " + e.getMessage());
            });
    }
    
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
} 