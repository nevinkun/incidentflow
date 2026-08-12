import { useParams, Link } from 'react-router'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { apiGet, apiPost } from '../api/client'
import type { Incident, IncidentTimelineEvent, Alert } from '../types/incident'

function IncidentDetailPage() {
  const { id } = useParams<{ id: string }>()
  const queryClient = useQueryClient()

  const { data: incident } = useQuery({
    queryKey: ['incident', id],
    queryFn: () => apiGet<Incident>(`/incidents/${id}`),
  })

  const { data: timeline } = useQuery({
    queryKey: ['incident-timeline', id],
    queryFn: () => apiGet<IncidentTimelineEvent[]>(`/incidents/${id}/timeline`),
  })

  const { data: alerts } = useQuery({
    queryKey: ['incident-alerts', id],
    queryFn: () => apiGet<Alert[]>(`/incidents/${id}/alerts`),
  })

  function invalidate() {
    queryClient.invalidateQueries({ queryKey: ['incident', id] })
    queryClient.invalidateQueries({ queryKey: ['incident-timeline', id] })
  }

  const acknowledge = useMutation({
    mutationFn: () => apiPost(`/incidents/${id}/acknowledge`, {}),
    onSuccess: invalidate,
  })

  const resolve = useMutation({
    mutationFn: () => apiPost(`/incidents/${id}/resolve`, {}),
    onSuccess: invalidate,
  })

  if (!incident) return <div>Loading...</div>

  return (
    <div>
      <Link to="/incidents" className="text-sm text-blue-600 hover:underline">&larr; Back to incidents</Link>
      <h2 className="text-xl font-semibold mt-2 mb-1">{incident.title}</h2>
      <div className="text-sm text-gray-500 mb-4">
        {incident.service} &middot; {incident.team.name} &middot; {incident.severity} &middot; {incident.status}
      </div>

      <div className="flex gap-2 mb-6">
        <button
          data-testid="acknowledge-button"
          disabled={incident.status !== 'OPEN' || acknowledge.isPending}
          onClick={() => acknowledge.mutate()}
          className="border border-gray-300 rounded px-3 py-1 text-sm disabled:opacity-40"
        >
          Acknowledge
        </button>
        <button
          data-testid="resolve-button"
          disabled={incident.status === 'RESOLVED' || resolve.isPending}
          onClick={() => resolve.mutate()}
          className="border border-gray-300 rounded px-3 py-1 text-sm disabled:opacity-40"
        >
          Resolve
        </button>
      </div>

      <h3 className="font-semibold mb-2">Attached Alerts ({alerts?.length ?? 0})</h3>
      <ul className="text-sm mb-6 space-y-1">
        {alerts?.map((alert) => (
          <li key={alert.id} className="border-b border-gray-100 pb-1">
            {alert.alertType} on {alert.resourceId} — {alert.severity}
          </li>
        ))}
      </ul>

      <h3 className="font-semibold mb-2">Timeline</h3>
      <ul className="text-sm space-y-1">
        {timeline?.map((event) => (
          <li key={event.id} className="border-b border-gray-100 pb-1">
            <span className="text-gray-400">{new Date(event.createdAt).toLocaleTimeString()}</span>{' '}
            <strong>{event.eventType}</strong> — {event.description}
          </li>
        ))}
      </ul>
    </div>
  )
}

export default IncidentDetailPage
