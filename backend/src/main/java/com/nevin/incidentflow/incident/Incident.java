package com.nevin.incidentflow.incident;

import com.nevin.incidentflow.team.ResponseTeam;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "incidents")
public class Incident {

    public enum Severity { LOW, MEDIUM, HIGH, CRITICAL }
    public enum Status { OPEN, ACKNOWLEDGED, RESOLVED }

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 64)
    private String fingerprint;

    @Column(nullable = false)
    private String service;

    @Column(nullable = false, length = 500)
    private String title;

    @ManyToOne
    @JoinColumn(name = "team_id", nullable = false)
    private ResponseTeam team;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.OPEN;

    @Column(name = "alert_count", nullable = false)
    private int alertCount = 0;

    @Column(name = "first_seen_at", nullable = false)
    private OffsetDateTime firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private OffsetDateTime lastSeenAt;

    @Column(name = "acknowledged_at")
    private OffsetDateTime acknowledgedAt;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @Version
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Incident() {}

    public Incident(String fingerprint, String service, String title, ResponseTeam team,
                     Severity severity, OffsetDateTime firstSeenAt, OffsetDateTime lastSeenAt) {
        this.fingerprint = fingerprint;
        this.service = service;
        this.title = title;
        this.team = team;
        this.severity = severity;
        this.firstSeenAt = firstSeenAt;
        this.lastSeenAt = lastSeenAt;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public String getFingerprint() { return fingerprint; }
    public String getService() { return service; }
    public String getTitle() { return title; }
    public ResponseTeam getTeam() { return team; }
    public Severity getSeverity() { return severity; }
    public Status getStatus() { return status; }
    public int getAlertCount() { return alertCount; }
    public OffsetDateTime getFirstSeenAt() { return firstSeenAt; }
    public OffsetDateTime getLastSeenAt() { return lastSeenAt; }
    public OffsetDateTime getAcknowledgedAt() { return acknowledgedAt; }
    public OffsetDateTime getResolvedAt() { return resolvedAt; }
    public long getVersion() { return version; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    public void setSeverity(Severity severity) { this.severity = severity; }
    public void setStatus(Status status) { this.status = status; }
    public void setAlertCount(int alertCount) { this.alertCount = alertCount; }
    public void setLastSeenAt(OffsetDateTime lastSeenAt) { this.lastSeenAt = lastSeenAt; }
    public void setAcknowledgedAt(OffsetDateTime acknowledgedAt) { this.acknowledgedAt = acknowledgedAt; }
    public void setResolvedAt(OffsetDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
    public void setTeam(ResponseTeam team) { this.team = team; }
}
