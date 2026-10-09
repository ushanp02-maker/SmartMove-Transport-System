import { request, isApiMode } from './apiClient'

const demo = (items = []) => Promise.resolve({ mode: 'DEMO', items })

export const createBooking = payload =>
  isApiMode()
    ? request('/bookings', { method: 'POST', body: JSON.stringify(payload) })
    : demo()

export const getSeatAvailability = ({ tripId, boardingStopId, destinationStopId }) =>
  isApiMode()
    ? request(`/bookings/availability?${new URLSearchParams({ tripId, boardingStopId, destinationStopId })}`)
    : demo()

export const getBookingQuote = ({ tripId, boardingStopId, destinationStopId, seatCount = 1 }) =>
  isApiMode()
    ? request(`/bookings/quote?${new URLSearchParams({ tripId, boardingStopId, destinationStopId, seatCount })}`)
    : demo()

export const listPassengerBookings = async passengerId => {
  if (!isApiMode()) return demo()
  const id = passengerId ?? (await request('/users/me/passenger')).passengerId
  if (!id) throw new Error('No passenger profile is linked to this account.')
  return request(`/bookings/passenger/${encodeURIComponent(id)}`)
}

export const getBooking = bookingId =>
  isApiMode() ? request(`/bookings/${encodeURIComponent(bookingId)}`) : demo()

export const cancelBooking = bookingId =>
  isApiMode()
    ? request(`/bookings/${encodeURIComponent(bookingId)}/cancel`, { method: 'PATCH' })
    : demo()

// Staff requests are not seat bookings; they use a separate workflow.
export const createStaffReservation = () =>
  Promise.reject(new Error('Staff reservations must use the staff transport request workflow.'))
