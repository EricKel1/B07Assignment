package com.example.b07project.models;

public class Streak {
    private String id;
    private String userId;
    private String type; // "controller" or "technique"
    private int currentStreak;
    private int longestStreak;
    private long lastUpdated; // timestamp in milliseconds

    public Streak() {
        // Required empty constructor for Firestore
    }

    public Streak(String id, String userId, String type, int currentStreak, int longestStreak, long lastUpdated) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.currentStreak = currentStreak;
        this.longestStreak = longestStreak;
        this.lastUpdated = lastUpdated;
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

    public int getCurrentStreak() {
        return currentStreak;
    }

    public int getLongestStreak() {
        return longestStreak;
    }

    public long getLastUpdated() {
        return lastUpdated;
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

    public void setCurrentStreak(int currentStreak) {
        this.currentStreak = currentStreak;
    }

    public void setLongestStreak(int longestStreak) {
        this.longestStreak = longestStreak;
    }

    public void setLastUpdated(long lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
