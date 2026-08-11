export interface SystemSummary {
  openIncidents: number
  criticalIncidents: number
  alertsReceived: number
  alertsProcessed: number
  failedEvents: number
  pendingOutboxEvents: number
}
