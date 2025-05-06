package com.example.projetguermah.model;

import java.util.Date;

public class SavingChallenge {
    private String id;
    private String name;
    private String description;
    private double targetAmount;
    private String duration;
    private Date startDate;
    private Date endDate;
    private double currentAmount;
    private boolean isCompleted;
    private boolean isActive;
    private String type; // "predefined" or "active"
    private String challengeType;

    public SavingChallenge() {}

    // For predefined challenges
    public SavingChallenge(String name, String description, double targetAmount, String duration) {
        this.name = name;
        this.description = description;
        this.targetAmount = targetAmount;
        this.duration = duration;
        this.type = "predefined";
        this.isActive = false;
    }

    // For active challenges
    public SavingChallenge(String name, String description, double targetAmount, String duration,
                           Date startDate, Date endDate) {
        this.name = name;
        this.description = description;
        this.targetAmount = targetAmount;
        this.duration = duration;
        this.startDate = startDate;
        this.endDate = endDate;
        this.type = "active";
        this.isActive = true;
    }

    public SavingChallenge(String name, String description, double targetAmount,
                           String duration, String challengeType) {
        this.name = name;
        this.description = description;
        this.targetAmount = targetAmount;
        this.duration = duration;
        this.challengeType = challengeType;
        this.type = "predefined";
        this.isActive = false;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getTargetAmount() { return targetAmount; }
    public void setTargetAmount(double targetAmount) { this.targetAmount = targetAmount; }
    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }
    public Date getStartDate() { return startDate; }
    public void setStartDate(Date startDate) { this.startDate = startDate; }
    public Date getEndDate() { return endDate; }
    public void setEndDate(Date endDate) { this.endDate = endDate; }
    public double getCurrentAmount() { return currentAmount; }
    public void setCurrentAmount(double currentAmount) { this.currentAmount = currentAmount; }
    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getChallengeType() { return challengeType; }
    public void setChallengeType(String challengeType) { this.challengeType = challengeType; }
}