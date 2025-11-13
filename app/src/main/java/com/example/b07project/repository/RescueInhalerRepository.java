package com.example.b07project.repository;

import com.example.b07project.models.RescueInhalerLog;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RescueInhalerRepository {
    private final FirebaseFirestore db;
    private static final String COLLECTION_NAME = "rescue_inhaler_logs";

    public RescueInhalerRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public interface SaveCallback {
        void onSuccess(String documentId);
        void onFailure(String error);
    }

    public interface LoadCallback {
        void onSuccess(List<RescueInhalerLog> logs);
        void onFailure(String error);
    }

    public void saveLog(RescueInhalerLog log, SaveCallback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", log.getUserId());
        data.put("timestamp", log.getTimestamp());
        data.put("doseCount", log.getDoseCount());
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
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<RescueInhalerLog> logs = new ArrayList<>();
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    RescueInhalerLog log = new RescueInhalerLog();
                    log.setId(document.getId());
                    log.setUserId(document.getString("userId"));
                    log.setTimestamp(document.getDate("timestamp"));
                    Long doseCountLong = document.getLong("doseCount");
                    log.setDoseCount(doseCountLong != null ? doseCountLong.intValue() : 0);
                    log.setNotes(document.getString("notes"));
                    logs.add(log);
                }
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
            .whereGreaterThanOrEqualTo("timestamp", startDate)
            .whereLessThanOrEqualTo("timestamp", endDate)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<RescueInhalerLog> logs = new ArrayList<>();
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    RescueInhalerLog log = new RescueInhalerLog();
                    log.setId(document.getId());
                    log.setUserId(document.getString("userId"));
                    log.setTimestamp(document.getDate("timestamp"));
                    Long doseCountLong = document.getLong("doseCount");
                    log.setDoseCount(doseCountLong != null ? doseCountLong.intValue() : 0);
                    log.setNotes(document.getString("notes"));
                    logs.add(log);
                }
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
