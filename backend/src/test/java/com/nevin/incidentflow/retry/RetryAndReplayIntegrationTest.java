package com.nevin.incidentflow.retry;

import com.nevin.incidentflow.AbstractIntegrationTest;
import com.nevin.incidentflow.alert.Alert;
import com.nevin.incidentflow.alert.AlertRepository;
import com.nevin.incidentflow.alert.AlertRequest;
import com.nevin.incidentflow.alert.FailureSimulation;
import com.nevin.incidentflow.failure.FailureRecord;
import com.nevin.incidentflow.failure.FailureRecordRepository;
import com.nevin.incidentflow.messaging.KafkaTopicConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class RetryAndReplayIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private FailureRecordRepository failureRecordRepository;

    private AlertRequest request(String externalEventId, FailureSimulation simulation) {
        AlertRequest request = new AlertRequest();
        request.setExternalEventId(externalEventId);
        request.setSource("monitoring-service");
        request.setService("billing-service");
        request.setAlertType("DB_TIMEOUT");
        request.setResourceId("retry-test-handler-" + UUID.randomUUID());
        request.setSeverity(Alert.Severity.HIGH);
        request.setOccurredAt(OffsetDateTime.now());
        request.setFailureSimulation(simulation);
        return request;
    }

    private FailureRecord submitAndWaitForFailureRecord(String externalEventId) {
        restTemplate.postForEntity(
                "/api/v1/alerts", request(externalEventId, FailureSimulation.PERMANENT), Alert.class);

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            List<FailureRecord> matches = failureRecordRepository.findAll().stream()
                    .filter(f -> f.getPayload().contains(externalEventId))
                    .toList();
            assertThat(matches).hasSize(1);
        });

        return failureRecordRepository.findAll().stream()
                .filter(f -> f.getPayload().contains(externalEventId))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void transientFailureSucceedsAfterRetries() {
        String externalEventId = "evt-transient-itest-" + UUID.randomUUID();
        var response = restTemplate.postForEntity(
                "/api/v1/alerts", request(externalEventId, FailureSimulation.TRANSIENT), Alert.class);
        UUID alertId = response.getBody().getId();

        await().atMost(Duration.ofSeconds(45)).untilAsserted(() -> {
            Alert alert = alertRepository.findById(alertId).orElseThrow();
            assertThat(alert.getStatus()).isEqualTo(Alert.Status.PROCESSED);
            assertThat(alert.getIncidentId()).isNotNull();
        });
    }

    @Test
    void permanentFailureReachesDeadLetterPathWithoutExhaustingRetries() {
        String externalEventId = "evt-permanent-itest-" + UUID.randomUUID();
        FailureRecord record = submitAndWaitForFailureRecord(externalEventId);

        assertThat(record.getRetryCount()).isLessThanOrEqualTo(2);
    }

    @Test
    void deadLetterEventCreatesACompleteFailureRecord() {
        String externalEventId = "evt-permanent-itest-" + UUID.randomUUID();
        FailureRecord record = submitAndWaitForFailureRecord(externalEventId);

        assertThat(record.getOriginalTopic()).isEqualTo(KafkaTopicConfig.ALERTS_RECEIVED_TOPIC);
        assertThat(record.getOriginalEventId()).isNotNull();
        assertThat(record.getPayload()).contains(externalEventId);
        assertThat(record.getErrorMessage()).isNotBlank();
        assertThat(record.getFailedAt()).isNotNull();
        assertThat(record.getReplayedAt()).isNull();
        assertThat(record.getReplayEventId()).isNull();
    }

    @Test
    void replayedFailureProcessesSuccessfully() {
        String externalEventId = "evt-replay-itest-" + UUID.randomUUID();
        FailureRecord record = submitAndWaitForFailureRecord(externalEventId);

        restTemplate.postForEntity("/api/v1/failures/" + record.getId() + "/replay", null, Void.class);

        Alert originalAlert = alertRepository.findAll().stream()
                .filter(a -> a.getExternalEventId().equals(externalEventId))
                .findFirst()
                .orElseThrow();

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            Alert refreshed = alertRepository.findById(originalAlert.getId()).orElseThrow();
            assertThat(refreshed.getStatus()).isEqualTo(Alert.Status.PROCESSED);
            assertThat(refreshed.getIncidentId()).isNotNull();
        });

        FailureRecord refreshedRecord = failureRecordRepository.findById(record.getId()).orElseThrow();
        assertThat(refreshedRecord.getReplayedAt()).isNotNull();
        assertThat(refreshedRecord.getReplayEventId()).isNotNull();
    }
}
