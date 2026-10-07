
import { request, isApiMode } from './apiClient'
import { findSupportedPlace } from './placeCatalogue'

// Normalize routes so every route has ordered stops.
// Older routes without a stop list still work.
const normalize = route => {
  const routeId = route.routeId || route.id

  const rawStops = route.stops?.length
    ? route.stops
    : [
        {
          stopOrder: 1,
          name: route.origin,
          ...(() => {
            const place = findSupportedPlace(route.origin)
            return place
              ? { latitude: place.latitude, longitude: place.longitude }
              : {}
          })(),
        },
        {
          stopOrder: 2,
          name: route.destination,
          ...(() => {
            const place = findSupportedPlace(route.destination)
            return place
              ? { latitude: place.latitude, longitude: place.longitude }
              : {}
          })(),
        },
      ]

  const stops = rawStops
    .map((stop, index) => ({
      ...stop,
      stopOrder: Number(stop.stopOrder ?? index + 1),
    }))
    .sort((a, b) => a.stopOrder - b.stopOrder)
    .map(stop => ({
      ...stop,
      stopId:
        stop.stopId ||
        stop.id ||
        `${routeId}-STOP-${stop.stopOrder}`,
    }))

  return {
    ...route,
    serviceType: route.serviceType || 'COMMUTER',
    stops,
  }
}

const routeForTrip = (trip, data) => {
  if (trip.serviceType === 'STAFF') {
    return data.staffRoutes.find(
      route => (route.routeId || route.id) === trip.routeId
    )
  }

  return data.routes.find(
    route => (route.routeId || route.id) === trip.routeId
  )
}

// Find a valid boarding and destination pair.
// The destination must appear AFTER boarding in route order.
export function matchRouteStops(
  route,
  { from = '', to = '' } = {}
) {
  if (!route) return null

  const stops = normalize(route).stops

  if (stops.length < 2) return null

  const normalizeName = value =>
    String(value ?? '').trim().toLocaleLowerCase()

  const matches = (query, stop) =>
    !normalizeName(query) ||
    normalizeName(stop.name).includes(normalizeName(query))

  const boardingStops = stops.filter(stop => matches(from, stop))
  const destinationStops = stops.filter(stop => matches(to, stop))

  for (const boardStop of boardingStops) {
    const alightStop = destinationStops.find(
      stop => stop.stopOrder > boardStop.stopOrder
    )

    if (!alightStop) continue

    const servedStops = stops.filter(
      stop =>
        stop.stopOrder >= boardStop.stopOrder &&
        stop.stopOrder <= alightStop.stopOrder
    )

    return {
      boardStop,
      alightStop,
      servedStops,
    }
  }

  return null
}

// Resolve a passenger's selected stops using stable identifiers.
// This is used when moving from search results to booking.
export function resolveBookingStops(
  route,
  { boardingStopId, destinationStopId } = {}
) {
  if (!route) return null

  const stops = normalize(route).stops

  const boardStop = stops.find(
    stop => String(stop.stopId) === String(boardingStopId)
  )

  const alightStop = stops.find(
    stop => String(stop.stopId) === String(destinationStopId)
  )

  if (!boardStop || !alightStop) return null

  if (boardStop.stopOrder >= alightStop.stopOrder) {
    return null
  }

  return {
    boardStop,
    alightStop,
    servedStops: stops.filter(
      stop =>
        stop.stopOrder >= boardStop.stopOrder &&
        stop.stopOrder <= alightStop.stopOrder
    ),
  }
}

// Find scheduled trips matching a passenger's requested journey.
export function searchScheduledTrips(
  data,
  {
    from = '',
    to = '',
    date = '',
    serviceType = 'COMMUTER',
    passengers = 1,
  } = {}
) {
  const now = new Date()
  const today =
    `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`

  const passengerCount = Number(passengers)

  if (
    !Number.isInteger(passengerCount) ||
    passengerCount < 1
  ) {
    return []
  }

  const trips = (data.trips || []).filter(
    trip =>
      trip.status === 'Scheduled' &&
      trip.date >= today &&
      (!date || trip.date === date) &&
      (trip.serviceType || 'COMMUTER') === serviceType
  )

  return trips
    .flatMap(trip => {
      const route = routeForTrip(trip, data)

      if (!route || route.status !== 'Active') {
        return []
      }

      const normalizedRoute = normalize(route)

      const routeMatch = matchRouteStops(
        normalizedRoute,
        { from, to }
      )

      if (!routeMatch) return []

      const booked = (data.bookings || [])
        .filter(
          booking =>
            booking.tripId === trip.id &&
            booking.status !== 'Cancelled'
        )
        .reduce(
          (sum, booking) =>
            sum + Number(booking.seats || 0),
          0
        )

      const staffBooked = (data.staffBookings || [])
        .filter(
          booking =>
            booking.tripId === trip.id &&
            booking.status !== 'Cancelled'
        )
        .reduce(
          (sum, booking) =>
            sum + Number(booking.seats || 0),
          0
        )

      const availableSeats = Math.max(
        0,
        Number(trip.seats || 0) - booked - staffBooked
      )

      if (availableSeats < passengerCount) {
        return []
      }

      return [
        {
          trip,
          route: normalizedRoute,
          ...routeMatch,
          availableSeats,
        },
      ]
    })
    .sort(
      (a, b) =>
        a.trip.date.localeCompare(b.trip.date) ||
        a.trip.departure.localeCompare(b.trip.departure)
    )
}

// Future Spring Boot API integration.
export async function searchRoutesApi(params) {
  if (!isApiMode()) {
    return { mode: 'DEMO', items: [] }
  }

  return request(
    `/routes/search?${new URLSearchParams(params)}`
  )
}

export const getRouteStops = routeId =>
  isApiMode()
    ? request(`/routes/${encodeURIComponent(routeId)}/stops`)
    : Promise.resolve({ mode: 'DEMO', items: [] })

export const getNearbyStops = params =>
  isApiMode()
    ? request(`/stops/nearby?${new URLSearchParams(params)}`)
    : Promise.resolve({ mode: 'DEMO', items: [] })
