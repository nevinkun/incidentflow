package com.nevin.incidentflow.incident;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface IncidentRepository extends JpaRepository<Incident, UUID> {

    Optional<Incident> findFirstByFingerprintAndStatusNotAndLastSeenAtAfterOrderByLastSeenAtDesc(
            String fingerprint, Incident.Status excludedStatus, OffsetDateTime cutoff);
}
