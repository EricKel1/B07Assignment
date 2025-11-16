package com.example.b07project.repository;

import com.example.b07project.models.ControllerMedicineLog;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ControllerMedicineRepository {
    private final FirebaseFirestore db;
    private static final String COLLECTION_NAME = "controller_medicine_logs";

    public ControllerMedicineRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public interface SaveCallback {
        void onSuccess(String documentId);
        void onFailure(String error);
    }

    public interface LoadCallback {
        void onSuccess(List<ControllerMedicineLog> logs);
        void onFailure(String error);
    }

    public void saveLog(ControllerMedicineLog log, SaveCallback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", log.getUserId());
        data.put("timestamp", log.getTimestamp());
        data.put("doseCount", log.getDoseCount());
        data.put("scheduledTime", log.getScheduledTime());
        data.put("takenOnTime", log.isTakenOnTime());
        data.put("triggers", log.getTriggers());
        data.put("notes", log.getNotes());

        db.collection(COLLECTION_NAME)
            .add(data)
            .addOnSuccessListener(documentReference -> {
                if (callback != null) {
                    callback.onSuccess(documentReference.getId());
                }
            })
            .addOnFailureListener(e -> {
                if (callback != null) {
                    callback.onFailure(e.getMessage());
                }
            });
    }

    public void getLogsForUser(String userId, LoadCallback callback) {
        db.collection(COLLECTION_NAME)
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<ControllerMedicineLog> logs = new ArrayList<>();
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    ControllerMedicineLog log = new ControllerMedicineLog();
                    log.setId(document.getId());
                    log.setUserId(document.getString("userId"));
                    log.setTimestamp(document.getDate("timestamp"));
                    Long doseCountLong = document.getLong("doseCount");
                    log.setDoseCount(doseCountLong != null ? doseCountLong.intValue() : 0);
                    log.setScheduledTime(document.getDate("scheduledTime"));
                    Boolean takenOnTime = document.getBoolean("takenOnTime");
                    log.setTakenOnTime(takenOnTime != null ? takenOnTime : false);
                    log.setTriggers((List<String>) document.get("triggers"));
                    log.setNotes(document.getString("notes"));
                    logs.add(log);
                }
                // Sort by timestamp in memory (newest first)
                logs.sort((a, b) -> {
                    if (a.getTimestamp() == null) return 1;
                    if (b.getTimestamp() == null) return -1;
                    return b.getTimestamp().compareTo(a.getTimestamp());
                });
                if (callback != null) {
                    callback.onSuccess(logs);
                }
            })
            .addOnFailureListener(e -> {
                if (callback != null) {
                    callback.onFailure(e.getMessage());
                }
            });
    }

    public void getLogsForUserInDateRange(String userId, Date startDate, Date endDate, LoadCallback callback) {
        db.collection(COLLECTION_NAME)
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<ControllerMedicineLog> logs = new ArrayList<>();
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    Date timestamp = document.getDate("timestamp");
                    if (timestamp != null && 
                        !timestamp.before(startDate) && 
                        !timestamp.after(endDate)) {
                        
                        ControllerMedicineLog log = new ControllerMedicineLog();
                        log.setId(document.getId());
                        log.setUserId(document.getString("userId"));
                        log.setTimestamp(timestamp);
                        Long doseCountLong = document.getLong("doseCount");
                        log.setDoseCount(doseCountLong != null ? doseCountLong.intValue() : 0);
                        log.setScheduledTime(document.getDate("scheduledTime"));
                        Boolean takenOnTime = document.getBoolean("takenOnTime");
                        log.setTakenOnTime(takenOnTime != null ? takenOnTime : false);
                        log.setTriggers((List<String>) document.get("triggers"));
                        log.setNotes(document.getString("notes"));
                        logs.add(log);
                    }
                }
                // Sort by timestamp (newest first)
                logs.sort((a, b) -> {
                    if (a.getTimestamp() == null) return 1;
                    if (b.getTimestamp() == null) return -1;
                    return b.getTimestamp().compareTo(a.getTimestamp());
                });
                if (callback != null) {
                    callback.onSuccess(logs);
                }
            })
            .addOnFailureListener(e -> {
                if (callback != null) {
                    callback.onFailure(e.getMessage());
                }
            });
    }
}
