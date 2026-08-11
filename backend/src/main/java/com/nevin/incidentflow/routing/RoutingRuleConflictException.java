package com.nevin.incidentflow.routing;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class RoutingRuleConflictException extends RuntimeException {
    public RoutingRuleConflictException(String service) {
        super("A routing rule already exists for service: " + service);
    }
}
