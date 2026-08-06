CREATE TABLE response_teams (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_response_teams_single_default
    ON response_teams (is_default)
    WHERE is_default = TRUE;

CREATE TABLE routing_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service VARCHAR(255) NOT NULL,
    team_id UUID NOT NULL REFERENCES response_teams(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_routing_rules_service UNIQUE (service)
);

INSERT INTO response_teams (name, description, is_default) VALUES
    ('General Operations', 'Default team for unmapped services', TRUE),
    ('Payments Platform', 'Owns payment processing services', FALSE),
    ('Identity', 'Owns authentication and identity services', FALSE),
    ('Personalization', 'Owns recommendation services', FALSE);

INSERT INTO routing_rules (service, team_id)
SELECT 'payments-api', id FROM response_teams WHERE name = 'Payments Platform';

INSERT INTO routing_rules (service, team_id)
SELECT 'identity-service', id FROM response_teams WHERE name = 'Identity';

INSERT INTO routing_rules (service, team_id)
SELECT 'recommendation-api', id FROM response_teams WHERE name = 'Personalization';
