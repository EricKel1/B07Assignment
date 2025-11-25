package com.example.b07project.models;

public class ProviderSharingSettings {
    public String providerId;
    public String childId;
    public boolean rescueLogs;
    public boolean controllerAdherence;
    public boolean symptoms;
    public boolean triggers;
    public boolean peakFlow;
    public boolean triageIncidents;
    public boolean summaryCharts;

    public ProviderSharingSettings() {
    }

    public ProviderSharingSettings(String providerId, String childId) {
        this.providerId = providerId;
        this.childId = childId;
        this.rescueLogs = false;
        this.controllerAdherence = false;
        this.symptoms = false;
        this.triggers = false;
        this.peakFlow = false;
        this.triageIncidents = false;
        this.summaryCharts = false;
    }
}
