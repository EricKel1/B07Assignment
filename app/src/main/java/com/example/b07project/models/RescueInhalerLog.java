package com.example.b07project.models;

import java.util.Date;

public class RescueInhalerLog extends MedicineLog {

    public RescueInhalerLog() {
        super();
    }

    public RescueInhalerLog(String userId, Date timestamp, int doseCount, String notes) {
        super(userId, timestamp, doseCount, notes);
    }
}
