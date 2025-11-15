package com.example.b07project.models;

import java.util.Date;

public class ControllerMedicineLog extends MedicineLog {
    private Date scheduledTime;
    private boolean takenOnTime;

    public ControllerMedicineLog() {
        super();
    }

    public ControllerMedicineLog(String userId, Date timestamp, int doseCount, Date scheduledTime, boolean takenOnTime, String notes) {
        super(userId, timestamp, doseCount, notes);
        this.scheduledTime = scheduledTime;
        this.takenOnTime = takenOnTime;
    }

    public Date getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(Date scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public boolean isTakenOnTime() {
        return takenOnTime;
    }

    public void setTakenOnTime(boolean takenOnTime) {
        this.takenOnTime = takenOnTime;
    }
}
