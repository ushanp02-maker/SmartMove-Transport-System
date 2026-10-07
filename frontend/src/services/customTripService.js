import { request, isApiMode } from './apiClient'

export const CUSTOM_TRIP_STATUSES = ['PENDING', 'APPROVED', 'REJECTED', 'ASSIGNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED']
export const createCustomTripRequest = payload => isApiMode() ? request('/custom-trips', { method: 'POST', body: JSON.stringify(payload) }) : Promise.resolve({ mode: 'DEMO', item: null })
export const listMyCustomRequests = () => isApiMode() ? request('/custom-trips/my') : Promise.resolve({ mode: 'DEMO', items: [] })
export const listAdminCustomRequests = () => isApiMode() ? request('/admin/custom-trips') : Promise.resolve({ mode: 'DEMO', items: [] })
export const updateCustomRequestStatus = (id, status, notes) => isApiMode() ? request(`/admin/custom-trips/${encodeURIComponent(id)}/status`, { method: 'PATCH', body: JSON.stringify({ status, notes }) }) : Promise.resolve({ mode: 'DEMO', status })
export const assignCustomRequest = (id, assignment) => isApiMode() ? request(`/admin/custom-trips/${encodeURIComponent(id)}/assign`, { method: 'POST', body: JSON.stringify(assignment) }) : Promise.resolve({ mode: 'DEMO', assignment })
