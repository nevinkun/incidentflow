package com.nevin.incidentflow.alert;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.Map;

public class AlertRequest {

    @NotBlank(message = "externalEventId is required")
    private String externalEventId;

    @NotBlank(message = "source is required")
    private String source;

    @NotBlank(message = "service is required")
    private String service;

    @NotBlank(message = "alertType is required")
    private String alertType;

    @NotBlank(message = "resourceId is required")
    private String resourceId;

    @NotNull(message = "severity is required")
    private Alert.Severity severity;

    private String summary;

    @NotNull(message = "occurredAt is required")
    private OffsetDateTime occurredAt;

    private Map<String, Object> metadata;

    private FailureSimulation failureSimulation = FailureSimulation.NONE;

    public String getExternalEventId() { return externalEventId; }
    public void setExternalEventId(String externalEventId) { this.externalEventId = externalEventId; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getService() { return service; }
    public void setService(String service) { this.service = service; }

    public String getAlertType() { return alertType; }
    public void setAlertType(String alertType) { this.alertType = alertType; }

    public String getResourceId() { return resourceId; }
    public void setResourceId(String resourceId) { this.resourceId = resourceId; }

    public Alert.Severity getSeverity() { return severity; }
    public void setSeverity(Alert.Severity severity) { this.severity = severity; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public OffsetDateTime getOccurredAt() { return occurredAt; }
    public void setOccurredAt(OffsetDateTime occurredAt) { this.occurredAt = occurredAt; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    public FailureSimulation getFailureSimulation() { return failureSimulation; }
    public void setFailureSimulation(FailureSimulation failureSimulation) { this.failureSimulation = failureSimulation; }
}
