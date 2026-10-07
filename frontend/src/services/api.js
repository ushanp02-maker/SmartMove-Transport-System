/**
 * SmartMove API integration placeholder.
 *
 * Demo modules currently read/write browser localStorage through AppContext.
 * Replace these examples with fetch/HTTP client calls when a Spring Boot API
 * is available. No backend persistence, authentication, or payment processing
 * is implemented in this frontend demo.
 * See frontend/PORTAL_BACKEND_NOTES.md for the planned REST contract.
 * TODO: wire API calls, loading/error handling, and DTO validation here.
 * TODO: backend must hash passwords, issue/manage sessions or JWTs, and enforce
 * role/ownership permissions; the browser demo role is not a security boundary.
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

export const plannedEndpoints = {
	auth: ['/auth/passengers/register', '/auth/login', '/auth/logout', '/auth/me'],
	trips: ['/trips', '/trips/{tripId}'],
	passenger: ['/passengers/me/profile', '/passengers/me/bookings', '/passengers/me/payments', '/passengers/me/reviews'],
	bookings: ['/bookings', '/bookings/{bookingId}/ticket', '/bookings/{bookingId}/cancel'],
	driver: ['/drivers/me/trips', '/drivers/me/vehicle', '/drivers/me/profile', '/driver/trips/{tripId}/status', '/driver/issues'],
	announcements: ['/announcements?audience={role}'],
}