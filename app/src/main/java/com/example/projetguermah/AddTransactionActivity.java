package com.example.projetguermah;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AddTransactionActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    
    private EditText amountInput;
    private EditText noteInput;
    private MaterialButton dateButton;
    private MaterialButton timeButton;
    private MaterialButton expendButton;
    private MaterialButton incomeButton;
    private MaterialButton cancelButton;
    private MaterialButton saveButton;
    private RecyclerView categoriesGrid;
    
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
        
        // Setup date and time pickers
        setupDateTimePickers();
        
        // Setup cancel and save buttons
        setupActionButtons();
        
        // Setup categories grid
        setupCategoriesGrid();
    }
    
    private void initViews() {
        // Initialize main inputs
        amountInput = findViewById(R.id.et_transaction_amount);
        noteInput = findViewById(R.id.et_note);
        
        // Initialize buttons
        dateButton = findViewById(R.id.btn_date);
        timeButton = findViewById(R.id.btn_time);
        expendButton = findViewById(R.id.btn_expend);
        incomeButton = findViewById(R.id.btn_income);
        cancelButton = findViewById(R.id.btn_cancel);
        saveButton = findViewById(R.id.btn_save);
        
        // Initialize categories grid
        categoriesGrid = findViewById(R.id.categories_grid);
    }
    
    private void setupTransactionTypeToggle() {
        expendButton.setOnClickListener(v -> {
            expendButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1D4752")));
            expendButton.setTextColor(Color.WHITE);
            incomeButton.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
            incomeButton.setTextColor(Color.parseColor("#757575"));
            transactionType = "expense";
        });
        
        incomeButton.setOnClickListener(v -> {
            incomeButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1D4752")));
            incomeButton.setTextColor(Color.WHITE);
            expendButton.setBackgroundTintList(ColorStateList.valueOf(Color.WHITE));
            expendButton.setTextColor(Color.parseColor("#757575"));
            transactionType = "income";
        });
    }
    
    private void setupCategoriesGrid() {
        // Set up the RecyclerView with a GridLayoutManager
        GridLayoutManager layoutManager = new GridLayoutManager(this, 4);
        categoriesGrid.setLayoutManager(layoutManager);
        
        // Create and set the adapter
        CategoryAdapter adapter = new CategoryAdapter(category -> {
            selectedCategory = category;
            // You could show a toast or update UI to indicate selection
            showToast("Selected category: " + category);
        });
        categoriesGrid.setAdapter(adapter);
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
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
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
        String amount = amountInput.getText().toString().trim();
        if (TextUtils.isEmpty(amount)) {
            showToast("Please enter an amount");
            return false;
        }
        
        try {
            double amountValue = Double.parseDouble(amount);
            if (amountValue <= 0) {
                showToast("Amount must be greater than 0");
                return false;
            }
        } catch (NumberFormatException e) {
            showToast("Invalid amount format");
            return false;
        }
        
        return true;
    }
    
    private void saveTransaction() {
        if (mAuth.getCurrentUser() == null) {
            showToast("Please sign in to add transactions");
            return;
        }
        
        String userId = mAuth.getCurrentUser().getUid();
        
        // Create transaction data
        Map<String, Object> transaction = new HashMap<>();
        transaction.put("title", noteInput.getText().toString().trim());
        transaction.put("amount", Double.parseDouble(amountInput.getText().toString().trim()));
        transaction.put("type", transactionType);
        transaction.put("category", selectedCategory);
        transaction.put("date", selectedDate.getTime());
        transaction.put("createdAt", Calendar.getInstance().getTime());

        // Save to Firestore using the consistent collection pattern
        db.collection("transaction")
                .document(userId)
                .collection("transactions")
                .add(transaction)
                .addOnSuccessListener(documentReference -> {
                    showToast("Transaction added successfully");
                    setResult(RESULT_OK);
                    finish();
                })
                .addOnFailureListener(e -> {
                    showToast("Failed to add transaction: " + e.getMessage());
                });
    }
    
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
} 