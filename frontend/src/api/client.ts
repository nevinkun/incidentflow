const BASE_URL = '/api/v1'

export async function apiGet<T>(path: string): Promise<T> {
  const response = await fetch(`${BASE_URL}${path}`)
  if (!response.ok) {
    throw new Error(`Request failed: ${response.status}`)
  }
  return response.json()
}

export async function apiPost<T>(path: string, body: unknown): Promise<T> {
  const response = await fetch(`${BASE_URL}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  if (!response.ok) {
    throw new Error(`Request failed: ${response.status}`)
  }
  return response.json()
}

export async function apiPatch<T>(path: string, body: unknown): Promise<T> {
  const response = await fetch(`${BASE_URL}${path}`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  if (!response.ok) {
    throw new Error(`Request failed: ${response.status}`)
  }
  return response.json()
}

export async function apiDelete(path: string): Promise<void> {
  const response = await fetch(`${BASE_URL}${path}`, { method: 'DELETE' })
  if (!response.ok) {
    throw new Error(`Request failed: ${response.status}`)
  }
}
