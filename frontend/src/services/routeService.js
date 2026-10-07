import { request, isApiMode } from './apiClient'
import { findSupportedPlace } from './placeCatalogue'

const normalize = route => {
  const stops=[...(route.stops||[])].sort((a,b)=>a.stopOrder-b.stopOrder)
  if(stops.length)return {...route,serviceType:route.serviceType||'COMMUTER',stops}
  const origin=findSupportedPlace(route.origin),destination=findSupportedPlace(route.destination)
  return {
    ...route,
    serviceType: route.serviceType || 'COMMUTER',
    stops: [
      { stopOrder: 1, name: route.origin, ...(origin ? { latitude: origin.latitude, longitude: origin.longitude } : {}) },
      { stopOrder: 2, name: route.destination, ...(destination ? { latitude: destination.latitude, longitude: destination.longitude } : {}) },
    ],
  }
}
const routeForTrip = (trip, data) => {
  if (trip.serviceType === 'STAFF') return data.staffRoutes.find(route => route.routeId === trip.routeId)
  return data.routes.find(route => route.id === trip.routeId)
}

export function matchRouteStops(route, { from = '', to = '' } = {}) {
  const stops = normalize(route).stops
  const needle = value => value.trim().toLocaleLowerCase()
  const match = (query, stop) => !query || needle(stop.name).includes(needle(query))
  const starts = stops.filter(stop => match(from, stop))
  const ends = stops.filter(stop => match(to, stop))
  if (from && !starts.length) return null
  if (to && !ends.length) return null
  if (from && to) {
    const ordered = starts.some(start => ends.some(end => start.stopOrder < end.stopOrder))
    if (!ordered) return null
  }
  const board = from ? starts[0] : stops[0]
  const alight = to ? ends.find(stop => stop.stopOrder > board.stopOrder) : stops[stops.length - 1]
  return { boardStop: board, alightStop: alight, servedStops: stops.filter(stop => stop.stopOrder >= board.stopOrder && stop.stopOrder <= alight.stopOrder) }
}

export function searchScheduledTrips(data, { from = '', to = '', date = '', serviceType = 'COMMUTER', passengers = 1 } = {}) {
  const today = new Date().toISOString().slice(0, 10)
  const trips = (data.trips || []).filter(trip => trip.status === 'Scheduled' && trip.date >= today && (!date || trip.date === date) && (trip.serviceType || 'COMMUTER') === serviceType)
  return trips.flatMap(trip => {
    const route = routeForTrip(trip, data)
    if (!route || route.status !== 'Active') return []
    const routeMatch = matchRouteStops(route, { from, to })
    if (!routeMatch) return []
    const booked = (data.bookings || []).filter(booking => booking.tripId === trip.id && booking.status !== 'Cancelled').reduce((sum, booking) => sum + Number(booking.seats || 0), 0)
    const staffBooked = (data.staffBookings || []).filter(booking => booking.tripId === trip.id && booking.status !== 'Cancelled').reduce((sum, booking) => sum + Number(booking.seats || 0), 0)
    const availableSeats = Math.max(0, Number(trip.seats || 0) - booked - staffBooked)
    return availableSeats >= Number(passengers) ? [{ trip, route: normalize(route), ...routeMatch, availableSeats }] : []
  }).sort((a, b) => a.trip.date.localeCompare(b.trip.date) || a.trip.departure.localeCompare(b.trip.departure))
}

export async function searchRoutesApi(params) {
  if (!isApiMode()) return { mode: 'DEMO', items: [] }
  return request(`/routes/search?${new URLSearchParams(params)}`)
}

export const getRouteStops = routeId => isApiMode() ? request(`/routes/${encodeURIComponent(routeId)}/stops`) : Promise.resolve({ mode: 'DEMO', items: [] })
export const getNearbyStops = params => isApiMode() ? request(`/stops/nearby?${new URLSearchParams(params)}`) : Promise.resolve({ mode: 'DEMO', items: [] })
