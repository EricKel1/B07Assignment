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

public class NotificationHelper {
    private static final String CHANNEL_ID = "smart_air_alerts";
    private static final String CHANNEL_NAME = "Smart Air Alerts";
    private static final String CHANNEL_DESC = "Alerts for low medicine and expiry";

    public static void sendAlert(Context context, String userId, String title, String message) {
        android.util.Log.d("NotificationHelper", "sendAlert called for userId: " + userId);

        // 1. Send System Notification (Local - for the device triggering it, e.g. Child)
        // We might want to suppress this if we only want Parent to see it, but usually feedback is good.
        // However, the user asked for "Parent getting push notifications".
        
        // 2. Save to Firestore for the Target User (usually the Child's ID is passed here)
        // We need to check if this user has a parent, and if so, send to the parent instead/also.
        
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        // Check 'children' collection first as it definitely contains the link
        db.collection("children").document(userId).get().addOnSuccessListener(doc -> {
            String targetId = userId;
            if (doc.exists()) {
                String parentId = doc.getString("parentId");
                if (parentId != null && !parentId.isEmpty()) {
                    targetId = parentId; // Send to Parent
                    android.util.Log.d("NotificationHelper", "Found parentId: " + parentId + " in children collection. Redirecting notification.");
                } else {
                    android.util.Log.d("NotificationHelper", "No parentId found in children collection for " + userId);
                }
            } else {
                android.util.Log.d("NotificationHelper", "No document found in children collection for " + userId);
                // Fallback: Check 'users' collection
                checkUsersCollection(userId, title, message);
                return; 
            }
            
            saveToFirestore(targetId, title, message);
        }).addOnFailureListener(e -> {
            android.util.Log.e("NotificationHelper", "Error checking children collection: " + e.getMessage());
            // Fallback
            checkUsersCollection(userId, title, message);
        });

        // Local notification
        showLocalNotification(context, title, message);
    }

    private static void checkUsersCollection(String userId, String title, String message) {
        FirebaseFirestore.getInstance().collection("users").document(userId).get().addOnSuccessListener(doc -> {
            String targetId = userId;
            if (doc.exists()) {
                String parentId = doc.getString("parentId");
                if (parentId != null && !parentId.isEmpty()) {
                    targetId = parentId;
                    android.util.Log.d("NotificationHelper", "Found parentId: " + parentId + " in users collection.");
                }
            }
            saveToFirestore(targetId, title, message);
        }).addOnFailureListener(e -> {
            saveToFirestore(userId, title, message);
        });
    }

    private static void saveToFirestore(String targetId, String title, String message) {
        android.util.Log.d("NotificationHelper", "Saving notification to Firestore for targetId: " + targetId);
        NotificationRepository repo = new NotificationRepository();
        repo.saveNotification(new AppNotification(targetId, title, message));
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
