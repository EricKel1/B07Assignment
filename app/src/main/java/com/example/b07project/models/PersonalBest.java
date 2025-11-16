package com.example.b07project.models;

import java.util.Date;

public class PersonalBest {
    private String id;
    private String userId; // Child's user ID
    private int value; // PEF value in L/min
    private String setByUserId; // Parent's user ID who set this
    private Date dateSet;
    private String notes;

    public PersonalBest() {
        this.dateSet = new Date();
    }

    public PersonalBest(String userId, int value, String setByUserId) {
        this.userId = userId;
        this.value = value;
        this.setByUserId = setByUserId;
        this.dateSet = new Date();
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

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public String getSetByUserId() {
        return setByUserId;
    }

    public void setSetByUserId(String setByUserId) {
        this.setByUserId = setByUserId;
    }

    public Date getDateSet() {
        return dateSet;
    }

    public void setDateSet(Date dateSet) {
        this.dateSet = dateSet;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    // Zone calculation helpers
    public static String calculateZone(int pefValue, int personalBest) {
        if (personalBest <= 0) {
            return "unknown";
        }
        
        double percentage = (pefValue * 100.0) / personalBest;
        
        if (percentage >= 80) {
            return "green";
        } else if (percentage >= 50) {
            return "yellow";
        } else {
            return "red";
        }
    }

    public static int getZoneColor(String zone) {
        switch (zone) {
            case "green":
                return 0xFF4CAF50; // Green
            case "yellow":
                return 0xFFFFEB3B; // Yellow
            case "red":
                return 0xFFF44336; // Red
            default:
                return 0xFF9E9E9E; // Gray
        }
    }

    public static String getZoneLabel(String zone) {
        switch (zone) {
            case "green":
                return "Green Zone - All Clear";
            case "yellow":
                return "Yellow Zone - Caution";
            case "red":
                return "Red Zone - Danger";
            default:
                return "Unknown Zone";
        }
    }
}
