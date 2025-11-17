package com.example.b07project.services;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.b07project.models.Badge;
import com.example.b07project.models.Streak;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MotivationService {
    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private final Context context;
    private static final String PREFS_NAME = "MotivationPrefs";

    // Default thresholds
    private static final int DEFAULT_TECHNIQUE_SESSIONS_REQUIRED = 10;
    private static final int DEFAULT_LOW_RESCUE_THRESHOLD = 4;
    private static final int DEFAULT_LOW_RESCUE_PERIOD = 30;

    public MotivationService(Context context) {
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    // Streak Methods
    public interface StreakCallback {
        void onSuccess(Streak streak);
        void onFailure(Exception e);
    }

    public interface BadgeListCallback {
        void onSuccess(List<Badge> badges);
        void onFailure(Exception e);
    }

    public void getStreak(String type, StreakCallback callback) {
        String userId = auth.getCurrentUser().getUid();
        db.collection("streaks")
                .whereEqualTo("userId", userId)
                .whereEqualTo("type", type)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                        Streak streak = doc.toObject(Streak.class);
                        // Ensure ID is set from document
                        if (streak != null) {
                            streak.setId(doc.getId());
                            callback.onSuccess(streak);
                        } else {
                            callback.onFailure(new Exception("Failed to parse streak"));
                        }
                    } else {
                        // Create new streak with generated ID
                        String newId = db.collection("streaks").document().getId();
                        Streak newStreak = new Streak(
                                newId,
                                userId,
                                type,
                                0,
                                0,
                                0  // Set lastUpdated to 0 so first update will trigger
                        );
                        callback.onSuccess(newStreak);
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    public void updateControllerStreak(UpdateCallback callback) {
        getStreak("controller", new StreakCallback() {
            @Override
            public void onSuccess(Streak streak) {
                android.util.Log.d("MotivationService", "updateControllerStreak - Got streak: " + 
                    streak.getId() + ", current=" + streak.getCurrentStreak() + 
                    ", lastUpdated=" + streak.getLastUpdated());
                
                long today = getStartOfDay(System.currentTimeMillis());
                long lastUpdated = getStartOfDay(streak.getLastUpdated());
                long daysDiff = TimeUnit.MILLISECONDS.toDays(today - lastUpdated);

                android.util.Log.d("MotivationService", "updateControllerStreak - daysDiff=" + daysDiff + 
                    ", currentStreak=" + streak.getCurrentStreak());

                // Update if: it's a new day OR it's the first time (currentStreak == 0 and lastUpdated == 0)
                boolean isFirstTime = (streak.getCurrentStreak() == 0 && streak.getLastUpdated() == 0);
                
                if (daysDiff >= 1 || isFirstTime) {
                    if (daysDiff == 1 && !isFirstTime) {
                        // Continue streak (consecutive day)
                        streak.setCurrentStreak(streak.getCurrentStreak() + 1);
                    } else {
                        // Streak broken, or first time - start at 1
                        streak.setCurrentStreak(1);
                    }

                    if (streak.getCurrentStreak() > streak.getLongestStreak()) {
                        streak.setLongestStreak(streak.getCurrentStreak());
                    }

                    streak.setLastUpdated(System.currentTimeMillis());
                    
                    android.util.Log.d("MotivationService", "updateControllerStreak - Saving streak: current=" + 
                        streak.getCurrentStreak() + ", longest=" + streak.getLongestStreak());
                    
                    saveStreak(streak, callback);
                    checkBadgesAfterStreakUpdate(streak);
                } else {
                    // Already updated today, just complete callback
                    android.util.Log.d("MotivationService", "updateControllerStreak - Already updated today");
                    callback.onComplete();
                }
            }

            @Override
            public void onFailure(Exception e) {
                android.util.Log.e("MotivationService", "updateControllerStreak - Failed to get streak", e);
                callback.onComplete();
            }
        });
    }

    public void updateTechniqueStreak(UpdateCallback callback) {
        getStreak("technique", new StreakCallback() {
            @Override
            public void onSuccess(Streak streak) {
                long today = getStartOfDay(System.currentTimeMillis());
                long lastUpdated = getStartOfDay(streak.getLastUpdated());
                long daysDiff = TimeUnit.MILLISECONDS.toDays(today - lastUpdated);

                // Only update if this is a new day
                if (daysDiff >= 1) {
                    if (daysDiff == 1) {
                        // Continue streak (consecutive day)
                        streak.setCurrentStreak(streak.getCurrentStreak() + 1);
                    } else if (daysDiff > 1 || streak.getCurrentStreak() == 0) {
                        // Streak broken or first time, start at 1
                        streak.setCurrentStreak(1);
                    }

                    if (streak.getCurrentStreak() > streak.getLongestStreak()) {
                        streak.setLongestStreak(streak.getCurrentStreak());
                    }

                    streak.setLastUpdated(System.currentTimeMillis());
                    saveStreak(streak, callback);
                    updateTechniqueSessionBadge();
                } else {
                    // Already updated today, just complete callback
                    callback.onComplete();
                }
            }

            @Override
            public void onFailure(Exception e) {
                callback.onComplete();
            }
        });
    }

    private void saveStreak(Streak streak, UpdateCallback callback) {
        if (streak.getId() == null || streak.getId().isEmpty()) {
            // Generate new ID if not set
            streak.setId(db.collection("streaks").document().getId());
            android.util.Log.d("MotivationService", "saveStreak - Generated new ID: " + streak.getId());
        }
        
        android.util.Log.d("MotivationService", "saveStreak - Saving to Firestore: ID=" + streak.getId() + 
            ", userId=" + streak.getUserId() + ", type=" + streak.getType() + 
            ", current=" + streak.getCurrentStreak() + ", longest=" + streak.getLongestStreak());
        
        Map<String, Object> streakData = new HashMap<>();
        streakData.put("userId", streak.getUserId());
        streakData.put("type", streak.getType());
        streakData.put("currentStreak", streak.getCurrentStreak());
        streakData.put("longestStreak", streak.getLongestStreak());
        streakData.put("lastUpdated", streak.getLastUpdated());

        db.collection("streaks").document(streak.getId())
                .set(streakData)
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("MotivationService", "saveStreak - Successfully saved to Firestore");
                    if (callback != null) callback.onComplete();
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("MotivationService", "saveStreak - Failed to save to Firestore", e);
                    e.printStackTrace();
                    if (callback != null) callback.onComplete();
                });
    }

    // Badge Methods
    public void getAllBadges(BadgeListCallback callback) {
        String userId = auth.getCurrentUser().getUid();
        db.collection("badges")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Badge> badges = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Badge badge = doc.toObject(Badge.class);
                        if (badge != null) {
                            badges.add(badge);
                        }
                    }
                    
                    // Initialize badges if none exist
                    if (badges.isEmpty()) {
                        initializeBadges(callback);
                    } else {
                        callback.onSuccess(badges);
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    private void initializeBadges(BadgeListCallback callback) {
        String userId = auth.getCurrentUser().getUid();
        List<Badge> defaultBadges = new ArrayList<>();

        String perfectWeekId = db.collection("badges").document().getId();
        Badge perfectWeek = new Badge(
                perfectWeekId,
                userId,
                "perfect_controller_week",
                "Perfect Week",
                "Take controller medication every day for 7 consecutive days",
                false,
                0,
                0,
                7
        );
        defaultBadges.add(perfectWeek);

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        int techniqueRequired = prefs.getInt("technique_sessions_required", DEFAULT_TECHNIQUE_SESSIONS_REQUIRED);

        String techniqueMasterId = db.collection("badges").document().getId();
        Badge techniqueMaster = new Badge(
                techniqueMasterId,
                userId,
                "technique_sessions",
                "Technique Master",
                "Complete " + techniqueRequired + " high-quality technique practice sessions",
                false,
                0,
                0,
                techniqueRequired
        );
        defaultBadges.add(techniqueMaster);

        // Save all badges with their pre-assigned IDs
        int[] saveCount = {0};
        for (Badge badge : defaultBadges) {
            saveBadge(badge, () -> {
                saveCount[0]++;
                if (saveCount[0] == defaultBadges.size()) {
                    callback.onSuccess(defaultBadges);
                }
            });
        }
    }

    private void saveBadge(Badge badge, UpdateCallback callback) {
        if (badge.getId() == null || badge.getId().isEmpty()) {
            // Generate new ID if not set
            badge.setId(db.collection("badges").document().getId());
            android.util.Log.d("MotivationService", "saveBadge - Generated new ID: " + badge.getId());
        }
        
        Map<String, Object> badgeData = new HashMap<>();
        badgeData.put("userId", badge.getUserId());
        badgeData.put("type", badge.getType());
        badgeData.put("name", badge.getName());
        badgeData.put("description", badge.getDescription());
        badgeData.put("earned", badge.isEarned());
        badgeData.put("earnedDate", badge.getEarnedDate());
        badgeData.put("progress", badge.getProgress());
        badgeData.put("requirement", badge.getRequirement());

        db.collection("badges").document(badge.getId())
                .set(badgeData)
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onComplete();
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("MotivationService", "saveBadge - Failed to save", e);
                    if (callback != null) callback.onComplete();
                });
    }

    private void checkBadgesAfterStreakUpdate(Streak streak) {
        android.util.Log.d("MotivationService", "checkBadgesAfterStreakUpdate - Called with streak type: " + 
            streak.getType() + ", currentStreak: " + streak.getCurrentStreak());
        
        if (streak.getType().equals("controller")) {
            // Update perfect week badge progress
            String userId = auth.getCurrentUser().getUid();
            android.util.Log.d("MotivationService", "checkBadgesAfterStreakUpdate - Querying for controller badge, userId: " + userId);
            
            db.collection("badges")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("type", "perfect_controller_week")
                    .limit(1)
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        android.util.Log.d("MotivationService", "checkBadgesAfterStreakUpdate - Query result: isEmpty=" + querySnapshot.isEmpty());
                        
                        if (!querySnapshot.isEmpty()) {
                            DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                            Badge badge = doc.toObject(Badge.class);
                            android.util.Log.d("MotivationService", "checkBadgesAfterStreakUpdate - Found badge: " + 
                                (badge != null ? "id=" + doc.getId() + ", progress=" + badge.getProgress() : "null"));
                            
                            if (badge != null) {
                                badge.setId(doc.getId()); // Set ID from document
                                
                                android.util.Log.d("MotivationService", "checkBadgesAfterStreakUpdate - Controller badge progress: " + 
                                    badge.getProgress() + " -> " + streak.getCurrentStreak());
                                
                                // Update progress to match current streak
                                badge.setProgress(streak.getCurrentStreak());
                                
                                // Check if earned
                                if (!badge.isEarned() && streak.getCurrentStreak() >= badge.getRequirement()) {
                                    badge.setEarned(true);
                                    badge.setEarnedDate(System.currentTimeMillis());
                                    android.util.Log.d("MotivationService", "checkBadgesAfterStreakUpdate - Badge earned!");
                                }
                                
                                saveBadge(badge, null);
                            } else {
                                android.util.Log.e("MotivationService", "checkBadgesAfterStreakUpdate - Badge object is null");
                            }
                        } else {
                            android.util.Log.e("MotivationService", "checkBadgesAfterStreakUpdate - No badge found in query");
                        }
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e("MotivationService", "checkBadgesAfterStreakUpdate - Query failed", e);
                    });
        } else {
            android.util.Log.d("MotivationService", "checkBadgesAfterStreakUpdate - Not controller type, skipping");
        }
    }

    private void updateTechniqueSessionBadge() {
        String userId = auth.getCurrentUser().getUid();
        android.util.Log.d("MotivationService", "updateTechniqueSessionBadge - Starting for userId: " + userId);
        
        db.collection("badges")
                .whereEqualTo("userId", userId)
                .whereEqualTo("type", "technique_sessions")
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    android.util.Log.d("MotivationService", "updateTechniqueSessionBadge - Query result: isEmpty=" + querySnapshot.isEmpty());
                    
                    if (!querySnapshot.isEmpty()) {
                        DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                        Badge badge = doc.toObject(Badge.class);
                        android.util.Log.d("MotivationService", "updateTechniqueSessionBadge - Found badge: " + 
                            (badge != null ? "progress=" + badge.getProgress() + ", earned=" + badge.isEarned() : "null"));
                        
                        if (badge != null) {
                            badge.setId(doc.getId()); // Set ID from document
                            if (!badge.isEarned()) {
                                int oldProgress = badge.getProgress();
                                badge.setProgress(badge.getProgress() + 1);
                                android.util.Log.d("MotivationService", "updateTechniqueSessionBadge - Updated progress: " + 
                                    oldProgress + " -> " + badge.getProgress() + ", requirement: " + badge.getRequirement());
                                
                                if (badge.getProgress() >= badge.getRequirement()) {
                                    badge.setEarned(true);
                                    badge.setEarnedDate(System.currentTimeMillis());
                                    android.util.Log.d("MotivationService", "updateTechniqueSessionBadge - Badge earned!");
                                }
                                saveBadge(badge, null);
                            } else {
                                android.util.Log.d("MotivationService", "updateTechniqueSessionBadge - Badge already earned");
                            }
                        } else {
                            android.util.Log.e("MotivationService", "updateTechniqueSessionBadge - Badge object is null");
                        }
                    } else {
                        android.util.Log.e("MotivationService", "updateTechniqueSessionBadge - No badge found in query");
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("MotivationService", "updateTechniqueSessionBadge - Query failed", e);
                });
    }

    // Helper methods
    private long getStartOfDay(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private void checkControllerMedicationForToday(MedicationCheckCallback callback) {
        String userId = auth.getCurrentUser().getUid();
        long todayStart = getStartOfDay(System.currentTimeMillis());
        long todayEnd = todayStart + TimeUnit.DAYS.toMillis(1) - 1;

        // Check if user has logged controller medication today
        db.collection("controller_medicine_logs")
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("timestamp", todayStart)
                .whereLessThanOrEqualTo("timestamp", todayEnd)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> callback.onResult(!querySnapshot.isEmpty()))
                .addOnFailureListener(e -> callback.onResult(false));
    }

    // Settings methods
    public void updateBadgeThresholds(int techniqueSessions, int lowRescueThreshold, int lowRescuePeriod) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putInt("technique_sessions_required", techniqueSessions)
                .putInt("low_rescue_threshold", lowRescueThreshold)
                .putInt("low_rescue_period", lowRescuePeriod)
                .apply();

        // Update existing badges with new requirements
        String userId = auth.getCurrentUser().getUid();
        
        db.collection("badges")
                .whereEqualTo("userId", userId)
                .whereEqualTo("type", "technique_sessions")
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        Badge badge = querySnapshot.getDocuments().get(0).toObject(Badge.class);
                        if (badge != null) {
                            badge.setRequirement(techniqueSessions);
                            badge.setDescription("Complete " + techniqueSessions + " high-quality technique practice sessions");
                            saveBadge(badge, null);
                        }
                    }
                });

        db.collection("badges")
                .whereEqualTo("userId", userId)
                .whereEqualTo("type", "low_rescue_month")
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        Badge badge = querySnapshot.getDocuments().get(0).toObject(Badge.class);
                        if (badge != null) {
                            badge.setRequirement(lowRescueThreshold);
                            badge.setDescription("Use rescue inhaler ≤" + lowRescueThreshold + " days in " + lowRescuePeriod + " days");
                            saveBadge(badge, null);
                        }
                    }
                });
    }

    public Map<String, Integer> getBadgeThresholds() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Map<String, Integer> thresholds = new HashMap<>();
        thresholds.put("technique_sessions", prefs.getInt("technique_sessions_required", DEFAULT_TECHNIQUE_SESSIONS_REQUIRED));
        thresholds.put("low_rescue_threshold", prefs.getInt("low_rescue_threshold", DEFAULT_LOW_RESCUE_THRESHOLD));
        thresholds.put("low_rescue_period", prefs.getInt("low_rescue_period", DEFAULT_LOW_RESCUE_PERIOD));
        return thresholds;
    }

    // Cleanup method to remove duplicate badges
    public void cleanupDuplicateBadges(UpdateCallback callback) {
        String userId = auth.getCurrentUser().getUid();
        db.collection("badges")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    // Group badges by type
                    Map<String, List<DocumentSnapshot>> badgesByType = new HashMap<>();
                    
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String type = doc.getString("type");
                        if (type != null) {
                            if (!badgesByType.containsKey(type)) {
                                badgesByType.put(type, new ArrayList<>());
                            }
                            badgesByType.get(type).add(doc);
                        }
                    }
                    
                    // Delete duplicates (keep only the first one of each type)
                    int deletionsNeeded = 0;
                    for (Map.Entry<String, List<DocumentSnapshot>> entry : badgesByType.entrySet()) {
                        List<DocumentSnapshot> badges = entry.getValue();
                        if (badges.size() > 1) {
                            // Delete all except the first one
                            for (int i = 1; i < badges.size(); i++) {
                                deletionsNeeded++;
                                badges.get(i).getReference().delete();
                            }
                        }
                    }
                    
                    if (callback != null) {
                        callback.onComplete();
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onComplete();
                    }
                });
    }

    // Callback interfaces
    public interface UpdateCallback {
        void onComplete();
    }

    private interface MedicationCheckCallback {
        void onResult(boolean hasMedication);
    }
}
