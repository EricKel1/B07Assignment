package com.example.b07project.models;

import java.util.Date;
import java.util.List;

public class MedicineLog {
    private String id;
    private String userId;
    private Date timestamp;
    private int doseCount;
    private List<String> triggers;
    private String notes;
    private String enteredBy;

    public MedicineLog() {
    }

    public MedicineLog(String userId, Date timestamp, int doseCount, List<String> triggers, String notes) {
        this.userId = userId;
        this.timestamp = timestamp;
        this.doseCount = doseCount;
        this.triggers = triggers;
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

    public List<String> getTriggers() {
        return triggers;
    }

    public void setTriggers(List<String> triggers) {
        this.triggers = triggers;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getEnteredBy() {
        return enteredBy;
    }

    public void setEnteredBy(String enteredBy) {
        this.enteredBy = enteredBy;
    }
}
