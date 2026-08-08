package com.nevin.incidentflow.incident;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.List;

public interface IncidentTimelineEventRepository extends JpaRepository<IncidentTimelineEvent, UUID> {
    List<IncidentTimelineEvent> findByIncidentIdOrderByCreatedAtAsc(UUID incidentId);
}
