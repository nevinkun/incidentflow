package com.nevin.incidentflow.observability;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class KafkaHealthIndicator implements HealthIndicator {

    private final KafkaAdmin kafkaAdmin;

    public KafkaHealthIndicator(KafkaAdmin kafkaAdmin) {
        this.kafkaAdmin = kafkaAdmin;
    }

    @Override
    public Health health() {
        try (Admin admin = Admin.create(kafkaAdmin.getConfigurationProperties())) {
            DescribeClusterResult result = admin.describeCluster();
            String clusterId = result.clusterId().get(3, TimeUnit.SECONDS);
            int nodeCount = result.nodes().get(3, TimeUnit.SECONDS).size();
            return Health.up()
                    .withDetail("clusterId", clusterId)
                    .withDetail("nodes", nodeCount)
                    .build();
        } catch (Exception ex) {
            return Health.down(ex).build();
        }
    }
}
