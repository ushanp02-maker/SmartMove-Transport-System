import { useEffect, useMemo, useState } from 'react'
import { BusFront, CircleAlert, Clock3, MapPin, Radio, WifiOff } from 'lucide-react'
import { MapContainer, Marker, Popup, TileLayer } from 'react-leaflet'
import { divIcon } from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { useAppData } from '../../services/useAppData'
import { subscribeToTripLocation } from '../../services/trackingService'
import { SRI_LANKA_CENTER, SRI_LANKA_BOUNDS } from '../../services/locationService'
import { formatDate } from '../../services/formatters'
import '../../components/MapPicker.css'

const types=['ALL','COMMUTER','STAFF','CUSTOM']
const liveIcon=divIcon({className:'sm-map-marker sm-map-marker-live',html:'<span>●</span>',iconSize:[24,30],iconAnchor:[12,28]})

export default function LiveFleet(){
  const {data}=useAppData()
  const [type,setType]=useState('ALL')
  const [live,setLive]=useState({})
  const [clock,setClock]=useState(0)
  useEffect(()=>{const timer=window.setInterval(()=>setClock(Date.now()),15000);return ()=>window.clearInterval(timer)},[])
  const active=useMemo(()=>data.trips.filter(trip=>['In progress','IN_PROGRESS'].includes(trip.status)&&(type==='ALL'||(trip.serviceType||'COMMUTER')===type)),[data.trips,type])
  const tripSignature=active.map(trip=>trip.id).sort().join('|')
  useEffect(()=>{
    const stops=[]
    for(const tripId of tripSignature?tripSignature.split('|'):[]){
      stops.push(subscribeToTripLocation(tripId,location=>setLive(current=>({...current,[tripId]:{location,status:'active',receivedAt:Date.now()}})),status=>setLive(current=>({...current,[tripId]:{...current[tripId],status:status.status,message:status.message,receivedAt:status.status==='active'?Date.now():current[tripId]?.receivedAt}}))))
    }
    return ()=>stops.forEach(stop=>stop())
  },[tripSignature])
  const rows=useMemo(()=>active.map(trip=>({trip,vehicle:data.vehicles.find(item=>item.id===trip.vehicleId),driver:data.drivers.find(item=>item.id===trip.driverId),route:data.routes.find(item=>item.id===trip.routeId)||data.staffRoutes.find(item=>item.routeId===trip.routeId),tracking:live[trip.id]})),[active,data,live])
  const points=rows.filter(row=>row.tracking?.location).map(row=>[row.tracking.location.latitude,row.tracking.location.longitude])
  return <div className="page-content"><div className="page-heading"><div><div className="eyebrow">OPERATIONS MONITORING · LOCATION DATA IS OPTIONAL</div><h1>Live fleet tracking</h1><p>Active trips appear here. Markers are shown only for locations received from the configured live service.</p></div><span className="tracking-mode-pill"><Radio size={14}/>{import.meta.env.VITE_DATA_MODE==='API'?'API mode':'Demo mode'}</span></div><div className="fleet-filter-tabs">{types.map(item=><button key={item} className={type===item?'active':''} onClick={()=>setType(item)}>{item==='ALL'?'All services':item}</button>)}</div><div className="fleet-live-summary"><span><BusFront size={17}/><strong>{rows.length}</strong> active trip(s)</span><span><MapPin size={17}/><strong>{points.length}</strong> location update(s)</span><span><WifiOff size={16}/> No coordinates are fabricated</span></div><div className="fleet-map-panel portal-panel"><div className="portal-panel-head"><div><h2>Vehicle locations</h2><p>OpenStreetMap · a marker requires a received backend update</p></div></div><div className="map-container fleet-map"><MapContainer center={SRI_LANKA_CENTER} zoom={7} minZoom={6} maxBounds={SRI_LANKA_BOUNDS} maxBoundsViscosity={0.6} scrollWheelZoom style={{height:'100%',width:'100%'}}><TileLayer attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors' url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"/>{rows.filter(row=>row.tracking?.location).map(row=><Marker key={row.trip.id} position={[row.tracking.location.latitude,row.tracking.location.longitude]} icon={liveIcon}><Popup>{row.vehicle?.plate} · {row.route?.name}</Popup></Marker>)}</MapContainer>{points.length===0&&<div className="fleet-map-empty"><span><MapPin size={19}/></span><strong>Live coordinates unavailable</strong><p>Active trip records are listed below. The demo does not generate or persist vehicle positions.</p></div>}</div></div><section className="portal-panel fleet-trip-list-panel"><div className="portal-panel-head"><div><h2>Active vehicles</h2><p>Only scheduled trips currently marked in progress.</p></div></div>{rows.length?<div className="fleet-active-list">{rows.map(({trip,vehicle,driver,route,tracking})=>{const stale=Boolean(tracking?.receivedAt&&clock&&clock-tracking.receivedAt>60000);return <article key={trip.id}><span className={`fleet-state-dot ${tracking?.status==='active'&&!stale?'online':''}`}/><div className="fleet-row-main"><strong>{vehicle?.plate||trip.vehicleId} · {vehicle?.name||'Vehicle'}</strong><small>{route?.name||`${route?.origin||'—'} → ${route?.destination||'—'}`} · {trip.serviceType||'COMMUTER'}</small><small>{formatDate(trip.date)} {trip.departure} · Driver {driver?.name||trip.driverId}</small></div><div className="fleet-row-status">{tracking?.location?<><span className={stale?'stale':'online'}>{stale?'STALE':'LOCATION RECEIVED'}</span><small>{new Date(tracking.location.recordedAt).toLocaleTimeString()}</small></>:<><span className="offline">{tracking?.status==='error'?'OFFLINE':'NO LIVE FEED'}</span><small>{tracking?.message||'Awaiting backend location service'}</small></>}</div></article>})}</div>:<div className="portal-empty compact"><span><Clock3 size={20}/></span><strong>No trips currently in progress</strong><p>Scheduled but inactive vehicles are not shown as moving.</p></div>}</section><div className="tracking-demo-note full"><CircleAlert size={14}/> Demo and disconnected API states are labeled unavailable. Fleet positions require authorized GPS updates plus a backend/WebSocket connection.</div></div>
}
