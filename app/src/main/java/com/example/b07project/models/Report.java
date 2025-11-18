package com.example.b07project.models;

public class Report {
    private String id;
    private String userId;
    private int days; // 7, 30, or 90
    private long generatedDate;
    private long startDate;
    private long endDate;
    private int totalRescueUses;
    private int totalControllerDoses;
    private double avgRescuePerDay;
    private double avgControllerPerDay;

    public Report() {
        // Required empty constructor for Firestore
    }

    public Report(String userId, int days, long generatedDate, long startDate, long endDate,
                  int totalRescueUses, int totalControllerDoses, 
                  double avgRescuePerDay, double avgControllerPerDay) {
        this.userId = userId;
        this.days = days;
        this.generatedDate = generatedDate;
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalRescueUses = totalRescueUses;
        this.totalControllerDoses = totalControllerDoses;
        this.avgRescuePerDay = avgRescuePerDay;
        this.avgControllerPerDay = avgControllerPerDay;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public int getDays() { return days; }
    public void setDays(int days) { this.days = days; }

    public long getGeneratedDate() { return generatedDate; }
    public void setGeneratedDate(long generatedDate) { this.generatedDate = generatedDate; }

    public long getStartDate() { return startDate; }
    public void setStartDate(long startDate) { this.startDate = startDate; }

    public long getEndDate() { return endDate; }
    public void setEndDate(long endDate) { this.endDate = endDate; }

    public int getTotalRescueUses() { return totalRescueUses; }
    public void setTotalRescueUses(int totalRescueUses) { this.totalRescueUses = totalRescueUses; }

    public int getTotalControllerDoses() { return totalControllerDoses; }
    public void setTotalControllerDoses(int totalControllerDoses) { this.totalControllerDoses = totalControllerDoses; }

    public double getAvgRescuePerDay() { return avgRescuePerDay; }
    public void setAvgRescuePerDay(double avgRescuePerDay) { this.avgRescuePerDay = avgRescuePerDay; }

    public double getAvgControllerPerDay() { return avgControllerPerDay; }
    public void setAvgControllerPerDay(double avgControllerPerDay) { this.avgControllerPerDay = avgControllerPerDay; }
}
