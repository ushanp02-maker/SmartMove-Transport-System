import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { BusFront, Users, UserRound, Route, CalendarDays, Ticket, Wallet, ArrowRight, RefreshCw, Activity, TrendingUp, Navigation, Clock3, CheckCircle2 } from 'lucide-react'
import { request } from '../services/apiClient'
import './smartmoveDashboards.css'

const number = value => typeof value === 'number' ? value.toLocaleString() : value == null ? '0' : String(value)
const metrics = [
  ['totalPassengers','Passengers',Users],['totalDrivers','Drivers',UserRound],['totalVehicles','Vehicles',BusFront],['totalRoutes','Routes',Route],
  ['totalTrips','Total trips',CalendarDays],['scheduledTrips','Scheduled trips',Clock3],['totalBookings','Bookings',Ticket],['grossRevenue','Gross revenue',Wallet],
  ['availableVehicles','Available vehicles',CheckCircle2],['activeRoutes','Active routes',Navigation],['completedTrips','Completed trips',Activity],['confirmedBookings','Confirmed bookings',TrendingUp],
]
export default function ApiDashboard(){
  const [data,setData]=useState(null),[error,setError]=useState(''),[refresh,setRefresh]=useState(0)
  useEffect(()=>{let active=true;request('/reports/dashboard').then(value=>{if(active){setData(value);setError('')}}).catch(e=>{if(active)setError(e.message)});return()=>{active=false}},[refresh])
  const all=Object.entries(data||{})
  const other=all.filter(([key])=>!metrics.some(([name])=>name===key))
  const tripTotal=Number(data?.totalTrips)||0
  const statuses=[['Scheduled',Number(data?.scheduledTrips)||0],['Completed',Number(data?.completedTrips)||0],['Cancelled',Number(data?.cancelledTrips)||0]]
  return <div className="page-content sm-dashboard">
    <section className="sm-hero"><div><span className="sm-eyebrow">SmartMove · Operations workspace</span><h1>Welcome back, Administrator.</h1><p>Here is your live transport overview, powered by Spring Boot and Oracle.</p></div><div className="sm-hero-aside"><Activity size={19}/> Live operations overview</div></section>
    <div className="sm-dash-actions"><Link to="/admin/routes"><Route size={16}/> Manage routes <ArrowRight size={14}/></Link><Link to="/admin/trips"><CalendarDays size={16}/> Schedule trips</Link><Link to="/admin/vehicles"><BusFront size={16}/> Vehicles</Link><Link to="/admin/drivers"><UserRound size={16}/> Drivers</Link><button type="button" onClick={()=>setRefresh(n=>n+1)}><RefreshCw size={16}/> Refresh data</button></div>
    {error&&<p role="alert" className="auth-error">{error}</p>}
    {!data?<p>Loading live dashboard…</p>:<>
      <div className="sm-metrics">{metrics.map(([key,label,Icon])=><article className="sm-metric" key={key}><div className="sm-metric-top"><span className="sm-metric-icon"><Icon size={22}/></span></div><strong>{number(data[key])}</strong><span className="sm-metric-label">{label}</span></article>)}</div>
      <div className="sm-dash-grid"><section className="sm-panel"><h2>Trip status overview</h2><p className="sm-panel-subtitle">Actual status counts from your operational records.</p>{statuses.map(([label,count])=><div key={label}><div className="sm-status-line"><span>{label}</span><b>{count}</b></div><div className="sm-progress"><div style={{width:`${tripTotal?Math.min(100,count/tripTotal*100):0}%`}}/></div></div>)}<div className="sm-list-row"><span>Total trips</span><strong>{number(data.totalTrips)}</strong></div></section><section className="sm-panel"><h2>Operations at a glance</h2><p className="sm-panel-subtitle">Quick access to your day-to-day tasks.</p><div className="sm-status-list"><div className="sm-status-line"><span>Available vehicles</span><b>{number(data.availableVehicles)} / {number(data.totalVehicles)}</b></div><div className="sm-status-line"><span>Active routes</span><b>{number(data.activeRoutes)} / {number(data.totalRoutes)}</b></div><div className="sm-status-line"><span>Confirmed bookings</span><b>{number(data.confirmedBookings)}</b></div><div className="sm-status-line"><span>Pending bookings</span><b>{number(data.pendingBookings)}</b></div></div><div className="sm-dash-actions"><Link to="/admin/reports">View reports <ArrowRight size={14}/></Link><Link to="/admin/live-fleet">Live fleet</Link></div></section></div>
      {other.length>0&&<section className="sm-panel" style={{marginTop:16}}><h2>Additional operational statistics</h2><div className="sm-metrics" style={{marginBottom:0}}>{other.map(([key,value])=><article className="sm-metric" key={key}><strong>{number(value)}</strong><span className="sm-metric-label">{key.replace(/([A-Z])/g,' $1')}</span></article>)}</div></section>}
    </>}
  </div>
}
