import { useState } from 'react'
import { useMutation, useQuery } from '@tanstack/react-query'
import { apiGet, apiPost } from '../api/client'
import type { AlertRequest, AlertResponse, AlertStatusResponse } from '../types/alert'

const PRESETS: Record<string, Partial<AlertRequest>> = {
  'Repeated payment errors': {
    service: 'payments-api', alertType: 'HIGH_ERROR_RATE',
    resourceId: 'checkout-handler', severity: 'HIGH',
  },
  'Database latency spike': {
    service: 'billing-service', alertType: 'DB_TIMEOUT',
    resourceId: 'invoice-worker', severity: 'MEDIUM',
  },
  'Authentication failures': {
    service: 'identity-service', alertType: 'AUTH_FAILURE',
    resourceId: 'login-api', severity: 'HIGH',
  },
  'Recommendation-service timeout': {
    service: 'recommendation-api', alertType: 'TIMEOUT',
    resourceId: 'recommendation-engine', severity: 'LOW',
  },
}

function emptyForm(): AlertRequest {
  return {
    externalEventId: `evt-sim-${Date.now()}`,
    source: 'operator-console',
    service: '', alertType: '', resourceId: '',
    severity: 'HIGH', summary: '', occurredAt: '',
    failureSimulation: 'NONE',
  }
}

function AlertStatusTracker({ alertId }: { alertId: string }) {
  const { data } = useQuery({
    queryKey: ['alert-status', alertId],
    queryFn: () => apiGet<AlertStatusResponse>(`/alerts/${alertId}/status`),
    refetchInterval: 2000,
  })

  return (
    <div className="mt-4 border border-gray-200 rounded-lg p-4">
      <div className="text-sm text-gray-500">Alert Status</div>
      <div data-testid="alert-status-value" className="text-lg font-semibold">{data?.status ?? 'Loading...'}</div>
    </div>
  )
}

function AlertSimulatorPage() {
  const [form, setForm] = useState<AlertRequest>(emptyForm())
  const [submittedId, setSubmittedId] = useState<string | null>(null)

  const mutation = useMutation({
    mutationFn: () =>
      apiPost<AlertResponse>('/alerts', { ...form, occurredAt: new Date().toISOString() }),
    onSuccess: (data) => setSubmittedId(data.id),
  })

  function applyPreset(name: string) {
    setForm({ ...form, ...PRESETS[name], externalEventId: `evt-sim-${Date.now()}` })
  }

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    mutation.mutate()
  }

  return (
    <div>
      <h2 className="text-xl font-semibold mb-4">Alert Simulator</h2>

      <div className="mb-4 space-x-2">
        {Object.keys(PRESETS).map((name) => (
          <button
            key={name}
            type="button"
            onClick={() => applyPreset(name)}
            className="text-sm border border-gray-300 rounded px-3 py-1 hover:bg-gray-100"
          >
            {name}
          </button>
        ))}
      </div>

      <form onSubmit={handleSubmit} className="space-y-3 max-w-md">
        <input
          className="w-full border border-gray-300 rounded px-3 py-2"
          data-testid="service-input"
          placeholder="Service"
          value={form.service}
          onChange={(e) => setForm({ ...form, service: e.target.value })}
        />
        <input
          className="w-full border border-gray-300 rounded px-3 py-2"
          data-testid="alert-type-input"
          placeholder="Alert Type"
          value={form.alertType}
          onChange={(e) => setForm({ ...form, alertType: e.target.value })}
        />
        <input
          className="w-full border border-gray-300 rounded px-3 py-2"
          data-testid="resource-id-input"
          placeholder="Resource ID"
          value={form.resourceId}
          onChange={(e) => setForm({ ...form, resourceId: e.target.value })}
        />
        <select
          className="w-full border border-gray-300 rounded px-3 py-2"
          value={form.severity}
          onChange={(e) => setForm({ ...form, severity: e.target.value })}
        >
          <option value="LOW">LOW</option>
          <option value="MEDIUM">MEDIUM</option>
          <option value="HIGH">HIGH</option>
          <option value="CRITICAL">CRITICAL</option>
        </select>
        <textarea
          className="w-full border border-gray-300 rounded px-3 py-2"
          placeholder="Summary (optional)"
          value={form.summary}
          onChange={(e) => setForm({ ...form, summary: e.target.value })}
        />
        <select
          className="w-full border border-gray-300 rounded px-3 py-2"
          data-testid="failure-simulation-select"
          value={form.failureSimulation}
          onChange={(e) => setForm({ ...form, failureSimulation: e.target.value })}
        >
          <option value="NONE">No simulated failure</option>
          <option value="TRANSIENT">Simulate transient failure</option>
          <option value="PERMANENT">Simulate permanent failure</option>
        </select>
        <button
          data-testid="submit-alert-button"
          type="submit"
          disabled={mutation.isPending}
          className="bg-black text-white rounded px-4 py-2 disabled:opacity-50"
        >
          {mutation.isPending ? 'Submitting...' : 'Submit Alert'}
        </button>
      </form>

      {mutation.isError && (
        <div className="mt-4 text-red-600">Submission failed. Check the required fields.</div>
      )}

      {submittedId && <AlertStatusTracker alertId={submittedId} />}
    </div>
  )
}

export default AlertSimulatorPage
