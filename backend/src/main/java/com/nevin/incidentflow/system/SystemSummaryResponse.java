package com.nevin.incidentflow.system;

public class SystemSummaryResponse {

    private final long openIncidents;
    private final long criticalIncidents;
    private final long alertsReceived;
    private final long alertsProcessed;
    private final long failedEvents;
    private final long pendingOutboxEvents;

    public SystemSummaryResponse(long openIncidents, long criticalIncidents, long alertsReceived,
                                  long alertsProcessed, long failedEvents, long pendingOutboxEvents) {
        this.openIncidents = openIncidents;
        this.criticalIncidents = criticalIncidents;
        this.alertsReceived = alertsReceived;
        this.alertsProcessed = alertsProcessed;
        this.failedEvents = failedEvents;
        this.pendingOutboxEvents = pendingOutboxEvents;
    }

    public long getOpenIncidents() { return openIncidents; }
    public long getCriticalIncidents() { return criticalIncidents; }
    public long getAlertsReceived() { return alertsReceived; }
    public long getAlertsProcessed() { return alertsProcessed; }
    public long getFailedEvents() { return failedEvents; }
    public long getPendingOutboxEvents() { return pendingOutboxEvents; }
}
