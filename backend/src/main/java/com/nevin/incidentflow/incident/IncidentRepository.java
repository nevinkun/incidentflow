package com.nevin.incidentflow.incident;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, UUID> {

    long countByStatus(Incident.Status status);

    long countBySeverityAndStatusNot(Incident.Severity severity, Incident.Status excludedStatus);

    Optional<Incident> findFirstByFingerprintAndStatusNotAndLastSeenAtAfterOrderByLastSeenAtDesc(
            String fingerprint, Incident.Status excludedStatus, OffsetDateTime cutoff);

    @Query("SELECT i FROM Incident i WHERE (:status IS NULL OR i.status = :status) " +
        "AND (:severity IS NULL OR i.severity = :severity) " +
        "AND (:service IS NULL OR i.service = :service) " +
        "AND (:teamId IS NULL OR i.team.id = :teamId) " +
        "ORDER BY i.lastSeenAt DESC")
    List<Incident> findWithFilters(@Param("status") Incident.Status status,
                                @Param("severity") Incident.Severity severity,
                                @Param("service") String service,
                                @Param("teamId") UUID teamId);
}
