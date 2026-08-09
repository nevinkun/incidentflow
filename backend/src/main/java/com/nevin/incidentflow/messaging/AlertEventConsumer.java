package com.nevin.incidentflow.messaging;

import com.nevin.incidentflow.failure.FailureRecord;
import com.nevin.incidentflow.failure.FailureRecordRepository;
import com.nevin.incidentflow.failure.PermanentProcessingException;
import com.nevin.incidentflow.incident.IncidentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.RetryTopicHeaders;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

@Profile("worker")
@Component
public class AlertEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AlertEventConsumer.class);

    private final JsonMapper jsonMapper;
    private final IncidentService incidentService;
    private final FailureRecordRepository failureRecordRepository;

    public AlertEventConsumer(JsonMapper jsonMapper, IncidentService incidentService,
                               FailureRecordRepository failureRecordRepository) {
        this.jsonMapper = jsonMapper;
        this.incidentService = incidentService;
        this.failureRecordRepository = failureRecordRepository;
    }

    @RetryableTopic(
            attempts = "4",
            backOff = @BackOff(delay = 1000, multiplier = 3, maxDelay = 10000),
            exclude = {PermanentProcessingException.class},
            retryTopicSuffix = ".retry",
            dltTopicSuffix = ".dlt")
    @KafkaListener(topics = KafkaTopicConfig.ALERTS_RECEIVED_TOPIC)
    public void onAlertReceived(String payload, Acknowledgment acknowledgment,
                                 @Header(name = RetryTopicHeaders.DEFAULT_HEADER_ATTEMPTS, required = false) Integer attemptsHeader) {
        AlertEventPayload event = parsePayload(payload);
        int deliveryAttempt = (attemptsHeader == null) ? 1 : attemptsHeader;

        incidentService.processAlertEvent(event, deliveryAttempt);
        acknowledgment.acknowledge();
    }

    @DltHandler
    public void onDeadLetter(String payload,
                              @Header("kafka_original-topic") String originalTopic,
                              @Header("kafka_original-partition") int originalPartition,
                              @Header("kafka_original-offset") long originalOffset,
                              @Header("kafka_exception-fqcn") String exceptionType,
                              @Header("kafka_exception-message") String errorMessage,
                              @Header(name = RetryTopicHeaders.DEFAULT_HEADER_ATTEMPTS, required = false) Integer attemptsHeader) {
        UUID originalEventId = extractEventId(payload);
        int retryCount = (attemptsHeader == null) ? 0 : attemptsHeader;

        log.warn("Dead-lettering event {} from {} after {} attempts: {}",
                originalEventId, originalTopic, retryCount, errorMessage);

        FailureRecord record = new FailureRecord(
                originalEventId, originalTopic, originalPartition, originalOffset,
                payload, exceptionType, errorMessage, retryCount);

        failureRecordRepository.save(record);
    }

    private AlertEventPayload parsePayload(String payload) {
        try {
            return jsonMapper.readValue(payload, AlertEventPayload.class);
        } catch (JacksonException e) {
            throw new PermanentProcessingException("Malformed event payload: " + e.getMessage());
        }
    }

    private UUID extractEventId(String payload) {
        try {
            return jsonMapper.readValue(payload, AlertEventPayload.class).getEventId();
        } catch (JacksonException e) {
            return UUID.randomUUID();
        }
    }
}
