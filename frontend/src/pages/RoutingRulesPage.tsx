import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { apiGet, apiPost, apiPatch, apiDelete } from '../api/client'
import type { Team, RoutingRule } from '../types/routing'

function RoutingRulesPage() {
  const queryClient = useQueryClient()
  const [newService, setNewService] = useState('')
  const [newTeamId, setNewTeamId] = useState('')

  const { data: teams } = useQuery({
    queryKey: ['teams'],
    queryFn: () => apiGet<Team[]>('/teams'),
  })

  const { data: rules, isLoading } = useQuery({
    queryKey: ['routing-rules'],
    queryFn: () => apiGet<RoutingRule[]>('/routing-rules'),
  })

  function invalidate() {
    queryClient.invalidateQueries({ queryKey: ['routing-rules'] })
  }

  const createRule = useMutation({
    mutationFn: () => apiPost('/routing-rules', { service: newService, teamId: newTeamId }),
    onSuccess: () => {
      invalidate()
      setNewService('')
      setNewTeamId('')
    },
  })

  const updateRule = useMutation({
    mutationFn: ({ ruleId, teamId, service }: { ruleId: string; teamId: string; service: string }) =>
      apiPatch(`/routing-rules/${ruleId}`, { service, teamId }),
    onSuccess: invalidate,
  })

  const deleteRule = useMutation({
    mutationFn: (ruleId: string) => apiDelete(`/routing-rules/${ruleId}`),
    onSuccess: invalidate,
  })

  if (isLoading) return <div>Loading...</div>

  return (
    <div>
      <h2 className="text-xl font-semibold mb-4">Routing Rules</h2>

      <table className="w-full text-sm text-left border border-gray-200 mb-6">
        <thead className="bg-gray-50">
          <tr>
            <th className="p-2">Service</th>
            <th className="p-2">Team</th>
            <th className="p-2"></th>
          </tr>
        </thead>
        <tbody>
          {rules?.map((rule) => (
            <tr key={rule.id} className="border-t border-gray-200">
              <td className="p-2">{rule.service}</td>
              <td className="p-2">
                <select
                  value={rule.team.id}
                  onChange={(e) =>
                    updateRule.mutate({ ruleId: rule.id, teamId: e.target.value, service: rule.service })
                  }
                  className="border border-gray-300 rounded px-2 py-1"
                >
                  {teams?.map((team) => (
                    <option key={team.id} value={team.id}>{team.name}</option>
                  ))}
                </select>
              </td>
              <td className="p-2">
                <button
                  onClick={() => deleteRule.mutate(rule.id)}
                  className="text-red-600 text-xs hover:underline"
                >
                  Delete
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      <h3 className="font-semibold mb-2">Add Mapping</h3>
      <div className="flex gap-2 max-w-md">
        <input
          placeholder="Service name"
          value={newService}
          onChange={(e) => setNewService(e.target.value)}
          className="border border-gray-300 rounded px-2 py-1 flex-1"
        />
        <select
          value={newTeamId}
          onChange={(e) => setNewTeamId(e.target.value)}
          className="border border-gray-300 rounded px-2 py-1"
        >
          <option value="">Select team</option>
          {teams?.map((team) => (
            <option key={team.id} value={team.id}>{team.name}</option>
          ))}
        </select>
        <button
          disabled={!newService || !newTeamId || createRule.isPending}
          onClick={() => createRule.mutate()}
          className="bg-black text-white rounded px-3 py-1 text-sm disabled:opacity-40"
        >
          Add
        </button>
      </div>

      {createRule.isError && (
        <div className="mt-2 text-red-600 text-sm">
          Failed to add — a mapping for that service may already exist.
        </div>
      )}
    </div>
  )
}

export default RoutingRulesPage
