package com.nevin.incidentflow.failure;

import com.nevin.incidentflow.alert.FailureSimulation;
import com.nevin.incidentflow.messaging.AlertEventPayload;
import com.nevin.incidentflow.outbox.OutboxEvent;
import com.nevin.incidentflow.outbox.OutboxEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class FailureService {

    private final FailureRecordRepository failureRecordRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final JsonMapper jsonMapper;

    private final Counter replaysCounter;

    public FailureService(FailureRecordRepository failureRecordRepository,
                           OutboxEventRepository outboxEventRepository,
                           JsonMapper jsonMapper,
                           MeterRegistry meterRegistry) {
        this.failureRecordRepository = failureRecordRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.jsonMapper = jsonMapper;

        this.replaysCounter = Counter.builder("incidentflow.replays")
                .description("Total number of failed events replayed")
                .register(meterRegistry);
    }

    public List<FailureRecord> listFailures() {
        return failureRecordRepository.findAllByOrderByFailedAtDesc();
    }

    public FailureRecord getFailure(UUID failureId) {
        return failureRecordRepository.findById(failureId)
                .orElseThrow(() -> new FailureNotFoundException(failureId));
    }

    @Transactional
    public FailureRecord replay(UUID failureId) {
        FailureRecord failureRecord = getFailure(failureId);

        AlertEventPayload original;
        try {
            original = jsonMapper.readValue(failureRecord.getPayload(), AlertEventPayload.class);
        } catch (JacksonException e) {
            throw new IllegalStateException("Cannot replay: stored payload is not valid JSON", e);
        }

        UUID newEventId = UUID.randomUUID();
        original.setEventId(newEventId);
        original.setFailureSimulation(FailureSimulation.NONE.name());

        String newPayloadJson;
        try {
            newPayloadJson = jsonMapper.writeValueAsString(original);
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to serialize replay payload", e);
        }

        OutboxEvent outboxEvent = new OutboxEvent(
                "ALERT_RECEIVED", "Alert", original.getAlertId(), original.getFingerprint(), newPayloadJson);
        outboxEventRepository.save(outboxEvent);

        failureRecord.setReplayedAt(OffsetDateTime.now());
        failureRecord.setReplayEventId(newEventId);

        replaysCounter.increment();

        return failureRecord;
    }
}
