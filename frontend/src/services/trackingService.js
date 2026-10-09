import { request, isApiMode } from './apiClient'
import { distanceMetres, watchCurrentPosition } from './locationService'

export const TRACKING_CONFIG = Object.freeze({ minDistanceMetres: 100, maxIntervalMs: 15000, maximumAccuracyMetres: 80 })

export function startTripTracking(tripId, onState, config = TRACKING_CONFIG) {
  const apiEnabled = isApiMode()
  let lastSent = null
  let stopped = false
  const timer = window.setInterval(() => {
    if (lastSent && Date.now() - lastSent.sentAt >= config.maxIntervalMs) publish(lastSent.location, true)
  }, Math.min(config.maxIntervalMs, 1000))
  const publish = async (location, intervalElapsed = false) => {
    if (stopped || location.accuracy > config.maximumAccuracyMetres) return
    const moved = !lastSent || distanceMetres(lastSent.location, location) >= config.minDistanceMetres
    if (!moved && !intervalElapsed) return
    if (!apiEnabled) {
      lastSent = { location, sentAt: Date.now() }
      onState?.({ status: 'demo-local', location, lastUpdate: location.recordedAt, accuracy: location.accuracy, message: 'GPS is visible only in this active browser page. No location was sent or saved.' })
      return
    }
    try {
      onState?.({ status: 'sending', location })
      await request('/tracking/location', { method: 'POST', body: JSON.stringify({ tripId: Number(tripId), latitude: location.latitude, longitude: location.longitude, accuracyMeters: location.accuracy, speedKmh: location.speed == null ? null : Math.max(0, location.speed * 3.6), headingDegrees: location.heading, altitudeMeters: location.altitude, moving: location.speed != null ? location.speed > 0 : null }) })
      lastSent = { location, sentAt: Date.now() }
      onState?.({ status: 'active', lastUpdate: location.recordedAt, accuracy: location.accuracy })
    } catch (error) {
      onState?.({ status: 'error', message: error.message })
    }
  }
  const stopWatch = watchCurrentPosition(location => {
    if (location.accuracy > config.maximumAccuracyMetres) {
      onState?.({ status: 'low-accuracy', accuracy: location.accuracy, message: 'Waiting for a more accurate GPS fix.' })
      return
    }
    publish(location)
  }, error => onState?.({ status: 'error', message: error.message }))
  onState?.({ status: 'starting', message: 'Requesting location permission. Tracking runs only while this page is active.' })
  return () => { stopped = true; window.clearInterval(timer); stopWatch(); onState?.({ status: 'stopped' }) }
}

export function subscribeToTripLocation(tripId, onLocation, onState) {
  let stopped = false
  let inFlight = false
  const poll = async () => {
    if (stopped || inFlight || !isApiMode()) return
    inFlight = true
    try {
      const result = await request(`/tracking/trips/${encodeURIComponent(tripId)}/live`)
      if (!stopped && result) {
        const point = result.location ?? result
        if (Number.isFinite(Number(point.latitude)) && Number.isFinite(Number(point.longitude))) {
          onLocation?.({ ...point, latitude: Number(point.latitude), longitude: Number(point.longitude) })
          onState?.({ status: 'active' })
        } else onState?.({ status: 'unavailable', message: 'No GPS fix received yet.' })
      }
    } catch (error) { if (!stopped) onState?.({ status: 'error', message: error.message }) }
    finally { inFlight = false }
  }
  if (!isApiMode()) {
    onState?.({ status: 'unavailable', message: 'Live tracking requires API mode.' })
    return () => {}
  }
  poll()
  const timer = window.setInterval(poll, 15000)
  return () => { stopped = true; window.clearInterval(timer) }
}
