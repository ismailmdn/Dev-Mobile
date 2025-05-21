package com.example.projetguermah;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.example.projetguermah.model.SavingsGoal;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
    private Button selectMonthButton, confirmMonthButton, cancelMonthButton;
    private Spinner monthSpinner, yearSpinner;
    private RelativeLayout monthPickerContainer;
    private LinearLayout monthPickerLayout, menuPopup, logoutButton;
    private RelativeLayout editBudgetPopup, editSavingsPopup;
    private EditText editBudgetInput, editSavingsInput;
    private Button saveBudgetButton, cancelBudgetButton, saveSavingsButton, cancelSavingsButton;
    private View editBudgetIcon, editSavingsIcon;
    private ImageView editMenuIcon;
    private RelativeLayout profileFragment;
    private Button generateReportButton;

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
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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
        logoutButton = view.findViewById(R.id.logout);
        editMenuIcon = view.findViewById(R.id.menuIcon);
        generateReportButton = view.findViewById(R.id.generateReportButton);

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
        generateReportButton.setOnClickListener(v -> generateAndDownloadPdf());

        // Menu button click
        editMenuIcon.setOnClickListener(v -> {
            if (menuPopup.getVisibility() == View.VISIBLE) {
                menuPopup.setVisibility(View.GONE);
            } else {
                menuPopup.setVisibility(View.VISIBLE);
            }
        });

        // Logout button
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

        // Handle clicks outside popups
        monthPickerContainer.setOnClickListener(v -> hideMonthPicker());
        editBudgetPopup.setOnClickListener(v -> editBudgetPopup.setVisibility(View.GONE));
        editSavingsPopup.setOnClickListener(v -> editSavingsPopup.setVisibility(View.GONE));
        profileFragment.setOnClickListener(v -> {
            if (menuPopup.getVisibility() == View.VISIBLE) {
                menuPopup.setVisibility(View.GONE);
            }
        });
    }

    private void generateAndDownloadPdf() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) return;

        // Create a temporary file
        String fileName = "Financial_Report_" + currentMonthYear.replace(" ", "_") + ".pdf";
        File file = new File(requireContext().getExternalCacheDir(), fileName);

        // Step 1: Get user info
        db.collection("users").document(userId).get().addOnSuccessListener(userDoc -> {
            String name = userDoc.getString("name");
            String email = userDoc.getString("email");



            // Step 2: Get transactions
            db.collection("transaction").document(userId).collection("transactions")
                    .get().addOnSuccessListener(transactionDocs -> {
                        Map<String, Double> expenseMap = new HashMap<>();
                        Map<String, Double> incomeMap = new HashMap<>();
                        // Use final arrays to hold the values that need to be modified
                        final double[] totalExpensesHolder = {0.0};
                        final double[] totalIncomeHolder = {0.0};

                        for (QueryDocumentSnapshot doc : transactionDocs) {
                            String type = doc.getString("type");
                            String category = doc.getString("category");
                            Double amount = doc.getDouble("amount");
                            Date date = doc.getDate("date");

                            if (amount == null || date == null) continue;

                            String txMonth = new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(date);
                            if (!txMonth.equals(currentMonthYear)) continue;

                            String key = category != null ? category : "Uncategorized";

                            if ("expense".equals(type)) {
                                totalExpensesHolder[0] += amount;
                                expenseMap.put(key, expenseMap.getOrDefault(key, 0.0) + amount);
                            } else if ("income".equals(type)) {
                                totalIncomeHolder[0] += amount;
                                incomeMap.put(key, incomeMap.getOrDefault(key, 0.0) + amount);
                            }
                        }

                        // Step 3: Get savings goals
                        db.collection("finance").document(userId).collection("savingsGoals")
                                .get().addOnSuccessListener(goalDocs -> {
                                    List<SavingsGoal> goals = new ArrayList<>();
                                    for (QueryDocumentSnapshot goalDoc : goalDocs) {
                                        SavingsGoal goal = goalDoc.toObject(SavingsGoal.class);
                                        goals.add(goal);
                                    }

                                    // Step 4: Generate PDF
                                    PdfDocument pdf = new PdfDocument();
                                    Paint paint = new Paint();
                                    final int x = 40;
                                    final int[] yHolder = {50};
                                    PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
                                    PdfDocument.Page page = pdf.startPage(pageInfo);
                                    Canvas canvas = page.getCanvas();

                                    paint.setTextSize(18);
                                    canvas.drawText("Financial Report - " + currentMonthYear, x, yHolder[0], paint);
                                    yHolder[0] += 30;
                                    paint.setTextSize(14);
                                    canvas.drawText("Name: " + name, x, yHolder[0], paint);
                                    yHolder[0] += 20;
                                    canvas.drawText("Email: " + email, x, yHolder[0], paint);
                                    yHolder[0] += 30;

                                    canvas.drawText("Summary", x, yHolder[0], paint);
                                    yHolder[0] += 20;
                                    canvas.drawText("Budget: $" + String.format("%.2f", budget), x, yHolder[0], paint);
                                    yHolder[0] += 20;
                                    canvas.drawText("Total Savings: $" + String.format("%.2f", savings), x, yHolder[0], paint);
                                    yHolder[0] += 20;
                                    canvas.drawText("Total Income: $" + String.format("%.2f", totalIncomeHolder[0]), x, yHolder[0], paint);
                                    yHolder[0] += 20;
                                    canvas.drawText("Total Expenses: $" + String.format("%.2f", totalExpensesHolder[0]), x, yHolder[0], paint);
                                    yHolder[0] += 20;
                                    canvas.drawText("Balance: $" + String.format("%.2f", totalIncomeHolder[0] - totalExpensesHolder[0]), x, yHolder[0], paint);
                                    yHolder[0] += 30;

                                    canvas.drawText("Expenses by Category", x, yHolder[0], paint);
                                    yHolder[0] += 20;
                                    for (Map.Entry<String, Double> entry : expenseMap.entrySet()) {
                                        canvas.drawText(entry.getKey() + ": $" + String.format("%.2f", entry.getValue()), x + 20, yHolder[0], paint);
                                        yHolder[0] += 20;
                                    }
                                    yHolder[0] += 10;

                                    canvas.drawText("Income by Category", x, yHolder[0], paint);
                                    yHolder[0] += 20;
                                    for (Map.Entry<String, Double> entry : incomeMap.entrySet()) {
                                        canvas.drawText(entry.getKey() + ": $" + String.format("%.2f", entry.getValue()), x + 20, yHolder[0], paint);
                                        yHolder[0] += 20;
                                    }
                                    yHolder[0] += 30;

                                    canvas.drawText("Savings Goals", x, yHolder[0], paint);
                                    yHolder[0] += 20;
                                    for (SavingsGoal goal : goals) {
                                        double progress = goal.getCurrentAmount() / goal.getTargetAmount() * 100;
                                        canvas.drawText(goal.getName() + " - $" + goal.getCurrentAmount() + " / $" + goal.getTargetAmount() +
                                                " (" + String.format("%.1f", progress) + "%)", x + 20, yHolder[0], paint);
                                        yHolder[0] += 20;
                                    }

                                    pdf.finishPage(page);

                                    // Step 5: Save to file
                                    try {
                                        FileOutputStream fos = new FileOutputStream(file);
                                        pdf.writeTo(fos);
                                        fos.close();
                                        pdf.close();

                                        // Share the PDF file
                                        sharePdfFile(file);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        Toast.makeText(requireContext(), "Error saving PDF", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    });
        });
    }

    private void sharePdfFile(File file) {
        Uri contentUri = FileProvider.getUriForFile(
                requireContext(),
                requireContext().getPackageName() + ".provider",
                file
        );

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("application/pdf");
        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        // Create a chooser intent
        Intent chooser = Intent.createChooser(shareIntent, "Save PDF Report");
        try {
            startActivity(chooser);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "No app available to handle PDF", Toast.LENGTH_SHORT).show();
        }
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
        int thisYear = Calendar.getInstance().get(Calendar.YEAR);
        String[] years = new String[11];
        for (int i = 0; i < 11; i++) {
            years[i] = String.valueOf(thisYear - 5 + i);
        }

        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                years
        );
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        yearSpinner.setAdapter(yearAdapter);

        // Set current year as selected
        yearSpinner.setSelection(5);
    }

    private void showMonthPicker() {
        monthPickerContainer.setVisibility(View.VISIBLE);
    }

    private void hideMonthPicker() {
        monthPickerContainer.setVisibility(View.GONE);
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

        db.collection("finance")
                .document(uid)
                .collection("months")
                .document(documentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Number budgetNumber = documentSnapshot.getDouble("budget");
                        Number savingsNumber = documentSnapshot.getDouble("savings");

                        budget = budgetNumber != null ? budgetNumber.doubleValue() : 0;
                        savings = savingsNumber != null ? savingsNumber.doubleValue() : 0;
                    } else {
                        budget = 0;
                        savings = 0;
                    }
                    calculateTransactions();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Failed to load finance data", Toast.LENGTH_SHORT).show();
                });
    }



    private void calculateTransactions() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        // Calculate date range for the selected month
        Calendar calendar = Calendar.getInstance();
        calendar.set(selectedYear, selectedMonth, 1, 0, 0, 0);
        Date startOfMonth = calendar.getTime();
        calendar.set(selectedYear, selectedMonth, calendar.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59);
        Date endOfMonth = calendar.getTime();

        // Reset values
        expenses = 0;
        income = 0;

        db.collection("transaction")
                .document(uid)
                .collection("transactions")
                .orderBy("date", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Transaction transaction = document.toObject(Transaction.class);
                            Date transactionDate = transaction.getDate();

                            if (transactionDate != null &&
                                    !transactionDate.before(startOfMonth) &&
                                    !transactionDate.after(endOfMonth)) {

                                if ("income".equals(transaction.getType())) {
                                    income += transaction.getAmount();
                                } else if ("expense".equals(transaction.getType())) {
                                    expenses += transaction.getAmount();
                                }
                            }
                        } catch (Exception e) {
                            Log.e("TransactionCalc", "Error parsing transaction", e);
                        }
                    }
                    updateFinanceDocument();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "Failed to load transactions", Toast.LENGTH_SHORT).show();
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
        budgetValue.setText(String.format(Locale.getDefault(), "$%.2f", budget));
        expensesValue.setText(String.format(Locale.getDefault(), "$%.2f", expenses));
        savingsValue.setText(String.format(Locale.getDefault(), "$%.2f", savings));
        incomeValue.setText(String.format(Locale.getDefault(), "$%.2f", income));

        if (budget > 0) {
            int progress = (int) ((expenses / budget) * 100);
            budgetProgressBar.setProgress(Math.min(progress, 100));
        } else {
            budgetProgressBar.setProgress(0);
        }
    }

    private void saveBudget() {
        try {
            budget = Double.parseDouble(editBudgetInput.getText().toString());
            updateFinanceDocument();
            editBudgetPopup.setVisibility(View.GONE);
            Toast.makeText(requireContext(), "Budget updated", Toast.LENGTH_SHORT).show();
        } catch (NumberFormatException e) {
            editBudgetInput.setError("Invalid number");
        }
    }

    private void saveSavings() {
        try {
            savings = Double.parseDouble(editSavingsInput.getText().toString());
            updateFinanceDocument();
            editSavingsPopup.setVisibility(View.GONE);
            Toast.makeText(requireContext(), "Savings updated", Toast.LENGTH_SHORT).show();
        } catch (NumberFormatException e) {
            editSavingsInput.setError("Invalid number");
        }
    }
}