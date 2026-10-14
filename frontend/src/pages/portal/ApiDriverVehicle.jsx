import { useEffect, useState } from 'react'
import { BusFront, CalendarDays, CheckCircle2, Gauge, Hash, Settings, Users, Wrench } from 'lucide-react'
import { request } from '../../services/apiClient'
import './DriverPolish.css'
const details = vehicle => [
  ['Registration number',vehicle.registrationNumber,Hash],
  ['Vehicle type',vehicle.vehicleType,BusFront],
  ['Seating capacity',vehicle.seatingCapacity,Users],
  ['Current mileage',vehicle.currentMileage==null?'—':vehicle.currentMileage+' km',Gauge],
  ['Status',vehicle.status,CheckCircle2],
  ['Next service date',vehicle.nextServiceDate,CalendarDays],
  ['Manufacture year',vehicle.manufactureYear,Settings],
  ['Service overdue',vehicle.serviceOverdue?'Yes':'No',Wrench],
  ['Maintenance in progress',vehicle.maintenanceInProgress?'Yes':'No',Wrench],
]
export default function ApiDriverVehicle() {
  const [items,setItems]=useState([]),[error,setError]=useState(''),[loading,setLoading]=useState(true)
  useEffect(()=>{let active=true;const load=async()=>{const data=await request('/trip-status/driver/my-trips');const trips=Array.isArray(data)?data:data?.content||[];const ids=[...new Set(trips.map(t=>t.vehicleId).filter(id=>id!=null))];const vehicles=await Promise.all(ids.map(id=>request('/vehicles/'+id)));if(active)setItems(vehicles)};load().catch(e=>{if(active)setError(e.message)}).finally(()=>{if(active)setLoading(false)});return()=>{active=false}},[])
  return <div className="dp-page"><header className="dp-hero"><div><span className="dp-kicker">YOUR ASSIGNED FLEET</span><h1>My assigned vehicles</h1><p>View vehicle specifications and current service status.</p></div><div className="dp-hero-symbol"><BusFront size={46}/></div></header><div className="dp-body">
    {error&&<p className="dp-error" role="alert">{error}</p>}
    {items.map((v,i)=><article className="dp-vehicle" key={v.id??i}><div><div className="dp-vehicle-title"><span className="dp-icon"><BusFront size={27}/></span><div><h2>{v.registrationNumber||'Vehicle #'+v.id}</h2><p>{v.name||v.vehicleType||'Assigned vehicle'}</p></div></div><div className="dp-vehicle-visual"><BusFront size={94} strokeWidth={1.2}/><strong>{v.name||'SmartMove transport'}</strong><span>Vehicle #{v.id??'—'}</span></div></div><div>{details(v).map(([label,value,Icon])=><div className="dp-detail" key={label}><Icon size={17}/><span>{label}</span><strong>{label==='Status'?<span className={'dp-pill '+String(value||'').toLowerCase()}>{value||'—'}</span>:String(value??'—')}</strong></div>)}</div></article>)}
    {!loading&&!items.length&&!error&&<div className="dp-empty"><BusFront size={30}/><h3>No vehicles assigned</h3><p>Vehicles associated with your trips will appear here.</p></div>}
    {loading&&<p>Loading your assigned vehicles...</p>}
  </div></div>
}
