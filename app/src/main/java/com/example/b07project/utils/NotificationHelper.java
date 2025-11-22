package com.example.b07project.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.example.b07project.R;
import com.example.b07project.models.AppNotification;
import com.example.b07project.repository.NotificationRepository;

import com.google.firebase.firestore.FirebaseFirestore;

import com.google.firebase.firestore.FirebaseFirestore;

public class NotificationHelper {
    private static final String CHANNEL_ID = "smart_air_alerts";
    private static final String CHANNEL_NAME = "Smart Air Alerts";
    private static final String CHANNEL_DESC = "Alerts for low medicine and expiry";

    public static void sendAlert(Context context, String userId, String title, String message) {
        android.util.Log.d("NotificationDebug", "sendAlert START. userId=" + userId + ", title=" + title);

        // 1. Send System Notification (Local)
        showLocalNotification(context, title, message);
        
        // 2. Save to Firestore
        // We check the 'users' collection FIRST because it's the most reliable source 
        // for the user's own profile (and their parentId).
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        db.collection("users").document(userId).get().addOnSuccessListener(doc -> {
            android.util.Log.d("NotificationDebug", "Users collection lookup success. Exists: " + doc.exists());
            String targetId = userId;
            if (doc.exists()) {
                String parentId = doc.getString("parentId");
                android.util.Log.d("NotificationDebug", "Found parentId in users: " + parentId);
                if (parentId != null && !parentId.isEmpty()) {
                    targetId = parentId;
                } else {
                    // If not found in users, try children collection as backup
                    // (This handles cases where the user profile might not be fully synced but the link exists)
                    checkChildrenCollection(userId, title, message);
                    return;
                }
            }
            saveToFirestore(targetId, title, message);
        }).addOnFailureListener(e -> {
            android.util.Log.w("NotificationDebug", "Users collection lookup failed: " + e.getMessage());
            // Fallback to children collection
            checkChildrenCollection(userId, title, message);
        });
    }

    private static void checkChildrenCollection(String userId, String title, String message) {
        FirebaseFirestore.getInstance().collection("children").document(userId).get().addOnSuccessListener(doc -> {
            String targetId = userId;
            if (doc.exists()) {
                String parentId = doc.getString("parentId");
                if (parentId != null && !parentId.isEmpty()) {
                    targetId = parentId;
                }
            }
            saveToFirestore(targetId, title, message);
        }).addOnFailureListener(e -> {
            android.util.Log.w("NotificationDebug", "Children collection lookup failed: " + e.getMessage());
            // If both fail, just save to the user ID
            saveToFirestore(userId, title, message);
        });
    }

    private static void saveToFirestore(String targetId, String title, String message) {
        android.util.Log.d("NotificationDebug", "FINAL SAVE: Saving notification for targetId=" + targetId);
        NotificationRepository repo = new NotificationRepository();
        repo.saveNotification(new AppNotification(targetId, title, message));
        
        // Note: The actual push notification will be sent by a Firebase Cloud Function
        // that listens to the 'notifications' collection. This is required for FCM V1 API.
    }

    public static void showLocalNotification(Context context, String title, String message) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(CHANNEL_DESC);
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        manager.notify((int) System.currentTimeMillis(), builder.build());
    }
}
