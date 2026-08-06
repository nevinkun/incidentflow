CREATE TABLE alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_event_id VARCHAR(255) NOT NULL,
    source VARCHAR(255) NOT NULL,
    service VARCHAR(255) NOT NULL,
    alert_type VARCHAR(255) NOT NULL,
    resource_id VARCHAR(255) NOT NULL,
    severity VARCHAR(20) NOT NULL CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    summary TEXT,
    fingerprint VARCHAR(64) NOT NULL,
    metadata JSONB,
    status VARCHAR(20) NOT NULL DEFAULT 'RECEIVED'
        CHECK (status IN ('RECEIVED', 'QUEUED', 'PROCESSING', 'PROCESSED', 'FAILED')),
    incident_id UUID,
    occurred_at TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    processed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_alerts_external_event_id UNIQUE (external_event_id)
);

CREATE INDEX idx_alerts_fingerprint ON alerts (fingerprint);
CREATE INDEX idx_alerts_status ON alerts (status);
CREATE INDEX idx_alerts_incident_id ON alerts (incident_id);
CREATE INDEX idx_alerts_service_received_at ON alerts (service, received_at);
