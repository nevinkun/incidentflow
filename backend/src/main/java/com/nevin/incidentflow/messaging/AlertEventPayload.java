package com.nevin.incidentflow.messaging;

import java.util.Map;
import java.util.UUID;

public class AlertEventPayload {

    private UUID eventId;
    private UUID alertId;
    private String externalEventId;
    private String fingerprint;
    private String source;
    private String service;
    private String alertType;
    private String resourceId;
    private String severity;
    private String summary;
    private String occurredAt;
    private Map<String, Object> metadata;
    private String failureSimulation;

    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }

    public UUID getAlertId() { return alertId; }
    public void setAlertId(UUID alertId) { this.alertId = alertId; }

    public String getExternalEventId() { return externalEventId; }
    public void setExternalEventId(String externalEventId) { this.externalEventId = externalEventId; }

    public String getFingerprint() { return fingerprint; }
    public void setFingerprint(String fingerprint) { this.fingerprint = fingerprint; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getService() { return service; }
    public void setService(String service) { this.service = service; }

    public String getAlertType() { return alertType; }
    public void setAlertType(String alertType) { this.alertType = alertType; }

    public String getResourceId() { return resourceId; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getOccurredAt() { return occurredAt; }
    public void setOccurredAt(String occurredAt) { this.occurredAt = occurredAt; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    public String getFailureSimulation() { return failureSimulation; }
    public void setFailureSimulation(String failureSimulation) { this.failureSimulation = failureSimulation; }
}
