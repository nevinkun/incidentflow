package com.nevin.incidentflow.failure;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "failure_records")
public class FailureRecord {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "original_event_id", nullable = false)
    private UUID originalEventId;

    @Column(name = "original_topic", nullable = false)
    private String originalTopic;

    @Column(name = "original_partition", nullable = false)
    private int originalPartition;

    @Column(name = "original_offset", nullable = false)
    private long originalOffset;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(name = "exception_type", nullable = false)
    private String exceptionType;

    @Column(name = "error_message", nullable = false)
    private String errorMessage;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    @Column(name = "failed_at", nullable = false, updatable = false)
    private OffsetDateTime failedAt;

    @Column(name = "replayed_at")
    private OffsetDateTime replayedAt;

    @Column(name = "replay_event_id")
    private UUID replayEventId;

    protected FailureRecord() {}

    public FailureRecord(UUID originalEventId, String originalTopic, int originalPartition,
                          long originalOffset, String payload, String exceptionType,
                          String errorMessage, int retryCount) {
        this.originalEventId = originalEventId;
        this.originalTopic = originalTopic;
        this.originalPartition = originalPartition;
        this.originalOffset = originalOffset;
        this.payload = payload;
        this.exceptionType = exceptionType;
        this.errorMessage = errorMessage;
        this.retryCount = retryCount;
    }

    @PrePersist
    protected void onCreate() {
        failedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getOriginalEventId() { return originalEventId; }
    public String getOriginalTopic() { return originalTopic; }
    public int getOriginalPartition() { return originalPartition; }
    public long getOriginalOffset() { return originalOffset; }
    public String getPayload() { return payload; }
    public String getExceptionType() { return exceptionType; }
    public String getErrorMessage() { return errorMessage; }
    public int getRetryCount() { return retryCount; }
    public OffsetDateTime getFailedAt() { return failedAt; }
    public OffsetDateTime getReplayedAt() { return replayedAt; }
    public UUID getReplayEventId() { return replayEventId; }

    public void setReplayedAt(OffsetDateTime replayedAt) { this.replayedAt = replayedAt; }
    public void setReplayEventId(UUID replayEventId) { this.replayEventId = replayEventId; }
}
