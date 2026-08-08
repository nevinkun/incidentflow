package com.nevin.incidentflow.messaging;

import com.nevin.incidentflow.incident.IncidentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Profile("worker")
@Component
public class AlertEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AlertEventConsumer.class);

    private final JsonMapper jsonMapper;
    private final IncidentService incidentService;

    public AlertEventConsumer(JsonMapper jsonMapper, IncidentService incidentService) {
        this.jsonMapper = jsonMapper;
        this.incidentService = incidentService;
    }

    @KafkaListener(topics = KafkaTopicConfig.ALERTS_RECEIVED_TOPIC)
    public void onAlertReceived(String payload, Acknowledgment acknowledgment) {
        try {
            AlertEventPayload event = jsonMapper.readValue(payload, AlertEventPayload.class);
            incidentService.processAlertEvent(event);
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Failed to process alert event, not acknowledging - Kafka will redeliver", e);
        }
    }
}
