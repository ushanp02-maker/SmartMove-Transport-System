import { useEffect, useState } from 'react'
import { MapContainer, Marker, Popup, TileLayer } from 'react-leaflet'
import { request } from '../../services/apiClient'
import 'leaflet/dist/leaflet.css'
import '../../components/MapPicker.css'
const valid = point => point && Number.isFinite(Number(point.latitude)) && Number.isFinite(Number(point.longitude))
const idOf = trip => trip.tripId ?? trip.id
export default function ApiLiveFleet() {
  const [trips,setTrips]=useState([]),[locations,setLocations]=useState({}),[error,setError]=useState('')
  useEffect(()=>{
    let active=true
    const poll=async()=>{
      try {
        const list=await request('/trip-status/admin/active')
        if(!active)return
        const rows=Array.isArray(list)?list:list?.content||[]
        setTrips(rows);setError('')
        const results=await Promise.all(rows.map(async trip=>{
          const id=idOf(trip)
          try {const result=await request(`/tracking/trips/${encodeURIComponent(id)}/live`);return [id,result?.location??result]}catch{return [id,null]}
        }))
        if(active)setLocations(Object.fromEntries(results.filter(([,point])=>valid(point))))
      }catch(err){if(active)setError(err.message)}
    }
    poll();const timer=window.setInterval(poll,15000)
    return()=>{active=false;window.clearInterval(timer)}
  },[])
  const positioned=trips.filter(trip=>valid(locations[idOf(trip)]))
  return <div className="page-content"><div className="page-heading"><div><h1>Live fleet</h1><p>Active trips and genuine GPS fixes from the Spring Boot tracking service. Refreshes every 15 seconds.</p></div></div>{error&&<p className="auth-error" role="alert">{error}</p>}<section className="portal-panel"><h2>{trips.length} active trips · {positioned.length} with GPS fixes</h2><div className="map-container fleet-map" style={{height:420}}><MapContainer center={[7.8731,80.7718]} zoom={7} style={{height:'100%',width:'100%'}}><TileLayer attribution='&copy; OpenStreetMap contributors' url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"/>{positioned.map(trip=>{const point=locations[idOf(trip)];return <Marker key={idOf(trip)} position={[Number(point.latitude),Number(point.longitude)]}><Popup>Trip #{idOf(trip)} · GPS received</Popup></Marker>})}</MapContainer></div><div style={{overflowX:'auto'}}><table className="data-table"><thead><tr><th>Trip</th><th>Status</th><th>Latitude</th><th>Longitude</th></tr></thead><tbody>{trips.map(trip=>{const point=locations[idOf(trip)];return <tr key={idOf(trip)}><td>{idOf(trip)}</td><td>{trip.status??trip.tripStatus}</td><td>{point?.latitude??'Awaiting GPS'}</td><td>{point?.longitude??'Awaiting GPS'}</td></tr>})}</tbody></table></div></section></div>
}
