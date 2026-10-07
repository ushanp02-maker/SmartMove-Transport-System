export const SRI_LANKA_CENTER = [7.8731, 80.7718]
export const SRI_LANKA_BOUNDS = [[5.8, 79.4], [10.1, 82.2]]

export function getCurrentPosition(options = {}) {
  return new Promise((resolve, reject) => {
    if (!navigator.geolocation) return reject(new Error('Location is not available in this browser.'))
    navigator.geolocation.getCurrentPosition(position => resolve({
      latitude: position.coords.latitude,
      longitude: position.coords.longitude,
      accuracy: position.coords.accuracy,
      recordedAt: new Date(position.timestamp).toISOString(),
    }), error => reject(new Error(error.code === 1 ? 'Location permission was denied. You can search for a place or choose it on the map.' : error.code === 2 ? 'Your location could not be determined. Try selecting a place on the map.' : 'Location request timed out. You can select a location manually.')), {
      enableHighAccuracy: true, timeout: options.timeout || 12000, maximumAge: options.maximumAge ?? 30000,
    })
  })
}

export function watchCurrentPosition(onPosition, onError, options = {}) {
  if (!navigator.geolocation) {
    onError?.(new Error('GPS tracking is not available in this browser.'))
    return () => {}
  }
  const watchId = navigator.geolocation.watchPosition(position => onPosition({
    latitude: position.coords.latitude,
    longitude: position.coords.longitude,
    accuracy: position.coords.accuracy,
    recordedAt: new Date(position.timestamp).toISOString(),
  }), error => onError?.(new Error(error.code === 1 ? 'Location permission was denied.' : error.code === 2 ? 'GPS location is unavailable.' : 'GPS request timed out.')), {
    enableHighAccuracy: true, timeout: options.timeout || 15000, maximumAge: options.maximumAge ?? 5000,
  })
  return () => navigator.geolocation.clearWatch(watchId)
}

export function distanceMetres(a, b) {
  const radians = value => value * Math.PI / 180
  const lat = radians(b.latitude - a.latitude)
  const lon = radians(b.longitude - a.longitude)
  const h = Math.sin(lat / 2) ** 2 + Math.cos(radians(a.latitude)) * Math.cos(radians(b.latitude)) * Math.sin(lon / 2) ** 2
  return 6371000 * 2 * Math.atan2(Math.sqrt(h), Math.sqrt(1 - h))
}
