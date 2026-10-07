/** Shared future HTTP client. API mode is opt-in; demo mode remains local AppContext state. */
const DATA_MODE = import.meta.env.VITE_DATA_MODE || 'DEMO'
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || ''

export const isApiMode = () => DATA_MODE.toUpperCase() === 'API'
export const dataMode = isApiMode() ? 'API' : 'DEMO'

export async function request(path, options = {}) {
  if (!isApiMode()) throw new Error('API mode is not enabled. Set VITE_DATA_MODE=API and configure VITE_API_BASE_URL.')
  if (!API_BASE_URL) throw new Error('VITE_API_BASE_URL is required when API mode is enabled.')
  const response = await fetch(`${API_BASE_URL.replace(/\/$/, '')}${path}`, {
    ...options,
    headers: { Accept: 'application/json', ...(options.body ? { 'Content-Type': 'application/json' } : {}), ...options.headers },
  })
  if (!response.ok) {
    const body = await response.json().catch(() => null)
    throw new Error(body?.message || `API request failed (${response.status})`)
  }
  if (response.status === 204) return null
  return response.json()
}

export function demoOnly(operation) {
  return { mode: 'DEMO', operation }
}
