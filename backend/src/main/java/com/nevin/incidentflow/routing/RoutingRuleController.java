package com.nevin.incidentflow.routing;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/routing-rules")
public class RoutingRuleController {

    private final RoutingRuleService routingRuleService;

    public RoutingRuleController(RoutingRuleService routingRuleService) {
        this.routingRuleService = routingRuleService;
    }

    @GetMapping
    public ResponseEntity<List<RoutingRule>> listRules() {
        return ResponseEntity.ok(routingRuleService.listRules());
    }

    @PostMapping
    public ResponseEntity<RoutingRule> createRule(@jakarta.validation.Valid @RequestBody RoutingRuleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(routingRuleService.createRule(request));
    }

    @PatchMapping("/{ruleId}")
    public ResponseEntity<RoutingRule> updateRule(@PathVariable UUID ruleId,
                                                   @jakarta.validation.Valid @RequestBody RoutingRuleRequest request) {
        return ResponseEntity.ok(routingRuleService.updateRule(ruleId, request));
    }

    @DeleteMapping("/{ruleId}")
    public ResponseEntity<Void> deleteRule(@PathVariable UUID ruleId) {
        routingRuleService.deleteRule(ruleId);
        return ResponseEntity.noContent().build();
    }
}
