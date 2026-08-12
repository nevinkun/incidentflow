package com.nevin.incidentflow.alert;

import com.nevin.incidentflow.AbstractIntegrationTest;
import com.nevin.incidentflow.messaging.KafkaTopicConfig;
import com.nevin.incidentflow.outbox.OutboxEvent;
import com.nevin.incidentflow.outbox.OutboxEventRepository;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class AlertApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private org.springframework.boot.resttestclient.TestRestTemplate restTemplate;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    private AlertRequest sampleRequest(String externalEventId) {
        AlertRequest request = new AlertRequest();
        request.setExternalEventId(externalEventId);
        request.setSource("monitoring-service");
        request.setService("payments-api");
        request.setAlertType("HIGH_ERROR_RATE");
        request.setResourceId("checkout-handler");
        request.setSeverity(Alert.Severity.HIGH);
        request.setOccurredAt(OffsetDateTime.now());
        return request;
    }

    @Test
    void alertAndOutboxEventAreCreatedAtomically() {
        String externalEventId = "evt-itest-" + UUID.randomUUID();

        ResponseEntity<Alert> response = restTemplate.postForEntity(
                "/api/v1/alerts", sampleRequest(externalEventId), Alert.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        UUID alertId = response.getBody().getId();

        Optional<Alert> savedAlert = alertRepository.findById(alertId);
        assertThat(savedAlert).isPresent();

        List<OutboxEvent> outboxEvents = outboxEventRepository.findAll().stream()
                .filter(e -> e.getAggregateId().equals(alertId))
                .toList();

        assertThat(outboxEvents).hasSize(1);
        assertThat(outboxEvents.get(0).getPartitionKey()).isEqualTo(savedAlert.get().getFingerprint());
    }

    @Test
    void duplicateExternalEventIdDoesNotCreateSecondAlert() {
        String externalEventId = "evt-itest-dup-" + UUID.randomUUID();
        AlertRequest request = sampleRequest(externalEventId);

        ResponseEntity<Alert> first = restTemplate.postForEntity("/api/v1/alerts", request, Alert.class);
        ResponseEntity<Alert> second = restTemplate.postForEntity("/api/v1/alerts", request, Alert.class);

        assertThat(first.getBody().getId()).isEqualTo(second.getBody().getId());

        long count = alertRepository.findAll().stream()
                .filter(a -> a.getExternalEventId().equals(externalEventId))
                .count();

        assertThat(count).isEqualTo(1);
    }

    @Test
    void outboxEventReachesKafka() {
        String externalEventId = "evt-itest-kafka-" + UUID.randomUUID();
        restTemplate.postForEntity("/api/v1/alerts", sampleRequest(externalEventId), Alert.class);

        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(KafkaTopicConfig.ALERTS_RECEIVED_TOPIC));

            await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(500));
                boolean found = false;
                for (ConsumerRecord<String, String> record : records) {
                    if (record.value().contains(externalEventId)) {
                        found = true;
                    }
                }
                assertThat(found).isTrue();
            });
        }
    }
}
