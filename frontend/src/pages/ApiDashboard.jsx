import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { BusFront, Users, UserRound, Route, CalendarDays, Ticket, Wallet, ArrowRight, RefreshCw, Activity, Navigation, Clock3 } from 'lucide-react'
import { request } from '../services/apiClient'
import './smartmoveDashboards.css'

const n = value => Number(value)||0
const fmt = value => n(value).toLocaleString()
const stats = [['totalPassengers','Passengers',Users],['totalDrivers','Drivers',UserRound],['totalVehicles','Vehicles',BusFront],['totalRoutes','Routes',Route],['totalTrips','Trips',CalendarDays],['totalBookings','Bookings',Ticket],['grossRevenue','Revenue (LKR)',Wallet],['availableVehicles','Available fleet',Activity]]
const COLORS=['#287755','#8fc6a2','#c8dfce','#e5ede7']
function Donut({items,total,label}){
 const radius=61,circ=2*Math.PI*radius;let offset=0
 return <div className="sm-donut-layout"><div className="sm-donut"><svg viewBox="0 0 170 170" role="img" aria-label={`${label}: ${items.map(x=>x[0]+' '+x[1]).join(', ')}`}><circle cx="85" cy="85" r={radius} stroke="#edf3ef" strokeWidth="17" fill="none"/>{total>0&&items.map(([name,count],i)=>{const len=Math.max(0,count)/total*circ;const el=<circle key={name} cx="85" cy="85" r={radius} fill="none" stroke={COLORS[i%COLORS.length]} strokeWidth="17" strokeDasharray={`${len} ${circ-len}`} strokeDashoffset={-offset} transform="rotate(-90 85 85)"/>;offset+=len;return el})}<text x="85" y="80" textAnchor="middle" className="sm-donut-number">{fmt(total)}</text><text x="85" y="99" textAnchor="middle" className="sm-donut-caption">{label}</text></svg></div><div className="sm-donut-legend">{items.map(([name,count],i)=><div key={name}><span style={{background:COLORS[i%COLORS.length]}}/><label>{name}</label><strong>{fmt(count)}</strong></div>)}</div></div>
}
function BarChart({items}){const max=Math.max(1,...items.map(x=>n(x[1])));return <div className="sm-bars">{items.map(([label,value])=><div className="sm-bar-row" key={label}><span>{label}</span><div className="sm-bar-track"><div style={{width:`${n(value)/max*100}%`}}/></div><strong>{fmt(value)}</strong></div>)}</div>}
export default function ApiDashboard(){
 const [data,setData]=useState(null),[error,setError]=useState(''),[refresh,setRefresh]=useState(0)
 useEffect(()=>{let active=true;request('/reports/dashboard').then(value=>{if(active){setData(value);setError('')}}).catch(e=>{if(active)setError(e.message)});return()=>{active=false}},[refresh])
 const tripTotal=n(data?.totalTrips)
 const tripItems=[['Scheduled',n(data?.scheduledTrips)],['Completed',n(data?.completedTrips)],['Cancelled',n(data?.cancelledTrips)]]
 const otherTrips=Math.max(0,tripTotal-tripItems.reduce((a,x)=>a+x[1],0))
 const bookingItems=[['Confirmed',n(data?.confirmedBookings)],['Pending',n(data?.pendingBookings)],['Cancelled',n(data?.cancelledBookings)]]
 const otherBookings=Math.max(0,n(data?.totalBookings)-bookingItems.reduce((a,x)=>a+x[1],0))
 const fleetItems=[['Available',n(data?.availableVehicles)],['Other',Math.max(0,n(data?.totalVehicles)-n(data?.availableVehicles))]]
 return <div className="page-content sm-dashboard sm-compact">
  <div className="sm-dash-header"><div><span className="sm-eyebrow">OPERATIONS / OVERVIEW</span><h1>Good to see you, Administrator.</h1><p>Everything happening across your transport network, in one place.</p></div><button className="sm-refresh" onClick={()=>setRefresh(x=>x+1)}><RefreshCw size={14}/> Refresh</button></div>
  <div className="sm-dash-shortcuts"><Link to="/admin/trips"><CalendarDays size={14}/> Schedule trip <ArrowRight size={13}/></Link><Link to="/admin/routes"><Route size={14}/> Routes</Link><Link to="/admin/vehicles"><BusFront size={14}/> Fleet</Link><Link to="/admin/reports"><Activity size={14}/> Reports</Link></div>
  {error&&<p role="alert" className="auth-error">{error}</p>}
  {!data?<div className="sm-panel">Loading live dashboard…</div>:<>
   <div className="sm-metrics">{stats.map(([key,label,Icon])=><div className="sm-metric" key={key}><div className="sm-metric-top"><span className="sm-metric-label">{label}</span><span className="sm-metric-icon"><Icon size={15}/></span></div><strong>{fmt(data[key])}</strong></div>)}</div>
   <div className="sm-chart-grid"><section className="sm-panel"><div className="sm-panel-head"><div><h2>Trip distribution</h2><p>Live breakdown of all scheduled and recorded journeys</p></div><span className="sm-mini-tag">Trips</span></div><Donut label="Total trips" total={tripTotal} items={[...tripItems,...(otherTrips?[['Other',otherTrips]]:[])]}/></section><section className="sm-panel"><div className="sm-panel-head"><div><h2>Booking activity</h2><p>Current booking statuses from Oracle</p></div><span className="sm-mini-tag">Bookings</span></div><BarChart items={[...bookingItems,...(otherBookings?[['Other',otherBookings]]:[])]}/><div className="sm-chart-foot"><span>Total bookings</span><strong>{fmt(data.totalBookings)}</strong></div></section></div>
   <div className="sm-chart-grid sm-bottom-grid"><section className="sm-panel"><div className="sm-panel-head"><div><h2>Fleet availability</h2><p>Vehicles ready for service versus the rest of your fleet</p></div></div><div className="sm-fleet-summary"><strong>{fmt(data.availableVehicles)} <small>/ {fmt(data.totalVehicles)} vehicles</small></strong><span>{n(data.totalVehicles)?Math.round(n(data.availableVehicles)/n(data.totalVehicles)*100):0}% available</span></div><div className="sm-stacked-track">{fleetItems.map(([label,value],i)=><div key={label} title={`${label}: ${value}`} style={{width:`${n(data.totalVehicles)?value/n(data.totalVehicles)*100:0}%`,background:COLORS[i]}}/>)}</div><div className="sm-inline-legend"><span><i style={{background:COLORS[0]}}/> Available</span><span><i style={{background:COLORS[1]}}/> Other</span></div></section><section className="sm-panel"><div className="sm-panel-head"><div><h2>At a glance</h2><p>Essential operational indicators</p></div></div><div className="sm-overview-list"><div><span>Active routes</span><strong>{fmt(data.activeRoutes)} / {fmt(data.totalRoutes)}</strong></div><div><span>Completed trips</span><strong>{fmt(data.completedTrips)}</strong></div><div><span>Gross revenue</span><strong>LKR {fmt(data.grossRevenue)}</strong></div><div><span>Total payments</span><strong>{fmt(data.totalPayments)}</strong></div></div></section></div>
   <p className="sm-data-note">All values shown are returned by your live backend. Charts reflect current totals, not fabricated historical trends.</p>
  </>}
 </div>
}
