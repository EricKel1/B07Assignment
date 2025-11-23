package com.example.b07project.models;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MedicationSchedule {
    private String userId;
    private String medicationName;
    private int dosagePerIntake; // e.g., 2 puffs
    private int frequency; // times per day
    private List<String> scheduledTimes; // e.g., "08:00", "20:00"
    private Date startDate;

    public MedicationSchedule() {
        this.scheduledTimes = new ArrayList<>();
        this.startDate = new Date();
    }

    public MedicationSchedule(String userId, String medicationName, int dosagePerIntake, int frequency, List<String> scheduledTimes) {
        this.userId = userId;
        this.medicationName = medicationName;
        this.dosagePerIntake = dosagePerIntake;
        this.frequency = frequency;
        this.scheduledTimes = scheduledTimes;
        this.startDate = new Date();
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getMedicationName() {
        return medicationName;
    }

    public void setMedicationName(String medicationName) {
        this.medicationName = medicationName;
    }

    public int getDosagePerIntake() {
        return dosagePerIntake;
    }

    public void setDosagePerIntake(int dosagePerIntake) {
        this.dosagePerIntake = dosagePerIntake;
    }

    public int getFrequency() {
        return frequency;
    }

    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }

    public List<String> getScheduledTimes() {
        return scheduledTimes;
    }

    public void setScheduledTimes(List<String> scheduledTimes) {
        this.scheduledTimes = scheduledTimes;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }
}
