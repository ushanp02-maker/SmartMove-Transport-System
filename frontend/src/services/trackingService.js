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
      await request(`/trips/${encodeURIComponent(tripId)}/location`, { method: 'POST', body: JSON.stringify(location) })
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
  if (!isApiMode() || !('WebSocket' in window)) {
    onState?.({ status: 'unavailable', message: 'Live vehicle location is not connected. No simulated positions are displayed.' })
    return () => {}
  }
  const base = import.meta.env.VITE_WS_BASE_URL
  if (!base) {
    onState?.({ status: 'unavailable', message: 'VITE_WS_BASE_URL is not configured for live location events.' })
    return () => {}
  }
  const socket = new WebSocket(`${base.replace(/\/$/, '')}/trips/${encodeURIComponent(tripId)}/locations`)
  socket.onopen = () => onState?.({ status: 'connecting' })
  socket.onmessage = event => {
    try { onLocation(JSON.parse(event.data)); onState?.({ status: 'active' }) } catch { onState?.({ status: 'error', message: 'Received an invalid location update.' }) }
  }
  socket.onerror = () => onState?.({ status: 'error', message: 'Could not connect to the live location service.' })
  socket.onclose = () => onState?.({ status: 'offline' })
  return () => socket.close()
}
