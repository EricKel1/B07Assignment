package com.example.b07project.repository;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class ProviderRepository {

    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    public interface SharingSettingsCallback {
        void onSuccess(boolean rescueLogs, boolean controllerAdherence, boolean symptoms, 
                      boolean triggers, boolean peakFlow, boolean triageIncidents, boolean summaryCharts);
        void onFailure(String error);
    }

    public interface SharedChildrenCallback {
        void onSuccess(List<String> childIds);
        void onFailure(String error);
    }

    /**
     * Fetch sharing settings for a specific child
     */
    public void getProviderSharingSettings(String providerId, String childId, SharingSettingsCallback callback) {
        db.collection("providerSharing")
                .whereEqualTo("providerId", providerId)
                .whereEqualTo("childId", childId)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        QueryDocumentSnapshot doc = (QueryDocumentSnapshot) querySnapshot.getDocuments().get(0);
                        boolean rescueLogs = doc.getBoolean("rescueLogs") != null && doc.getBoolean("rescueLogs");
                        boolean controllerAdherence = doc.getBoolean("controllerAdherence") != null && doc.getBoolean("controllerAdherence");
                        boolean symptoms = doc.getBoolean("symptoms") != null && doc.getBoolean("symptoms");
                        boolean triggers = doc.getBoolean("triggers") != null && doc.getBoolean("triggers");
                        boolean peakFlow = doc.getBoolean("peakFlow") != null && doc.getBoolean("peakFlow");
                        boolean triageIncidents = doc.getBoolean("triageIncidents") != null && doc.getBoolean("triageIncidents");
                        boolean summaryCharts = doc.getBoolean("summaryCharts") != null && doc.getBoolean("summaryCharts");

                        callback.onSuccess(rescueLogs, controllerAdherence, symptoms, triggers, peakFlow, triageIncidents, summaryCharts);
                    } else {
                        callback.onFailure("No sharing settings found");
                    }
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Fetch all children shared with a provider
     */
    public void getSharedChildren(String providerId, SharedChildrenCallback callback) {
        db.collection("providerSharing")
                .whereEqualTo("providerId", providerId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> childIds = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String childId = doc.getString("childId");
                        if (childId != null) {
                            childIds.add(childId);
                        }
                    }
                    callback.onSuccess(childIds);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    /**
     * Fetch rescue inhaler logs for a child (if shared)
     */
    public void getRescueInhalerLogs(String childId, int limitDays, RescueInhalerRepository.LoadCallback<List<com.example.b07project.models.RescueInhalerLog>> callback) {
        new RescueInhalerRepository().getRescueLogsForProvider(childId, limitDays, callback);
    }

    /**
     * Fetch symptom entries for a child (if shared)
     */
    public void getSymptomEntries(String childId, int limitDays, SymptomCheckInRepository.LoadCallback<List<com.example.b07project.models.SymptomCheckIn>> callback) {
        new SymptomCheckInRepository().getSymptomEntriesForProvider(childId, limitDays, callback);
    }

    /**
     * Fetch PEF readings for a child (if shared)
     */
    public void getPEFReadings(String childId, int limitDays, PEFRepository.LoadCallback<List<com.example.b07project.models.PEFReading>> callback) {
        new PEFRepository().getPEFReadingsForProvider(childId, limitDays, callback);
    }

    /**
     * Fetch triage incidents for a child (if shared)
     */
    public void getTriageIncidents(String childId, int limitDays, TriageRepository.LoadCallback<List<com.example.b07project.models.TriageSession>> callback) {
        new TriageRepository().getTriageSessionsForProvider(childId, limitDays, callback);
    }
}
