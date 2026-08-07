package com.nevin.incidentflow.alert;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @PostMapping
    public ResponseEntity<Alert> submitAlert(@Valid @RequestBody AlertRequest request) {
        Alert alert = alertService.submitAlert(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(alert);
    }

    @GetMapping("/{alertId}/status")
    public ResponseEntity<Map<String, String>> getAlertStatus(@PathVariable UUID alertId) {
        Alert alert = alertService.getAlertStatus(alertId);
        return ResponseEntity.ok(Map.of("status", alert.getStatus().name()));
    }
}
