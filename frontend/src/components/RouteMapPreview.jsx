import MapPicker from './MapPicker'
import { findSupportedPlace } from '../services/placeCatalogue'

const pointFor = (name, latitude, longitude) => {
  if (Number.isFinite(Number(latitude)) && Number.isFinite(Number(longitude)) && latitude != null && longitude != null) return { latitude: Number(latitude), longitude: Number(longitude), label: name || 'Location' }
  const place = findSupportedPlace(name)
  return place ? { latitude: place.latitude, longitude: place.longitude, label: name || place.name } : null
}

export default function RouteMapPreview({ route, stops = [], height = 300 }) {
  if (!route) return <p className="sm-map-note">Select a route to preview its locations on the map.</p>
  const ordered = [...stops].sort((a,b)=>(a.stopOrder??0)-(b.stopOrder??0))
  const points = ordered.map((stop,i) => {
    const point = pointFor(stop.stopName || stop.name, stop.latitude, stop.longitude)
    return point ? { ...point, name: stop.stopName || stop.name, stopOrder: i+1 } : null
  }).filter(Boolean)
  const origin = pointFor(route.origin || route.originName, route.originLatitude, route.originLongitude) || points[0]
  const destination = pointFor(route.destination || route.destinationName, route.destinationLatitude, route.destinationLongitude) || points.at(-1)
  if (!origin && !destination) return <p className="sm-map-note">No coordinates are available for this route. Add supported place names or route-stop coordinates to display it accurately.</p>
  return <div className="sm-route-preview"><div className="sm-route-preview-heading"><strong>{route.name || route.routeName || 'Selected route'}</strong><span>{origin?.label || route.origin || 'Origin'} → {destination?.label || route.destination || 'Destination'}</span></div><MapPicker readOnly showCurrentLocation={false} pickup={origin} destination={destination} routeStops={points.length>1?points:[]} height={height}/><p className="sm-map-note">Markers indicate known locations. Lines are schematic connections, not road directions or live vehicle tracking.</p></div>
}
