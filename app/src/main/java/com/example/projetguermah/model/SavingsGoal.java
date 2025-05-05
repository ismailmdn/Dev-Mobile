package com.example.projetguermah.model;


public class SavingsGoal {
    private String id;
    private String name;
    private double targetAmount;
    private double currentAmount;
    private String targetDate;
    private boolean isActive;

    public SavingsGoal() {}

    public SavingsGoal(String name, double targetAmount, String targetDate) {
        this.name = name;
        this.targetAmount = targetAmount;
        this.targetDate = targetDate;
        this.currentAmount = 0;
        this.isActive = true;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getTargetAmount() { return targetAmount; }
    public void setTargetAmount(double targetAmount) { this.targetAmount = targetAmount; }
    public double getCurrentAmount() { return currentAmount; }
    public void setCurrentAmount(double currentAmount) { this.currentAmount = currentAmount; }
    public String getTargetDate() { return targetDate; }
    public void setTargetDate(String targetDate) { this.targetDate = targetDate; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}