package com.example.projetguermah;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

// sync
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import android.widget.Toast;


import org.json.JSONArray;
import org.json.JSONObject;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class HomeFragment extends Fragment {
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String userId;

    // Charts
    private LineChart lineChart;
    private PieChart pieChart;

    // Savings
    private LinearProgressIndicator savingsProgressBar;
    private TextView currentSavingsText, savingsGoalText, savingsProgressText;
    private ImageView savingsTrendIcon;

    // Budget comparison
    private LinearLayout budgetComparisonContainer;

    // Filters
    private Spinner timeRangeSpinner;
    private Spinner pieChartRangeSpinner;
    private Spinner savingsRangeSpinner;
    private Spinner budgetRangeSpinner;
    private SwitchMaterial typeSwitch;

    // Data
    private Map<String, Map<String, Double>> monthlyCategoryExpenses = new HashMap<>();
    private Map<String, Map<String, Double>> monthlyCategoryIncome = new HashMap<>();
    private Map<String, Double> monthlyIncome = new HashMap<>();
    private Map<String, Double> monthlyExpenses = new HashMap<>();
    private Map<String, Double> monthlySavings = new HashMap<>();
    private Map<String, Double> monthlyBudgets = new HashMap<>();
    private List<String> availableMonths = new ArrayList<>();

    private static final int PICK_JSON_FILE = 1001;
    private Button btnSyncBank;
    private TextView tvSyncStatus;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            userId = user.getUid();
        }

        // Initialize views
        initializeViews(view);
        setupTimeRangeSpinner();
        setupCharts();

        // Load data
        loadData();
        btnSyncBank.setOnClickListener(v -> simulateBankSync());


        return view;
    }

    private void initializeViews(View view) {
        lineChart = view.findViewById(R.id.lineChart);
        pieChart = view.findViewById(R.id.pieChart);
        timeRangeSpinner = view.findViewById(R.id.timeRangeSpinner);
        pieChartRangeSpinner = view.findViewById(R.id.pieChartRangeSpinner);
        savingsRangeSpinner = view.findViewById(R.id.savingsRangeSpinner);
        budgetRangeSpinner = view.findViewById(R.id.budgetRangeSpinner);
        typeSwitch = view.findViewById(R.id.typeSwitch);
        savingsProgressBar = view.findViewById(R.id.savingsProgressBar);
        currentSavingsText = view.findViewById(R.id.currentSavingsText);
        savingsGoalText = view.findViewById(R.id.savingsGoalText);
        savingsProgressText = view.findViewById(R.id.savingsProgressText);
        savingsTrendIcon = view.findViewById(R.id.savingsTrendIcon);
        budgetComparisonContainer = view.findViewById(R.id.budgetComparisonContainer);
        btnSyncBank = view.findViewById(R.id.btnSyncBank);
        tvSyncStatus = view.findViewById(R.id.tvSyncStatus);
    }

    private void setupTimeRangeSpinner() {
        String[] timeRanges = new String[]{"Last 3 months", "Last 6 months", "Last year"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                timeRanges
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        timeRangeSpinner.setAdapter(adapter);

        timeRangeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateLineChart();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupMonthSpinners() {
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                availableMonths
        );
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        pieChartRangeSpinner.setAdapter(monthAdapter);
        savingsRangeSpinner.setAdapter(monthAdapter);
        budgetRangeSpinner.setAdapter(monthAdapter);

        // Set default to current month
        String currentMonth = new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(new Date());
        int currentPosition = availableMonths.indexOf(currentMonth);
        if (currentPosition >= 0) {
            pieChartRangeSpinner.setSelection(currentPosition);
            savingsRangeSpinner.setSelection(currentPosition);
            budgetRangeSpinner.setSelection(currentPosition);
        }

        pieChartRangeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updatePieChart();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        savingsRangeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateSavingsTracker();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        budgetRangeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateBudgetComparison();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        typeSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                typeSwitch.setText(isChecked ? "Income" : "Expenses");
                updatePieChart();
            }
        });
    }

    private void setupCharts() {
        // Line Chart setup
        lineChart.getDescription().setEnabled(false);
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setPinchZoom(true);
        lineChart.setDrawGridBackground(false);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);

        // Pie Chart setup
        pieChart.getDescription().setEnabled(false);
        pieChart.setUsePercentValues(true);
        pieChart.setExtraOffsets(20, 10, 20, 10); // Increased offsets
        pieChart.setDragDecelerationFrictionCoef(0.95f);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleColor(Color.WHITE);
        pieChart.setTransparentCircleColor(Color.WHITE);
        pieChart.setTransparentCircleAlpha(110);
        pieChart.setHoleRadius(35f); // Reduced hole size
        pieChart.setTransparentCircleRadius(40f);
        pieChart.setDrawCenterText(true);
        pieChart.setRotationAngle(0);
        pieChart.setRotationEnabled(true);
        pieChart.setHighlightPerTapEnabled(true);

        // Improve legend
        Legend l = pieChart.getLegend();
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        l.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        l.setDrawInside(false);
        l.setXEntrySpace(7f);
        l.setYEntrySpace(0f);
        l.setYOffset(5f);
        l.setTextSize(12f);
    }

    private void loadData() {
        loadTransactions();
        loadMonthlyFinanceData();
    }

    private void loadTransactions() {
        if (userId == null) return;

        db.collection("transaction")
                .document(userId)
                .collection("transactions")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    monthlyCategoryExpenses.clear();
                    monthlyCategoryIncome.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            String type = document.getString("type");
                            String category = document.getString("category");
                            Double amount = document.getDouble("amount");
                            Date date = document.getDate("date");

                            if (amount == null || date == null) continue;

                            String monthYear = new SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(date);
                            String categoryKey = category != null ? category : "Uncategorized";

                            // Initialize month maps if not exists
                            if (!monthlyCategoryExpenses.containsKey(monthYear)) {
                                monthlyCategoryExpenses.put(monthYear, new HashMap<>());
                            }
                            if (!monthlyCategoryIncome.containsKey(monthYear)) {
                                monthlyCategoryIncome.put(monthYear, new HashMap<>());
                            }

                            if ("expense".equals(type)) {
                                double current = monthlyCategoryExpenses.get(monthYear).getOrDefault(categoryKey, 0.0);
                                monthlyCategoryExpenses.get(monthYear).put(categoryKey, current + amount);
                            } else if ("income".equals(type)) {
                                double current = monthlyCategoryIncome.get(monthYear).getOrDefault(categoryKey, 0.0);
                                monthlyCategoryIncome.get(monthYear).put(categoryKey, current + amount);
                            }
                        } catch (Exception e) {
                            Log.e("Dashboard", "Error parsing transaction", e);
                        }
                    }
                    updatePieChart();
                })
                .addOnFailureListener(e -> {
                    Log.e("Dashboard", "Error loading transactions", e);
                });
    }

    private void loadMonthlyFinanceData() {
        if (userId == null) return;

        db.collection("finance")
                .document(userId)
                .collection("months")
                .orderBy("monthYear", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    monthlyIncome.clear();
                    monthlyExpenses.clear();
                    monthlySavings.clear();
                    monthlyBudgets.clear();
                    availableMonths.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            String monthYear = document.getString("monthYear");
                            Double income = document.getDouble("income");
                            Double expenses = document.getDouble("expenses");
                            Double savings = document.getDouble("savings");
                            Double budget = document.getDouble("budget");

                            if (monthYear == null) continue;

                            availableMonths.add(monthYear);

                            // Parse monthYear to get year-month key (e.g., "March 2025" -> "2025-03")
                            SimpleDateFormat inputFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
                            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
                            Date date = inputFormat.parse(monthYear);
                            String monthKey = outputFormat.format(date);

                            if (income != null) monthlyIncome.put(monthKey, income);
                            if (expenses != null) monthlyExpenses.put(monthKey, expenses);
                            if (savings != null) monthlySavings.put(monthKey, savings);
                            if (budget != null) monthlyBudgets.put(monthKey, budget);
                        } catch (Exception e) {
                            Log.e("Dashboard", "Error parsing finance data", e);
                        }
                    }
                    setupMonthSpinners();
                    updateLineChart();
                    updateSavingsTracker();
                    updateBudgetComparison();
                })
                .addOnFailureListener(e -> {
                    Log.e("Dashboard", "Error loading finance data", e);
                });
    }

    private void updateLineChart() {
        // Get selected time range
        String range = timeRangeSpinner.getSelectedItem().toString();
        int months = getMonthsFromRange(range);

        // Prepare data for the selected range
        List<Entry> incomeEntries = new ArrayList<>();
        List<Entry> expenseEntries = new ArrayList<>();
        List<Entry> savingsEntries = new ArrayList<>();
        List<String> monthsList = new ArrayList<>();

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat monthFormat = new SimpleDateFormat("MMM yyyy", Locale.getDefault());
        SimpleDateFormat monthKeyFormat = new SimpleDateFormat("yyyy-MM", Locale.getDefault());

        for (int i = months-1; i >= 0; i--) {
            calendar.add(Calendar.MONTH, -1);
            String monthKey = monthKeyFormat.format(calendar.getTime());

            monthsList.add(monthFormat.format(calendar.getTime()));

            // Add data points (x = index, y = value)
            incomeEntries.add(new Entry(months-1-i, monthlyIncome.getOrDefault(monthKey, 0.0).floatValue()));
            expenseEntries.add(new Entry(months-1-i, monthlyExpenses.getOrDefault(monthKey, 0.0).floatValue()));
            savingsEntries.add(new Entry(months-1-i, monthlySavings.getOrDefault(monthKey, 0.0).floatValue()));
        }

        // Create datasets
        LineDataSet incomeDataSet = new LineDataSet(incomeEntries, "Income");
        incomeDataSet.setColor(Color.parseColor("#38A169"));
        incomeDataSet.setCircleColor(Color.parseColor("#38A169"));
        incomeDataSet.setLineWidth(2f);
        incomeDataSet.setCircleRadius(4f);
        incomeDataSet.setValueTextSize(10f);

        LineDataSet expenseDataSet = new LineDataSet(expenseEntries, "Expenses");
        expenseDataSet.setColor(Color.parseColor("#E53E3E"));
        expenseDataSet.setCircleColor(Color.parseColor("#E53E3E"));
        expenseDataSet.setLineWidth(2f);
        expenseDataSet.setCircleRadius(4f);
        expenseDataSet.setValueTextSize(10f);

        LineDataSet savingsDataSet = new LineDataSet(savingsEntries, "Savings");
        savingsDataSet.setColor(Color.parseColor("#4299E1"));
        savingsDataSet.setCircleColor(Color.parseColor("#4299E1"));
        savingsDataSet.setLineWidth(2f);
        savingsDataSet.setCircleRadius(4f);
        savingsDataSet.setValueTextSize(10f);

        // Combine datasets
        LineData lineData = new LineData(incomeDataSet, expenseDataSet, savingsDataSet);
        lineChart.setData(lineData);

        // Set x-axis labels
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(monthsList));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);

        // Refresh chart
        lineChart.invalidate();
    }

    private void updatePieChart() {
        List<PieEntry> entries = new ArrayList<>();
        String selectedMonth = (String) pieChartRangeSpinner.getSelectedItem();
        boolean showIncome = typeSwitch.isChecked();

        if (selectedMonth != null) {
            // Get the appropriate category map based on the switch
            Map<String, Double> categories = showIncome ?
                    monthlyCategoryIncome.getOrDefault(selectedMonth, new HashMap<>()) :
                    monthlyCategoryExpenses.getOrDefault(selectedMonth, new HashMap<>());

            // Convert category data to pie entries
            for (Map.Entry<String, Double> entry : categories.entrySet()) {
                if (entry.getValue() > 0) {
                    entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
                }
            }
        }

        if (entries.isEmpty()) {
            // Add a dummy entry if no data
            entries.add(new PieEntry(1f, "No data"));
        }

        PieDataSet dataSet = new PieDataSet(entries, showIncome ? "Income by Category" : "Spending by Category");
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);
        dataSet.setValueFormatter(new PercentFormatter(pieChart));
        dataSet.setValueTextSize(14f);
        dataSet.setValueTextColor(Color.BLACK);
        dataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);

        ArrayList<Integer> colors = new ArrayList<>();
        colors.add(Color.parseColor("#FF6B6B")); // Coral Red
        colors.add(Color.parseColor("#4ECDC4")); // Turquoise
        colors.add(Color.parseColor("#556270")); // Charcoal Blue
        colors.add(Color.parseColor("#C7F464")); // Lime Yellow
        colors.add(Color.parseColor("#FFCC5C")); // Soft Yellow
        colors.add(Color.parseColor("#96CEB4")); // Mint
        colors.add(Color.parseColor("#D9534F")); // Soft Red
        colors.add(Color.parseColor("#5BC0EB")); // Sky Blue
        colors.add(Color.parseColor("#FDE74C")); // Bright Yellow
        colors.add(Color.parseColor("#9BC53D")); // Lime Green


        dataSet.setColors(colors);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);

        // Refresh chart
        pieChart.invalidate();
        pieChart.animateY(500);
    }

    private void updateSavingsTracker() {
        String selectedMonth = (String) savingsRangeSpinner.getSelectedItem();
        if (selectedMonth == null) return;

        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
            Date date = inputFormat.parse(selectedMonth);
            String monthKey = outputFormat.format(date);

            double currentSavings = monthlySavings.getOrDefault(monthKey, 0.0);
            currentSavingsText.setText(String.format(Locale.getDefault(), "$%.2f", currentSavings));

            // Get budget for selected month (as savings goal)
            double goal = monthlyBudgets.getOrDefault(monthKey, 0.0);
            savingsGoalText.setText(String.format(Locale.getDefault(), "$%.2f", goal));

            // Calculate progress
            if (goal > 0) {
                int progress = (int) ((currentSavings / goal) * 100);
                savingsProgressBar.setProgress(Math.min(progress, 100));
                savingsProgressText.setText(String.format(Locale.getDefault(), "%d%% of goal", progress));

                // Compare with previous month
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(inputFormat.parse(selectedMonth));
                calendar.add(Calendar.MONTH, -1);
                String prevMonth = inputFormat.format(calendar.getTime());
                String prevMonthKey = outputFormat.format(calendar.getTime());
                double prevSavings = monthlySavings.getOrDefault(prevMonthKey, 0.0);

                if (currentSavings > prevSavings) {
                    savingsTrendIcon.setImageResource(R.drawable.growth);
                    savingsTrendIcon.setColorFilter(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark));
                } else {
                    savingsTrendIcon.setImageResource(R.drawable.recession);
                    savingsTrendIcon.setColorFilter(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark));
                }
            }
        } catch (Exception e) {
            Log.e("Dashboard", "Error updating savings tracker", e);
        }
    }

    private void updateBudgetComparison() {
        budgetComparisonContainer.removeAllViews();
        String selectedMonth = (String) budgetRangeSpinner.getSelectedItem();
        if (selectedMonth == null) return;

        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
            Date date = inputFormat.parse(selectedMonth);
            String monthKey = outputFormat.format(date);

            double budget = monthlyBudgets.getOrDefault(monthKey, 0.0);
            double expenses = monthlyExpenses.getOrDefault(monthKey, 0.0);

            // Add overall budget comparison
            addBudgetComparisonItem("Overall Budget", budget, expenses);
        } catch (Exception e) {
            Log.e("Dashboard", "Error updating budget comparison", e);
        }
    }

    private void addBudgetComparisonItem(String category, double budget, double actual) {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View itemView = inflater.inflate(R.layout.item_budget_comparison, budgetComparisonContainer, false);

        TextView categoryText = itemView.findViewById(R.id.categoryText);
        TextView budgetText = itemView.findViewById(R.id.budgetText);
        TextView actualText = itemView.findViewById(R.id.actualText);
        ProgressBar comparisonBar = itemView.findViewById(R.id.comparisonBar);
        ImageView trendIcon = itemView.findViewById(R.id.trendIcon);

        categoryText.setText(category);
        budgetText.setText(String.format(Locale.getDefault(), "$%.2f", budget));
        actualText.setText(String.format(Locale.getDefault(), "$%.2f", actual));

        // Calculate percentage (cap at 200% for visualization)
        int percentage = (int) ((actual / budget) * 100);
        comparisonBar.setProgress(Math.min(percentage, 200));

        if (actual > budget) {
            comparisonBar.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#E53E3E")));
            trendIcon.setImageResource(R.drawable.growth);
            trendIcon.setColorFilter(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark));
        } else {
            comparisonBar.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#38A169")));
            trendIcon.setImageResource(R.drawable.recession);
            trendIcon.setColorFilter(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark));
        }

        budgetComparisonContainer.addView(itemView);
    }

    private int getMonthsFromRange(String range) {
        switch (range) {
            case "Last 3 months": return 3;
            case "Last 6 months": return 6;
            case "Last year": return 12;
            default: return 3;
        }
    }
    private void simulateBankSync() {
        try {
            // Load JSON from assets
            InputStream is = getActivity().getAssets().open("transactions.json");
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            String jsonString = sb.toString();
            is.close();

            // Process the transactions
            processJsonTransactions(jsonString);

        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error syncing with bank: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("BankSync", "Error loading test data", e);
        }
    }

    // Update your processJsonTransactions method (using AtomicInteger as previously shown)
    private void processJsonTransactions(String jsonString) {
        try {
            JSONArray transactionsArray = new JSONArray(jsonString);
            AtomicInteger importedCount = new AtomicInteger(0);

            for (int i = 0; i < transactionsArray.length(); i++) {
                JSONObject jsonTransaction = transactionsArray.getJSONObject(i);

                // Create transaction map (same as before)
                Map<String, Object> transaction = new HashMap<>();
                transaction.put("title", jsonTransaction.getString("title"));
                transaction.put("amount", jsonTransaction.getDouble("amount"));
                transaction.put("type", jsonTransaction.getString("type"));
                transaction.put("category", jsonTransaction.optString("category", "Uncategorized"));
                transaction.put("date", new Date(jsonTransaction.getLong("date")));
                transaction.put("createdAt", Calendar.getInstance().getTime());

                // Save to Firestore
                db.collection("transaction")
                        .document(userId)
                        .collection("transactions")
                        .add(transaction)
                        .addOnSuccessListener(documentReference -> {
                            int count = importedCount.incrementAndGet();
                            if (count == transactionsArray.length()) {
                                String message = "Bank sync complete! Imported " + count + " transactions";
                                tvSyncStatus.setText("Last sync: " + new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(new Date()));
                                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                                loadData(); // Refresh the UI
                            }
                        })
                        .addOnFailureListener(e -> {
                            Log.e("BankSync", "Error saving transaction", e);
                        });
            }

        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error processing transactions", Toast.LENGTH_LONG).show();
            Log.e("BankSync", "Error processing JSON", e);
        }
    }
}