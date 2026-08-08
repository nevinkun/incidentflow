package com.nevin.incidentflow.outbox;

import com.nevin.incidentflow.messaging.KafkaTopicConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.annotation.Profile;

import java.time.OffsetDateTime;
import java.util.List;

@Profile("api")
@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final int batchSize;

    public OutboxPublisher(OutboxEventRepository outboxEventRepository,
                            KafkaTemplate<String, String> kafkaTemplate,
                            @Value("${incidentflow.outbox.batch-size:50}") int batchSize) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${incidentflow.outbox.poll-interval-ms:5000}")
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> batch = outboxEventRepository.findUnpublishedBatch(batchSize);

        for (OutboxEvent event : batch) {
            publish(event);
        }
    }

    private void publish(OutboxEvent event) {
        try {
            SendResult<String, String> result = kafkaTemplate
                    .send(KafkaTopicConfig.ALERTS_RECEIVED_TOPIC, event.getPartitionKey(), event.getPayload())
                    .get();

            event.setPublishedAt(OffsetDateTime.now());
            log.info("Published outbox event {} to partition {}", event.getId(),
                    result.getRecordMetadata().partition());
        } catch (Exception e) {
            event.setPublishAttempts(event.getPublishAttempts() + 1);
            event.setLastError(e.getMessage());
            log.warn("Failed to publish outbox event {} (attempt {})", event.getId(), event.getPublishAttempts(), e);
        }
    }
}
