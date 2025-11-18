package com.example.b07project.models;

public class Badge {
    private String id;
    private String userId;
    private String type; // "perfect_controller_week", "technique_sessions", "low_rescue_month"
    private String name;
    private String description;
    private boolean earned;
    private long earnedDate; // timestamp in milliseconds
    private int progress; // current progress towards badge
    private int requirement; // requirement to earn badge
    private long periodEndDate; // For time-based badges like low_rescue_month

    public Badge() {
        // Required empty constructor for Firestore
    }

    public Badge(String id, String userId, String type, String name, String description, 
                 boolean earned, long earnedDate, int progress, int requirement) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.name = name;
        this.description = description;
        this.earned = earned;
        this.earnedDate = earnedDate;
        this.progress = progress;
        this.requirement = requirement;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isEarned() {
        return earned;
    }

    public long getEarnedDate() {
        return earnedDate;
    }

    public int getProgress() {
        return progress;
    }

    public int getRequirement() {
        return requirement;
    }

    public long getPeriodEndDate() {
        return periodEndDate;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setEarned(boolean earned) {
        this.earned = earned;
    }

    public void setEarnedDate(long earnedDate) {
        this.earnedDate = earnedDate;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public void setRequirement(int requirement) {
        this.requirement = requirement;
    }

    public void setPeriodEndDate(long periodEndDate) {
        this.periodEndDate = periodEndDate;
    }
}
