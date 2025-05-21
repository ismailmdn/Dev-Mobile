package com.example.projetguermah;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class FinancialRecommendationActivity extends AppCompatActivity {

    private static final String TAG = "FinancialRecommendation";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String API_URL = "http://10.0.2.2:5000/recommend";
    private static final String FEEDBACK_API_URL = "http://10.0.2.2:5000/feedback";

    private FirebaseFirestore db;
    private String userId;
    private String selectedMonth;

    private TextView[] recommendationTextViews = new TextView[3];
    private TextView[] confidenceTextViews = new TextView[3];
    private Button[] applyButtons = new Button[3];
    private Button[] dismissButtons = new Button[3];
    private View[] recommendationCards = new View[3];

    private ApiResponse apiResponse;
    private boolean[] feedbackGiven = new boolean[3];

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_financial_recommendation);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }

        // Initialize views for all 3 recommendations
        recommendationTextViews[0] = findViewById(R.id.recommendation_text_1);
        recommendationTextViews[1] = findViewById(R.id.recommendation_text_2);
        recommendationTextViews[2] = findViewById(R.id.recommendation_text_3);

        confidenceTextViews[0] = findViewById(R.id.confidence_text_1);
        confidenceTextViews[1] = findViewById(R.id.confidence_text_2);
        confidenceTextViews[2] = findViewById(R.id.confidence_text_3);

        applyButtons[0] = findViewById(R.id.apply_button_1);
        applyButtons[1] = findViewById(R.id.apply_button_2);
        applyButtons[2] = findViewById(R.id.apply_button_3);

        dismissButtons[0] = findViewById(R.id.dismiss_button_1);
        dismissButtons[1] = findViewById(R.id.dismiss_button_2);
        dismissButtons[2] = findViewById(R.id.dismiss_button_3);

        recommendationCards[0] = findViewById(R.id.recommendation_card_1);
        recommendationCards[1] = findViewById(R.id.recommendation_card_2);
        recommendationCards[2] = findViewById(R.id.recommendation_card_3);

        db = FirebaseFirestore.getInstance();
        userId = getIntent().getStringExtra("userId");
        selectedMonth = getIntent().getStringExtra("month");

        setupButtons();
        loadFinancialData();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupButtons() {
        for (int i = 0; i < 3; i++) {
            final int index = i;
            applyButtons[i].setOnClickListener(v -> {
                if (!feedbackGiven[index] && apiResponse != null && apiResponse.getRecommendations() != null
                        && apiResponse.getRecommendations().size() > index) {
                    sendFeedbackToApi("applied", index);
                    disableButtons(index);
                    Toast.makeText(this, "Recommendation applied!", Toast.LENGTH_SHORT).show();
                }
            });

            dismissButtons[i].setOnClickListener(v -> {
                if (!feedbackGiven[index] && apiResponse != null && apiResponse.getRecommendations() != null
                        && apiResponse.getRecommendations().size() > index) {
                    sendFeedbackToApi("rejected", index);
                    disableButtons(index);
                    Toast.makeText(this, "Recommendation dismissed", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void disableButtons(int index) {
        feedbackGiven[index] = true;
        applyButtons[index].setEnabled(false);
        dismissButtons[index].setEnabled(false);
    }

    private void sendFeedbackToApi(String feedbackType, int recommendationIndex) {
        if (apiResponse == null || apiResponse.getContext() == null) return;

        OkHttpClient client = new OkHttpClient();
        Gson gson = new Gson();

        // Create specific context for this recommendation
        Map<String, Object> feedbackContext = new HashMap<>();
        feedbackContext.put("state", apiResponse.getContext().get("state"));
        feedbackContext.put("action", apiResponse.getRecommendations().get(recommendationIndex).getAction_type());
        feedbackContext.put("confidence", apiResponse.getRecommendations().get(recommendationIndex).getConfidence());

        Map<String, Object> feedbackData = new HashMap<>();
        feedbackData.put("context", feedbackContext);
        feedbackData.put("feedback", feedbackType);

        String json = gson.toJson(feedbackData);
        Log.d(TAG, "Feedback JSON: " + json);
        RequestBody body = RequestBody.create(json, JSON);

        Request request = new Request.Builder()
                .url(FEEDBACK_API_URL)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Feedback API call failed", e);
                    Toast.makeText(FinancialRecommendationActivity.this,
                            "Failed to send feedback", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                runOnUiThread(() -> {
                    if (!response.isSuccessful()) {
                        Log.e(TAG, "Feedback API error: " + response.code());
                    }
                });
            }
        });
    }

    private void loadFinancialData() {
        if (userId == null || selectedMonth == null) {
            Toast.makeText(this, "User ID or month not provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String monthYear = selectedMonth;

        db.collection("finance")
                .document(userId)
                .collection("months")
                .whereEqualTo("monthYear", monthYear)
                .get()
                .addOnSuccessListener(monthSnapshot -> {
                    if (monthSnapshot.isEmpty()) {
                        Toast.makeText(this, "No data for selected month", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Double monthlyIncome = 0.0;
                    Double monthlySavings = 0.0;

                    for (QueryDocumentSnapshot document : monthSnapshot) {
                        monthlyIncome = document.getDouble("income");
                        monthlySavings = document.getDouble("savings");
                    }

                    loadCategorySpending(monthYear, monthlyIncome, monthlySavings);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading monthly data", e);
                    Toast.makeText(this, "Failed to load monthly data", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadCategorySpending(String monthYear, Double monthlyIncome, Double monthlySavings) {
        db.collection("finance")
                .document(userId)
                .collection("transactions")
                .whereEqualTo("monthYear", monthYear)
                .whereEqualTo("type", "expense")
                .get()
                .addOnSuccessListener(transactionsSnapshot -> {
                    Map<String, Double> categorySpending = new HashMap<>();

                    // Initialize all required categories with 0 if not present
                    String[] requiredCategories = {"food", "transport", "entertainment", "health", "shopping", "utilities"};
                    for (String category : requiredCategories) {
                        categorySpending.put(category, 0.0);
                    }

                    for (QueryDocumentSnapshot document : transactionsSnapshot) {
                        String category = document.getString("category");
                        Double amount = document.getDouble("amount");

                        if (category != null && amount != null) {
                            categorySpending.put(category,
                                    categorySpending.getOrDefault(category, 0.0) + amount);
                        }
                    }

                    loadSavingsGoals(monthYear, monthlyIncome, monthlySavings, categorySpending);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading transactions", e);
                    Toast.makeText(this, "Failed to load transactions", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadSavingsGoals(String monthYear, Double monthlyIncome, Double monthlySavings,
                                  Map<String, Double> categorySpending) {
        db.collection("finance")
                .document(userId)
                .collection("savingsGoals")
                .whereEqualTo("isActive", true)
                .get()
                .addOnSuccessListener(goalsSnapshot -> {
                    List<Map<String, Object>> savingGoals = new ArrayList<>();

                    for (QueryDocumentSnapshot document : goalsSnapshot) {
                        Map<String, Object> goal = new HashMap<>();
                        goal.put("name", document.getString("name"));
                        goal.put("current", document.getDouble("currentAmount"));
                        goal.put("target", document.getDouble("targetAmount"));
                        savingGoals.add(goal);
                    }

                    // Ensure all required fields are present and not null
                    Map<String, Object> requestData = new HashMap<>();
                    requestData.put("user_id", userId);
                    requestData.put("category_spending", categorySpending);
                    requestData.put("monthly_income", monthlyIncome != null ? monthlyIncome : 0);
                    requestData.put("monthly_savings", monthlySavings != null ? monthlySavings : 0);
                    requestData.put("saving_goals", savingGoals);

                    Log.d(TAG, "Prepared request data: " + requestData.toString());
                    callRecommendationApi(requestData);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading savings goals", e);
                    Toast.makeText(this, "Failed to load savings goals", Toast.LENGTH_SHORT).show();
                });
    }

    private void callRecommendationApi(Map<String, Object> requestData) {
        OkHttpClient client = new OkHttpClient();
        Gson gson = new Gson();

        // Validate required fields
        if (!requestData.containsKey("user_id") ||
                !requestData.containsKey("category_spending") ||
                !requestData.containsKey("monthly_income")) {
            Log.e(TAG, "Missing required fields in request data");
            Toast.makeText(this, "Missing required financial data", Toast.LENGTH_SHORT).show();
            return;
        }

        // Ensure all spending categories are present
        Map<String, Double> categorySpending = (Map<String, Double>) requestData.get("category_spending");
        String[] requiredCategories = {"food", "transport", "entertainment", "health", "shopping", "utilities"};
        for (String category : requiredCategories) {
            if (!categorySpending.containsKey(category)) {
                categorySpending.put(category, 0.0);
            }
        }

        String json = gson.toJson(requestData);
        Log.d(TAG, "Sending JSON to API: " + json);

        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(API_URL)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    Log.e(TAG, "API call failed", e);
                    Toast.makeText(FinancialRecommendationActivity.this,
                            "Failed to get recommendations: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                runOnUiThread(() -> {
                    if (!response.isSuccessful()) {
                        Log.e(TAG, "API error: " + response.code() + " - " + responseBody);
                        Toast.makeText(FinancialRecommendationActivity.this,
                                "API error: " + response.code() + " - " + response.message(), Toast.LENGTH_LONG).show();
                        return;
                    }

                    try {
                        Log.d(TAG, "API response: " + responseBody);
                        Type responseType = new TypeToken<ApiResponse>(){}.getType();
                        apiResponse = gson.fromJson(responseBody, responseType);
                        updateUIWithRecommendations();
                    } catch (Exception e) {
                        Log.e(TAG, "Parsing error: " + e.getMessage());
                        Toast.makeText(FinancialRecommendationActivity.this,
                                "Failed to parse response", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void updateUIWithRecommendations() {
        if (apiResponse != null && apiResponse.getRecommendations() != null) {
            List<Recommendation> recommendations = apiResponse.getRecommendations();

            for (int i = 0; i < 3; i++) {
                if (i < recommendations.size()) {
                    // Show this recommendation
                    recommendationTextViews[i].setText(recommendations.get(i).getRecommendation());
                    //confidenceTextViews[i].setText(String.format(Locale.getDefault(),
                    //        "Confidence: %.1f%%", recommendations.get(i).getConfidence() * 100));
                    recommendationCards[i].setVisibility(View.VISIBLE);

                    // Reset feedback state for this recommendation
                    feedbackGiven[i] = false;
                    applyButtons[i].setEnabled(true);
                    dismissButtons[i].setEnabled(true);
                } else {
                    // Hide this card if there are less than 3 recommendations
                    recommendationCards[i].setVisibility(View.GONE);
                }
            }
        }
    }

    public static class ApiResponse {
        private String recommendation_id;
        private String user_id;
        private String timestamp;
        private List<Recommendation> recommendations;
        private Map<String, Object> context;

        public String getRecommendation_id() {
            return recommendation_id;
        }

        public String getUser_id() {
            return user_id;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public List<Recommendation> getRecommendations() {
            return recommendations;
        }

        public Map<String, Object> getContext() {
            return context;
        }
    }

    public static class Recommendation {
        private int action_type;
        private String recommendation;
        private double confidence;

        public int getAction_type() {
            return action_type;
        }

        public String getRecommendation() {
            return recommendation;
        }

        public double getConfidence() {
            return confidence;
        }
    }
}