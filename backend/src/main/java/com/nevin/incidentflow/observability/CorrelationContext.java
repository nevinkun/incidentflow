package com.nevin.incidentflow.observability;

import org.slf4j.MDC;

public final class CorrelationContext implements AutoCloseable {

    private CorrelationContext() {
    }

    public static CorrelationContext open() {
        return new CorrelationContext();
    }

    public CorrelationContext put(String key, Object value) {
        if (value != null) {
            MDC.put(key, value.toString());
        }
        return this;
    }

    @Override
    public void close() {
        MDC.clear();
    }
}
