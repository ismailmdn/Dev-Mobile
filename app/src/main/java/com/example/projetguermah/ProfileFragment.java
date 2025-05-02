package com.example.projetguermah;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import android.content.Intent;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    // UI Elements
    private TextView budgetTitle, currentMonthYearText, budgetValue, expensesValue, savingsValue, incomeValue;
    private LinearProgressIndicator budgetProgressBar;
    private Button selectMonthButton, confirmMonthButton, cancelMonthButton, logoutButton;
    private Spinner monthSpinner, yearSpinner;
    private RelativeLayout monthPickerContainer;
    private LinearLayout monthPickerLayout, menuPopup;
    private RelativeLayout editBudgetPopup, editSavingsPopup;
    private EditText editBudgetInput, editSavingsInput;
    private Button saveBudgetButton, cancelBudgetButton, saveSavingsButton, cancelSavingsButton;
    private View editBudgetIcon, editSavingsIcon;

    private ImageView editMenuIcon;

    private RelativeLayout profileFragment;

    // Data
    private String uid;
    private String currentMonthYear;
    private int selectedYear;
    private int selectedMonth;
    private double budget = 0;
    private double expenses = 0;
    private double savings = 0;
    private double income = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize UI elements
        profileFragment = view.findViewById(R.id.profileFragment);

        budgetTitle = view.findViewById(R.id.budgetTitle);
        currentMonthYearText = view.findViewById(R.id.currentMonthYearText);
        budgetValue = view.findViewById(R.id.budgetValue);
        expensesValue = view.findViewById(R.id.expensesValue);
        savingsValue = view.findViewById(R.id.savingsValue);
        incomeValue = view.findViewById(R.id.incomeValue);
        budgetProgressBar = view.findViewById(R.id.budgetProgressBar);
        selectMonthButton = view.findViewById(R.id.selectMonthButton);
        monthPickerContainer = view.findViewById(R.id.monthPickerContainer);
        monthPickerLayout = view.findViewById(R.id.monthPickerLayout);
        monthSpinner = view.findViewById(R.id.monthSpinner);
        yearSpinner = view.findViewById(R.id.yearSpinner);
        confirmMonthButton = view.findViewById(R.id.confirmMonthButton);
        cancelMonthButton = view.findViewById(R.id.cancelMonthButton);
        editBudgetPopup = view.findViewById(R.id.editBudgetPopup);
        editSavingsPopup = view.findViewById(R.id.editSavingsPopup);
        editBudgetInput = view.findViewById(R.id.editBudgetInput);
        editSavingsInput = view.findViewById(R.id.editSavingsInput);
        saveBudgetButton = view.findViewById(R.id.saveBudgetButton);
        cancelBudgetButton = view.findViewById(R.id.cancelBudgetButton);
        saveSavingsButton = view.findViewById(R.id.saveSavingsButton);
        cancelSavingsButton = view.findViewById(R.id.cancelSavingsButton);
        editBudgetIcon = view.findViewById(R.id.editBudgetIcon);
        editSavingsIcon = view.findViewById(R.id.editSavingsIcon);
        menuPopup = view.findViewById(R.id.menuPopup);
        logoutButton = view.findViewById(R.id.logoutButton);
        editMenuIcon = view.findViewById(R.id.menuIcon);

        // Set current month and year
        Calendar calendar = Calendar.getInstance();
        selectedYear = calendar.get(Calendar.YEAR);
        selectedMonth = calendar.get(Calendar.MONTH);
        currentMonthYear = new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.getTime());
        currentMonthYearText.setText(currentMonthYear);

        // Initialize spinners
        setupMonthSpinner();
        setupYearSpinner();

        // Check authentication and load data
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            uid = user.getUid();
            loadUserData();
            loadFinanceData();
        }

        logoutButton.setOnClickListener(v -> {
            SharedPreferences prefs = requireActivity().getSharedPreferences("MyAppPrefs", 0);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("isLoggedIn", false);
            editor.apply();

            mAuth.signOut();
            Intent intent = new Intent(requireActivity(), AuthActivity.class);
            startActivity(intent);
            requireActivity().finish();
        });


        // Set up click listeners
        selectMonthButton.setOnClickListener(v -> showMonthPicker());
        confirmMonthButton.setOnClickListener(v -> confirmMonthSelection());
        cancelMonthButton.setOnClickListener(v -> hideMonthPicker());
        editBudgetIcon.setOnClickListener(v -> showEditBudgetDialog());
        editSavingsIcon.setOnClickListener(v -> showEditSavingsDialog());
        saveBudgetButton.setOnClickListener(v -> saveBudget());
        cancelBudgetButton.setOnClickListener(v -> editBudgetPopup.setVisibility(View.GONE));
        saveSavingsButton.setOnClickListener(v -> saveSavings());
        cancelSavingsButton.setOnClickListener(v -> editSavingsPopup.setVisibility(View.GONE));

        // Handle clicks outside popups
        monthPickerContainer.setOnClickListener(v -> hideMonthPicker());
        editBudgetPopup.setOnClickListener(v -> editBudgetPopup.setVisibility(View.GONE));
        editSavingsPopup.setOnClickListener(v -> editSavingsPopup.setVisibility(View.GONE));

        editMenuIcon.setOnClickListener(v -> {
            if (menuPopup.getVisibility() == View.VISIBLE) {
                menuPopup.setVisibility(View.GONE);
            } else {
                menuPopup.setVisibility(View.VISIBLE);
                editBudgetIcon.setVisibility(View.GONE);
            }
        });

        profileFragment.setOnClickListener(v -> {
            if (menuPopup.getVisibility() == View.VISIBLE) {
                menuPopup.setVisibility(View.GONE);
            }
        });

        return view;
    }

    private void setupMonthSpinner() {
        ArrayAdapter<CharSequence> monthAdapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.months_array,
                android.R.layout.simple_spinner_item
        );
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        monthSpinner.setAdapter(monthAdapter);
        monthSpinner.setSelection(selectedMonth);
    }

    private void setupYearSpinner() {
        List<String> years = new ArrayList<>();
        int thisYear = Calendar.getInstance().get(Calendar.YEAR);
        for (int i = thisYear - 5; i <= thisYear + 5; i++) {
            years.add(String.valueOf(i));
        }

        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                years
        );
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        yearSpinner.setAdapter(yearAdapter);

        // Set current year as selected
        int currentYearPosition = years.indexOf(String.valueOf(selectedYear));
        if (currentYearPosition >= 0) {
            yearSpinner.setSelection(currentYearPosition);
        }
    }

    private void showMonthPicker() {
        monthPickerContainer.setVisibility(View.VISIBLE);
        monthPickerContainer.setAlpha(0f);
        monthPickerContainer.animate()
                .alpha(1f)
                .setDuration(200)
                .start();
    }

    private void hideMonthPicker() {
        monthPickerContainer.animate()
                .alpha(0f)
                .setDuration(200)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        monthPickerContainer.setVisibility(View.GONE);
                    }
                })
                .start();
    }

    private void confirmMonthSelection() {
        String month = monthSpinner.getSelectedItem().toString();
        String year = yearSpinner.getSelectedItem().toString();
        currentMonthYear = month + " " + year;
        currentMonthYearText.setText(currentMonthYear);

        // Update selected month and year
        selectedMonth = monthSpinner.getSelectedItemPosition();
        selectedYear = Integer.parseInt(year);

        hideMonthPicker();
        loadFinanceData();
    }

    private void showEditBudgetDialog() {
        editBudgetInput.setText(String.format(Locale.getDefault(), "%.2f", budget));
        editBudgetPopup.setVisibility(View.VISIBLE);
    }

    private void showEditSavingsDialog() {
        editSavingsInput.setText(String.format(Locale.getDefault(), "%.2f", savings));
        editSavingsPopup.setVisibility(View.VISIBLE);
    }

    private void loadUserData() {
        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        if (name != null && !name.isEmpty()) {
                            budgetTitle.setText(name + "'s Budget");
                        }
                    }
                });
    }

    private void loadFinanceData() {
        String documentId = String.format(Locale.getDefault(), "%04d-%02d", selectedYear, selectedMonth + 1);

        // First load existing finance data
        db.collection("finance")
                .document(uid)
                .collection("months")
                .document(documentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // Document exists, load data
                        Number budgetNumber = documentSnapshot.getDouble("budget");
                        Number savingsNumber = documentSnapshot.getDouble("savings");

                        budget = budgetNumber != null ? budgetNumber.doubleValue() : 0;
                        savings = savingsNumber != null ? savingsNumber.doubleValue() : 0;

                        // Now calculate expenses and income from transactions
                        calculateTransactions();
                    } else {
                        // Document doesn't exist, create with default values
                        budget = 0;
                        savings = 0;

                        // Calculate expenses and income from transactions
                        calculateTransactions();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Failed to load finance data", Toast.LENGTH_SHORT).show();
                });
    }

    private void calculateTransactions() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = user.getUid();

        // Calculate date range for the selected month
        Calendar calendar = Calendar.getInstance();
        calendar.set(selectedYear, selectedMonth, 1, 0, 0, 0);
        Date startOfMonth = calendar.getTime();
        calendar.set(selectedYear, selectedMonth, calendar.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59);
        Date endOfMonth = calendar.getTime();

        // Reset values
        expenses = 0;
        income = 0;

        // Load all transactions (using your working method)
        db.collection("transaction")
                .document(uid)
                .collection("transactions")
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Transaction> allTransactions = new ArrayList<>();

                    // Convert all documents to Transaction objects
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Transaction transaction = document.toObject(Transaction.class);
                            transaction.setId(document.getId());
                            allTransactions.add(transaction);
                        } catch (Exception e) {
                            Log.e("TransactionCalc", "Error parsing transaction", e);
                        }
                    }

                    // Now filter and calculate
                    for (Transaction transaction : allTransactions) {
                        Date transactionDate = transaction.getDate();

                        // Check if transaction is within our month
                        if (transactionDate != null &&
                                !transactionDate.before(startOfMonth) &&
                                !transactionDate.after(endOfMonth)) {

                            if ("income".equals(transaction.getType())) {
                                income += transaction.getAmount();
                            } else if ("expense".equals(transaction.getType())) {
                                expenses += transaction.getAmount();
                            }
                        }
                    }


                    updateFinanceDocument();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(),
                            "Failed to load transactions: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    Log.e("TransactionCalc", "Error loading transactions", e);
                });
    }



    private void updateFinanceDocument() {
        String documentId = String.format(Locale.getDefault(), "%04d-%02d", selectedYear, selectedMonth + 1);

        Map<String, Object> financeData = new HashMap<>();
        financeData.put("budget", budget);
        financeData.put("expenses", expenses);
        financeData.put("income", income);
        financeData.put("savings", savings);
        financeData.put("monthYear", currentMonthYear);

        db.collection("finance")
                .document(uid)
                .collection("months")
                .document(documentId)
                .set(financeData)
                .addOnSuccessListener(aVoid -> updateUI())
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Failed to update finance data", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateUI() {
        // Update text views
        budgetValue.setText(String.format(Locale.getDefault(), "$%.2f", budget));
        expensesValue.setText(String.format(Locale.getDefault(), "$%.2f", expenses));
        savingsValue.setText(String.format(Locale.getDefault(), "$%.2f", savings));
        incomeValue.setText(String.format(Locale.getDefault(), "$%.2f", income));

        // Update progress bar
        if (budget > 0) {
            int progress = (int) ((expenses / budget) * 100);
            budgetProgressBar.setProgress(Math.min(progress, 100));
        } else {
            budgetProgressBar.setProgress(0);
        }
    }

    private void saveBudget() {
        String input = editBudgetInput.getText().toString().trim();
        if (input.isEmpty()) {
            editBudgetInput.setError("Please enter a budget");
            return;
        }

        try {
            double newBudget = Double.parseDouble(input);
            budget = newBudget;
            updateFinanceDocument();
            editBudgetPopup.setVisibility(View.GONE);
            Toast.makeText(requireContext(), "Budget updated", Toast.LENGTH_SHORT).show();
        } catch (NumberFormatException e) {
            editBudgetInput.setError("Invalid number format");
        }
    }

    private void saveSavings() {
        String input = editSavingsInput.getText().toString().trim();
        if (input.isEmpty()) {
            editSavingsInput.setError("Please enter savings amount");
            return;
        }

        try {
            double newSavings = Double.parseDouble(input);
            savings = newSavings;
            updateFinanceDocument();
            editSavingsPopup.setVisibility(View.GONE);
            Toast.makeText(requireContext(), "Savings updated", Toast.LENGTH_SHORT).show();
        } catch (NumberFormatException e) {
            editSavingsInput.setError("Invalid number format");
        }
    }
}