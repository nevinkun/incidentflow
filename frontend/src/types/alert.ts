export interface AlertRequest {
  externalEventId: string
  source: string
  service: string
  alertType: string
  resourceId: string
  severity: string
  summary: string
  occurredAt: string
  failureSimulation: string
}

export interface AlertResponse {
  id: string
  status: string
}

export interface AlertStatusResponse {
  status: string
}
