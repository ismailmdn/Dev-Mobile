package com.example.projetguermah.model;


public class SavingChallenge {
    private String name;
    private String description;
    private double targetAmount;
    private String duration;
    private boolean isCompleted;

    public SavingChallenge() {}

    public SavingChallenge(String name, String description, double targetAmount, String duration) {
        this.name = name;
        this.description = description;
        this.targetAmount = targetAmount;
        this.duration = duration;
        this.isCompleted = false;
    }

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getTargetAmount() { return targetAmount; }
    public void setTargetAmount(double targetAmount) { this.targetAmount = targetAmount; }
    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }
    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }
}
