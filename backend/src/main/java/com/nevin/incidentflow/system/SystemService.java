package com.nevin.incidentflow.system;

import com.nevin.incidentflow.alert.Alert;
import com.nevin.incidentflow.alert.AlertRepository;
import com.nevin.incidentflow.failure.FailureRecordRepository;
import com.nevin.incidentflow.incident.Incident;
import com.nevin.incidentflow.incident.IncidentRepository;
import com.nevin.incidentflow.outbox.OutboxEventRepository;
import org.springframework.stereotype.Service;

@Service
public class SystemService {

    private final IncidentRepository incidentRepository;
    private final AlertRepository alertRepository;
    private final FailureRecordRepository failureRecordRepository;
    private final OutboxEventRepository outboxEventRepository;

    public SystemService(IncidentRepository incidentRepository,
                          AlertRepository alertRepository,
                          FailureRecordRepository failureRecordRepository,
                          OutboxEventRepository outboxEventRepository) {
        this.incidentRepository = incidentRepository;
        this.alertRepository = alertRepository;
        this.failureRecordRepository = failureRecordRepository;
        this.outboxEventRepository = outboxEventRepository;
    }

    public SystemSummaryResponse getSummary() {
        return new SystemSummaryResponse(
                incidentRepository.countByStatus(Incident.Status.OPEN),
                incidentRepository.countBySeverityAndStatusNot(Incident.Severity.CRITICAL, Incident.Status.RESOLVED),
                alertRepository.count(),
                alertRepository.countByStatus(Alert.Status.PROCESSED),
                failureRecordRepository.count(),
                outboxEventRepository.countByPublishedAtIsNull());
    }
}
