package com.nevin.incidentflow.routing;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class RoutingRuleNotFoundException extends RuntimeException {
    public RoutingRuleNotFoundException(UUID ruleId) {
        super("Routing rule not found: " + ruleId);
    }
}
