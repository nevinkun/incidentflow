package com.nevin.incidentflow.alert;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AlertRepository extends JpaRepository<Alert, UUID> {

    Optional<Alert> findByExternalEventId(String externalEventId);

    long countByStatus(Alert.Status status);

    List<Alert> findByIncidentId(UUID incidentId);
}
