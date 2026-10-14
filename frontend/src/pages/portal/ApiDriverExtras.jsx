import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { CalendarDays, BusFront, Clock3, Play, CheckCircle2, Megaphone, UserRound, Mail, ShieldCheck, Hash, Route, ArrowRight, RefreshCw } from 'lucide-react'
import { request } from '../../services/apiClient'
import './DriverPolish.css'

const list = data => Array.isArray(data) ? data : data?.content || []
const statusOf = trip => String(trip.status || 'SCHEDULED').toUpperCase()
const displayDate = value => { if (!value) return 'Date not set'; const d = new Date(value); return Number.isNaN(d.getTime()) ? String(value) : d.toLocaleDateString('en-GB', {day:'2-digit',month:'short',year:'numeric'}) }
const displayTime = value => { if (!value) return '—'; const d = new Date(value); return Number.isNaN(d.getTime()) ? String(value).slice(11,16) || String(value) : d.toLocaleTimeString('en-GB',{hour:'2-digit',minute:'2-digit'}) }
function Hero({kind,eyebrow,title,subtitle,Icon}) { return <header className={'dp-hero '+kind}><div><span className="dp-kicker">{eyebrow}</span><h1>{title}</h1><p>{subtitle}</p></div><div className="dp-hero-symbol"><Icon size={45}/></div></header> }
export function ApiDriverSchedule() {
  const [rows,setRows] = useState([])
  const [error,setError] = useState('')
  const [filter,setFilter] = useState('upcoming')
  const [busy,setBusy] = useState(null)
  const [reload,setReload] = useState(0)
  useEffect(() => { let active=true;request('/trip-status/driver/my-trips').then(data=>{if(active)setRows(list(data))}).catch(e=>{if(active)setError(e.message)});return()=>{active=false} },[reload])
  const action = async (id,name) => { setBusy(id);setError('');try { await request('/trip-status/driver/trips/'+encodeURIComponent(id)+'/'+name,{method:'PATCH'});setReload(n=>n+1) } catch(e){setError(e.message)}finally{setBusy(null)} }
  const visible=rows.filter(t=>filter==='all'||(filter==='completed'?statusOf(t)==='COMPLETED':statusOf(t)!=='COMPLETED'))
  return <div className="dp-page"><Hero kind="schedule" eyebrow="YOUR JOURNEYS" title="My schedule" subtitle="View your assigned trips and plan your upcoming journeys." Icon={CalendarDays}/><div className="dp-body">
    <div className="dp-tabs"><button type="button" className={filter==='upcoming'?'active':''} onClick={()=>setFilter('upcoming')}><CalendarDays size={16}/> Upcoming trips</button><button type="button" className={filter==='completed'?'active':''} onClick={()=>setFilter('completed')}><CheckCircle2 size={16}/> Completed trips</button><button type="button" className={filter==='all'?'active':''} onClick={()=>setFilter('all')}><Route size={16}/> All trips</button><button type="button" onClick={()=>setReload(n=>n+1)}><RefreshCw size={15}/> Refresh</button></div>
    {error&&<p className="dp-error" role="alert">{error}</p>}
    {visible.length ? visible.map(t=>{const id=t.tripId??t.id, status=statusOf(t);return <article className="dp-trip" key={id}><div><div className="dp-trip-date"><CalendarDays size={16}/>{displayDate(t.scheduledDeparture??t.departureTime)}<span className={'dp-pill '+status.toLowerCase()}>{status.replaceAll('_',' ')}</span></div><div className="dp-route-line"><div><strong>{displayTime(t.scheduledDeparture??t.departureTime)}</strong> Departure</div><div><strong>{displayTime(t.scheduledArrival??t.arrivalTime)}</strong> Arrival</div></div></div><div className="dp-trip-info"><h3>Trip #{id}</h3><p><Route size={15}/> Route #{t.routeId??'—'}</p><p><BusFront size={15}/> Vehicle #{t.vehicleId??'—'}</p><p><UserRound size={15}/> Driver #{t.driverId??'—'}</p></div><div className="dp-trip-actions">{status==='SCHEDULED'&&<button type="button" className="dp-button primary" disabled={busy!==null} onClick={()=>action(id,'start')}><Play size={15}/> Start trip</button>}{status==='IN_PROGRESS'&&<button type="button" className="dp-button primary" disabled={busy!==null} onClick={()=>action(id,'complete')}><CheckCircle2 size={15}/> Complete trip</button>}{status==='PAUSED'&&<button type="button" className="dp-button primary" disabled={busy!==null} onClick={()=>action(id,'resume')}>Resume trip</button>}<Link className="dp-button" to={'/driver/trip/'+id}>View details <ArrowRight size={14}/></Link></div></article>}) : <div className="dp-empty"><CalendarDays size={30}/><h3>No trips in this category</h3><p>Your assigned trips will appear here.</p></div>}
  </div></div>
}
export function ApiDriverProfile() {
  const [data,setData]=useState(null),[error,setError]=useState('')
  useEffect(()=>{request('/users/me').then(setData).catch(e=>setError(e.message))},[])
  const fields=data?[['Account ID',data.accountId,Hash],['Username',data.username,UserRound],['Email',data.email,Mail],['Role',data.role,ShieldCheck],['Account status',data.accountStatus,CheckCircle2],['Driver ID',data.driverId,BusFront],['Created at',data.createdAt,CalendarDays]]:[]
  const initials=String(data?.username||'DR').slice(0,2).toUpperCase()
  return <div className="dp-page"><Hero kind="profile" eyebrow="DRIVER ACCOUNT" title="My driver profile" subtitle="Your personal information and account details." Icon={UserRound}/><div className="dp-body">{error&&<p className="dp-error" role="alert">{error}</p>}{data?<div className="dp-profile"><section className="dp-identity"><div className="dp-avatar">{initials}</div><h2>{data.username||'Driver'}</h2><p>SmartMove driver</p><span className={'dp-pill '+String(data.accountStatus||'').toLowerCase()}>{data.accountStatus||'DRIVER'}</span></section><section className="dp-profile-info">{fields.map(([label,value,Icon])=><div className="dp-detail" key={label}><Icon size={17}/><span>{label}</span><strong>{value==null?'—':String(value)}</strong></div>)}</section></div>:!error&&<p>Loading driver profile...</p>}</div></div>
}
export function ApiDriverAnnouncements() {
  const [rows,setRows]=useState([]),[error,setError]=useState('')
  useEffect(()=>{request('/announcements/public').then(data=>setRows(list(data))).catch(e=>setError(e.message))},[])
  return <div className="dp-page"><Hero kind="announcements" eyebrow="OPERATIONS UPDATES" title="Announcements" subtitle="Important updates from the SmartMove operations team." Icon={Megaphone}/><div className="dp-body">{error&&<p className="dp-error" role="alert">{error}</p>}{rows.length?rows.map((item,i)=><article className="dp-announcement" key={item.id??i}><Megaphone size={22}/><div><h3>{item.title}</h3><p>{item.message}</p></div></article>):!error&&<div className="dp-empty"><Megaphone size={30}/><h3>No published announcements</h3><p>New operational updates will appear here.</p></div>}</div></div>
}
