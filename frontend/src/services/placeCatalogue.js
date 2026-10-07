import { distanceMetres } from './locationService'

// Curated demo place catalogue for map search. Coordinates are illustrative landmarks, not geocoding results.
export const supportedPlaces = [
  { name: 'Colombo Fort', kind: 'Station · Colombo', latitude: 6.9344, longitude: 79.8428 },
  { name: 'Maharagama', kind: 'Town · Western Province', latitude: 6.8480, longitude: 79.9265 },
  { name: 'Piliyandala', kind: 'Town · Western Province', latitude: 6.8014, longitude: 79.9227 },
  { name: 'Nugegoda', kind: 'Town · Western Province', latitude: 6.8729, longitude: 79.8881 },
  { name: 'Moratuwa', kind: 'Town · Western Province', latitude: 6.7730, longitude: 79.8816 },
  { name: 'Dehiwala', kind: 'Town · Western Province', latitude: 6.8510, longitude: 79.8650 },
  { name: 'Wellawatte', kind: 'Town · Western Province', latitude: 6.8741, longitude: 79.8608 },
  { name: 'Bambalapitiya', kind: 'Town · Western Province', latitude: 6.8964, longitude: 79.8560 },
  { name: 'Borella', kind: 'Town · Colombo', latitude: 6.9147, longitude: 79.8778 },
  { name: 'Wattala', kind: 'Town · Western Province', latitude: 6.9890, longitude: 79.8920 },
  { name: 'Ja-Ela', kind: 'Town · Western Province', latitude: 7.0752, longitude: 79.8919 },
  { name: 'Negombo', kind: 'City · Western Province', latitude: 7.2083, longitude: 79.8358 },
  { name: 'Kalutara', kind: 'Town · Western Province', latitude: 6.5854, longitude: 79.9607 },
  { name: 'Galle', kind: 'City · Southern Province', latitude: 6.0535, longitude: 80.2210 },
  { name: 'Kadawatha', kind: 'Town · Western Province', latitude: 7.0010, longitude: 79.9500 },
  { name: 'Kurunegala', kind: 'City · North Western Province', latitude: 7.4863, longitude: 80.3647 },
  { name: 'Kandy', kind: 'City · Central Province', latitude: 7.2906, longitude: 80.6337 },
  { name: 'Gampola', kind: 'Town · Central Province', latitude: 7.1644, longitude: 80.5697 },
  { name: 'Nuwara Eliya', kind: 'City · Central Province', latitude: 6.9497, longitude: 80.7891 },
  { name: 'Ella', kind: 'Town · Uva Province', latitude: 6.8667, longitude: 81.0466 },
  { name: 'Anuradhapura', kind: 'City · North Central Province', latitude: 8.3114, longitude: 80.4037 },
  { name: 'Vavuniya', kind: 'City · Northern Province', latitude: 8.7514, longitude: 80.4971 },
  { name: 'Jaffna', kind: 'City · Northern Province', latitude: 9.6615, longitude: 80.0255 },
  { name: 'Orion City, Colombo', kind: 'Workplace · Colombo', latitude: 6.9271, longitude: 79.8612 },
  { name: 'World Trade Center, Colombo', kind: 'Workplace · Colombo', latitude: 6.9345, longitude: 79.8420 },
]

export const findSupportedPlace = name => supportedPlaces.find(place => place.name.toLocaleLowerCase() === String(name || '').trim().toLocaleLowerCase())

export function suggestNearbyStops(point, routes, maxMetres = 5000) {
  if (!point) return []
  return routes.flatMap(route => (route.stops || []).map(stop => ({ routeId: route.id, routeName: route.name, stop, distance: distanceMetres(point, stop) })))
    .filter(match => match.distance <= maxMetres)
    .sort((a, b) => a.distance - b.distance)
}
