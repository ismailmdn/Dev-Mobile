package com.example.projetguermah;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
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
import android.app.Activity;

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
    private static final int ADD_TRANSACTION_REQUEST_CODE = 1001;

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

        // Setup FAB to launch AddTransactionActivity
        fabAddTransaction.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddTransactionActivity.class);
            startActivityForResult(intent, ADD_TRANSACTION_REQUEST_CODE);
        });

        // Check if user is logged in
        if (mAuth.getCurrentUser() == null) {
            showError("Please log in to view transactions");
            return;
        }

        // Load transactions
        loadTransactions();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == ADD_TRANSACTION_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // Refresh transactions list after adding a new one
            loadTransactions();
        }
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

        // Create dialog
        AlertDialog dialog = dialogBuilder.create();
        dialog.show();

        // Initialize dialog views
        EditText amountInput = dialogView.findViewById(R.id.et_transaction_amount);
        EditText noteInput = dialogView.findViewById(R.id.et_transaction_note);
        Button dateButton = dialogView.findViewById(R.id.btn_date);
        Button timeButton = dialogView.findViewById(R.id.btn_time);
        Button expendButton = dialogView.findViewById(R.id.btn_expend);
        Button incomeButton = dialogView.findViewById(R.id.btn_income);
        Button loanButton = dialogView.findViewById(R.id.btn_loan);
        TextView cancelButton = dialogView.findViewById(R.id.btn_cancel);
        TextView saveButton = dialogView.findViewById(R.id.btn_save);

        // Category views - Row 1
        View foodCategory = dialogView.findViewById(R.id.category_food);
        View socialCategory = dialogView.findViewById(R.id.category_social);
        View trafficCategory = dialogView.findViewById(R.id.category_traffic);
        View shoppingCategory = dialogView.findViewById(R.id.category_shopping);
        
        // Category views - Row 2
        View groceryCategory = dialogView.findViewById(R.id.category_grocery);
        View educationCategory = dialogView.findViewById(R.id.category_education);
        View billsCategory = dialogView.findViewById(R.id.category_bills);
        View rentalsCategory = dialogView.findViewById(R.id.category_rentals);
        
        // Category views - Row 3
        View medicalCategory = dialogView.findViewById(R.id.category_medical);
        View investmentCategory = dialogView.findViewById(R.id.category_investment);
        View giftCategory = dialogView.findViewById(R.id.category_gift);
        View otherCategory = dialogView.findViewById(R.id.category_other);

        // Set current date and time as default
        selectedDate = Calendar.getInstance();
        updateDateTimeButtons(dateButton, timeButton);

        // Initialize transaction type (default is Expend)
        final String[] transactionType = {"expense"};
        final String[] selectedCategory = {"Food"}; // Default category

        // Setup transaction type toggle
        expendButton.setOnClickListener(v -> {
            resetTransactionTypes(expendButton, incomeButton, loanButton);
            expendButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1D4752")));
            expendButton.setTextColor(Color.WHITE);
            transactionType[0] = "expense";
        });

        incomeButton.setOnClickListener(v -> {
            resetTransactionTypes(expendButton, incomeButton, loanButton);
            incomeButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1D4752")));
            incomeButton.setTextColor(Color.WHITE);
            transactionType[0] = "income";
        });

        loanButton.setOnClickListener(v -> {
            resetTransactionTypes(expendButton, incomeButton, loanButton);
            loanButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1D4752")));
            loanButton.setTextColor(Color.WHITE);
            transactionType[0] = "loan";
        });

        // Setup date picker
        dateButton.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        selectedDate.set(Calendar.YEAR, year);
                        selectedDate.set(Calendar.MONTH, month);
                        selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        updateDateTimeButtons(dateButton, timeButton);
                    },
                    selectedDate.get(Calendar.YEAR),
                    selectedDate.get(Calendar.MONTH),
                    selectedDate.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });

        // Setup time picker
        timeButton.setOnClickListener(v -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(
                    requireContext(),
                    (view, hourOfDay, minute) -> {
                        selectedDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        selectedDate.set(Calendar.MINUTE, minute);
                        updateDateTimeButtons(dateButton, timeButton);
                    },
                    selectedDate.get(Calendar.HOUR_OF_DAY),
                    selectedDate.get(Calendar.MINUTE),
                    true
            );
            timePickerDialog.show();
        });

        // Setup category selection
        View.OnClickListener categoryClickListener = v -> {
            // Reset all categories
            resetCategorySelection(
                foodCategory, socialCategory, trafficCategory, shoppingCategory,
                groceryCategory, educationCategory, billsCategory, rentalsCategory,
                medicalCategory, investmentCategory, giftCategory, otherCategory
            );
            
            // Highlight selected category
            v.setSelected(true);
            v.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E0E0E0")));
            
            // Get selected category
            int id = v.getId();
            if (id == R.id.category_food) {
                selectedCategory[0] = "Food";
            } else if (id == R.id.category_social) {
                selectedCategory[0] = "Social";
            } else if (id == R.id.category_traffic) {
                selectedCategory[0] = "Traffic";
            } else if (id == R.id.category_shopping) {
                selectedCategory[0] = "Shopping";
            } else if (id == R.id.category_grocery) {
                selectedCategory[0] = "Grocery";
            } else if (id == R.id.category_education) {
                selectedCategory[0] = "Education";
            } else if (id == R.id.category_bills) {
                selectedCategory[0] = "Bills";
            } else if (id == R.id.category_rentals) {
                selectedCategory[0] = "Rentals";
            } else if (id == R.id.category_medical) {
                selectedCategory[0] = "Medical";
            } else if (id == R.id.category_investment) {
                selectedCategory[0] = "Investment";
            } else if (id == R.id.category_gift) {
                selectedCategory[0] = "Gift";
            } else if (id == R.id.category_other) {
                selectedCategory[0] = "Other";
            }
        };

        // Apply click listener to all categories
        foodCategory.setOnClickListener(categoryClickListener);
        socialCategory.setOnClickListener(categoryClickListener);
        trafficCategory.setOnClickListener(categoryClickListener);
        shoppingCategory.setOnClickListener(categoryClickListener);
        groceryCategory.setOnClickListener(categoryClickListener);
        educationCategory.setOnClickListener(categoryClickListener);
        billsCategory.setOnClickListener(categoryClickListener);
        rentalsCategory.setOnClickListener(categoryClickListener);
        medicalCategory.setOnClickListener(categoryClickListener);
        investmentCategory.setOnClickListener(categoryClickListener);
        giftCategory.setOnClickListener(categoryClickListener);
        otherCategory.setOnClickListener(categoryClickListener);

        // Highlight the default category
        foodCategory.setSelected(true);
        foodCategory.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E0E0E0")));

        // Setup cancel button
        cancelButton.setOnClickListener(v -> dialog.dismiss());

        // Setup save button
        saveButton.setOnClickListener(v -> {
            String amountStr = amountInput.getText().toString().trim();
            String note = noteInput.getText().toString().trim();
            
            // Validate inputs
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
            
            // Get title from note or use category as title
            String title = note.isEmpty() ? selectedCategory[0] : note;
            
            // Save transaction
            saveTransaction(title, amount, selectedDate.getTime(), transactionType[0], selectedCategory[0]);
            
            // Dismiss dialog
            dialog.dismiss();
        });
    }

    private void updateDateTimeButtons(Button dateButton, Button timeButton) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM, dd yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        
        dateButton.setText(dateFormat.format(selectedDate.getTime()));
        timeButton.setText(timeFormat.format(selectedDate.getTime()));
    }

    private void resetCategorySelection(View... categories) {
        for (View category : categories) {
            category.setSelected(false);
            category.setBackgroundTintList(null);
        }
    }

    private void resetTransactionTypes(Button... buttons) {
        for (Button button : buttons) {
            button.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
            button.setTextColor(Color.parseColor("#757575"));
        }
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