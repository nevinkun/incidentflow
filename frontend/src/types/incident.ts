export interface Team {
  id: string
  name: string
  description: string
  default: boolean
}

export interface Incident {
  id: string
  fingerprint: string
  service: string
  title: string
  team: Team
  severity: string
  status: string
  alertCount: number
  firstSeenAt: string
  lastSeenAt: string
  acknowledgedAt: string | null
  resolvedAt: string | null
}

export interface IncidentTimelineEvent {
  id: string
  eventType: string
  description: string
  alertId: string | null
  createdAt: string
}

export interface Alert {
  id: string
  externalEventId: string
  service: string
  alertType: string
  resourceId: string
  severity: string
  summary: string | null
  status: string
  occurredAt: string
}
