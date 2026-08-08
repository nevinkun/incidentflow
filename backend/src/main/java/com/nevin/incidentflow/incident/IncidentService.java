package com.nevin.incidentflow.incident;

import com.nevin.incidentflow.alert.Alert;
import com.nevin.incidentflow.alert.AlertRepository;
import com.nevin.incidentflow.idempotency.ProcessedEvent;
import com.nevin.incidentflow.idempotency.ProcessedEventRepository;
import com.nevin.incidentflow.messaging.AlertEventPayload;
import com.nevin.incidentflow.routing.RoutingRule;
import com.nevin.incidentflow.routing.RoutingRuleRepository;
import com.nevin.incidentflow.team.ResponseTeam;
import com.nevin.incidentflow.team.ResponseTeamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class IncidentService {

    private static final Logger log = LoggerFactory.getLogger(IncidentService.class);
    private static final String CONSUMER_NAME = "incident-processor";

    private final AlertRepository alertRepository;
    private final IncidentRepository incidentRepository;
    private final IncidentTimelineEventRepository timelineEventRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final ResponseTeamRepository responseTeamRepository;
    private final RoutingRuleRepository routingRuleRepository;
    private final CorrelationCacheService correlationCacheService;
    private final Duration correlationWindow;

    public IncidentService(AlertRepository alertRepository,
                            IncidentRepository incidentRepository,
                            IncidentTimelineEventRepository timelineEventRepository,
                            ProcessedEventRepository processedEventRepository,
                            ResponseTeamRepository responseTeamRepository,
                            RoutingRuleRepository routingRuleRepository,
                            CorrelationCacheService correlationCacheService,
                            @Value("${incidentflow.correlation.window-minutes:15}") long windowMinutes) {
        this.alertRepository = alertRepository;
        this.incidentRepository = incidentRepository;
        this.timelineEventRepository = timelineEventRepository;
        this.processedEventRepository = processedEventRepository;
        this.responseTeamRepository = responseTeamRepository;
        this.routingRuleRepository = routingRuleRepository;
        this.correlationCacheService = correlationCacheService;
        this.correlationWindow = Duration.ofMinutes(windowMinutes);
    }

    @Transactional
    public void processAlertEvent(AlertEventPayload event) {
        ProcessedEvent.ProcessedEventId processedEventId =
                new ProcessedEvent.ProcessedEventId(event.getEventId(), CONSUMER_NAME);

        if (processedEventRepository.existsById(processedEventId)) {
            log.info("Event {} already processed by {}, skipping", event.getEventId(), CONSUMER_NAME);
            return;
        }

        OffsetDateTime occurredAt = OffsetDateTime.parse(event.getOccurredAt());
        Incident.Severity severity = Incident.Severity.valueOf(event.getSeverity());

        Optional<Incident> existing = findActiveIncident(event.getFingerprint());

        Incident incident;
        if (existing.isPresent()) {
            incident = attachAlert(existing.get(), occurredAt, severity, event.getAlertId());
        } else {
            incident = createIncident(event.getFingerprint(), event.getService(), event.getAlertType(),
                    severity, occurredAt);
        }

        correlationCacheService.put(event.getFingerprint(), incident.getId());
        linkAlertToIncident(event.getAlertId(), incident.getId());
        processedEventRepository.save(new ProcessedEvent(event.getEventId(), CONSUMER_NAME));
    }

    private Optional<Incident> findActiveIncident(String fingerprint) {
        Optional<UUID> cachedIncidentId = correlationCacheService.get(fingerprint);

        if (cachedIncidentId.isPresent()) {
            Optional<Incident> cached = incidentRepository.findById(cachedIncidentId.get());
            if (cached.isPresent()) {
                return cached;
            }
        }

        OffsetDateTime cutoff = OffsetDateTime.now().minus(correlationWindow);
        return incidentRepository.findFirstByFingerprintAndStatusNotAndLastSeenAtAfterOrderByLastSeenAtDesc(
                fingerprint, Incident.Status.RESOLVED, cutoff);
    }

    private Incident attachAlert(Incident incident, OffsetDateTime occurredAt,
                                  Incident.Severity newSeverity, UUID alertId) {
        incident.setAlertCount(incident.getAlertCount() + 1);
        incident.setLastSeenAt(occurredAt);

        if (newSeverity.ordinal() > incident.getSeverity().ordinal()) {
            incident.setSeverity(newSeverity);
            timelineEventRepository.save(new IncidentTimelineEvent(
                    incident.getId(), IncidentTimelineEvent.EventType.SEVERITY_INCREASED,
                    "Severity increased to " + newSeverity, alertId));
        }

        timelineEventRepository.save(new IncidentTimelineEvent(
                incident.getId(), IncidentTimelineEvent.EventType.ALERT_ATTACHED,
                "Alert attached", alertId));

        return incident;
    }

    private Incident createIncident(String fingerprint, String service, String alertType,
                                     Incident.Severity severity, OffsetDateTime occurredAt) {
        ResponseTeam team = resolveTeam(service);
        String title = service + " - " + alertType;

        Incident incident = new Incident(fingerprint, service, title, team, severity, occurredAt, occurredAt);
        incident.setAlertCount(1);
        incidentRepository.save(incident);

        timelineEventRepository.save(new IncidentTimelineEvent(
                incident.getId(), IncidentTimelineEvent.EventType.INCIDENT_CREATED,
                "Incident created for " + service, null));

        timelineEventRepository.save(new IncidentTimelineEvent(
                incident.getId(), IncidentTimelineEvent.EventType.TEAM_ASSIGNED,
                "Assigned to " + team.getName(), null));

        return incident;
    }

    private ResponseTeam resolveTeam(String service) {
        Optional<RoutingRule> rule = routingRuleRepository.findByService(service);
        if (rule.isPresent()) {
            return rule.get().getTeam();
        }

        return responseTeamRepository.findByIsDefaultTrue()
                .orElseThrow(() -> new IllegalStateException(
                        "No routing rule or default team configured for service: " + service));
    }

    private void linkAlertToIncident(UUID alertId, UUID incidentId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalStateException("Alert not found: " + alertId));
        alert.setIncidentId(incidentId);
        alert.setStatus(Alert.Status.PROCESSED);
        alert.setProcessedAt(OffsetDateTime.now());
    }

    public List<Incident> listIncidents(Incident.Status status, Incident.Severity severity,
                                         String service, UUID teamId) {
        return incidentRepository.findWithFilters(status, severity, service, teamId);
    }

    public Incident getIncident(UUID incidentId) {
        return incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IncidentNotFoundException(incidentId));
    }

    public List<IncidentTimelineEvent> getTimeline(UUID incidentId) {
        getIncident(incidentId);
        return timelineEventRepository.findByIncidentIdOrderByCreatedAtAsc(incidentId);
    }

    @Transactional
    public Incident acknowledge(UUID incidentId) {
        Incident incident = getIncident(incidentId);

        if (incident.getStatus() != Incident.Status.OPEN) {
            throw new InvalidStatusTransitionException(incident.getStatus(), Incident.Status.ACKNOWLEDGED);
        }

        incident.setStatus(Incident.Status.ACKNOWLEDGED);
        incident.setAcknowledgedAt(OffsetDateTime.now());

        timelineEventRepository.save(new IncidentTimelineEvent(
                incident.getId(), IncidentTimelineEvent.EventType.ACKNOWLEDGED,
                "Incident acknowledged", null));

        return incident;
    }

    @Transactional
    public Incident resolve(UUID incidentId) {
        Incident incident = getIncident(incidentId);

        if (incident.getStatus() == Incident.Status.RESOLVED) {
            throw new InvalidStatusTransitionException(incident.getStatus(), Incident.Status.RESOLVED);
        }

        incident.setStatus(Incident.Status.RESOLVED);
        incident.setResolvedAt(OffsetDateTime.now());

        timelineEventRepository.save(new IncidentTimelineEvent(
                incident.getId(), IncidentTimelineEvent.EventType.RESOLVED,
                "Incident resolved", null));

        correlationCacheService.evict(incident.getFingerprint());

        return incident;
    }
}
