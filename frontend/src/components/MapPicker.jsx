import { useEffect, useMemo, useState } from 'react'
import { MapContainer, Marker, Polyline, Popup, TileLayer, useMap, useMapEvents } from 'react-leaflet'
import { divIcon, latLngBounds } from 'leaflet'
import { LocateFixed, MapPin, Search, X } from 'lucide-react'
import { getCurrentPosition, SRI_LANKA_BOUNDS, SRI_LANKA_CENTER } from '../services/locationService'
import { supportedPlaces } from '../services/placeCatalogue'
import 'leaflet/dist/leaflet.css'
import './MapPicker.css'

const markerIcon = kind => divIcon({ className: `sm-map-marker sm-map-marker-${kind}`, html: `<span>${kind === 'pickup' ? 'A' : kind === 'destination' ? 'B' : '•'}</span>`, iconSize: [28, 34], iconAnchor: [14, 32] })

function MapClicks({ onChoose }) {
  useMapEvents({ click: event => onChoose(event.latlng) })
  return null
}

function MapBounds({ locations }) {
  const map = useMap()
  const signature = locations.map(point => `${point.latitude}:${point.longitude}`).join('|')
  useEffect(() => {
    const points=signature?signature.split('|').map(value=>value.split(':').map(Number)):[]
    if (points.length > 1) map.fitBounds(latLngBounds(points), { padding: [36, 36], maxZoom: 12 })
    else if (points.length === 1) map.setView(points[0], Math.max(map.getZoom(), 10))
  }, [signature, map])
  return null
}

export default function MapPicker({ pickup, destination, routeStops = [], onPickupChange, onDestinationChange, pickTarget = 'pickup', onTargetChange, showCurrentLocation = true, readOnly = false, height = 330, children }) {
  const [query, setQuery] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const places = useMemo(() => {
    const text = query.trim().toLocaleLowerCase()
    return text ? supportedPlaces.filter(place => place.name.toLocaleLowerCase().includes(text)).slice(0, 7) : []
  }, [query])
  const routePoints = routeStops.filter(stop => Number.isFinite(stop.latitude) && Number.isFinite(stop.longitude))
  const locations = [...routePoints, pickup, destination].filter(Boolean)
  const linePoints = routeStops.length ? routePoints.map(point => [point.latitude, point.longitude]) : [pickup, destination].filter(Boolean).map(point => [point.latitude, point.longitude])

  const update = (target, point) => {
    const latitude = point.latitude ?? point.lat
    const longitude = point.longitude ?? point.lng
    const value = { latitude, longitude, label: point.label || point.name || `${latitude.toFixed(5)}, ${longitude.toFixed(5)}` }
    if (target === 'pickup') onPickupChange?.(value)
    else onDestinationChange?.(value)
  }
  const chooseMapPoint = latlng => {
    if (readOnly) return
    update(pickTarget, { latitude: latlng.lat, longitude: latlng.lng, label: `${latlng.lat.toFixed(5)}, ${latlng.lng.toFixed(5)}` })
  }
  const useCurrent = async () => {
    setError('')
    setBusy(true)
    try {
      const current = await getCurrentPosition()
      update(pickTarget, { ...current, label: 'Current location' })
    } catch (locationError) {
      setError(locationError.message)
    } finally {
      setBusy(false)
    }
  }
  const choosePlace = place => {
    update(pickTarget, place)
    setQuery('')
    setError('')
  }
  return (
    <section className="map-picker" aria-label="Location map selector">
      {!readOnly && <div className="map-picker-tools">
        <label className="map-place-search"><Search size={15}/><input value={query} onChange={event => setQuery(event.target.value)} placeholder="Search supported Sri Lankan places" aria-label="Search supported places"/>{query && <button type="button" onClick={() => setQuery('')} aria-label="Clear place search"><X size={14}/></button>}</label>
        <div className="map-pick-target" role="group" aria-label="Choose which location to set">
          <button type="button" className={pickTarget === 'pickup' ? 'active' : ''} onClick={() => onTargetChange?.('pickup')}><span className="target-dot pickup"/> Set pickup</button>
          <button type="button" className={pickTarget === 'destination' ? 'active' : ''} onClick={() => onTargetChange?.('destination')}><span className="target-dot destination"/> Set destination</button>
        </div>
        {showCurrentLocation && <button type="button" className="button button-outline map-current-btn" disabled={busy} onClick={useCurrent}><LocateFixed size={15}/>{busy ? 'Finding location…' : 'Use my location'}</button>}
      </div>}
      {places.length > 0 && <div className="map-place-results">{places.map(place => <button type="button" key={place.name} onClick={() => choosePlace(place)}><MapPin size={14}/><span><strong>{place.name}</strong><small>{place.kind}</small></span></button>)}</div>}
      {error && <div className="map-error" role="alert">{error}</div>}
      <div className="map-container" style={{ height }}>
        <MapContainer center={SRI_LANKA_CENTER} zoom={7} minZoom={6} maxBounds={SRI_LANKA_BOUNDS} maxBoundsViscosity={0.6} scrollWheelZoom={false} style={{ height: '100%', width: '100%' }}>
          <TileLayer attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors' url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"/>
          <MapClicks onChoose={chooseMapPoint}/>
          <MapBounds locations={locations}/>
          {routePoints.map(stop => <Marker key={`route-${stop.stopOrder}`} position={[stop.latitude, stop.longitude]} icon={markerIcon(stop.stopOrder === 1 ? 'pickup' : stop.stopOrder === routeStops.length ? 'destination' : 'stop')}><Popup>{stop.stopOrder}. {stop.name}{stop.scheduledTime ? ` · ${stop.scheduledTime}` : ''}</Popup></Marker>)}
          {pickup && <Marker position={[pickup.latitude, pickup.longitude]} icon={markerIcon('pickup')}><Popup>Pickup · {pickup.label}</Popup></Marker>}
          {destination && <Marker position={[destination.latitude, destination.longitude]} icon={markerIcon('destination')}><Popup>Destination · {destination.label}</Popup></Marker>}
          {linePoints.length > 1 && <Polyline positions={linePoints} pathOptions={{ color: '#517b59', weight: 4, dashArray: routeStops.length ? undefined : '8 8' }}/>}
        </MapContainer>
      </div>
      <div className="map-picker-footer"><span>{readOnly ? 'Route stops are reference landmarks; no live vehicle position is implied.' : 'Select a supported place or click the map. Current location is requested only when you ask.'}</span>{children}</div>
    </section>
  )
}
