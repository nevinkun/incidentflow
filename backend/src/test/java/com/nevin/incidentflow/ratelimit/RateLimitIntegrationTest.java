package com.nevin.incidentflow.ratelimit;

import com.nevin.incidentflow.AbstractIntegrationTest;
import com.nevin.incidentflow.alert.Alert;
import com.nevin.incidentflow.alert.AlertRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void returns429AfterExceedingConfiguredLimit() {
        String source = "rate-limit-test-source-" + UUID.randomUUID();

        for (int i = 0; i < 100; i++) {
            ResponseEntity<Alert> response = restTemplate.postForEntity(
                    "/api/v1/alerts", buildRequest(source, i), Alert.class);
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        }

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/alerts", buildRequest(source, 100), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    private AlertRequest buildRequest(String source, int index) {
        AlertRequest request = new AlertRequest();
        request.setExternalEventId("evt-rl-" + index + "-" + UUID.randomUUID());
        request.setSource(source);
        request.setService("payments-api");
        request.setAlertType("HIGH_ERROR_RATE");
        request.setResourceId("checkout-handler");
        request.setSeverity(Alert.Severity.HIGH);
        request.setOccurredAt(OffsetDateTime.now());
        return request;
    }
}
