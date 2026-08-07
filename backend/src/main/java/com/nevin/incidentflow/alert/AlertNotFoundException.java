package com.nevin.incidentflow.alert;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class AlertNotFoundException extends RuntimeException {

    public AlertNotFoundException(UUID alertId) {
        super("Alert not found: " + alertId);
    }
}
