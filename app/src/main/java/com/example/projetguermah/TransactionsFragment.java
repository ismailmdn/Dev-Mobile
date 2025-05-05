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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TransactionsFragment extends Fragment implements TransactionAdapter.OnTransactionClickListener {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private RecyclerView recyclerView;
    private ProgressBar loadingProgress;
    private TextView emptyStateText;
    private TransactionAdapter adapter;
    private List<Transaction> transactions = new ArrayList<>();
    private List<Transaction> allTransactions = new ArrayList<>();
    private FloatingActionButton fabAddTransaction;
    private MaterialButton btnFilterMonth, btnFilterType;
    private static final int TRANSACTION_DETAILS_REQUEST_CODE = 1002;

    // Filter variables
    private String selectedMonth = null;
    private String selectedType = null;
    private final SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
    private final SimpleDateFormat firestoreMonthFormat = new SimpleDateFormat("yyyy-MM", Locale.getDefault());

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
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
        btnFilterMonth = view.findViewById(R.id.btn_filter_month);
        btnFilterType = view.findViewById(R.id.btn_filter_type);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TransactionAdapter(transactions, this);
        recyclerView.setAdapter(adapter);

        // Setup FAB
        fabAddTransaction.setOnClickListener(v -> showAddTransactionDialog());

        // Setup filter buttons
        btnFilterMonth.setOnClickListener(v -> showMonthFilterDialog());
        btnFilterType.setOnClickListener(v -> showTypeFilterDialog());

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

        db.collection("transaction")
                .document(userId)
                .collection("transactions")
                .orderBy("date", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allTransactions.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Transaction transaction = document.toObject(Transaction.class);
                            transaction.setId(document.getId());
                            allTransactions.add(transaction);
                        } catch (Exception e) {
                            showError("Error parsing transaction: " + e.getMessage());
                        }
                    }
                    applyFilters();
                    showLoading(false);
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

    private void applyFilters() {
        transactions.clear();

        for (Transaction transaction : allTransactions) {
            boolean matchesMonth = true;
            boolean matchesType = true;

            // Apply month filter if selected
            if (selectedMonth != null) {
                String transactionMonth = firestoreMonthFormat.format(transaction.getDate());
                matchesMonth = selectedMonth.equals(transactionMonth);
            }

            // Apply type filter if selected
            if (selectedType != null) {
                matchesType = selectedType.equalsIgnoreCase(transaction.getType());
            }

            // Add to filtered list if matches both filters
            if (matchesMonth && matchesType) {
                transactions.add(transaction);
            }
        }

        adapter.notifyDataSetChanged();
        updateEmptyState();
        updateFilterButtonsText();
    }

    private void updateFilterButtonsText() {
        // Update month filter button text
        if (selectedMonth == null) {
            btnFilterMonth.setText("All Months");
        } else {
            try {
                Date date = firestoreMonthFormat.parse(selectedMonth);
                btnFilterMonth.setText(monthFormat.format(date));
            } catch (Exception e) {
                btnFilterMonth.setText(selectedMonth);
            }
        }

        // Update type filter button text
        btnFilterType.setText(selectedType == null ? "All Types" :
                selectedType.equalsIgnoreCase("income") ? "Income" : "Expense");
    }

    private void showMonthFilterDialog() {
        // Get unique months from transactions
        Map<String, String> monthsMap = new HashMap<>();
        Calendar cal = Calendar.getInstance();

        for (Transaction t : allTransactions) {
            String monthKey = firestoreMonthFormat.format(t.getDate());
            String monthDisplay = monthFormat.format(t.getDate());
            monthsMap.put(monthKey, monthDisplay);
        }

        if (monthsMap.isEmpty()) {
            Toast.makeText(getContext(), "No transactions available to filter", Toast.LENGTH_SHORT).show();
            return;
        }

        final String[] monthKeys = monthsMap.keySet().toArray(new String[0]);
        final String[] monthDisplayNames = new String[monthKeys.length + 1];
        monthDisplayNames[0] = "All Months";
        for (int i = 0; i < monthKeys.length; i++) {
            monthDisplayNames[i + 1] = monthsMap.get(monthKeys[i]);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Filter by Month");

        int checkedItem = 0; // "All Months" is selected by default
        if (selectedMonth != null) {
            for (int i = 0; i < monthKeys.length; i++) {
                if (monthKeys[i].equals(selectedMonth)) {
                    checkedItem = i + 1;
                    break;
                }
            }
        }

        builder.setSingleChoiceItems(monthDisplayNames, checkedItem, (dialog, which) -> {
            if (which == 0) {
                selectedMonth = null;
            } else {
                selectedMonth = monthKeys[which - 1];
            }
            applyFilters();
            dialog.dismiss();
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showTypeFilterDialog() {
        String[] types = {"All Types", "Income", "Expense"};

        int checkedItem = 0; // "All Types" is selected by default
        if (selectedType != null) {
            if (selectedType.equalsIgnoreCase("income")) {
                checkedItem = 1;
            } else if (selectedType.equalsIgnoreCase("expense")) {
                checkedItem = 2;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Filter by Type");
        builder.setSingleChoiceItems(types, checkedItem, (dialog, which) -> {
            if (which == 0) {
                selectedType = null;
            } else {
                selectedType = types[which].toLowerCase();
            }
            applyFilters();
            dialog.dismiss();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showAddTransactionDialog() {
        // TODO: Implement dialog for adding new transaction
        Toast.makeText(getContext(), "Add transaction dialog will be implemented", Toast.LENGTH_SHORT).show();
    }

    private void showLoading(boolean show) {
        loadingProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(show ? View.GONE : View.VISIBLE);
        emptyStateText.setVisibility(View.GONE);
    }

    private void showError(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void updateEmptyState() {
        emptyStateText.setVisibility(transactions.isEmpty() ? View.VISIBLE : View.GONE);
        emptyStateText.setText(selectedMonth == null && selectedType == null ?
                "No transactions yet" : "No matching transactions");
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
                    // Remove from both lists
                    allTransactions.remove(transaction);
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