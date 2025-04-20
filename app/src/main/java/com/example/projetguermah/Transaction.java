package com.example.projetguermah;

import java.util.Date;

public class Transaction {
    private String id;
    private String userId;
    private String title;
    private double amount;
    private Date date;
    private String type; // "income" or "expense"
    private String category;

    // Required empty constructor for Firestore
    public Transaction() {
    }

    public Transaction(String id, String userId, String title, double amount, Date date, String type, String category) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.amount = amount;
        this.date = date;
        this.type = type;
        this.category = category;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
} 