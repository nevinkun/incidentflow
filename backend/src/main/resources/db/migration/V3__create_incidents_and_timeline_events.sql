CREATE TABLE incidents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    fingerprint VARCHAR(64) NOT NULL,
    service VARCHAR(255) NOT NULL,
    title VARCHAR(500) NOT NULL,
    team_id UUID NOT NULL REFERENCES response_teams(id),
    severity VARCHAR(20) NOT NULL CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN'
        CHECK (status IN ('OPEN', 'ACKNOWLEDGED', 'RESOLVED')),
    alert_count INTEGER NOT NULL DEFAULT 0,
    first_seen_at TIMESTAMPTZ NOT NULL,
    last_seen_at TIMESTAMPTZ NOT NULL,
    acknowledged_at TIMESTAMPTZ,
    resolved_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_incidents_status_severity ON incidents (status, severity);
CREATE INDEX idx_incidents_service_status ON incidents (service, status);
CREATE INDEX idx_incidents_team_status ON incidents (team_id, status);
CREATE INDEX idx_incidents_fingerprint_status ON incidents (fingerprint, status);
CREATE INDEX idx_incidents_last_seen_at ON incidents (last_seen_at);

CREATE TABLE incident_timeline_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    incident_id UUID NOT NULL REFERENCES incidents(id),
    event_type VARCHAR(30) NOT NULL
        CHECK (event_type IN ('INCIDENT_CREATED', 'ALERT_ATTACHED', 'SEVERITY_INCREASED',
                               'TEAM_ASSIGNED', 'ACKNOWLEDGED', 'RESOLVED')),
    description TEXT NOT NULL,
    alert_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_timeline_events_incident_id ON incident_timeline_events (incident_id);

ALTER TABLE alerts
    ADD CONSTRAINT fk_alerts_incident FOREIGN KEY (incident_id) REFERENCES incidents(id);
