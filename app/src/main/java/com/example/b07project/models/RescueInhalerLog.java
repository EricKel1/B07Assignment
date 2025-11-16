package com.example.b07project.models;

import java.util.Date;
import java.util.List;

public class RescueInhalerLog extends MedicineLog {

    public RescueInhalerLog() {
        super();
    }

    public RescueInhalerLog(String userId, Date timestamp, int doseCount, List<String> triggers, String notes) {
        super(userId, timestamp, doseCount, triggers, notes);
    }
}
