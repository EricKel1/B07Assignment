package com.example.b07project.repository;

import com.example.b07project.models.MedicationSchedule;
import com.google.firebase.firestore.FirebaseFirestore;

public class ScheduleRepository {
    private final FirebaseFirestore db;

    public ScheduleRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public interface SaveCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public interface LoadCallback {
        void onSuccess(MedicationSchedule schedule);
        void onFailure(String error);
    }

    public void saveSchedule(String childId, MedicationSchedule schedule, SaveCallback callback) {
        db.collection("medication_schedules").document(childId)
                .set(schedule)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void getSchedule(String childId, LoadCallback callback) {
        db.collection("medication_schedules").document(childId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        MedicationSchedule schedule = documentSnapshot.toObject(MedicationSchedule.class);
                        callback.onSuccess(schedule);
                    } else {
                        callback.onSuccess(null); // No schedule set yet
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }
}
