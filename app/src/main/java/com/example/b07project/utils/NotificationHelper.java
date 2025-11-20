package com.example.b07project.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.example.b07project.R;
import com.example.b07project.models.AppNotification;
import com.example.b07project.repository.NotificationRepository;

public class NotificationHelper {
    private static final String CHANNEL_ID = "smart_air_alerts";
    private static final String CHANNEL_NAME = "Smart Air Alerts";
    private static final String CHANNEL_DESC = "Alerts for low medicine and expiry";

    public static void sendAlert(Context context, String userId, String title, String message) {
        // 1. Save to Firestore (In-App)
        NotificationRepository repo = new NotificationRepository();
        repo.saveNotification(new AppNotification(userId, title, message));

        // 2. Send System Notification
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(CHANNEL_DESC);
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert) // Replace with app icon
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        manager.notify((int) System.currentTimeMillis(), builder.build());
    }
}
