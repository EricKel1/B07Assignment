package com.example.b07project.models;

import java.util.Date;

public class AppNotification {
    private String id;
    private String userId; // Parent ID
    private String title;
    private String message;
    private Date timestamp;
    private boolean isRead;

    public AppNotification() {}

    public AppNotification(String userId, String title, String message) {
        this.userId = userId;
        this.title = title;
        this.message = message;
        this.timestamp = new Date();
        this.isRead = false;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
}
