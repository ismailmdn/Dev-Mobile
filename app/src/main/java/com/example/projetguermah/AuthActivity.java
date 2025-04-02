package com.example.projetguermah;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;

import java.util.HashMap;
import java.util.Map;

public class AuthActivity extends AppCompatActivity {

    private LinearLayout loginView;
    private LinearLayout signupView;
    private Button loginTabButton;
    private Button signupTabButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        loginView = findViewById(R.id.login_view);
        signupView = findViewById(R.id.signup_view);
        loginTabButton = findViewById(R.id.login_tab_button);
        signupTabButton = findViewById(R.id.signup_tab_button);

        // Check if the user is already authenticated
        SharedPreferences prefs = getSharedPreferences("MyAppPrefs", MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);

        if (isLoggedIn) {
            navigateToMain();
        }

        // Set up tab button listeners
        loginTabButton.setOnClickListener(v -> switchToLogin());
        signupTabButton.setOnClickListener(v -> switchToSignup());

        // Set up login button listener
        findViewById(R.id.login_button).setOnClickListener(v -> handleLogin());

        // Set up signup button listener
        findViewById(R.id.signup_button).setOnClickListener(v -> handleSignup());
    }

    private void switchToLogin() {
        loginView.setVisibility(View.VISIBLE);
        signupView.setVisibility(View.GONE);
        loginTabButton.setBackgroundResource(R.drawable.tab_button_selected);
        signupTabButton.setBackgroundResource(R.drawable.tab_button_unselected);
    }

    private void switchToSignup() {
        loginView.setVisibility(View.GONE);
        signupView.setVisibility(View.VISIBLE);
        loginTabButton.setBackgroundResource(R.drawable.tab_button_unselected);
        signupTabButton.setBackgroundResource(R.drawable.tab_button_selected);
    }

    private void handleLogin() {
        EditText emailEditText = findViewById(R.id.login_email_edittext);
        EditText passwordEditText = findViewById(R.id.login_password_edittext);

        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show loading state
        Button loginButton = findViewById(R.id.login_button);
        loginButton.setEnabled(false);
        loginButton.setText("Logging in...");

        // Firebase Authentication
        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    // Login successful
                    SharedPreferences.Editor editor = getSharedPreferences("MyAppPrefs", MODE_PRIVATE).edit();
                    editor.putBoolean("isLoggedIn", true);
                    editor.apply();
                    navigateToMain();
                } else {
                    // Login failed
                    Toast.makeText(AuthActivity.this, "Login failed: " + task.getException().getMessage(),
                            Toast.LENGTH_SHORT).show();
                    loginButton.setEnabled(true);
                    loginButton.setText("Login");
                }
            });
    }

    private void handleSignup() {
        EditText nameEditText = findViewById(R.id.signup_name_edittext);
        EditText emailEditText = findViewById(R.id.signup_email_edittext);
        EditText passwordEditText = findViewById(R.id.signup_password_edittext);

        String name = nameEditText.getText().toString();
        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        signUp(email, password, name);
    }

    private void signUp(String email, String password, String name) {
        showLoading(true);
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Create user document in Firestore
                            Map<String, Object> userData = new HashMap<>();
                            userData.put("name", name);
                            userData.put("email", email);
                            userData.put("createdAt", FieldValue.serverTimestamp());

                            db.collection("users")
                                    .document(user.getUid())
                                    .set(userData)
                                    .addOnSuccessListener(aVoid -> {
                                        showLoading(false);
                                        navigateToMain();
                                    })
                                    .addOnFailureListener(e -> {
                                        showLoading(false);
                                        showError("Failed to create user profile: " + e.getMessage());
                                        // Delete the auth user if Firestore fails
                                        user.delete();
                                    });
                        }
                    } else {
                        showLoading(false);
                        String errorMessage = "Sign up failed: ";
                        if (task.getException() != null) {
                            if (task.getException().getMessage().contains("email address is already in use")) {
                                errorMessage += "Email already registered";
                            } else if (task.getException().getMessage().contains("password is invalid")) {
                                errorMessage += "Invalid password format";
                            } else {
                                errorMessage += task.getException().getMessage();
                            }
                        }
                        showError(errorMessage);
                    }
                });
    }

    private void navigateToMain() {
        Intent intent = new Intent(AuthActivity.this, MainActivity.class);
        startActivity(intent);
        finish(); // Close AuthActivity so users cannot navigate back to it
    }

    private void showLoading(boolean show) {
        // Implementation of showLoading method
    }

    private void showError(String message) {
        // Implementation of showError method
    }
}
