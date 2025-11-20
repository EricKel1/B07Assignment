package com.example.b07project.models;

import java.util.Date;

public class MedicineInventory {
    private String id;
    private String userId;
    private String name;
    private String type; // "Rescue" or "Controller"
    private String childId; // ID of the child this is assigned to (optional)
    private String childName; // Name of the child (optional)
    private int totalDoses;
    private int remainingDoses;
    private Date expiryDate;
    private Date purchaseDate;

    public MedicineInventory() {}

    public MedicineInventory(String userId, String name, String type, int totalDoses, int remainingDoses, Date expiryDate, Date purchaseDate) {
        this.userId = userId;
        this.name = name;
        this.type = type;
        this.totalDoses = totalDoses;
        this.remainingDoses = remainingDoses;
        this.expiryDate = expiryDate;
        this.purchaseDate = purchaseDate;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getChildId() { return childId; }
    public void setChildId(String childId) { this.childId = childId; }

    public String getChildName() { return childName; }
    public void setChildName(String childName) { this.childName = childName; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getTotalDoses() { return totalDoses; }
    public void setTotalDoses(int totalDoses) { this.totalDoses = totalDoses; }

    public int getRemainingDoses() { return remainingDoses; }
    public void setRemainingDoses(int remainingDoses) { this.remainingDoses = remainingDoses; }

    public Date getExpiryDate() { return expiryDate; }
    public void setExpiryDate(Date expiryDate) { this.expiryDate = expiryDate; }

    public Date getPurchaseDate() { return purchaseDate; }
    public void setPurchaseDate(Date purchaseDate) { this.purchaseDate = purchaseDate; }

    public boolean isLow() {
        return totalDoses > 0 && ((double) remainingDoses / totalDoses) <= 0.20;
    }

    public boolean isExpired() {
        return expiryDate != null && new Date().after(expiryDate);
    }
}
