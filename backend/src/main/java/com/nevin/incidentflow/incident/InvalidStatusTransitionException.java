package com.nevin.incidentflow.incident;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class InvalidStatusTransitionException extends RuntimeException {

    public InvalidStatusTransitionException(Incident.Status from, Incident.Status to) {
        super("Cannot transition incident from " + from + " to " + to);
    }
}
