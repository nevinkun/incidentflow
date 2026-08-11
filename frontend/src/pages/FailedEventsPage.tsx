import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { apiGet, apiPost } from '../api/client'
import type { FailureRecord } from '../types/failure'

function FailedEventsPage() {
  const queryClient = useQueryClient()

  const { data: failures, isLoading } = useQuery({
    queryKey: ['failures'],
    queryFn: () => apiGet<FailureRecord[]>('/failures'),
  })

  const replay = useMutation({
    mutationFn: (failureId: string) => apiPost(`/failures/${failureId}/replay`, {}),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['failures'] }),
  })

  if (isLoading) return <div>Loading...</div>

  return (
    <div>
      <h2 className="text-xl font-semibold mb-4">Failed Events</h2>

      <table className="w-full text-sm text-left border border-gray-200">
        <thead className="bg-gray-50">
          <tr>
            <th className="p-2">Original Topic</th>
            <th className="p-2">Error</th>
            <th className="p-2">Retries</th>
            <th className="p-2">Failed At</th>
            <th className="p-2">Replay Status</th>
            <th className="p-2"></th>
          </tr>
        </thead>
        <tbody>
          {failures?.map((failure) => (
            <tr key={failure.id} className="border-t border-gray-200 hover:bg-gray-50">
              <td className="p-2">{failure.originalTopic}</td>
              <td className="p-2 max-w-xs truncate" title={failure.errorMessage}>
                {failure.errorMessage}
              </td>
              <td className="p-2">{failure.retryCount}</td>
              <td className="p-2">{new Date(failure.failedAt).toLocaleString()}</td>
              <td className="p-2">
                {failure.replayedAt
                  ? `Replayed at ${new Date(failure.replayedAt).toLocaleTimeString()}`
                  : 'Not replayed'}
              </td>
              <td className="p-2">
                <button
                  disabled={!!failure.replayedAt || replay.isPending}
                  onClick={() => replay.mutate(failure.id)}
                  className="border border-gray-300 rounded px-3 py-1 text-xs disabled:opacity-40"
                >
                  Replay
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

export default FailedEventsPage
