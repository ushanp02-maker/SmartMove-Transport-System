import { request, isApiMode } from './apiClient'

const demo = () => Promise.resolve({ mode: 'DEMO', items: [] })

export const listTrips = () => isApiMode() ? request('/trips') : demo()
export const listUpcomingTrips = () => isApiMode() ? request('/trips/upcoming') : demo()
export const listUpcomingTripsByRoute = routeId =>
  isApiMode() ? request(`/trips/route/${encodeURIComponent(routeId)}/upcoming`) : demo()
export const getTrip = id =>
  isApiMode() ? request(`/trips/${encodeURIComponent(id)}`) : demo()
export const startAssignedTrip = id =>
  isApiMode() ? request(`/trips/${encodeURIComponent(id)}/status`, {
    method: 'PATCH', body: JSON.stringify({ status: 'IN_PROGRESS' }),
  }) : demo()
export const endAssignedTrip = id =>
  isApiMode() ? request(`/trips/${encodeURIComponent(id)}/status`, {
    method: 'PATCH', body: JSON.stringify({ status: 'COMPLETED' }),
  }) : demo()
