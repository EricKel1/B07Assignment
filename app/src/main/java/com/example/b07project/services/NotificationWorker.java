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

        FirebaseFirestore.getInstance().collection("notifications")
                .whereEqualTo("userId", user.getUid())
                .whereGreaterThan("timestamp", lastCheckDate)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (!snapshots.isEmpty()) {
                        android.util.Log.d("NotificationWorker", "Found " + snapshots.size() + " new notifications");
                        for (DocumentSnapshot doc : snapshots) {
                            String title = doc.getString("title");
                            String message = doc.getString("message");
                            NotificationHelper.showLocalNotification(getApplicationContext(), title, message);
                        }
                        
                        // Update last check time to the latest notification's time
                        DocumentSnapshot lastDoc = snapshots.getDocuments().get(snapshots.size() - 1);
                        Date latestTime = lastDoc.getDate("timestamp");
                        if (latestTime != null) {
                            prefs.edit().putLong(KEY_LAST_CHECK, latestTime.getTime()).apply();
                        }
                    } else {
                         android.util.Log.d("NotificationWorker", "No new notifications found");
                         // Update check time to now to avoid gap? 
                         // Actually, if we found nothing, we should update to now so we don't query the same empty window again?
                         // But if we update to now, we might miss something that came in 1ms ago?
                         // It's safer to just update to now.
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
