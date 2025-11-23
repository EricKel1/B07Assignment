package com.example.b07project.repository;

import com.example.b07project.models.SymptomCheckIn;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SymptomCheckInRepository {
    private final FirebaseFirestore db;
    private static final String COLLECTION_NAME = "symptom_checkins";

    public SymptomCheckInRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public interface SaveCallback {
        void onSuccess(String documentId);
        void onFailure(String error);
    }

    public interface LoadCallback {
        void onSuccess(List<SymptomCheckIn> checkIns);
        void onFailure(String error);
    }

    public interface CheckInExistsCallback {
        void onResult(boolean exists, SymptomCheckIn existingCheckIn);
        void onFailure(String error);
    }

    public void saveCheckIn(SymptomCheckIn checkIn, SaveCallback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", checkIn.getUserId());
        data.put("date", checkIn.getDate());
        data.put("symptomLevel", checkIn.getSymptomLevel());
        data.put("symptoms", checkIn.getSymptoms());
        data.put("triggers", checkIn.getTriggers());
        data.put("notes", checkIn.getNotes());
        data.put("timestamp", checkIn.getTimestamp());
        data.put("enteredBy", checkIn.getEnteredBy());

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

    public void getCheckInsForUser(String userId, LoadCallback callback) {
        android.util.Log.d("childparentlink", "Repo: getCheckInsForUser for user: " + userId);
        db.collection(COLLECTION_NAME)
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                android.util.Log.d("childparentlink", "Repo: Found " + queryDocumentSnapshots.size() + " check-ins for user: " + userId);
                List<SymptomCheckIn> checkIns = new ArrayList<>();
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    SymptomCheckIn checkIn = new SymptomCheckIn();
                    checkIn.setId(document.getId());
                    checkIn.setUserId(document.getString("userId"));
                    checkIn.setDate(document.getDate("date"));
                    Long symptomLevelLong = document.getLong("symptomLevel");
                    checkIn.setSymptomLevel(symptomLevelLong != null ? symptomLevelLong.intValue() : 1);
                    checkIn.setSymptoms((List<String>) document.get("symptoms"));
                    checkIn.setTriggers((List<String>) document.get("triggers"));
                    checkIn.setNotes(document.getString("notes"));
                    checkIn.setTimestamp(document.getDate("timestamp"));
                    checkIn.setEnteredBy(document.getString("enteredBy"));
                    checkIns.add(checkIn);
                }
                // Sort by date in memory (newest first)
                checkIns.sort((a, b) -> {
                    if (a.getDate() == null) return 1;
                    if (b.getDate() == null) return -1;
                    return b.getDate().compareTo(a.getDate());
                });
                if (callback != null) {
                    callback.onSuccess(checkIns);
                }
            })
            .addOnFailureListener(e -> {
                android.util.Log.e("childparentlink", "Repo: Error fetching check-ins", e);
                if (callback != null) {
                    callback.onFailure(e.getMessage());
                }
            });
    }

    public void checkIfCheckInExistsForDate(String userId, Date date, CheckInExistsCallback callback) {
        // Get start and end of day
        Calendar startCal = Calendar.getInstance();
        startCal.setTime(date);
        startCal.set(Calendar.HOUR_OF_DAY, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.SECOND, 0);
        startCal.set(Calendar.MILLISECOND, 0);
        Date startOfDay = startCal.getTime();

        Calendar endCal = Calendar.getInstance();
        endCal.setTime(date);
        endCal.set(Calendar.HOUR_OF_DAY, 23);
        endCal.set(Calendar.MINUTE, 59);
        endCal.set(Calendar.SECOND, 59);
        endCal.set(Calendar.MILLISECOND, 999);
        Date endOfDay = endCal.getTime();

        db.collection(COLLECTION_NAME)
            .whereEqualTo("userId", userId)
            .whereGreaterThanOrEqualTo("date", startOfDay)
            .whereLessThanOrEqualTo("date", endOfDay)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (!queryDocumentSnapshots.isEmpty()) {
                    QueryDocumentSnapshot document = (QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments().get(0);
                    SymptomCheckIn checkIn = new SymptomCheckIn();
                    checkIn.setId(document.getId());
                    checkIn.setUserId(document.getString("userId"));
                    checkIn.setDate(document.getDate("date"));
                    Long symptomLevelLong = document.getLong("symptomLevel");
                    checkIn.setSymptomLevel(symptomLevelLong != null ? symptomLevelLong.intValue() : 1);
                    checkIn.setSymptoms((List<String>) document.get("symptoms"));
                    checkIn.setTriggers((List<String>) document.get("triggers"));
                    checkIn.setNotes(document.getString("notes"));
                    checkIn.setTimestamp(document.getDate("timestamp"));
                    checkIn.setEnteredBy(document.getString("enteredBy"));
                    if (callback != null) {
                        callback.onResult(true, checkIn);
                    }
                } else {
                    if (callback != null) {
                        callback.onResult(false, null);
                    }
                }
            })
            .addOnFailureListener(e -> {
                if (callback != null) {
                    callback.onFailure(e.getMessage());
                }
            });
    }
}
