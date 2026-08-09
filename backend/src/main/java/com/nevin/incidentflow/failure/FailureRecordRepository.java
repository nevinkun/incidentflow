package com.nevin.incidentflow.failure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FailureRecordRepository extends JpaRepository<FailureRecord, UUID> {

    List<FailureRecord> findAllByOrderByFailedAtDesc();
}
