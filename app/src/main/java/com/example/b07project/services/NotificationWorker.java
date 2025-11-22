package com.example.b07project.services;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.example.b07project.utils.NotificationHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NotificationWorker extends Worker {

    private static final String PREFS_NAME = "NotificationWorkerPrefs";
    private static final String KEY_LAST_CHECK = "last_check_timestamp";

    public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return Result.failure();
        }

        SharedPreferences prefs = getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        // Default to now if not set, to avoid fetching old history on first install
        long lastCheckTime = prefs.getLong(KEY_LAST_CHECK, System.currentTimeMillis());
        Date lastCheckDate = new Date(lastCheckTime);

        // Use a latch to wait for the async Firestore callback
        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] success = {false};

        android.util.Log.d("NotificationWorker", "Checking for notifications after: " + lastCheckDate);

        // Query ONLY by userId to avoid composite index requirements
        FirebaseFirestore.getInstance().collection("notifications")
                .whereEqualTo("userId", user.getUid())
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (!snapshots.isEmpty()) {
                        List<DocumentSnapshot> newNotifications = new ArrayList<>();
                        for (DocumentSnapshot doc : snapshots) {
                            Date timestamp = doc.getDate("timestamp");
                            if (timestamp != null && timestamp.after(lastCheckDate)) {
                                newNotifications.add(doc);
                            }
                        }

                        android.util.Log.d("NotificationWorker", "Found " + newNotifications.size() + " new notifications (local filter)");

                        if (!newNotifications.isEmpty()) {
                            // Sort by timestamp ascending
                            Collections.sort(newNotifications, (d1, d2) -> {
                                Date t1 = d1.getDate("timestamp");
                                Date t2 = d2.getDate("timestamp");
                                if (t1 == null || t2 == null) return 0;
                                return t1.compareTo(t2);
                            });

                            for (DocumentSnapshot doc : newNotifications) {
                                String title = doc.getString("title");
                                String message = doc.getString("message");
                                NotificationHelper.showLocalNotification(getApplicationContext(), title, message);
                            }
                            
                            // Update last check time
                            DocumentSnapshot lastDoc = newNotifications.get(newNotifications.size() - 1);
                            Date latestTime = lastDoc.getDate("timestamp");
                            if (latestTime != null) {
                                prefs.edit().putLong(KEY_LAST_CHECK, latestTime.getTime()).apply();
                            }
                        } else {
                             prefs.edit().putLong(KEY_LAST_CHECK, System.currentTimeMillis()).apply();
                        }
                    } else {
                         android.util.Log.d("NotificationWorker", "No notifications found for user");
                         prefs.edit().putLong(KEY_LAST_CHECK, System.currentTimeMillis()).apply();
                    }
                    success[0] = true;
                    latch.countDown();
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("NotificationWorker", "Error checking notifications", e);
                    latch.countDown();
                });

        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            return Result.retry();
        }

        if (success[0]) {
            return Result.success();
        } else {
            return Result.retry();
        }
    }
}
