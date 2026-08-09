package com.nevin.incidentflow.failure;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class FailureNotFoundException extends RuntimeException {
    public FailureNotFoundException(UUID failureId) {
        super("Failure record not found: " + failureId);
    }
}
