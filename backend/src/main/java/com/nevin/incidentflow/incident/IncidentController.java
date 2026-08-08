package com.nevin.incidentflow.incident;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/incidents")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @GetMapping
    public ResponseEntity<List<Incident>> listIncidents(
            @RequestParam(required = false) Incident.Status status,
            @RequestParam(required = false) Incident.Severity severity,
            @RequestParam(required = false) String service,
            @RequestParam(required = false) UUID teamId) {
        return ResponseEntity.ok(incidentService.listIncidents(status, severity, service, teamId));
    }

    @GetMapping("/{incidentId}")
    public ResponseEntity<Incident> getIncident(@PathVariable UUID incidentId) {
        return ResponseEntity.ok(incidentService.getIncident(incidentId));
    }

    @GetMapping("/{incidentId}/timeline")
    public ResponseEntity<List<IncidentTimelineEvent>> getTimeline(@PathVariable UUID incidentId) {
        return ResponseEntity.ok(incidentService.getTimeline(incidentId));
    }

    @PostMapping("/{incidentId}/acknowledge")
    public ResponseEntity<Incident> acknowledge(@PathVariable UUID incidentId) {
        return ResponseEntity.ok(incidentService.acknowledge(incidentId));
    }

    @PostMapping("/{incidentId}/resolve")
    public ResponseEntity<Incident> resolve(@PathVariable UUID incidentId) {
        return ResponseEntity.ok(incidentService.resolve(incidentId));
    }
}
