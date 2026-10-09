
/**
 * SmartMove shared HTTP client.
 *
 * DEMO mode:
 *   Uses existing frontend demo data.
 *
 * API mode:
 *   Connects to Spring Boot and Oracle.
 *
 * HTTP Basic credentials are held in memory only.
 * They are never saved to localStorage.
 */

const DATA_MODE = import.meta.env.VITE_DATA_MODE || 'DEMO'

const API_BASE_URL =
  import.meta.env.VITE_API_BASE_URL || ''

export const isApiMode = () =>
  DATA_MODE.toUpperCase() === 'API'

export const dataMode =
  isApiMode() ? 'API' : 'DEMO'

// Credentials exist only until logout or page refresh.
let basicCredentials = null

export function setApiCredentials(username, password) {
  if (!username || !password) {
    throw new Error('Username and password are required.')
  }

  basicCredentials = {
    username,
    password,
  }
}

export function clearApiCredentials() {
  basicCredentials = null
}

export function hasApiCredentials() {
  return basicCredentials !== null
}

function buildAuthorizationHeader() {
  if (!basicCredentials) return {}

  const credentials =
    `${basicCredentials.username}:${basicCredentials.password}`

  // Handles Unicode characters in credentials.
  const bytes = new TextEncoder().encode(credentials)

  let binary = ''

  for (const byte of bytes) {
    binary += String.fromCharCode(byte)
  }

  return {
    Authorization: `Basic ${btoa(binary)}`,
  }
}

export async function request(path, options = {}) {
  if (!isApiMode()) {
    throw new Error(
      'API mode is disabled. Set VITE_DATA_MODE=API.'
    )
  }

  if (!API_BASE_URL) {
    throw new Error(
      'VITE_API_BASE_URL is missing.'
    )
  }

  const base = API_BASE_URL.replace(/\/$/, '')

  const endpoint = path.startsWith('/')
    ? path
    : `/${path}`

  const headers = {
    Accept: 'application/json',
    ...buildAuthorizationHeader(),
    ...(options.body
      ? { 'Content-Type': 'application/json' }
      : {}),
    ...options.headers,
  }

  let response

  try {
    response = await fetch(`${base}${endpoint}`, {
      ...options,
      headers,
    })
  } catch {
    throw new Error(
      'Cannot connect to Spring Boot. Check that the backend is running on port 8080.'
    )
  }

  if (!response.ok) {
    const body = await response.json().catch(() => null)

    if (response.status === 401) {
      throw new Error(
        'Authentication required or credentials are invalid.'
      )
    }

    if (response.status === 403) {
      throw new Error(
        'You do not have permission to perform this action.'
      )
    }

    throw new Error(
      body?.message ||
      body?.detail ||
      `API request failed (${response.status}).`
    )
  }

  if (response.status === 204) {
    return null
  }

  const contentType =
    response.headers.get('content-type') || ''

  if (contentType.includes('application/json')) {
    return response.json()
  }

  return response.text()
}

export function demoOnly(operation) {
  return {
    mode: 'DEMO',
    operation,
  }
}
