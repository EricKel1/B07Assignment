package com.example.b07project.models;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class TriageSession {
    private String id;
    private String userId;
    private Date startTime;
    private Date endTime;
    
    // Red flag checks
    private boolean cantSpeakFullSentences;
    private boolean chestPullingInRetractions;
    private boolean blueGrayLipsNails;
    
    // Additional info
    private int rescueAttemptsLast3Hours;
    private Integer currentPEF; // Optional
    private String currentZone;
    
    // Decision
    private String decision; // "emergency", "home_steps", "monitor"
    private String guidanceShown;
    private List<String> actionSteps;
    
    // Timer & re-check
    private boolean timerStarted;
    private Date timerEndTime;
    private boolean userImproved;
    private boolean escalated;
    private String escalationReason;
    
    // Outcome
    private String userResponse; // User's reported outcome
    private Date responseTime;
    private boolean parentAlerted;

    public TriageSession() {
        this.startTime = new Date();
        this.actionSteps = new ArrayList<>();
    }

    public TriageSession(String userId) {
        this.userId = userId;
        this.startTime = new Date();
        this.actionSteps = new ArrayList<>();
    }

    // Helper methods
    public boolean hasCriticalFlags() {
        return cantSpeakFullSentences || chestPullingInRetractions || blueGrayLipsNails;
    }

    public int getCriticalFlagCount() {
        int count = 0;
        if (cantSpeakFullSentences) count++;
        if (chestPullingInRetractions) count++;
        if (blueGrayLipsNails) count++;
        return count;
    }

    public List<String> getCriticalFlagsList() {
        List<String> flags = new ArrayList<>();
        if (cantSpeakFullSentences) flags.add("Can't speak full sentences");
        if (chestPullingInRetractions) flags.add("Chest pulling in/retractions");
        if (blueGrayLipsNails) flags.add("Blue/gray lips or nails");
        return flags;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public boolean isCantSpeakFullSentences() {
        return cantSpeakFullSentences;
    }

    public void setCantSpeakFullSentences(boolean cantSpeakFullSentences) {
        this.cantSpeakFullSentences = cantSpeakFullSentences;
    }

    public boolean isChestPullingInRetractions() {
        return chestPullingInRetractions;
    }

    public void setChestPullingInRetractions(boolean chestPullingInRetractions) {
        this.chestPullingInRetractions = chestPullingInRetractions;
    }

    public boolean isBlueGrayLipsNails() {
        return blueGrayLipsNails;
    }

    public void setBlueGrayLipsNails(boolean blueGrayLipsNails) {
        this.blueGrayLipsNails = blueGrayLipsNails;
    }

    public int getRescueAttemptsLast3Hours() {
        return rescueAttemptsLast3Hours;
    }

    public void setRescueAttemptsLast3Hours(int rescueAttemptsLast3Hours) {
        this.rescueAttemptsLast3Hours = rescueAttemptsLast3Hours;
    }

    public Integer getCurrentPEF() {
        return currentPEF;
    }

    public void setCurrentPEF(Integer currentPEF) {
        this.currentPEF = currentPEF;
    }

    public String getCurrentZone() {
        return currentZone;
    }

    public void setCurrentZone(String currentZone) {
        this.currentZone = currentZone;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public String getGuidanceShown() {
        return guidanceShown;
    }

    public void setGuidanceShown(String guidanceShown) {
        this.guidanceShown = guidanceShown;
    }

    public List<String> getActionSteps() {
        return actionSteps;
    }

    public void setActionSteps(List<String> actionSteps) {
        this.actionSteps = actionSteps;
    }

    public boolean isTimerStarted() {
        return timerStarted;
    }

    public void setTimerStarted(boolean timerStarted) {
        this.timerStarted = timerStarted;
    }

    public Date getTimerEndTime() {
        return timerEndTime;
    }

    public void setTimerEndTime(Date timerEndTime) {
        this.timerEndTime = timerEndTime;
    }

    public boolean isUserImproved() {
        return userImproved;
    }

    public void setUserImproved(boolean userImproved) {
        this.userImproved = userImproved;
    }

    public boolean isEscalated() {
        return escalated;
    }

    public void setEscalated(boolean escalated) {
        this.escalated = escalated;
    }

    public String getEscalationReason() {
        return escalationReason;
    }

    public void setEscalationReason(String escalationReason) {
        this.escalationReason = escalationReason;
    }

    public String getUserResponse() {
        return userResponse;
    }

    public void setUserResponse(String userResponse) {
        this.userResponse = userResponse;
    }

    public Date getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(Date responseTime) {
        this.responseTime = responseTime;
    }

    public boolean isParentAlerted() {
        return parentAlerted;
    }

    public void setParentAlerted(boolean parentAlerted) {
        this.parentAlerted = parentAlerted;
    }
}
