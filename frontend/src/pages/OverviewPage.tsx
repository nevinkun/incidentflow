import { useQuery } from '@tanstack/react-query'
import { apiGet } from '../api/client'
import type { SystemSummary } from '../types/system'

function StatCard({ label, value }: { label: string; value: number }) {
  return (
    <div className="border border-gray-200 rounded-lg p-4">
      <div className="text-sm text-gray-500">{label}</div>
      <div className="text-2xl font-semibold">{value}</div>
    </div>
  )
}

function OverviewPage() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['system-summary'],
    queryFn: () => apiGet<SystemSummary>('/system/summary'),
  })

  if (isLoading) return <div>Loading...</div>
  if (error) return <div>Failed to load system summary</div>
  if (!data) return null

  return (
    <div>
      <h2 className="text-xl font-semibold mb-4">System Overview</h2>
      <div className="grid grid-cols-3 gap-4">
        <StatCard label="Open Incidents" value={data.openIncidents} />
        <StatCard label="Critical Incidents" value={data.criticalIncidents} />
        <StatCard label="Alerts Received" value={data.alertsReceived} />
        <StatCard label="Alerts Processed" value={data.alertsProcessed} />
        <StatCard label="Failed Events" value={data.failedEvents} />
        <StatCard label="Pending Outbox Events" value={data.pendingOutboxEvents} />
      </div>
    </div>
  )
}

export default OverviewPage
