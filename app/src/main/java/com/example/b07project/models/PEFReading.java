package com.example.b07project.models;

import java.util.Date;

public class PEFReading {
    private String id;
    private String userId;
    private Date timestamp;
    private int value; // Peak Expiratory Flow in L/min
    private boolean isPreMedication;
    private boolean isPostMedication;
    private String notes;
    private String zone; // "green", "yellow", "red"
    private int personalBest; // PB value at time of reading

    public PEFReading() {
        this.timestamp = new Date();
    }

    public PEFReading(String userId, int value, boolean isPreMedication, boolean isPostMedication, String notes) {
        this.userId = userId;
        this.timestamp = new Date();
        this.value = value;
        this.isPreMedication = isPreMedication;
        this.isPostMedication = isPostMedication;
        this.notes = notes;
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

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public boolean isPreMedication() {
        return isPreMedication;
    }

    public void setPreMedication(boolean preMedication) {
        isPreMedication = preMedication;
    }

    public boolean isPostMedication() {
        return isPostMedication;
    }

    public void setPostMedication(boolean postMedication) {
        isPostMedication = postMedication;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public int getPersonalBest() {
        return personalBest;
    }

    public void setPersonalBest(int personalBest) {
        this.personalBest = personalBest;
    }

    public int getPercentageOfPB() {
        if (personalBest > 0) {
            return (int) ((value * 100.0) / personalBest);
        }
        return 0;
    }
}
