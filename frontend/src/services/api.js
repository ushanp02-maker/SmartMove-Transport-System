/** Spring Boot integration facade. DEMO is the default and no API is connected. */
export { request as apiRequest, isApiMode, dataMode } from './apiClient'

export const apiIntegrationStatus = 'placeholder — no Spring Boot, Oracle, MongoDB, or payment service is connected'

export const plannedEndpoints = {
	auth: ['/auth/login', '/auth/register', '/auth/logout', '/auth/me'],
	routes: ['/routes', '/routes/search', '/routes/{id}/stops', '/stops/nearby'],
	trips: ['/trips', '/trips/{id}', '/bookings'],
	passenger: ['/passengers/me/profile', '/passengers/me/bookings', '/passengers/me/payments', '/passengers/me/reviews'],
	bookings: ['/bookings', '/bookings/{bookingId}/ticket', '/bookings/{bookingId}/cancel', '/passengers/me/bookings'],
	staff: ['/staff/routes', '/staff/trips', '/staff/bookings'],
	customTrips: ['/custom-trips', '/custom-trips/my', '/admin/custom-trips', '/admin/custom-trips/{id}/status', '/admin/custom-trips/{id}/assign'],
	driver: ['/drivers/me/trips', '/drivers/me/vehicle', '/drivers/me/profile', '/driver/trips/{tripId}/status', '/driver/issues'],
	tracking: ['/trips/{id}/location', '/admin/fleet/locations', 'WS /trips/{id}/locations'],
	adminStaff: ['/admin/staff/routes', '/admin/staff/routes/{id}', '/admin/staff/employees/{id}/eligibility'],
	adminDrivers: ['/admin/drivers', '/admin/drivers/{id}', '/admin/drivers/{id}/account-status'],
	superAdminAccounts: ['/admin/accounts', '/admin/accounts/{id}', '/admin/accounts/{id}/status'],
	announcements: ['/announcements?audience={role}'],
}