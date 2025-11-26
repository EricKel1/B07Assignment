package com.example.b07project.repository;

import com.example.b07project.models.AppNotification;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class NotificationRepository {
    private final FirebaseFirestore db;
    private static final String COLLECTION = "notifications";

    public NotificationRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public interface LoadCallback {
        void onSuccess(List<AppNotification> notifications);
        void onFailure(String error);
    }

    public void saveNotification(AppNotification notification) {
        android.util.Log.d("NotificationDebug", "Repo saving: userId=" + notification.getUserId() + ", title=" + notification.getTitle());
        db.collection(COLLECTION)
                .add(notification)
                .addOnSuccessListener(doc -> {
                    android.util.Log.d("NotificationDebug", "Repo save SUCCESS. DocID=" + doc.getId());
                    notification.setId(doc.getId());
                    doc.update("id", doc.getId());
                })
                .addOnFailureListener(e -> android.util.Log.e("NotificationDebug", "Repo save FAILED: " + e.getMessage()));
    }

    public void getNotifications(String userId, LoadCallback callback) {
        db.collection(COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<AppNotification> list = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        AppNotification notif = doc.toObject(AppNotification.class);
                        notif.setId(doc.getId());
                        list.add(notif);
                    }
                    // Sort client-side to avoid needing a composite index
                    list.sort((n1, n2) -> n2.getTimestamp().compareTo(n1.getTimestamp()));
                    callback.onSuccess(list);
                })
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public void markAsRead(String notificationId) {
        db.collection(COLLECTION).document(notificationId).update("read", true);
    }

    public void deleteNotification(String notificationId, final DeleteCallback callback) {
        db.collection(COLLECTION).document(notificationId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
    }

    public interface DeleteCallback {
        void onSuccess();
        void onFailure(String error);
    }
}
