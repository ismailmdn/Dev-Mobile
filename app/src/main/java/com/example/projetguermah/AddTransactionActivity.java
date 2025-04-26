package com.example.projetguermah;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AddTransactionActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    
    private EditText amountInput;
    private EditText noteInput;
    private Button dateButton;
    private Button timeButton;
    private Button expendButton;
    private Button incomeButton;
    private TextView cancelButton;
    private TextView saveButton;
    
    // Category views
    private View[] categoryViews;
    
    private Calendar selectedDate;
    private String selectedCategory = "Food";
    private String transactionType = "expense";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_transaction);
        
        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        
        // Initialize UI components
        initViews();
        
        // Setup transaction type toggle
        setupTransactionTypeToggle();
        
        // Set default date and time
        selectedDate = Calendar.getInstance();
        updateDateTimeButtons();
        
        // Set up category selection
        setupCategorySelection();
        
        // Setup date and time pickers
        setupDateTimePickers();
        
        // Setup cancel and save buttons
        setupActionButtons();
    }
    
    private void initViews() {
        // Initialize main inputs
        amountInput = findViewById(R.id.et_transaction_amount);
        noteInput = findViewById(R.id.et_transaction_note);
        
        // Initialize buttons
        dateButton = findViewById(R.id.btn_date);
        timeButton = findViewById(R.id.btn_time);
        expendButton = findViewById(R.id.btn_expend);
        incomeButton = findViewById(R.id.btn_income);
        cancelButton = findViewById(R.id.btn_cancel);
        saveButton = findViewById(R.id.btn_save);
        
        // Initialize category views
        categoryViews = new View[] {
            findViewById(R.id.category_food),
            findViewById(R.id.category_social),
            findViewById(R.id.category_traffic),
            findViewById(R.id.category_shopping),
            findViewById(R.id.category_grocery),
            findViewById(R.id.category_education),
            findViewById(R.id.category_bills),
            findViewById(R.id.category_rentals),
            findViewById(R.id.category_medical),
            findViewById(R.id.category_investment),
            findViewById(R.id.category_gift),
            findViewById(R.id.category_other)
        };
    }
    
    private void setupTransactionTypeToggle() {
        expendButton.setOnClickListener(v -> {
            // Reset both buttons
            expendButton.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
            expendButton.setTextColor(Color.parseColor("#757575"));
            incomeButton.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
            incomeButton.setTextColor(Color.parseColor("#757575"));
            
            // Highlight expend button
            expendButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1D4752")));
            expendButton.setTextColor(Color.WHITE);
            transactionType = "expense";
        });
        
        incomeButton.setOnClickListener(v -> {
            // Reset both buttons
            expendButton.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
            expendButton.setTextColor(Color.parseColor("#757575"));
            incomeButton.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
            incomeButton.setTextColor(Color.parseColor("#757575"));
            
            // Highlight income button
            incomeButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1D4752")));
            incomeButton.setTextColor(Color.WHITE);
            transactionType = "income";
        });
    }
    
    private void setupCategorySelection() {
        View.OnClickListener categoryClickListener = v -> {
            // Reset all categories
            resetCategorySelection();
            
            // Highlight selected category
            v.setSelected(true);
            v.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E0E0E0")));
            
            // Get selected category
            int id = v.getId();
            if (id == R.id.category_food) {
                selectedCategory = "Food";
            } else if (id == R.id.category_social) {
                selectedCategory = "Social";
            } else if (id == R.id.category_traffic) {
                selectedCategory = "Traffic";
            } else if (id == R.id.category_shopping) {
                selectedCategory = "Shopping";
            } else if (id == R.id.category_grocery) {
                selectedCategory = "Grocery";
            } else if (id == R.id.category_education) {
                selectedCategory = "Education";
            } else if (id == R.id.category_bills) {
                selectedCategory = "Bills";
            } else if (id == R.id.category_rentals) {
                selectedCategory = "Rentals";
            } else if (id == R.id.category_medical) {
                selectedCategory = "Medical";
            } else if (id == R.id.category_investment) {
                selectedCategory = "Investment";
            } else if (id == R.id.category_gift) {
                selectedCategory = "Gift";
            } else if (id == R.id.category_other) {
                selectedCategory = "Other";
            }
        };
        
        // Apply click listener to all categories
        for (View categoryView : categoryViews) {
            categoryView.setOnClickListener(categoryClickListener);
        }
        
        // Highlight the default category (Food)
        categoryViews[0].setSelected(true);
        categoryViews[0].setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E0E0E0")));
    }
    
    private void resetCategorySelection() {
        for (View categoryView : categoryViews) {
            categoryView.setSelected(false);
            categoryView.setBackgroundTintList(null);
        }
    }
    
    private void setupDateTimePickers() {
        dateButton.setOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this,
                    (view, year, month, dayOfMonth) -> {
                        selectedDate.set(Calendar.YEAR, year);
                        selectedDate.set(Calendar.MONTH, month);
                        selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        updateDateTimeButtons();
                    },
                    selectedDate.get(Calendar.YEAR),
                    selectedDate.get(Calendar.MONTH),
                    selectedDate.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.show();
        });
        
        timeButton.setOnClickListener(v -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(
                    this,
                    (view, hourOfDay, minute) -> {
                        selectedDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        selectedDate.set(Calendar.MINUTE, minute);
                        updateDateTimeButtons();
                    },
                    selectedDate.get(Calendar.HOUR_OF_DAY),
                    selectedDate.get(Calendar.MINUTE),
                    true
            );
            timePickerDialog.show();
        });
    }
    
    private void updateDateTimeButtons() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM, dd yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        
        dateButton.setText(dateFormat.format(selectedDate.getTime()));
        timeButton.setText(timeFormat.format(selectedDate.getTime()));
    }
    
    private void setupActionButtons() {
        cancelButton.setOnClickListener(v -> finish());
        
        saveButton.setOnClickListener(v -> {
            if (validateInputs()) {
                saveTransaction();
            }
        });
    }
    
    private boolean validateInputs() {
        String amountStr = amountInput.getText().toString().trim();
        
        if (amountStr.isEmpty()) {
            amountInput.setError("Amount is required");
            return false;
        }
        
        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                amountInput.setError("Amount must be greater than 0");
                return false;
            }
        } catch (NumberFormatException e) {
            amountInput.setError("Invalid amount format");
            return false;
        }
        
        return true;
    }
    
    private void saveTransaction() {
        // Check authentication
        if (mAuth.getCurrentUser() == null) {
            showToast("User not authenticated");
            return;
        }
        
        String userId = mAuth.getCurrentUser().getUid();
        String amountStr = amountInput.getText().toString().trim();
        String note = noteInput.getText().toString().trim();
        double amount = Double.parseDouble(amountStr);
        
        // Get title from note or use category as title
        String title = note.isEmpty() ? selectedCategory : note;
        
        // Create transaction object
        Map<String, Object> transactionData = new HashMap<>();
        transactionData.put("title", title);
        transactionData.put("amount", amount);
        transactionData.put("date", selectedDate.getTime());
        transactionData.put("type", transactionType);
        transactionData.put("category", selectedCategory);
        
        // Save to Firestore using nested collection structure
        db.collection("transaction")
            .document(userId)
            .collection("transactions")
            .add(transactionData)
            .addOnSuccessListener(documentReference -> {
                // Return success result to calling activity
                Intent resultIntent = new Intent();
                resultIntent.putExtra("TRANSACTION_ID", documentReference.getId());
                setResult(RESULT_OK, resultIntent);
                
                showToast("Transaction added successfully");
                finish();
            })
            .addOnFailureListener(e -> {
                String errorMessage = e.getMessage();
                if (errorMessage != null && errorMessage.contains("permission")) {
                    showToast("Permission denied. Please check your authentication.");
                } else {
                    showToast("Failed to add transaction: " + errorMessage);
                }
            });
    }
    
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
} 