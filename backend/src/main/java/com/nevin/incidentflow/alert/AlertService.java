package com.nevin.incidentflow.alert;

import com.nevin.incidentflow.observability.CorrelationContext;
import com.nevin.incidentflow.outbox.OutboxEvent;
import com.nevin.incidentflow.outbox.OutboxEventRepository;
import com.nevin.incidentflow.ratelimit.RateLimitExceededException;
import com.nevin.incidentflow.ratelimit.RateLimiter;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
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

    private final Counter alertsIngestedCounter;
    private final Counter alertsRateLimitedCounter;
    private final Counter duplicateAlertsCounter;

    public AlertService(AlertRepository alertRepository,
                         OutboxEventRepository outboxEventRepository,
                         RateLimiter rateLimiter,
                         JsonMapper jsonMapper,
                         MeterRegistry meterRegistry) {
        this.alertRepository = alertRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.rateLimiter = rateLimiter;
        this.jsonMapper = jsonMapper;

        this.alertsIngestedCounter = Counter.builder("incidentflow.alerts.ingested")
                .description("Total number of alerts successfully ingested")
                .register(meterRegistry);
        this.alertsRateLimitedCounter = Counter.builder("incidentflow.alerts.rate.limited")
                .description("Total number of alert submissions rejected by rate limiting")
                .register(meterRegistry);
        this.duplicateAlertsCounter = Counter.builder("incidentflow.duplicate.alerts")
                .description("Total number of duplicate alert submissions detected")
                .register(meterRegistry);
    }

    @Transactional
    public Alert submitAlert(AlertRequest request) {
        try (CorrelationContext ctx = CorrelationContext.open()
                .put("service", request.getService())) {

            if (!rateLimiter.isAllowed(request.getSource())) {
                alertsRateLimitedCounter.increment();
                throw new RateLimitExceededException(request.getSource());
            }

            Optional<Alert> existing = alertRepository.findByExternalEventId(request.getExternalEventId());
            if (existing.isPresent()) {
                duplicateAlertsCounter.increment();
                return existing.get();
            }

            String fingerprint = AlertFingerprintGenerator.generate(
                    request.getService(), request.getAlertType(), request.getResourceId());
            ctx.put("fingerprint", fingerprint);

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
            ctx.put("alertId", alert.getId());

            UUID eventId = UUID.randomUUID();
            ctx.put("eventId", eventId);

            OutboxEvent outboxEvent = new OutboxEvent(
                    "ALERT_RECEIVED",
                    "Alert",
                    alert.getId(),
                    fingerprint,
                    buildOutboxPayload(alert, eventId, request.getFailureSimulation()));

            outboxEventRepository.save(outboxEvent);

            alertsIngestedCounter.increment();

            return alert;
        }
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

    private String buildOutboxPayload(Alert alert, UUID eventId, FailureSimulation failureSimulation) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("eventId", eventId.toString());
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
            payload.put("failureSimulation", failureSimulation.name());

            return jsonMapper.writeValueAsString(payload);
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to build outbox payload", e);
        }
    }
}
