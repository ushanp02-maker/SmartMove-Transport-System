import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { BusFront, CalendarDays, ArrowRight, Navigation, Clock3, RefreshCw } from 'lucide-react'
import '../smartmoveDashboards.css'
import { request } from '../../services/apiClient'
import { startTripTracking } from '../../services/trackingService'
const asArray = value => Array.isArray(value) ? value : value?.content || []
const idOf = trip => trip.tripId ?? trip.id
const statusOf = trip => String(trip.status ?? trip.tripStatus ?? 'SCHEDULED').toUpperCase()
const format = value => value == null ? '—' : typeof value === 'object' ? JSON.stringify(value) : String(value)
function useDriverTrips() {
  const [trips,setTrips] = useState([])
  const [error,setError] = useState('')
  const [reload,setReload] = useState(0)
  useEffect(() => { let active = true;request('/trip-status/driver/my-trips').then(value => {if(active){setTrips(asArray(value));setError('')}}).catch(err => {if(active)setError(err.message)});return () => {active=false}},[reload])
  return {trips,error,reload:()=>setReload(n=>n+1),setError}
}
const transitions = { SCHEDULED:'start',IN_PROGRESS:'complete',PAUSED:'resume' }
function TripRow({trip,onAction,busy}) {
  const id=idOf(trip),status=statusOf(trip)
  return <article className="portal-panel" style={{marginBottom:12}}><h3>Trip #{id} · {status}</h3><p>{['routeId','vehicleId','departureTime','arrivalTime','driverId'].map(key=><span key={key} style={{marginRight:16}}>{key}: {format(trip[key])}</span>)}</p><div style={{display:'flex',gap:8,flexWrap:'wrap'}}><Link className="button button-outline" to={`/driver/trip/${id}`}>Trip details</Link>{transitions[status]&&<button className="button button-primary" disabled={busy} onClick={()=>onAction(id,transitions[status])}>{transitions[status]} trip</button>}{status==='IN_PROGRESS'&&<button className="button button-outline" disabled={busy} onClick={()=>onAction(id,'pause')}>Pause</button>}</div></article>
}
export function ApiDriverTrips({dashboard=false}) {
  const {trips,error,reload,setError}=useDriverTrips()
  const [busy,setBusy]=useState(false)
  const action=async(id,name)=>{setBusy(true);setError('');try{await request(`/trip-status/driver/trips/${encodeURIComponent(id)}/${name}`,{method:'PATCH'});reload()}catch(err){setError(err.message)}finally{setBusy(false)}}
  const scheduled=trips.filter(t=>statusOf(t)==='SCHEDULED').length
  const active=trips.filter(t=>statusOf(t)==='IN_PROGRESS').length
  const completed=trips.filter(t=>statusOf(t)==='COMPLETED').length
  return <div className="page-content sm-dashboard">
    <section className="sm-hero"><div><span className="sm-eyebrow">SmartMove · Driver workspace</span><h1>{dashboard?'Ready for the road?':'Your assigned journeys.'}</h1><p>Keep track of your schedule, update trip progress and share live locations safely.</p></div><div className="sm-hero-aside"><Navigation size={19}/> Your driver workspace</div></section>
    <div className="sm-dash-actions"><Link to="/driver/trips"><CalendarDays size={16}/> My trips <ArrowRight size={14}/></Link><Link to="/driver/schedule">Schedule</Link><Link to="/driver/vehicle"><BusFront size={16}/> My vehicle</Link><Link to="/driver/issues">Report an issue</Link><button type="button" onClick={reload}><RefreshCw size={16}/> Refresh</button></div>
    <div className="sm-metrics"><article className="sm-metric"><span className="sm-metric-icon"><BusFront size={22}/></span><strong>{trips.length}</strong><span className="sm-metric-label">Assigned trips</span></article><article className="sm-metric"><span className="sm-metric-icon"><CalendarDays size={22}/></span><strong>{scheduled}</strong><span className="sm-metric-label">Scheduled</span></article><article className="sm-metric"><span className="sm-metric-icon"><Navigation size={22}/></span><strong>{active}</strong><span className="sm-metric-label">In progress</span></article><article className="sm-metric"><span className="sm-metric-icon"><Clock3 size={22}/></span><strong>{completed}</strong><span className="sm-metric-label">Completed</span></article></div>
    {error&&<p className="auth-error" role="alert">{error}</p>}
    <section className="sm-panel"><h2>{dashboard?'Your next assignments':'All assigned trips'}</h2><p className="sm-panel-subtitle">Only journeys assigned to your authenticated driver profile are shown.</p>{trips.length?trips.map(trip=><TripRow key={idOf(trip)} trip={trip} onAction={action} busy={busy}/>):<div className="sm-empty">No trips have been assigned to you yet.</div>}</section>
  </div>
}
export function ApiDriverTripDetails(){
  const {tripId}=useParams()
  const [trip,setTrip]=useState(null),[error,setError]=useState(''),[gps,setGps]=useState({status:'stopped'}),[tracking,setTracking]=useState(false)
  const [reload,setReload]=useState(0)
  useEffect(()=>{let active=true;request(`/trip-status/trips/${encodeURIComponent(tripId)}`).then(value=>{if(active){setTrip(value);setError('')}}).catch(err=>{if(active)setError(err.message)});return()=>{active=false}},[tripId,reload])
  useEffect(()=>{if(!tracking||statusOf(trip)!=='IN_PROGRESS')return;return startTripTracking(tripId,setGps)},[tracking,tripId,trip])
  const action=async name=>{try{await request(`/trip-status/driver/trips/${encodeURIComponent(tripId)}/${name}`,{method:'PATCH'});setReload(n=>n+1);setTracking(false)}catch(err){setError(err.message)}}
  const status=statusOf(trip)
  return <section className="portal-panel"><Link to="/driver/trips">← My trips</Link><h1>Trip #{tripId}</h1>{error&&<p className="auth-error" role="alert">{error}</p>}{trip&&<><p>Status: <strong>{status}</strong></p>{Object.entries(trip).map(([key,value])=><p key={key}><strong>{key}</strong>: {format(value)}</p>)}<div style={{display:'flex',gap:8,flexWrap:'wrap'}}>{transitions[status]&&<button className="button button-primary" onClick={()=>action(transitions[status])}>{transitions[status]} trip</button>}{status==='IN_PROGRESS'&&<button className="button button-outline" onClick={()=>action('pause')}>Pause trip</button>}</div><h2>GPS sharing</h2><p>Only available during an active trip and while this browser page stays open. Location permission and a secure browser context are required.</p><button className="button button-primary" disabled={status!=='IN_PROGRESS'} onClick={()=>setTracking(value=>!value)}>{tracking?'Stop GPS':'Start GPS'}</button><p>{gps.message||gps.status}</p></>}</section>
}
