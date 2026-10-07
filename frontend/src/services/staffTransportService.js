import { request, isApiMode } from './apiClient'

export const listStaffRoutes = () => isApiMode() ? request('/staff/routes') : Promise.resolve({ mode: 'DEMO', items: [] })
export const listStaffTrips = params => isApiMode() ? request(`/staff/trips?${new URLSearchParams(params || {})}`) : Promise.resolve({ mode: 'DEMO', items: [] })
export const createStaffBooking = payload => isApiMode() ? request('/staff/bookings', { method: 'POST', body: JSON.stringify(payload) }) : Promise.resolve({ mode: 'DEMO', item: null })
