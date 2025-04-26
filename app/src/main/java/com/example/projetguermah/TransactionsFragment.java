package com.example.projetguermah;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class TransactionsFragment extends Fragment implements TransactionAdapter.OnTransactionClickListener {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private RecyclerView recyclerView;
    private ProgressBar loadingProgress;
    private TextView emptyStateText;
    private TransactionAdapter adapter;
    private List<Transaction> transactions;
    private FloatingActionButton fabAddTransaction;
    private static final int TRANSACTION_DETAILS_REQUEST_CODE = 1002;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        transactions = new ArrayList<>();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_transactions, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize views
        recyclerView = view.findViewById(R.id.transactions_recycler_view);
        loadingProgress = view.findViewById(R.id.loading_progress);
        emptyStateText = view.findViewById(R.id.empty_state_text);
        fabAddTransaction = view.findViewById(R.id.fab_add_transaction);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TransactionAdapter(transactions, this);
        recyclerView.setAdapter(adapter);

        // Setup FAB
        fabAddTransaction.setOnClickListener(v -> showAddTransactionDialog());

        // Check if user is logged in
        if (mAuth.getCurrentUser() == null) {
            showError("Please log in to view transactions");
            return;
        }

        // Load transactions
        loadTransactions();
    }

    private void loadTransactions() {
        showLoading(true);
        
        if (mAuth.getCurrentUser() == null) {
            showLoading(false);
            showError("User not authenticated");
            return;
        }
        
        String userId = mAuth.getCurrentUser().getUid();
        
        // Use consistent collection structure: /transaction/{userId}/transactions/
        db.collection("transaction")
                .document(userId)
                .collection("transactions")
                .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    transactions.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Transaction transaction = document.toObject(Transaction.class);
                            transaction.setId(document.getId());
                            transactions.add(transaction);
                        } catch (Exception e) {
                            showError("Error parsing transaction: " + e.getMessage());
                        }
                    }
                    adapter.notifyDataSetChanged();
                    showLoading(false);
                    updateEmptyState();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    String errorMessage = e.getMessage();
                    if (errorMessage != null && errorMessage.contains("permission")) {
                        showError("Permission denied. Please check your authentication.");
                    } else {
                        showError("Failed to load transactions: " + errorMessage);
                    }
                });
    }

    private void showAddTransactionDialog() {
        // TODO: Implement dialog for adding new transaction
        Toast.makeText(getContext(), "Add transaction dialog will be implemented", Toast.LENGTH_SHORT).show();
    }

    private void showLoading(boolean show) {
        loadingProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void showError(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void updateEmptyState() {
        emptyStateText.setVisibility(transactions.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onTransactionClick(Transaction transaction) {
        Intent intent = new Intent(getActivity(), TransactionDetailsActivity.class);
        intent.putExtra(TransactionDetailsActivity.EXTRA_TRANSACTION_ID, transaction.getId());
        intent.putExtra(TransactionDetailsActivity.EXTRA_USER_ID, mAuth.getCurrentUser().getUid());
        startActivityForResult(intent, TRANSACTION_DETAILS_REQUEST_CODE);
    }

    @Override
    public void onDeleteClick(Transaction transaction, int position) {
        // Ask for confirmation before deletion
        new AlertDialog.Builder(requireContext())
            .setTitle("Delete Transaction")
            .setMessage("Are you sure you want to delete this transaction?")
            .setPositiveButton("Delete", (dialog, which) -> {
                deleteTransaction(transaction, position);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void deleteTransaction(Transaction transaction, int position) {
        if (mAuth.getCurrentUser() == null) {
            showError("You need to be logged in to delete transactions");
            return;
        }
        
        showLoading(true);
        String userId = mAuth.getCurrentUser().getUid();
        
        db.collection("transaction")
            .document(userId)
            .collection("transactions")
            .document(transaction.getId())
            .delete()
            .addOnSuccessListener(aVoid -> {
                // Remove from local list
                transactions.remove(position);
                adapter.notifyItemRemoved(position);
                updateEmptyState();
                showLoading(false);
                Toast.makeText(getContext(), "Transaction deleted successfully", Toast.LENGTH_SHORT).show();
            })
            .addOnFailureListener(e -> {
                showLoading(false);
                showError("Failed to delete transaction: " + e.getMessage());
            });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == TRANSACTION_DETAILS_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // Refresh transactions list after potentially deleting a transaction
            loadTransactions();
        }
    }
} 