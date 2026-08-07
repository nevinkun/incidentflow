package com.nevin.incidentflow.alert;

import com.nevin.incidentflow.outbox.OutboxEvent;
import com.nevin.incidentflow.outbox.OutboxEventRepository;
import com.nevin.incidentflow.ratelimit.RateLimitExceededException;
import com.nevin.incidentflow.ratelimit.RateLimiter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class AlertService {

    private final AlertRepository alertRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final RateLimiter rateLimiter;
    private final JsonMapper jsonMapper;

    public AlertService(AlertRepository alertRepository,
                         OutboxEventRepository outboxEventRepository,
                         RateLimiter rateLimiter,
                         JsonMapper jsonMapper) {
        this.alertRepository = alertRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.rateLimiter = rateLimiter;
        this.jsonMapper = jsonMapper;
    }

    @Transactional
    public Alert submitAlert(AlertRequest request) {
        if (!rateLimiter.isAllowed(request.getSource())) {
            throw new RateLimitExceededException(request.getSource());
        }

        Optional<Alert> existing = alertRepository.findByExternalEventId(request.getExternalEventId());
        if (existing.isPresent()) {
            return existing.get();
        }

        String fingerprint = AlertFingerprintGenerator.generate(
                request.getService(), request.getAlertType(), request.getResourceId());

        String metadataJson = toJson(request.getMetadata());

        Alert alert = new Alert(
                request.getExternalEventId(),
                request.getSource(),
                request.getService(),
                request.getAlertType(),
                request.getResourceId(),
                request.getSeverity(),
                request.getSummary(),
                fingerprint,
                metadataJson,
                request.getOccurredAt());

        alertRepository.save(alert);

        OutboxEvent outboxEvent = new OutboxEvent(
                "ALERT_RECEIVED",
                "Alert",
                alert.getId(),
                fingerprint,
                buildOutboxPayload(alert));

        outboxEventRepository.save(outboxEvent);

        return alert;
    }

    public Alert getAlertStatus(UUID alertId) {
        return alertRepository.findById(alertId)
                .orElseThrow(() -> new AlertNotFoundException(alertId));
    }

    private String toJson(Map<String, Object> metadata) {
        try {
            return jsonMapper.writeValueAsString(metadata == null ? Map.of() : metadata);
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to serialize metadata to JSON", e);
        }
    }

    private String buildOutboxPayload(Alert alert) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("eventId", UUID.randomUUID().toString());
            payload.put("alertId", alert.getId().toString());
            payload.put("externalEventId", alert.getExternalEventId());
            payload.put("fingerprint", alert.getFingerprint());
            payload.put("source", alert.getSource());
            payload.put("service", alert.getService());
            payload.put("alertType", alert.getAlertType());
            payload.put("resourceId", alert.getResourceId());
            payload.put("severity", alert.getSeverity().name());
            payload.put("summary", alert.getSummary());
            payload.put("occurredAt", alert.getOccurredAt().toString());
            payload.put("metadata", jsonMapper.readTree(alert.getMetadata()));
            payload.put("failureSimulation", "NONE");

            return jsonMapper.writeValueAsString(payload);
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to build outbox payload", e);
        }
    }
}
