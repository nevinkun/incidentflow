package com.nevin.incidentflow.incident;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "incident_timeline_events")
public class IncidentTimelineEvent {

    public enum EventType {
        INCIDENT_CREATED, ALERT_ATTACHED, SEVERITY_INCREASED,
        TEAM_ASSIGNED, ACKNOWLEDGED, RESOLVED
    }

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "incident_id", nullable = false)
    private UUID incidentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private EventType eventType;

    @Column(nullable = false)
    private String description;

    @Column(name = "alert_id")
    private UUID alertId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected IncidentTimelineEvent() {}

    public IncidentTimelineEvent(UUID incidentId, EventType eventType, String description, UUID alertId) {
        this.incidentId = incidentId;
        this.eventType = eventType;
        this.description = description;
        this.alertId = alertId;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getIncidentId() { return incidentId; }
    public EventType getEventType() { return eventType; }
    public String getDescription() { return description; }
    public UUID getAlertId() { return alertId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
