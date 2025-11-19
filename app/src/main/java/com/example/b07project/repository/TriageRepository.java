package com.example.b07project.repository;

import com.example.b07project.models.RescueInhalerLog;
import com.example.b07project.models.TriageSession;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TriageRepository {
    private final FirebaseFirestore db;
    private static final String TRIAGE_COLLECTION = "triage_sessions";
    private static final String RESCUE_COLLECTION = "rescue_inhaler_logs";
    private static final long THREE_HOURS_MS = 3 * 60 * 60 * 1000;

    public TriageRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public interface SaveCallback {
        void onSuccess(String documentId);
        void onFailure(String error);
    }

    public interface LoadCallback<T> {
        void onSuccess(T data);
        void onFailure(String error);
    }

    // Save triage session
    public void saveTriageSession(TriageSession session, SaveCallback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", session.getUserId());
        data.put("startTime", session.getStartTime());
        data.put("endTime", session.getEndTime());
        data.put("cantSpeakFullSentences", session.isCantSpeakFullSentences());
        data.put("chestPullingInRetractions", session.isChestPullingInRetractions());
        data.put("blueGrayLipsNails", session.isBlueGrayLipsNails());
        data.put("rescueAttemptsLast3Hours", session.getRescueAttemptsLast3Hours());
        data.put("currentPEF", session.getCurrentPEF());
        data.put("currentZone", session.getCurrentZone());
        data.put("decision", session.getDecision());
        data.put("guidanceShown", session.getGuidanceShown());
        data.put("actionSteps", session.getActionSteps());
        data.put("timerStarted", session.isTimerStarted());
        data.put("timerEndTime", session.getTimerEndTime());
        data.put("userImproved", session.isUserImproved());
        data.put("escalated", session.isEscalated());
        data.put("escalationReason", session.getEscalationReason());
        data.put("userResponse", session.getUserResponse());
        data.put("responseTime", session.getResponseTime());
        data.put("parentAlerted", session.isParentAlerted());

        if (session.getId() == null || session.getId().isEmpty()) {
            // Create new
            db.collection(TRIAGE_COLLECTION)
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
        } else {
            // Update existing
            db.collection(TRIAGE_COLLECTION)
                .document(session.getId())
                .set(data)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) {
                        callback.onSuccess(session.getId());
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onFailure(e.getMessage());
                    }
                });
        }
    }

    // Get triage sessions for user
    public void getTriageSessions(String userId, LoadCallback<List<TriageSession>> callback) {
        db.collection(TRIAGE_COLLECTION)
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener(snapshots -> {
                List<TriageSession> sessions = new ArrayList<>();
                for (QueryDocumentSnapshot doc : snapshots) {
                    TriageSession session = documentToTriageSession(doc);
                    sessions.add(session);
                }
                
                // Sort by startTime in descending order (most recent first)
                sessions.sort((s1, s2) -> {
                    if (s1.getStartTime() == null || s2.getStartTime() == null) return 0;
                    return s2.getStartTime().compareTo(s1.getStartTime());
                });
                
                if (callback != null) {
                    callback.onSuccess(sessions);
                }
            })
            .addOnFailureListener(e -> {
                if (callback != null) {
                    callback.onFailure(e.getMessage());
                }
            });
    }

    // Get single triage session by ID
    public void getTriageSession(String sessionId, LoadCallback<TriageSession> callback) {
        db.collection(TRIAGE_COLLECTION)
            .document(sessionId)
            .get()
            .addOnSuccessListener(doc -> {
                if (doc.exists()) {
                    TriageSession session = documentToTriageSession(doc);
                    if (callback != null) {
                        callback.onSuccess(session);
                    }
                } else {
                    if (callback != null) {
                        callback.onFailure("Session not found");
                    }
                }
            })
            .addOnFailureListener(e -> {
                if (callback != null) {
                    callback.onFailure(e.getMessage());
                }
            });
    }

    // Check for rapid rescue alert (≥3 uses in last 3 hours)
    public void checkRapidRescueAlert(String userId, LoadCallback<Boolean> callback) {
        Date threeHoursAgo = new Date(System.currentTimeMillis() - THREE_HOURS_MS);
        
        db.collection(RESCUE_COLLECTION)
            .whereEqualTo("userId", userId)
            .whereGreaterThanOrEqualTo("timestamp", threeHoursAgo)
            .get()
            .addOnSuccessListener(snapshots -> {
                int count = snapshots.size();
                if (callback != null) {
                    callback.onSuccess(count >= 3);
                }
            })
            .addOnFailureListener(e -> {
                if (callback != null) {
                    callback.onFailure(e.getMessage());
                }
            });
    }

    // Get count of rescue uses in last 3 hours
    public void getRescueUsesLast3Hours(String userId, LoadCallback<Integer> callback) {
        Date threeHoursAgo = new Date(System.currentTimeMillis() - THREE_HOURS_MS);
        
        db.collection(RESCUE_COLLECTION)
            .whereEqualTo("userId", userId)
            .whereGreaterThanOrEqualTo("timestamp", threeHoursAgo)
            .get()
            .addOnSuccessListener(snapshots -> {
                if (callback != null) {
                    callback.onSuccess(snapshots.size());
                }
            })
            .addOnFailureListener(e -> {
                if (callback != null) {
                    callback.onFailure(e.getMessage());
                }
            });
    }

    // Helper method to convert Firestore document to TriageSession
    private TriageSession documentToTriageSession(com.google.firebase.firestore.DocumentSnapshot doc) {
        TriageSession session = new TriageSession();
        session.setId(doc.getId());
        session.setUserId(doc.getString("userId"));
        session.setStartTime(doc.getDate("startTime"));
        session.setEndTime(doc.getDate("endTime"));
        
        Boolean cantSpeak = doc.getBoolean("cantSpeakFullSentences");
        session.setCantSpeakFullSentences(cantSpeak != null && cantSpeak);
        
        Boolean chestPulling = doc.getBoolean("chestPullingInRetractions");
        session.setChestPullingInRetractions(chestPulling != null && chestPulling);
        
        Boolean blueLips = doc.getBoolean("blueGrayLipsNails");
        session.setBlueGrayLipsNails(blueLips != null && blueLips);
        
        Long rescueAttempts = doc.getLong("rescueAttemptsLast3Hours");
        session.setRescueAttemptsLast3Hours(rescueAttempts != null ? rescueAttempts.intValue() : 0);
        
        Long pef = doc.getLong("currentPEF");
        session.setCurrentPEF(pef != null ? pef.intValue() : null);
        
        session.setCurrentZone(doc.getString("currentZone"));
        session.setDecision(doc.getString("decision"));
        session.setGuidanceShown(doc.getString("guidanceShown"));
        
        List<String> steps = (List<String>) doc.get("actionSteps");
        session.setActionSteps(steps);
        
        Boolean timerStarted = doc.getBoolean("timerStarted");
        session.setTimerStarted(timerStarted != null && timerStarted);
        
        session.setTimerEndTime(doc.getDate("timerEndTime"));
        
        Boolean improved = doc.getBoolean("userImproved");
        session.setUserImproved(improved != null && improved);
        
        Boolean escalated = doc.getBoolean("escalated");
        session.setEscalated(escalated != null && escalated);
        
        session.setEscalationReason(doc.getString("escalationReason"));
        session.setUserResponse(doc.getString("userResponse"));
        session.setResponseTime(doc.getDate("responseTime"));
        
        Boolean alerted = doc.getBoolean("parentAlerted");
        session.setParentAlerted(alerted != null && alerted);
        
        return session;
    }
}
