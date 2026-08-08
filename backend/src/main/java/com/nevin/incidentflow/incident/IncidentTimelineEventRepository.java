package com.nevin.incidentflow.incident;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IncidentTimelineEventRepository extends JpaRepository<IncidentTimelineEvent, UUID> {
}
