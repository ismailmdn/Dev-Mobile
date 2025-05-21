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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TransactionsFragment extends Fragment implements TransactionAdapter.OnTransactionClickListener {

    private static final int TRANSACTION_DETAILS_REQUEST_CODE = 1002;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private RecyclerView recyclerView;
    private ProgressBar loadingProgress, loadingMoreProgress;
    private TextView emptyStateText;
    private TransactionAdapter adapter;
    private List<Transaction> transactions = new ArrayList<>();
    private FloatingActionButton fabAddTransaction;
    private MaterialButton btnFilterMonth, btnFilterType, btnFilterCategory;

    // Pagination variables
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private DocumentSnapshot lastVisible;
    private static final int PAGE_SIZE = 5;

    // Filter variables
    private String selectedMonth = null;
    private String selectedType = null;
    private String selectedCategory = null;
    private final SimpleDateFormat monthFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
    private final SimpleDateFormat firestoreMonthFormat = new SimpleDateFormat("yyyy-MM", Locale.getDefault());

    // Categories
    private final String[] categories = {
            "Other", "Gift", "Investment", "Medical", "Rentals",
            "Bills", "Education", "Grocery", "Shopping", "Traffic",
            "Social", "Food"
    };

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
        loadingMoreProgress = view.findViewById(R.id.loading_more_progress);
        emptyStateText = view.findViewById(R.id.empty_state_text);
        fabAddTransaction = view.findViewById(R.id.fab_add_transaction);
        btnFilterMonth = view.findViewById(R.id.btn_filter_month);
        btnFilterType = view.findViewById(R.id.btn_filter_type);
        btnFilterCategory = view.findViewById(R.id.btn_filter_category);

        // Setup RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        adapter = new TransactionAdapter(transactions, this);
        recyclerView.setAdapter(adapter);

        // Setup scroll listener for pagination
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                if (!isLoading && !isLastPage) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0
                            && totalItemCount >= PAGE_SIZE) {
                        loadMoreTransactions();
                    }
                }
            }
        });

        // Setup FAB
        fabAddTransaction.setOnClickListener(v -> showAddTransactionDialog());

        // Setup filter buttons
        btnFilterMonth.setOnClickListener(v -> showMonthFilterDialog());
        btnFilterType.setOnClickListener(v -> showTypeFilterDialog());
        btnFilterCategory.setOnClickListener(v -> showCategoryFilterDialog());

        // Check if user is logged in
        if (mAuth.getCurrentUser() == null) {
            showError("Please log in to view transactions");
            return;
        }

        // Load initial transactions
        resetPagination();
        loadInitialTransactions();
    }

    private void resetPagination() {
        transactions.clear();
        adapter.notifyDataSetChanged();
        lastVisible = null;
        isLastPage = false;
    }

    private void loadInitialTransactions() {
        if (isLoading) return;

        showLoading(true);
        isLoading = true;

        String userId = mAuth.getCurrentUser().getUid();
        Query query = buildBaseQuery(userId).limit(PAGE_SIZE);

        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        lastVisible = queryDocumentSnapshots.getDocuments().get(queryDocumentSnapshots.size() - 1);

                        transactions.clear();
                        for (DocumentSnapshot document : queryDocumentSnapshots) {
                            try {
                                Transaction transaction = document.toObject(Transaction.class);
                                if (transaction != null) {
                                    transaction.setId(document.getId());
                                    transactions.add(transaction);
                                }
                            } catch (Exception e) {
                                showError("Error parsing transaction: " + e.getMessage());
                            }
                        }

                        adapter.notifyDataSetChanged();
                        updateEmptyState();

                        // Check if this is the last page
                        if (queryDocumentSnapshots.size() < PAGE_SIZE) {
                            isLastPage = true;
                        }
                    } else {
                        isLastPage = true;
                        updateEmptyState();
                    }

                    isLoading = false;
                    showLoading(false);
                })
                .addOnFailureListener(e -> {
                    isLoading = false;
                    showLoading(false);
                    showError("Failed to load transactions: " + e.getMessage());
                });
    }

    private void loadMoreTransactions() {
        if (isLoading || isLastPage) return;

        loadingMoreProgress.setVisibility(View.VISIBLE);
        isLoading = true;

        String userId = mAuth.getCurrentUser().getUid();
        Query query = buildBaseQuery(userId).startAfter(lastVisible).limit(PAGE_SIZE);

        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        lastVisible = queryDocumentSnapshots.getDocuments().get(queryDocumentSnapshots.size() - 1);

                        int startPosition = transactions.size();
                        for (DocumentSnapshot document : queryDocumentSnapshots) {
                            try {
                                Transaction transaction = document.toObject(Transaction.class);
                                if (transaction != null) {
                                    transaction.setId(document.getId());
                                    transactions.add(transaction);
                                }
                            } catch (Exception e) {
                                showError("Error parsing transaction: " + e.getMessage());
                            }
                        }

                        adapter.notifyItemRangeInserted(startPosition, queryDocumentSnapshots.size());

                        // Check if this is the last page
                        if (queryDocumentSnapshots.size() < PAGE_SIZE) {
                            isLastPage = true;
                        }
                    } else {
                        isLastPage = true;
                    }

                    isLoading = false;
                    loadingMoreProgress.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    isLoading = false;
                    loadingMoreProgress.setVisibility(View.GONE);
                    showError("Failed to load more transactions: " + e.getMessage());
                });
    }

    private Query buildBaseQuery(String userId) {
        Query query = db.collection("transaction")
                .document(userId)
                .collection("transactions")
                .orderBy("date", Query.Direction.DESCENDING);

        // Apply month filter if selected
        if (selectedMonth != null) {
            Calendar cal = Calendar.getInstance();
            try {
                Date date = firestoreMonthFormat.parse(selectedMonth);
                if (date != null) {
                    cal.setTime(date);
                    cal.set(Calendar.DAY_OF_MONTH, 1);
                    Date startDate = cal.getTime();

                    cal.add(Calendar.MONTH, 1);
                    Date endDate = cal.getTime();

                    query = query.whereGreaterThanOrEqualTo("date", startDate)
                            .whereLessThan("date", endDate);
                }
            } catch (Exception e) {
                showError("Error applying month filter: " + e.getMessage());
            }
        }

        // Apply type filter if selected
        if (selectedType != null) {
            query = query.whereEqualTo("type", selectedType.toLowerCase());
        }

        // Apply category filter if selected
        if (selectedCategory != null) {
            query = query.whereEqualTo("category", selectedCategory);
        }

        return query;
    }

    private void showMonthFilterDialog() {
        // Get unique months from all transactions (we need to query without filters)
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();

        db.collection("transaction")
                .document(userId)
                .collection("transactions")
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Map<String, String> monthsMap = new HashMap<>();
                    Calendar cal = Calendar.getInstance();

                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Date date = document.getDate("date");
                            if (date != null) {
                                String monthKey = firestoreMonthFormat.format(date);
                                String monthDisplay = monthFormat.format(date);
                                monthsMap.put(monthKey, monthDisplay);
                            }
                        } catch (Exception e) {
                            // Ignore documents with invalid dates
                        }
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
                        resetPagination();
                        loadInitialTransactions();
                        updateFilterButtonsText();
                        dialog.dismiss();
                    });

                    builder.setNegativeButton("Cancel", null);
                    builder.show();
                })
                .addOnFailureListener(e -> {
                    showError("Failed to load months for filtering: " + e.getMessage());
                });
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
            resetPagination();
            loadInitialTransactions();
            updateFilterButtonsText();
            dialog.dismiss();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showCategoryFilterDialog() {
        String[] allCategories = new String[categories.length + 1];
        allCategories[0] = "All Categories";
        System.arraycopy(categories, 0, allCategories, 1, categories.length);

        int checkedItem = 0; // "All Categories" is selected by default
        if (selectedCategory != null) {
            for (int i = 0; i < categories.length; i++) {
                if (categories[i].equalsIgnoreCase(selectedCategory)) {
                    checkedItem = i + 1;
                    break;
                }
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Filter by Category");
        builder.setSingleChoiceItems(allCategories, checkedItem, (dialog, which) -> {
            if (which == 0) {
                selectedCategory = null;
            } else {
                selectedCategory = allCategories[which];
            }
            resetPagination();
            loadInitialTransactions();
            updateFilterButtonsText();
            dialog.dismiss();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
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

        // Update category filter button text
        btnFilterCategory.setText(selectedCategory == null ? "All Categories" : selectedCategory);
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
        if (transactions.isEmpty()) {
            if (selectedMonth != null || selectedType != null || selectedCategory != null) {
                emptyStateText.setText("No matching transactions found");
            } else {
                emptyStateText.setText("No transactions yet");
            }
        }
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
                    // Reload data to maintain pagination and filters
                    resetPagination();
                    loadInitialTransactions();
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
            resetPagination();
            loadInitialTransactions();
        }
    }
}