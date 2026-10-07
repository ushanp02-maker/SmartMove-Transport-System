import { request, isApiMode } from './apiClient'

export const listTrips = params => isApiMode() ? request(`/trips?${new URLSearchParams(params || {})}`) : Promise.resolve({ mode: 'DEMO', items: [] })
export const getTrip = id => isApiMode() ? request(`/trips/${encodeURIComponent(id)}`) : Promise.resolve({ mode: 'DEMO', item: null })
export const startAssignedTrip = id => isApiMode() ? request(`/driver/trips/${encodeURIComponent(id)}/status`, { method: 'PATCH', body: JSON.stringify({ status: 'IN_PROGRESS' }) }) : Promise.resolve({ mode: 'DEMO', status: 'IN_PROGRESS' })
export const endAssignedTrip = id => isApiMode() ? request(`/driver/trips/${encodeURIComponent(id)}/status`, { method: 'PATCH', body: JSON.stringify({ status: 'COMPLETED' }) }) : Promise.resolve({ mode: 'DEMO', status: 'COMPLETED' })
