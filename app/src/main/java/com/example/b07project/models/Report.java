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
    private double controllerAdherence;
    private int symptomBurdenDays;
    private int greenZoneCount;
    private int yellowZoneCount;
    private int redZoneCount;
    private int triageIncidentsCount;
    private boolean includeTriage;
    private boolean includeRescue;
    private boolean includeController;
    private boolean includeSymptoms;
    private boolean includeZones;
    private boolean includeDailyLogs;
    private boolean includeTriggerChart;

    public Report() {
        // Required empty constructor for Firestore
    }

    public Report(String userId, int days, long generatedDate, long startDate, long endDate,
                  int totalRescueUses, int totalControllerDoses, 
                  double avgRescuePerDay, double avgControllerPerDay,
                  double controllerAdherence, int symptomBurdenDays,
                  int greenZoneCount, int yellowZoneCount, int redZoneCount, int triageIncidentsCount,
                  boolean includeTriage, boolean includeRescue, boolean includeController, 
                  boolean includeSymptoms, boolean includeZones,
                  boolean includeDailyLogs, boolean includeTriggerChart) {
        this.userId = userId;
        this.days = days;
        this.generatedDate = generatedDate;
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalRescueUses = totalRescueUses;
        this.totalControllerDoses = totalControllerDoses;
        this.avgRescuePerDay = avgRescuePerDay;
        this.avgControllerPerDay = avgControllerPerDay;
        this.controllerAdherence = controllerAdherence;
        this.symptomBurdenDays = symptomBurdenDays;
        this.greenZoneCount = greenZoneCount;
        this.yellowZoneCount = yellowZoneCount;
        this.redZoneCount = redZoneCount;
        this.triageIncidentsCount = triageIncidentsCount;
        this.includeTriage = includeTriage;
        this.includeRescue = includeRescue;
        this.includeController = includeController;
        this.includeSymptoms = includeSymptoms;
        this.includeZones = includeZones;
        this.includeDailyLogs = includeDailyLogs;
        this.includeTriggerChart = includeTriggerChart;
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

    public double getControllerAdherence() { return controllerAdherence; }
    public void setControllerAdherence(double controllerAdherence) { this.controllerAdherence = controllerAdherence; }

    public int getSymptomBurdenDays() { return symptomBurdenDays; }
    public void setSymptomBurdenDays(int symptomBurdenDays) { this.symptomBurdenDays = symptomBurdenDays; }

    public int getGreenZoneCount() { return greenZoneCount; }
    public void setGreenZoneCount(int greenZoneCount) { this.greenZoneCount = greenZoneCount; }

    public int getYellowZoneCount() { return yellowZoneCount; }
    public void setYellowZoneCount(int yellowZoneCount) { this.yellowZoneCount = yellowZoneCount; }

    public int getRedZoneCount() { return redZoneCount; }
    public void setRedZoneCount(int redZoneCount) { this.redZoneCount = redZoneCount; }

    public int getTriageIncidentsCount() { return triageIncidentsCount; }
    public void setTriageIncidentsCount(int triageIncidentsCount) { this.triageIncidentsCount = triageIncidentsCount; }

    public boolean isIncludeTriage() { return includeTriage; }
    public void setIncludeTriage(boolean includeTriage) { this.includeTriage = includeTriage; }

    public boolean isIncludeRescue() { return includeRescue; }
    public void setIncludeRescue(boolean includeRescue) { this.includeRescue = includeRescue; }

    public boolean isIncludeController() { return includeController; }
    public void setIncludeController(boolean includeController) { this.includeController = includeController; }

    public boolean isIncludeSymptoms() { return includeSymptoms; }
    public void setIncludeSymptoms(boolean includeSymptoms) { this.includeSymptoms = includeSymptoms; }

    public boolean isIncludeZones() { return includeZones; }
    public void setIncludeZones(boolean includeZones) { this.includeZones = includeZones; }

    public boolean isIncludeDailyLogs() { return includeDailyLogs; }
    public void setIncludeDailyLogs(boolean includeDailyLogs) { this.includeDailyLogs = includeDailyLogs; }

    public boolean isIncludeTriggerChart() { return includeTriggerChart; }
    public void setIncludeTriggerChart(boolean includeTriggerChart) { this.includeTriggerChart = includeTriggerChart; }
}
