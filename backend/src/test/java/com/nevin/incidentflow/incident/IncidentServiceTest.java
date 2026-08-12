package com.nevin.incidentflow.incident;

import com.nevin.incidentflow.alert.Alert;
import com.nevin.incidentflow.alert.AlertRepository;
import com.nevin.incidentflow.alert.FailureSimulation;
import com.nevin.incidentflow.failure.PermanentProcessingException;
import com.nevin.incidentflow.failure.TransientProcessingException;
import com.nevin.incidentflow.idempotency.ProcessedEventRepository;
import com.nevin.incidentflow.messaging.AlertEventPayload;
import com.nevin.incidentflow.routing.RoutingRule;
import com.nevin.incidentflow.routing.RoutingRuleRepository;
import com.nevin.incidentflow.team.ResponseTeam;
import com.nevin.incidentflow.team.ResponseTeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock private AlertRepository alertRepository;
    @Mock private IncidentRepository incidentRepository;
    @Mock private IncidentTimelineEventRepository timelineEventRepository;
    @Mock private ProcessedEventRepository processedEventRepository;
    @Mock private ResponseTeamRepository responseTeamRepository;
    @Mock private RoutingRuleRepository routingRuleRepository;
    @Mock private CorrelationCacheService correlationCacheService;

    private IncidentService incidentService;

    @BeforeEach
    void setUp() {
        incidentService = new IncidentService(
                alertRepository, incidentRepository, timelineEventRepository,
                processedEventRepository, responseTeamRepository, routingRuleRepository,
                correlationCacheService, 15L, 2);

        lenient().when(processedEventRepository.existsById(any())).thenReturn(false);
        lenient().when(correlationCacheService.get(anyString())).thenReturn(Optional.empty());
        lenient().when(alertRepository.findById(any())).thenReturn(Optional.of(sampleAlert()));
    }

    private AlertEventPayload payload(String severity, String failureSimulation, int alertId) {
        AlertEventPayload event = new AlertEventPayload();
        event.setEventId(UUID.randomUUID());
        event.setAlertId(UUID.randomUUID());
        event.setFingerprint("fp-" + alertId);
        event.setService("payments-api");
        event.setAlertType("HIGH_ERROR_RATE");
        event.setResourceId("checkout-handler");
        event.setSeverity(severity);
        event.setOccurredAt(OffsetDateTime.now().toString());
        event.setFailureSimulation(failureSimulation);
        return event;
    }

    private Alert sampleAlert() {
        return new Alert("evt-1", "monitoring-service", "payments-api", "HIGH_ERROR_RATE",
                "checkout-handler", Alert.Severity.HIGH, null, "fp-1", "{}", OffsetDateTime.now());
    }

    private ResponseTeam team(String name, boolean isDefault) {
        return new ResponseTeam(name, "desc", isDefault);
    }

    private Incident existingIncident(Incident.Severity severity) {
        return new Incident("fp-1", "payments-api", "payments-api - HIGH_ERROR_RATE",
                team("Payments Platform", false), severity, OffsetDateTime.now(), OffsetDateTime.now());
    }

    @Nested
    @DisplayName("Severity escalation")
    class SeverityEscalation {

        @Test
        void escalatesWhenNewSeverityIsHigher() {
            Incident incident = existingIncident(Incident.Severity.LOW);
            when(incidentRepository.findFirstByFingerprintAndStatusNotAndLastSeenAtAfterOrderByLastSeenAtDesc(
                    anyString(), any(), any())).thenReturn(Optional.of(incident));

            incidentService.processAlertEvent(payload("CRITICAL", "NONE", 1), 1);

            assertThat(incident.getSeverity()).isEqualTo(Incident.Severity.CRITICAL);

            ArgumentCaptor<IncidentTimelineEvent> captor = ArgumentCaptor.forClass(IncidentTimelineEvent.class);
            verify(timelineEventRepository, atLeastOnce()).save(captor.capture());
            assertThat(captor.getAllValues())
                    .anyMatch(e -> e.getEventType() == IncidentTimelineEvent.EventType.SEVERITY_INCREASED);
        }

        @Test
        void doesNotEscalateWhenNewSeverityIsLower() {
            Incident incident = existingIncident(Incident.Severity.HIGH);
            when(incidentRepository.findFirstByFingerprintAndStatusNotAndLastSeenAtAfterOrderByLastSeenAtDesc(
                    anyString(), any(), any())).thenReturn(Optional.of(incident));

            incidentService.processAlertEvent(payload("LOW", "NONE", 1), 1);

            assertThat(incident.getSeverity()).isEqualTo(Incident.Severity.HIGH);

            ArgumentCaptor<IncidentTimelineEvent> captor = ArgumentCaptor.forClass(IncidentTimelineEvent.class);
            verify(timelineEventRepository).save(captor.capture());
            assertThat(captor.getAllValues())
                    .noneMatch(e -> e.getEventType() == IncidentTimelineEvent.EventType.SEVERITY_INCREASED);
        }
    }

    @Nested
    @DisplayName("Correlation branching")
    class CorrelationBranching {

        @Test
        void createsNewIncidentWhenNoActiveMatchExists() {
            when(incidentRepository.findFirstByFingerprintAndStatusNotAndLastSeenAtAfterOrderByLastSeenAtDesc(
                    anyString(), any(), any())).thenReturn(Optional.empty());
            when(responseTeamRepository.findByIsDefaultTrue()).thenReturn(Optional.of(team("General Ops", true)));
            when(routingRuleRepository.findByService(anyString())).thenReturn(Optional.empty());

            incidentService.processAlertEvent(payload("HIGH", "NONE", 1), 1);

            verify(incidentRepository).save(any(Incident.class));
        }

        @Test
        void attachesToExistingIncidentInsteadOfCreatingNew() {
            Incident incident = existingIncident(Incident.Severity.HIGH);
            when(incidentRepository.findFirstByFingerprintAndStatusNotAndLastSeenAtAfterOrderByLastSeenAtDesc(
                    anyString(), any(), any())).thenReturn(Optional.of(incident));

            incidentService.processAlertEvent(payload("LOW", "NONE", 1), 1);

            verify(incidentRepository, never()).save(any(Incident.class));
        }
    }

    @Nested
    @DisplayName("Resolved incidents are never reused")
    class ResolvedIncidentExclusion {

        @Test
        void alwaysExcludesResolvedStatusFromCorrelationQuery() {
            when(incidentRepository.findFirstByFingerprintAndStatusNotAndLastSeenAtAfterOrderByLastSeenAtDesc(
                    anyString(), any(), any())).thenReturn(Optional.empty());
            when(responseTeamRepository.findByIsDefaultTrue()).thenReturn(Optional.of(team("General Ops", true)));
            when(routingRuleRepository.findByService(anyString())).thenReturn(Optional.empty());

            incidentService.processAlertEvent(payload("HIGH", "NONE", 1), 1);

            verify(incidentRepository).findFirstByFingerprintAndStatusNotAndLastSeenAtAfterOrderByLastSeenAtDesc(
                    anyString(), eq(Incident.Status.RESOLVED), any());
        }
    }

    @Nested
    @DisplayName("Team routing fallback")
    class TeamRouting {

        @Test
        void usesExplicitRoutingRuleWhenPresent() {
            ResponseTeam explicitTeam = team("Payments Platform", false);
            when(incidentRepository.findFirstByFingerprintAndStatusNotAndLastSeenAtAfterOrderByLastSeenAtDesc(
                    anyString(), any(), any())).thenReturn(Optional.empty());
            when(routingRuleRepository.findByService("payments-api"))
                    .thenReturn(Optional.of(new RoutingRule("payments-api", explicitTeam)));

            ArgumentCaptor<Incident> captor = ArgumentCaptor.forClass(Incident.class);
            incidentService.processAlertEvent(payload("HIGH", "NONE", 1), 1);
            verify(incidentRepository).save(captor.capture());

            assertThat(captor.getValue().getTeam()).isEqualTo(explicitTeam);
        }

        @Test
        void fallsBackToDefaultTeamWhenNoRuleExists() {
            ResponseTeam defaultTeam = team("General Ops", true);
            when(incidentRepository.findFirstByFingerprintAndStatusNotAndLastSeenAtAfterOrderByLastSeenAtDesc(
                    anyString(), any(), any())).thenReturn(Optional.empty());
            when(routingRuleRepository.findByService(anyString())).thenReturn(Optional.empty());
            when(responseTeamRepository.findByIsDefaultTrue()).thenReturn(Optional.of(defaultTeam));

            ArgumentCaptor<Incident> captor = ArgumentCaptor.forClass(Incident.class);
            incidentService.processAlertEvent(payload("HIGH", "NONE", 1), 1);
            verify(incidentRepository).save(captor.capture());

            assertThat(captor.getValue().getTeam()).isEqualTo(defaultTeam);
        }

        @Test
        void throwsWhenNoRuleAndNoDefaultTeamConfigured() {
            when(incidentRepository.findFirstByFingerprintAndStatusNotAndLastSeenAtAfterOrderByLastSeenAtDesc(
                    anyString(), any(), any())).thenReturn(Optional.empty());
            when(routingRuleRepository.findByService(anyString())).thenReturn(Optional.empty());
            when(responseTeamRepository.findByIsDefaultTrue()).thenReturn(Optional.empty());

            assertThatThrownBy(() -> incidentService.processAlertEvent(payload("HIGH", "NONE", 1), 1))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("Transient vs permanent failure classification")
    class FailureClassification {

        @Test
        void permanentSimulationAlwaysThrowsRegardlessOfAttempt() {
            assertThatThrownBy(() -> incidentService.processAlertEvent(payload("HIGH", "PERMANENT", 5), 5))
                    .isInstanceOf(PermanentProcessingException.class);
        }

        @Test
        void transientSimulationThrowsWhileUnderAttemptThreshold() {
            assertThatThrownBy(() -> incidentService.processAlertEvent(payload("HIGH", "TRANSIENT", 1), 1))
                    .isInstanceOf(TransientProcessingException.class);
        }

        @Test
        void transientSimulationSucceedsAfterAttemptThreshold() {
            when(incidentRepository.findFirstByFingerprintAndStatusNotAndLastSeenAtAfterOrderByLastSeenAtDesc(
                    anyString(), any(), any())).thenReturn(Optional.empty());
            when(routingRuleRepository.findByService(anyString())).thenReturn(Optional.empty());
            when(responseTeamRepository.findByIsDefaultTrue()).thenReturn(Optional.of(team("General Ops", true)));

            incidentService.processAlertEvent(payload("HIGH", "TRANSIENT", 3), 3);

            verify(incidentRepository).save(any(Incident.class));
        }
    }

    @Nested
    @DisplayName("Legal incident status transitions")
    class StatusTransitions {

        @Test
        void acknowledgeSucceedsFromOpen() {
            Incident incident = existingIncident(Incident.Severity.HIGH);
            when(incidentRepository.findById(any())).thenReturn(Optional.of(incident));

            incidentService.acknowledge(UUID.randomUUID());

            assertThat(incident.getStatus()).isEqualTo(Incident.Status.ACKNOWLEDGED);
        }

        @Test
        void acknowledgeFailsWhenAlreadyAcknowledged() {
            Incident incident = existingIncident(Incident.Severity.HIGH);
            incident.setStatus(Incident.Status.ACKNOWLEDGED);
            when(incidentRepository.findById(any())).thenReturn(Optional.of(incident));

            assertThatThrownBy(() -> incidentService.acknowledge(UUID.randomUUID()))
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }

        @Test
        void resolveSucceedsFromAcknowledged() {
            Incident incident = existingIncident(Incident.Severity.HIGH);
            incident.setStatus(Incident.Status.ACKNOWLEDGED);
            when(incidentRepository.findById(any())).thenReturn(Optional.of(incident));

            incidentService.resolve(UUID.randomUUID());

            assertThat(incident.getStatus()).isEqualTo(Incident.Status.RESOLVED);
        }

        @Test
        void resolveFailsWhenAlreadyResolved() {
            Incident incident = existingIncident(Incident.Severity.HIGH);
            incident.setStatus(Incident.Status.RESOLVED);
            when(incidentRepository.findById(any())).thenReturn(Optional.of(incident));

            assertThatThrownBy(() -> incidentService.resolve(UUID.randomUUID()))
                    .isInstanceOf(InvalidStatusTransitionException.class);
        }
    }
}
