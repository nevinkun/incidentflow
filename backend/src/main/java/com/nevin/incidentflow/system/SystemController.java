package com.nevin.incidentflow.system;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system")
public class SystemController {

    private final SystemService systemService;

    public SystemController(SystemService systemService) {
        this.systemService = systemService;
    }

    @GetMapping("/summary")
    public ResponseEntity<SystemSummaryResponse> getSummary() {
        return ResponseEntity.ok(systemService.getSummary());
    }
}
