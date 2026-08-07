package com.nevin.incidentflow.alert;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class AlertFingerprintGenerator {

    private static final String DELIMITER = "|";

    public static String generate(String service, String alertType, String resourceId) {
        String canonical = normalize(service) + DELIMITER
                + normalize(alertType) + DELIMITER
                + normalize(resourceId);

        return sha256Hex(canonical);
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase();
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder();
            for (byte b : hashBytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
