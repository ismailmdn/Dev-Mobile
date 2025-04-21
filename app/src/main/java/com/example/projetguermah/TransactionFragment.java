package com.example.projetguermah;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
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

public class TransactionFragment extends Fragment implements TransactionAdapter.OnTransactionClickListener {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private RecyclerView recyclerView;
    private ProgressBar loadingProgress;
    private TextView emptyStateText;
    private TransactionAdapter adapter;
    private List<Transaction> transactions;
    private FloatingActionButton fabAddTransaction;
    private Calendar selectedDate;
    private SimpleDateFormat dateFormat;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        transactions = new ArrayList<>();
        selectedDate = Calendar.getInstance();
        dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_transaction, container, false);
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
        
        // Use nested collection structure: /transaction/{userId}/transactions/
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
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_add_transaction, null);
        dialogBuilder.setView(dialogView);

        // Initialize dialog views
        TextInputEditText titleInput = dialogView.findViewById(R.id.et_transaction_title);
        TextInputEditText amountInput = dialogView.findViewById(R.id.et_transaction_amount);
        TextInputEditText categoryInput = dialogView.findViewById(R.id.et_transaction_category);
        RadioGroup typeRadioGroup = dialogView.findViewById(R.id.rg_transaction_type);
        Button dateButton = dialogView.findViewById(R.id.btn_pick_date);

        // Set current date as default
        selectedDate = Calendar.getInstance();
        dateButton.setText("Date: " + dateFormat.format(selectedDate.getTime()));

        // Setup date picker
        dateButton.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        selectedDate.set(Calendar.YEAR, year);
                        selectedDate.set(Calendar.MONTH, month);
                        selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        dateButton.setText("Date: " + dateFormat.format(selectedDate.getTime()));
                    },
                    selectedDate.get(Calendar.YEAR),
                    selectedDate.get(Calendar.MONTH),
                    selectedDate.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });

        // Build the dialog with Save and Cancel buttons
        AlertDialog dialog = dialogBuilder.setPositiveButton("Save", null)
                .setNegativeButton("Cancel", (dialog1, which) -> dialog1.dismiss())
                .create();

        dialog.show();

        // Override the positive button click to prevent dismissal on validation error
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String title = titleInput.getText().toString().trim();
            String amountStr = amountInput.getText().toString().trim();
            String category = categoryInput.getText().toString().trim();
            
            // Validate inputs
            if (title.isEmpty()) {
                titleInput.setError("Title is required");
                return;
            }
            
            if (amountStr.isEmpty()) {
                amountInput.setError("Amount is required");
                return;
            }
            
            double amount;
            try {
                amount = Double.parseDouble(amountStr);
                if (amount <= 0) {
                    amountInput.setError("Amount must be greater than 0");
                    return;
                }
            } catch (NumberFormatException e) {
                amountInput.setError("Invalid amount format");
                return;
            }
            
            if (category.isEmpty()) {
                categoryInput.setError("Category is required");
                return;
            }

            // Get transaction type
            String type = typeRadioGroup.getCheckedRadioButtonId() == R.id.rb_income ? "income" : "expense";
            
            // Save transaction
            saveTransaction(title, amount, selectedDate.getTime(), type, category);
            
            // Dismiss dialog
            dialog.dismiss();
        });
    }

    private void saveTransaction(String title, double amount, Date date, String type, String category) {
        // Check authentication
        if (mAuth.getCurrentUser() == null) {
            showError("User not authenticated");
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();
        
        // Show loading indicator
        showLoading(true);
        
        // Create transaction object
        Map<String, Object> transactionData = new HashMap<>();
        // No need to store userId inside the document anymore since it's in the path
        transactionData.put("title", title);
        transactionData.put("amount", amount);
        transactionData.put("date", date);
        transactionData.put("type", type);
        transactionData.put("category", category);
        
        // Save to Firestore using nested collection structure
        db.collection("transaction")
            .document(userId)
            .collection("transactions")
            .add(transactionData)
            .addOnSuccessListener(documentReference -> {
                // Create local transaction object and add to list
                Transaction newTransaction = new Transaction(
                        documentReference.getId(),
                        userId,
                        title,
                        amount,
                        date,
                        type,
                        category
                );
                transactions.add(0, newTransaction); // Add to beginning of list since we're sorting by date DESC
                adapter.notifyDataSetChanged();
                
                // Update UI
                showLoading(false);
                updateEmptyState();
                showError("Transaction added successfully");
            })
            .addOnFailureListener(e -> {
                showLoading(false);
                String errorMessage = e.getMessage();
                if (errorMessage != null && errorMessage.contains("permission")) {
                    showError("Permission denied. Please check your authentication.");
                } else {
                    showError("Failed to add transaction: " + errorMessage);
                }
            });
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
        // TODO: Implement transaction details/editing
        Toast.makeText(getContext(), "Transaction details will be implemented", Toast.LENGTH_SHORT).show();
    }
}