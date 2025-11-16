package com.example.b07project.models;

import java.util.Date;
import java.util.List;

public class SymptomCheckIn {
    private String id;
    private String userId;
    private Date date;
    private int symptomLevel; // 1-5 scale
    private List<String> symptoms; // wheezing, coughing, shortness of breath, chest tightness, nighttime symptoms
    private List<String> triggers; // exercise, cold air, pets, pollen, stress, smoke, weather change, dust
    private String notes;
    private Date timestamp;

    public SymptomCheckIn() {
    }

    public SymptomCheckIn(String userId, Date date, int symptomLevel, List<String> symptoms, List<String> triggers, String notes, Date timestamp) {
        this.userId = userId;
        this.date = date;
        this.symptomLevel = symptomLevel;
        this.symptoms = symptoms;
        this.triggers = triggers;
        this.notes = notes;
        this.timestamp = timestamp;
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

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public int getSymptomLevel() {
        return symptomLevel;
    }

    public void setSymptomLevel(int symptomLevel) {
        this.symptomLevel = symptomLevel;
    }

    public List<String> getSymptoms() {
        return symptoms;
    }

    public void setSymptoms(List<String> symptoms) {
        this.symptoms = symptoms;
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

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
}
