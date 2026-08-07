package com.nevin.incidentflow.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String ALERTS_RECEIVED_TOPIC = "incidentflow.alerts.received";

    @Bean
    public NewTopic alertsReceivedTopic() {
        return TopicBuilder.name(ALERTS_RECEIVED_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
