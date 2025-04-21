package com.example.projetguermah;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class ProfileFragment extends Fragment {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private TextView titleTextView, budgetValue, expensesValue, savingsValue,  yearText;
    private Button logoutButton, saveBudgetButton, cancelBudgetButton;
    private EditText editBudgetInput;
    private RelativeLayout editPopup;
    private LinearLayout menuPopup, monthYearSelector;
    private ImageView editIcon, editMenuIcon;

    private String currentMonth;
    private String currentYear;
    private String uid;

    private Spinner monthSpinner; // Spinner to select month

    public ProfileFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        titleTextView = view.findViewById(R.id.budgetTitle);
        budgetValue = view.findViewById(R.id.budgetValue);
        expensesValue = view.findViewById(R.id.expensesValue);
        savingsValue = view.findViewById(R.id.savingsValue);

        yearText = view.findViewById(R.id.yearText); // TextView for year
        logoutButton = view.findViewById(R.id.logoutButton);
        editPopup = view.findViewById(R.id.editPopup);
        menuPopup = view.findViewById(R.id.menuPopup);
        editBudgetInput = view.findViewById(R.id.editBudgetInput);
        saveBudgetButton = view.findViewById(R.id.saveBudgetButton);
        cancelBudgetButton = view.findViewById(R.id.cancelBudgetButton);
        editIcon = view.findViewById(R.id.editIcon);
        editMenuIcon = view.findViewById(R.id.menuIcon);

        monthYearSelector = view.findViewById(R.id.monthYearSelector);

        monthSpinner = view.findViewById(R.id.monthSpinner); // Spinner for months


        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {
            uid = user.getUid();

            db.collection("users").document(uid).get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String name = documentSnapshot.getString("name");
                    if (name != null) {
                        titleTextView.setText(name + "'s Budget");
                    }
                }
            });

            // Set default values for current month and year
            Calendar calendar = Calendar.getInstance();
            currentMonth = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendar.getTime());
            currentYear = new SimpleDateFormat("yyyy", Locale.getDefault()).format(calendar.getTime());
            String monthYearDisplay = new SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(calendar.getTime());


            // Set the year on the TextView
            yearText.setText(currentYear);

            loadFinanceFromFirestore();

            // Populate the month spinner with month names
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
                    R.array.months_array, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            monthSpinner.setAdapter(adapter);

            // Set listener for month selection
            monthSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                    String selectedMonth = (String) parentView.getItemAtPosition(position);
                    currentMonth = selectedMonth + " " + currentYear; // Update the currentMonth based on selection

                    loadFinanceFromFirestore(); // Load finances for selected month
                }

                @Override
                public void onNothingSelected(AdapterView<?> parentView) {
                    // Do nothing
                }
            });
        }

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

        // Edit icon (pen) and month/year click for budget editing
        View.OnClickListener openEditor = v -> {
            editBudgetInput.setText(budgetValue.getText().toString());
            editPopup.setVisibility(View.VISIBLE);
            menuPopup.setVisibility(View.GONE); // Hide menu when opening editor
        };

        editIcon.setOnClickListener(openEditor);

        monthYearSelector.setOnClickListener(openEditor);

        // 3-dot menu icon click
        editMenuIcon.setOnClickListener(v -> {
            if (menuPopup.getVisibility() == View.VISIBLE) {
                menuPopup.setVisibility(View.GONE);
            } else {
                menuPopup.setVisibility(View.VISIBLE);
                editPopup.setVisibility(View.GONE); // Hide editor when opening menu
            }
        });

        // Cancel button to close the edit popup
        cancelBudgetButton.setOnClickListener(v -> {
            editPopup.setVisibility(View.GONE);
            menuPopup.setVisibility(View.GONE);
        });

        // Save button to save the budget
        saveBudgetButton.setOnClickListener(v -> {
            String input = editBudgetInput.getText().toString();
            if (TextUtils.isEmpty(input)) {
                Toast.makeText(getContext(), "Please enter a budget", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double newBudget = Double.parseDouble(input);

                db.collection("finance")
                        .document(uid)
                        .collection("months")
                        .document(currentMonth)
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                db.collection("finance")
                                        .document(uid)
                                        .collection("months")
                                        .document(currentMonth)
                                        .update("budget", newBudget)
                                        .addOnSuccessListener(aVoid -> {
                                            budgetValue.setText(String.valueOf(newBudget));
                                            editPopup.setVisibility(View.GONE);
                                            Toast.makeText(getContext(), "Budget updated", Toast.LENGTH_SHORT).show();
                                        })
                                        .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to update budget", Toast.LENGTH_SHORT).show());
                            } else {
                                db.collection("finance")
                                        .document(uid)
                                        .collection("months")
                                        .document(currentMonth)
                                        .set(new Finance(currentMonth, newBudget, 0.0, 0.0))
                                        .addOnSuccessListener(aVoid -> {
                                            budgetValue.setText(String.valueOf(newBudget));
                                            editPopup.setVisibility(View.GONE);
                                            Toast.makeText(getContext(), "Budget created", Toast.LENGTH_SHORT).show();
                                        })
                                        .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to create budget", Toast.LENGTH_SHORT).show());
                            }
                        })
                        .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to check budget document", Toast.LENGTH_SHORT).show());

            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Invalid number", Toast.LENGTH_SHORT).show();
            }
        });

        // Hide popups when clicking outside
        editPopup.setOnClickListener(v -> editPopup.setVisibility(View.GONE));
        view.findViewById(R.id.profileFragment).setOnClickListener(v -> {
            editPopup.setVisibility(View.GONE);
            menuPopup.setVisibility(View.GONE);
        });

        return view;
    }

    private void loadFinanceFromFirestore() {
        db.collection("finance")
                .document(uid)
                .collection("months")
                .document(currentMonth)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        double budget = documentSnapshot.getDouble("budget");
                        budgetValue.setText(String.valueOf(budget));
                    } else {
                        budgetValue.setText("No budget set");
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to load finance data", Toast.LENGTH_SHORT).show());
    }

    public static class Finance {
        private String month;
        private double budget;
        private double expenses;
        private double savings;

        public Finance(String month, double budget, double expenses, double savings) {
            this.month = month;
            this.budget = budget;
            this.expenses = expenses;
            this.savings = savings;
        }

        public String getMonth() {
            return month;
        }

        public double getBudget() {
            return budget;
        }

        public double getExpenses() {
            return expenses;
        }

        public double getSavings() {
            return savings;
        }
    }
}
