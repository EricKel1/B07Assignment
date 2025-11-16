package com.example.b07project.models;

import java.util.Date;

public class ZoneChangeLog {
    private String id;
    private String userId;
    private Date timestamp;
    private String previousZone;
    private String newZone;
    private int pefValue;
    private int personalBest;
    private int percentage;

    public ZoneChangeLog() {
        this.timestamp = new Date();
    }

    public ZoneChangeLog(String userId, String previousZone, String newZone, int pefValue, int personalBest) {
        this.userId = userId;
        this.timestamp = new Date();
        this.previousZone = previousZone;
        this.newZone = newZone;
        this.pefValue = pefValue;
        this.personalBest = personalBest;
        this.percentage = personalBest > 0 ? (int) ((pefValue * 100.0) / personalBest) : 0;
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

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getPreviousZone() {
        return previousZone;
    }

    public void setPreviousZone(String previousZone) {
        this.previousZone = previousZone;
    }

    public String getNewZone() {
        return newZone;
    }

    public void setNewZone(String newZone) {
        this.newZone = newZone;
    }

    public int getPefValue() {
        return pefValue;
    }

    public void setPefValue(int pefValue) {
        this.pefValue = pefValue;
    }

    public int getPersonalBest() {
        return personalBest;
    }

    public void setPersonalBest(int personalBest) {
        this.personalBest = personalBest;
    }

    public int getPercentage() {
        return percentage;
    }

    public void setPercentage(int percentage) {
        this.percentage = percentage;
    }
}
