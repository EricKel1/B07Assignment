package com.example.b07project.repository;

import com.example.b07project.models.MedicineLog;
import com.example.b07project.models.SymptomCheckIn;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TriggerAnalyticsRepository {
    private final FirebaseFirestore db;
    private final RescueInhalerRepository rescueRepository;
    private final ControllerMedicineRepository controllerRepository;
    private final SymptomCheckInRepository symptomRepository;

    public TriggerAnalyticsRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.rescueRepository = new RescueInhalerRepository();
        this.controllerRepository = new ControllerMedicineRepository();
        this.symptomRepository = new SymptomCheckInRepository();
    }

    public interface TriggerStatsCallback {
        void onSuccess(Map<String, TriggerStats> triggerStats);
        void onFailure(String error);
    }

    public interface CorrelationCallback {
        void onSuccess(Map<String, List<Integer>> triggerSeverityMap);
        void onFailure(String error);
    }

    public static class TriggerStats {
        public String triggerName;
        public int totalCount;
        public int medicineUseCount;
        public int symptomCheckInCount;
        public double averageSeverity;
        public List<Date> occurrences;

        public TriggerStats(String triggerName) {
            this.triggerName = triggerName;
            this.totalCount = 0;
            this.medicineUseCount = 0;
            this.symptomCheckInCount = 0;
            this.averageSeverity = 0.0;
            this.occurrences = new ArrayList<>();
        }
    }

    public void getTriggerStatistics(String userId, Date startDate, Date endDate, TriggerStatsCallback callback) {
        Map<String, TriggerStats> statsMap = new HashMap<>();
        
        // Initialize stats for common triggers
        String[] commonTriggers = {"exercise", "cold air", "pets", "pollen", "stress", "smoke", "weather change", "dust"};
        for (String trigger : commonTriggers) {
            statsMap.put(trigger, new TriggerStats(trigger));
        }

        // Query medicine logs
        db.collection("rescue_inhaler_logs")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener(rescueSnapshots -> {
                for (QueryDocumentSnapshot doc : rescueSnapshots) {
                    Date timestamp = doc.getDate("timestamp");
                    if (timestamp != null && !timestamp.before(startDate) && !timestamp.after(endDate)) {
                        List<String> triggers = (List<String>) doc.get("triggers");
                        if (triggers != null) {
                            for (String trigger : triggers) {
                                TriggerStats stats = statsMap.get(trigger);
                                if (stats != null) {
                                    stats.totalCount++;
                                    stats.medicineUseCount++;
                                    stats.occurrences.add(timestamp);
                                }
                            }
                        }
                    }
                }

                // Query controller medicine logs
                db.collection("controller_medicine_logs")
                    .whereEqualTo("userId", userId)
                    .get()
                    .addOnSuccessListener(controllerSnapshots -> {
                        for (QueryDocumentSnapshot doc : controllerSnapshots) {
                            Date timestamp = doc.getDate("timestamp");
                            if (timestamp != null && !timestamp.before(startDate) && !timestamp.after(endDate)) {
                                List<String> triggers = (List<String>) doc.get("triggers");
                                if (triggers != null) {
                                    for (String trigger : triggers) {
                                        TriggerStats stats = statsMap.get(trigger);
                                        if (stats != null) {
                                            stats.totalCount++;
                                            stats.medicineUseCount++;
                                            stats.occurrences.add(timestamp);
                                        }
                                    }
                                }
                            }
                        }

                        // Query symptom check-ins
                        db.collection("symptom_checkins")
                            .whereEqualTo("userId", userId)
                            .get()
                            .addOnSuccessListener(symptomSnapshots -> {
                                Map<String, List<Integer>> severityMap = new HashMap<>();
                                for (String trigger : commonTriggers) {
                                    severityMap.put(trigger, new ArrayList<>());
                                }

                                for (QueryDocumentSnapshot doc : symptomSnapshots) {
                                    Date date = doc.getDate("date");
                                    if (date != null && !date.before(startDate) && !date.after(endDate)) {
                                        List<String> triggers = (List<String>) doc.get("triggers");
                                        Long severityLong = doc.getLong("symptomLevel");
                                        int severity = severityLong != null ? severityLong.intValue() : 0;

                                        if (triggers != null) {
                                            for (String trigger : triggers) {
                                                TriggerStats stats = statsMap.get(trigger);
                                                if (stats != null) {
                                                    stats.totalCount++;
                                                    stats.symptomCheckInCount++;
                                                    stats.occurrences.add(date);
                                                    severityMap.get(trigger).add(severity);
                                                }
                                            }
                                        }
                                    }
                                }

                                // Calculate average severity
                                for (Map.Entry<String, TriggerStats> entry : statsMap.entrySet()) {
                                    List<Integer> severities = severityMap.get(entry.getKey());
                                    if (severities != null && !severities.isEmpty()) {
                                        double sum = 0;
                                        for (int sev : severities) {
                                            sum += sev;
                                        }
                                        entry.getValue().averageSeverity = sum / severities.size();
                                    }
                                }

                                if (callback != null) {
                                    callback.onSuccess(statsMap);
                                }
                            })
                            .addOnFailureListener(e -> {
                                if (callback != null) {
                                    callback.onFailure(e.getMessage());
                                }
                            });
                    })
                    .addOnFailureListener(e -> {
                        if (callback != null) {
                            callback.onFailure(e.getMessage());
                        }
                    });
            })
            .addOnFailureListener(e -> {
                if (callback != null) {
                    callback.onFailure(e.getMessage());
                }
            });
    }

    public void getTriggerSeverityCorrelation(String userId, Date startDate, Date endDate, CorrelationCallback callback) {
        Map<String, List<Integer>> correlationMap = new HashMap<>();
        
        String[] commonTriggers = {"exercise", "cold air", "pets", "pollen", "stress", "smoke", "weather change", "dust"};
        for (String trigger : commonTriggers) {
            correlationMap.put(trigger, new ArrayList<>());
        }

        db.collection("symptom_checkins")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener(snapshots -> {
                for (QueryDocumentSnapshot doc : snapshots) {
                    Date date = doc.getDate("date");
                    if (date != null && !date.before(startDate) && !date.after(endDate)) {
                        List<String> triggers = (List<String>) doc.get("triggers");
                        Long severityLong = doc.getLong("symptomLevel");
                        int severity = severityLong != null ? severityLong.intValue() : 0;

                        if (triggers != null) {
                            for (String trigger : triggers) {
                                List<Integer> severities = correlationMap.get(trigger);
                                if (severities != null) {
                                    severities.add(severity);
                                }
                            }
                        }
                    }
                }

                if (callback != null) {
                    callback.onSuccess(correlationMap);
                }
            })
            .addOnFailureListener(e -> {
                if (callback != null) {
                    callback.onFailure(e.getMessage());
                }
            });
    }

    public static Date getStartOfMonth() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    public static Date getStartOfWeek() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }
}
