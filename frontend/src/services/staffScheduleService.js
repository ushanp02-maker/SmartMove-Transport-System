
/**
 * SmartMove staff transport recurring scheduler.
 * Frontend demo mode — no backend required.
 */

export const STAFF_WEEKDAYS = [
  'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'
]

const DAY_MS = 24 * 60 * 60 * 1000
const datePattern = /^\d{4}-\d{2}-\d{2}$/

function parseDate(dateString) {
  if (!datePattern.test(String(dateString))) return null

  const [year, month, day] = dateString.split('-').map(Number)
  const date = new Date(Date.UTC(year, month - 1, day))

  return date.toISOString().slice(0, 10) === dateString
    ? date
    : null
}

function dateString(date) {
  return date.toISOString().slice(0, 10)
}

function minuteOfDay(value) {
  if (!/^([01]\d|2[0-3]):[0-5]\d$/.test(String(value))) {
    return null
  }

  const [hours, minutes] = value.split(':').map(Number)
  return hours * 60 + minutes
}

function tripInterval(date, departure, arrival) {
  const day = parseDate(date)
  const startMinute = minuteOfDay(departure)
  const endMinute = minuteOfDay(arrival)

  if (
    !day ||
    startMinute === null ||
    endMinute === null ||
    startMinute === endMinute
  ) {
    return null
  }

  const start = day.getTime() + startMinute * 60000
  let end = day.getTime() + endMinute * 60000

  // Support trips that finish after midnight.
  if (end <= start) end += DAY_MS

  return { start, end }
}

function overlaps(left, right) {
  return left.start < right.end && right.start < left.end
}

/**
 * Generate selected service dates over a number of weeks.
 * Default: 4 weeks (28 days).
 */
export function recurringDates(
  startDate,
  operatingDays,
  weeks = 4
) {
  const first = parseDate(startDate)

  if (!first) {
    throw new Error('Select a valid first service date.')
  }

  if (
    !Array.isArray(operatingDays) ||
    !operatingDays.length ||
    operatingDays.some(day => !STAFF_WEEKDAYS.includes(day))
  ) {
    throw new Error('Select at least one valid operating day.')
  }

  if (
    !Number.isInteger(weeks) ||
    weeks < 1 ||
    weeks > 12
  ) {
    throw new Error(
      'Schedule duration must be between 1 and 12 weeks.'
    )
  }

  const selected = new Set(operatingDays)
  const dates = []

  for (let offset = 0; offset < weeks * 7; offset++) {
    const date = new Date(
      first.getTime() + offset * DAY_MS
    )

    const weekday =
      STAFF_WEEKDAYS[(date.getUTCDay() + 6) % 7]

    if (selected.has(weekday)) {
      dates.push(dateString(date))
    }
  }

  return dates
}

/**
 * Prepare recurring staff trips.
 *
 * Checks for:
 * - Duplicate route schedules
 * - Vehicle conflicts
 * - Driver conflicts
 *
 * If any conflict exists, no trips are generated.
 */
export function prepareRecurringStaffTrips({
  trips = [],
  routeId,
  companyId,
  companyName,
  vehicleId,
  driverId,
  startDate,
  operatingDays,
  departure,
  arrival,
  seats,
  weeks = 4,
}) {
  if (
    !routeId ||
    !companyId ||
    !vehicleId ||
    !driverId
  ) {
    throw new Error(
      'Company, route, vehicle and driver are required.'
    )
  }

  if (
    !Number.isInteger(Number(seats)) ||
    Number(seats) < 1
  ) {
    throw new Error(
      'Vehicle capacity must be a positive whole number.'
    )
  }

  if (!tripInterval(startDate, departure, arrival)) {
    throw new Error(
      'Enter valid departure and arrival times that are different.'
    )
  }

  const dates = recurringDates(
    startDate,
    operatingDays,
    weeks
  )

  if (!dates.length) {
    throw new Error(
      'No service dates match the selected operating days.'
    )
  }

  const conflicts = []

  const activeTrips = trips.filter(
    trip => !['Cancelled', 'Canceled'].includes(trip.status)
  )

  for (const date of dates) {
    const proposed = tripInterval(
      date,
      departure,
      arrival
    )

    for (const trip of activeTrips) {
      const existing = tripInterval(
        trip.date,
        trip.departure,
        trip.arrival
      )

      if (!existing || !overlaps(proposed, existing)) {
        continue
      }

      if (
        trip.routeId === routeId ||
        trip.vehicleId === vehicleId ||
        trip.driverId === driverId
      ) {
        const reason =
          trip.routeId === routeId
            ? 'duplicate route schedule'
            : trip.vehicleId === vehicleId
              ? 'vehicle conflict'
              : 'driver conflict'

        conflicts.push({
          date,
          tripId: trip.id,
          reason,
        })
      }
    }
  }

  if (conflicts.length) {
    return {
      trips: [],
      dates,
      conflicts,
      error:
        `${conflicts.length} schedule conflict(s) found. ` +
        'No trips were created.',
    }
  }

  const createdAt = new Date().toISOString()

  const generatedTrips = dates.map(date => ({
    id: `TR-${crypto.randomUUID().slice(0, 8).toUpperCase()}`,
    routeId,
    companyId,
    companyName,
    vehicleId,
    driverId,
    date,
    departure,
    arrival,
    seats: Number(seats),
    status: 'Scheduled',
    serviceType: 'STAFF',
    scheduleType: 'RECURRING',
    createdAt,
  }))

  return {
    trips: generatedTrips,
    dates,
    conflicts: [],
    error: null,
  }
}
