package com.nevin.incidentflow.worker;

import com.nevin.incidentflow.AbstractIntegrationTest;
import com.nevin.incidentflow.alert.Alert;
import com.nevin.incidentflow.alert.AlertRepository;
import com.nevin.incidentflow.alert.AlertRequest;
import com.nevin.incidentflow.incident.Incident;
import com.nevin.incidentflow.incident.IncidentRepository;
import com.nevin.incidentflow.incident.IncidentTimelineEvent;
import com.nevin.incidentflow.incident.IncidentTimelineEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class WorkerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private IncidentTimelineEventRepository timelineEventRepository;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private AlertRequest sampleRequest(String externalEventId, String service, String resourceId) {
        AlertRequest request = new AlertRequest();
        request.setExternalEventId(externalEventId);
        request.setSource("monitoring-service");
        request.setService(service);
        request.setAlertType("HIGH_ERROR_RATE");
        request.setResourceId(resourceId);
        request.setSeverity(Alert.Severity.HIGH);
        request.setOccurredAt(OffsetDateTime.now());
        return request;
    }

    private Alert submitAndWaitForProcessing(AlertRequest request) {
        var response = restTemplate.postForEntity("/api/v1/alerts", request, Alert.class);
        UUID alertId = response.getBody().getId();

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            Optional<Alert> processed = alertRepository.findById(alertId);
            assertThat(processed).isPresent();
            assertThat(processed.get().getStatus()).isEqualTo(Alert.Status.PROCESSED);
        });

        return alertRepository.findById(alertId).orElseThrow();
    }

    @Test
    void kafkaEventCreatesAnIncident() {
        Alert alert = submitAndWaitForProcessing(
                sampleRequest("evt-worker-" + UUID.randomUUID(), "payments-api", "checkout-handler"));

        assertThat(alert.getIncidentId()).isNotNull();

        Optional<Incident> incident = incidentRepository.findById(alert.getIncidentId());
        assertThat(incident).isPresent();
        assertThat(incident.get().getService()).isEqualTo("payments-api");
    }

    @Test
    void matchingAlertsCorrelateIntoOneIncident() {
        String resourceId = "correlate-handler-" + UUID.randomUUID();

        Alert first = submitAndWaitForProcessing(
                sampleRequest("evt-corr-a-" + UUID.randomUUID(), "payments-api", resourceId));
        Alert second = submitAndWaitForProcessing(
                sampleRequest("evt-corr-b-" + UUID.randomUUID(), "payments-api", resourceId));

        assertThat(second.getIncidentId()).isEqualTo(first.getIncidentId());

        Incident incident = incidentRepository.findById(first.getIncidentId()).orElseThrow();
        assertThat(incident.getAlertCount()).isEqualTo(2);
    }

    @Test
    void redisCacheHitReturnsTheActiveIncident() {
        String resourceId = "cache-hit-handler-" + UUID.randomUUID();

        Alert first = submitAndWaitForProcessing(
                sampleRequest("evt-cache-a-" + UUID.randomUUID(), "payments-api", resourceId));

        String fingerprint = first.getFingerprint();
        String cachedIncidentId = stringRedisTemplate.opsForValue()
                .get("incidentflow:correlation:" + fingerprint);

        assertThat(cachedIncidentId).isEqualTo(first.getIncidentId().toString());
    }

    @Test
    void redisCacheMissFallsBackToPostgres() {
        String resourceId = "cache-miss-handler-" + UUID.randomUUID();

        Alert first = submitAndWaitForProcessing(
                sampleRequest("evt-miss-a-" + UUID.randomUUID(), "payments-api", resourceId));

        String fingerprint = first.getFingerprint();
        stringRedisTemplate.delete("incidentflow:correlation:" + fingerprint);

        Alert second = submitAndWaitForProcessing(
                sampleRequest("evt-miss-b-" + UUID.randomUUID(), "payments-api", resourceId));

        assertThat(second.getIncidentId()).isEqualTo(first.getIncidentId());
    }

    @Test
    void duplicateKafkaDeliveryDoesNotDuplicateSideEffects() {
        String externalEventId = "evt-dupe-" + UUID.randomUUID();
        AlertRequest request = sampleRequest(externalEventId, "payments-api", "dupe-handler-" + UUID.randomUUID());

        Alert first = submitAndWaitForProcessing(request);
        var secondResponse = restTemplate.postForEntity("/api/v1/alerts", request, Alert.class);

        assertThat(secondResponse.getBody().getId()).isEqualTo(first.getId());

        Incident incident = incidentRepository.findById(first.getIncidentId()).orElseThrow();
        assertThat(incident.getAlertCount()).isEqualTo(1);
    }

    @Test
    void concurrentIncidentUpdatesDoNotLoseData() throws InterruptedException {
        Alert seedAlert = submitAndWaitForProcessing(
                sampleRequest("evt-concurrent-seed-" + UUID.randomUUID(), "payments-api",
                        "concurrent-handler-" + UUID.randomUUID()));

        UUID incidentId = seedAlert.getIncidentId();
        String resourceId = seedAlert.getResourceId();

        int concurrentAlerts = 5;
        CountDownLatch latch = new CountDownLatch(concurrentAlerts);
        AtomicInteger failures = new AtomicInteger(0);

        for (int i = 0; i < concurrentAlerts; i++) {
            final int index = i;
            new Thread(() -> {
                try {
                    AlertRequest request = sampleRequest(
                            "evt-concurrent-" + index + "-" + UUID.randomUUID(), "payments-api", resourceId);
                    restTemplate.postForEntity("/api/v1/alerts", request, Alert.class);
                } catch (Exception e) {
                    failures.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await(10, TimeUnit.SECONDS);
        assertThat(failures.get()).isZero();

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            Incident incident = incidentRepository.findById(incidentId).orElseThrow();
            assertThat(incident.getAlertCount()).isEqualTo(concurrentAlerts + 1);
        });
    }

    @Test
    void acknowledgementCreatesATimelineEntry() {
        Alert alert = submitAndWaitForProcessing(
                sampleRequest("evt-ack-" + UUID.randomUUID(), "payments-api", "ack-handler-" + UUID.randomUUID()));

        restTemplate.postForEntity("/api/v1/incidents/" + alert.getIncidentId() + "/acknowledge", null, Void.class);

        List<IncidentTimelineEvent> timeline = timelineEventRepository
                .findByIncidentIdOrderByCreatedAtAsc(alert.getIncidentId());

        assertThat(timeline).anyMatch(e -> e.getEventType() == IncidentTimelineEvent.EventType.ACKNOWLEDGED);
    }

    @Test
    void resolutionCreatesATimelineEntry() {
        Alert alert = submitAndWaitForProcessing(
                sampleRequest("evt-resolve-" + UUID.randomUUID(), "payments-api", "resolve-handler-" + UUID.randomUUID()));

        restTemplate.postForEntity("/api/v1/incidents/" + alert.getIncidentId() + "/resolve", null, Void.class);

        List<IncidentTimelineEvent> timeline = timelineEventRepository
                .findByIncidentIdOrderByCreatedAtAsc(alert.getIncidentId());

        assertThat(timeline).anyMatch(e -> e.getEventType() == IncidentTimelineEvent.EventType.RESOLVED);
    }
}
