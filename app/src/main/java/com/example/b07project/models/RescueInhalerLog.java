package com.example.b07project.models;

import java.util.Date;

public class RescueInhalerLog {
    private String id;
    private String userId;
    private Date timestamp;
    private int doseCount;
    private String notes;

    public RescueInhalerLog() {
    }

    public RescueInhalerLog(String userId, Date timestamp, int doseCount, String notes) {
        this.userId = userId;
        this.timestamp = timestamp;
        this.doseCount = doseCount;
        this.notes = notes;
    }

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

    public int getDoseCount() {
        return doseCount;
    }

    public void setDoseCount(int doseCount) {
        this.doseCount = doseCount;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
