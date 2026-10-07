/**
 * SmartMove API integration placeholder.
 *
 * Demo modules currently read/write browser localStorage through AppContext.
 * Replace these examples with fetch/HTTP client calls when a Spring Boot API
 * is available. No backend persistence, authentication, or payment processing
 * is implemented in this frontend demo.
 */
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

export async function apiRequest(path, options = {}) {
	const response = await fetch(`${API_BASE_URL}${path}`, {
		headers: { 'Content-Type': 'application/json', ...options.headers },
		...options,
	})
	if (!response.ok) throw new Error(`API request failed (${response.status})`)
	if (response.status === 204) return null
	return response.json()
}

export const apiIntegrationStatus = 'placeholder — not connected to a backend'