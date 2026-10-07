import { request, isApiMode } from './apiClient'

export const createBooking = payload => isApiMode() ? request('/bookings', { method: 'POST', body: JSON.stringify(payload) }) : Promise.resolve({ mode: 'DEMO', item: null })
export const createStaffReservation = payload => isApiMode() ? request('/staff/bookings', { method: 'POST', body: JSON.stringify(payload) }) : Promise.resolve({ mode: 'DEMO', item: null })
export const listPassengerBookings = () => isApiMode() ? request('/passengers/me/bookings') : Promise.resolve({ mode: 'DEMO', items: [] })
