package com.nevin.incidentflow.alert;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AlertFingerprintGeneratorTest {

    @Test
    void normalizesWhitespaceAndCaseBeforeHashing() {
        String a = AlertFingerprintGenerator.generate("Payments-API ", " HIGH_ERROR_RATE", "checkout-handler ");
        String b = AlertFingerprintGenerator.generate("payments-api", "high_error_rate", "checkout-handler");

        assertThat(a).isEqualTo(b);
    }

    @Test
    void identicalFieldsProduceIdenticalFingerprints() {
        String first = AlertFingerprintGenerator.generate("payments-api", "HIGH_ERROR_RATE", "checkout-handler");
        String second = AlertFingerprintGenerator.generate("payments-api", "HIGH_ERROR_RATE", "checkout-handler");

        assertThat(first).isEqualTo(second);
        assertThat(first).hasSize(64);
    }

    @Test
    void differentResourceIdsProduceDifferentFingerprints() {
        String first = AlertFingerprintGenerator.generate("payments-api", "HIGH_ERROR_RATE", "checkout-handler");
        String second = AlertFingerprintGenerator.generate("payments-api", "HIGH_ERROR_RATE", "refund-handler");

        assertThat(first).isNotEqualTo(second);
    }
}
