package com.nevin.incidentflow.failure;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/failures")
public class FailureController {

    private final FailureService failureService;

    public FailureController(FailureService failureService) {
        this.failureService = failureService;
    }

    @GetMapping
    public ResponseEntity<List<FailureRecord>> listFailures() {
        return ResponseEntity.ok(failureService.listFailures());
    }

    @GetMapping("/{failureId}")
    public ResponseEntity<FailureRecord> getFailure(@PathVariable UUID failureId) {
        return ResponseEntity.ok(failureService.getFailure(failureId));
    }

    @PostMapping("/{failureId}/replay")
    public ResponseEntity<FailureRecord> replay(@PathVariable UUID failureId) {
        return ResponseEntity.ok(failureService.replay(failureId));
    }
}
