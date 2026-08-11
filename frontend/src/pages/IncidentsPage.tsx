import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router'
import { apiGet } from '../api/client'
import type { Incident } from '../types/incident'

function IncidentsPage() {
  const [status, setStatus] = useState('')
  const [severity, setSeverity] = useState('')
  const [service, setService] = useState('')

  const { data: incidents, isLoading } = useQuery({
    queryKey: ['incidents', status, severity, service],
    queryFn: () => {
      const params = new URLSearchParams()
      if (status) params.set('status', status)
      if (severity) params.set('severity', severity)
      if (service) params.set('service', service)
      return apiGet<Incident[]>(`/incidents?${params.toString()}`)
    },
  })

  return (
    <div>
      <h2 className="text-xl font-semibold mb-4">Incidents</h2>

      <div className="flex gap-2 mb-4">
        <select value={status} onChange={(e) => setStatus(e.target.value)} className="border border-gray-300 rounded px-2 py-1">
          <option value="">All statuses</option>
          <option value="OPEN">OPEN</option>
          <option value="ACKNOWLEDGED">ACKNOWLEDGED</option>
          <option value="RESOLVED">RESOLVED</option>
        </select>
        <select value={severity} onChange={(e) => setSeverity(e.target.value)} className="border border-gray-300 rounded px-2 py-1">
          <option value="">All severities</option>
          <option value="LOW">LOW</option>
          <option value="MEDIUM">MEDIUM</option>
          <option value="HIGH">HIGH</option>
          <option value="CRITICAL">CRITICAL</option>
        </select>
        <input
          placeholder="Filter by service"
          value={service}
          onChange={(e) => setService(e.target.value)}
          className="border border-gray-300 rounded px-2 py-1"
        />
      </div>

      {isLoading && <div>Loading...</div>}

      <table className="w-full text-sm text-left border border-gray-200">
        <thead className="bg-gray-50">
          <tr>
            <th className="p-2">Title</th>
            <th className="p-2">Service</th>
            <th className="p-2">Team</th>
            <th className="p-2">Severity</th>
            <th className="p-2">Status</th>
            <th className="p-2">Alerts</th>
            <th className="p-2">Last Seen</th>
          </tr>
        </thead>
        <tbody>
          {incidents?.map((incident) => (
            <tr key={incident.id} className="border-t border-gray-200 hover:bg-gray-50">
              <td className="p-2">
                <Link to={`/incidents/${incident.id}`} className="text-blue-600 hover:underline">
                  {incident.title}
                </Link>
              </td>
              <td className="p-2">{incident.service}</td>
              <td className="p-2">{incident.team.name}</td>
              <td className="p-2">{incident.severity}</td>
              <td className="p-2">{incident.status}</td>
              <td className="p-2">{incident.alertCount}</td>
              <td className="p-2">{new Date(incident.lastSeenAt).toLocaleString()}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

export default IncidentsPage
