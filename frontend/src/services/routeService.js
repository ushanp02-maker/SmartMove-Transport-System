
import { request, isApiMode } from './apiClient'
import { findSupportedPlace } from './placeCatalogue'

// Shared route normalization for DEMO and API records.
export function normalizeRoute(route) {
  if (!route) return null

  const routeId = route.routeId ?? route.id

  const rawStops = route.stops?.length
    ? route.stops
    : [
        {
          stopOrder: 1,
          name: route.origin,
          ...getPlaceCoordinates(route.origin),
        },
        {
          stopOrder: 2,
          name: route.destination,
          ...getPlaceCoordinates(route.destination),
        },
      ]

  const stops = rawStops
    .map((stop, index) => ({
      ...stop,
      name: stop.name ?? stop.stopName ?? '',
      stopOrder: Number(stop.stopOrder ?? index + 1),
    }))
    .sort((a, b) => a.stopOrder - b.stopOrder)
    .map(stop => ({
      ...stop,
      stopId:
        stop.stopId ??
        stop.id ??
        `${routeId}-STOP-${stop.stopOrder}`,
    }))

  return {
    ...route,
    id: routeId,
    routeId,
    status:
      String(route.status || 'ACTIVE').toUpperCase() ===
      'ACTIVE'
        ? 'Active'
        : 'Inactive',
    serviceType: route.serviceType || 'COMMUTER',
    stops,
  }
}

function getPlaceCoordinates(name) {
  const place = name ? findSupportedPlace(name) : null

  return place
    ? {
        latitude: place.latitude,
        longitude: place.longitude,
      }
    : {}
}

function routeForTrip(trip, data) {
  const routes =
    trip.serviceType === 'STAFF'
      ? data.staffRoutes || []
      : data.routes || []

  return routes.find(
    route =>
      String(route.routeId ?? route.id) ===
      String(trip.routeId)
  )
}

// Ensures boarding occurs before destination.
export function matchRouteStops(
  route,
  { from = '', to = '' } = {}
) {
  if (!route) return null

  const stops = normalizeRoute(route).stops
  if (stops.length < 2) return null

  const normalizeName = value =>
    String(value ?? '').trim().toLowerCase()

  const matches = (query, stop) =>
    !normalizeName(query) ||
    normalizeName(stop.name).includes(normalizeName(query))

  const boardingStops = stops.filter(stop =>
    matches(from, stop)
  )

  const destinationStops = stops.filter(stop =>
    matches(to, stop)
  )

  for (const boardStop of boardingStops) {
    const alightStop = destinationStops.find(
      stop => stop.stopOrder > boardStop.stopOrder
    )

    if (!alightStop) continue

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

  return null
}

export function resolveBookingStops(
  route,
  { boardingStopId, destinationStopId } = {}
) {
  if (!route) return null

  const stops = normalizeRoute(route).stops

  const boardStop = stops.find(
    stop =>
      String(stop.stopId) === String(boardingStopId)
  )

  const alightStop = stops.find(
    stop =>
      String(stop.stopId) === String(destinationStopId)
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

// Existing DEMO-mode trip search remains available.
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
    `${now.getFullYear()}-` +
    `${String(now.getMonth() + 1).padStart(2, '0')}-` +
    `${String(now.getDate()).padStart(2, '0')}`

  const passengerCount = Number(passengers)

  if (
    !Number.isInteger(passengerCount) ||
    passengerCount < 1
  ) {
    return []
  }

  const trips = (data.trips || []).filter(
    trip =>
      String(trip.status).toUpperCase() ===
        'SCHEDULED' &&
      trip.date >= today &&
      (!date || trip.date === date) &&
      (trip.serviceType || 'COMMUTER') === serviceType
  )

  return trips
    .flatMap(trip => {
      const route = routeForTrip(trip, data)

      if (!route) return []

      const normalizedRoute = normalizeRoute(route)

      if (normalizedRoute.status !== 'Active') {
        return []
      }

      const routeMatch = matchRouteStops(
        normalizedRoute,
        { from, to }
      )

      if (!routeMatch) return []

      const booked = (data.bookings || [])
        .filter(
          booking =>
            booking.tripId === trip.id &&
            String(booking.status).toUpperCase() !==
              'CANCELLED'
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
            String(booking.status).toUpperCase() !==
              'CANCELLED'
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
        String(a.trip.departure).localeCompare(
          String(b.trip.departure)
        )
    )
}

// ============================================
// ORACLE / SPRING BOOT ROUTE API
// ============================================

export async function listRoutesApi() {
  if (!isApiMode()) {
    return { mode: 'DEMO', items: [] }
  }

  const routes = await request('/routes')
  return routes.map(normalizeRoute)
}

export async function getActiveRoutesApi() {
  if (!isApiMode()) {
    return { mode: 'DEMO', items: [] }
  }

  const routes = await request('/routes/active')
  return routes.map(normalizeRoute)
}

export async function searchRoutesApi(params = {}) {
  if (!isApiMode()) {
    return { mode: 'DEMO', items: [] }
  }

  const keyword =
    typeof params === 'string'
      ? params
      : params.keyword ??
        params.from ??
        params.to ??
        ''

  const query = new URLSearchParams()

  if (keyword.trim()) {
    query.set('keyword', keyword.trim())
  }

  const url = query.toString()
    ? `/routes/search?${query}`
    : '/routes/search'

  const routes = await request(url)
  return routes.map(normalizeRoute)
}

export async function getRouteApi(routeId) {
  if (!isApiMode()) {
    return { mode: 'DEMO', item: null }
  }

  const route = await request(
    `/routes/${encodeURIComponent(routeId)}`
  )

  return normalizeRoute(route)
}

export async function getRouteDetailsApi(routeId) {
  if (!isApiMode()) {
    return { mode: 'DEMO', item: null }
  }

  const details = await request(
    `/routes/${encodeURIComponent(routeId)}/details`
  )

  return normalizeRoute(details)
}

export async function getRouteStops(routeId) {
  if (!isApiMode()) {
    return { mode: 'DEMO', items: [] }
  }

  const details = await getRouteDetailsApi(routeId)

  return details.stops
}

export async function findRoutesBetweenStops(
  boardingStop,
  destinationStop
) {
  if (!isApiMode()) {
    return { mode: 'DEMO', items: [] }
  }

  const query = new URLSearchParams({
    boardingStop,
    destinationStop,
  })

  const routes = await request(
    `/routes/between-stops?${query}`
  )

  return routes.map(normalizeRoute)
}

export async function getRouteSegment(
  routeId,
  boardingStopId,
  destinationStopId
) {
  if (!isApiMode()) {
    return { mode: 'DEMO', item: null }
  }

  const query = new URLSearchParams({
    boardingStopId: String(boardingStopId),
    destinationStopId: String(destinationStopId),
  })

  return request(
    `/routes/${encodeURIComponent(routeId)}/segment?${query}`
  )
}

export async function getNearbyStops() {
  if (!isApiMode()) {
    return { mode: 'DEMO', items: [] }
  }

  throw new Error(
    'Nearby-stop API is not yet implemented in Spring Boot.'
  )
}
