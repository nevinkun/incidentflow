package com.nevin.incidentflow.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    long countByPublishedAtIsNull();

    @Query(value = "SELECT * FROM outbox_events WHERE published_at IS NULL ORDER BY created_at ASC LIMIT :batchSize FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<OutboxEvent> findUnpublishedBatch(@Param("batchSize") int batchSize);
}
