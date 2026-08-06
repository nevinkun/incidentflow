package com.nevin.incidentflow.alert;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "alerts")
public class Alert {

    public enum Severity { LOW, MEDIUM, HIGH, CRITICAL }
    public enum Status { RECEIVED, QUEUED, PROCESSING, PROCESSED, FAILED }

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "external_event_id", nullable = false, unique = true)
    private String externalEventId;

    @Column(nullable = false)
    private String source;

    @Column(nullable = false)
    private String service;

    @Column(name = "alert_type", nullable = false)
    private String alertType;

    @Column(name = "resource_id", nullable = false)
    private String resourceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Severity severity;

    private String summary;

    @Column(nullable = false, length = 64)
    private String fingerprint;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String metadata;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.RECEIVED;

    @Column(name = "incident_id")
    private UUID incidentId;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    @Column(name = "received_at", nullable = false, updatable = false)
    private OffsetDateTime receivedAt;

    @Column(name = "processed_at")
    private OffsetDateTime processedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Alert() {}

    public Alert(String externalEventId, String source, String service, String alertType,
                 String resourceId, Severity severity, String summary, String fingerprint,
                 String metadata, OffsetDateTime occurredAt) {
        this.externalEventId = externalEventId;
        this.source = source;
        this.service = service;
        this.alertType = alertType;
        this.resourceId = resourceId;
        this.severity = severity;
        this.summary = summary;
        this.fingerprint = fingerprint;
        this.metadata = metadata;
        this.occurredAt = occurredAt;
    }

    @PrePersist
    protected void onCreate() {
        receivedAt = OffsetDateTime.now();
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public String getExternalEventId() { return externalEventId; }
    public String getSource() { return source; }
    public String getService() { return service; }
    public String getAlertType() { return alertType; }
    public String getResourceId() { return resourceId; }
    public Severity getSeverity() { return severity; }
    public String getSummary() { return summary; }
    public String getFingerprint() { return fingerprint; }
    public String getMetadata() { return metadata; }
    public Status getStatus() { return status; }
    public UUID getIncidentId() { return incidentId; }
    public OffsetDateTime getOccurredAt() { return occurredAt; }
    public OffsetDateTime getReceivedAt() { return receivedAt; }
    public OffsetDateTime getProcessedAt() { return processedAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    public void setStatus(Status status) { this.status = status; }
    public void setIncidentId(UUID incidentId) { this.incidentId = incidentId; }
    public void setProcessedAt(OffsetDateTime processedAt) { this.processedAt = processedAt; }
}
