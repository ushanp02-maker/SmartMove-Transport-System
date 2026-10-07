import { useState } from 'react'
import { ArrowRight, CalendarDays, Check, CircleAlert, MapPin, ShieldCheck, X } from 'lucide-react'
import { useAppData } from '../../services/useAppData'
import MapPicker from '../../components/MapPicker'
import { formatDate } from '../../services/formatters'

const ACTIVE_TRIP_STATUSES=['Scheduled','In progress','Assigned','IN_PROGRESS','ASSIGNED']
const toMinutes=time=>{const [hours,minutes]=String(time||'00:00').split(':').map(Number);return hours*60+minutes}
const overlaps=(startA,endA,startB,endB)=>startA<endB&&startB<endA
const Status=({children})=><span className={`portal-status ${String(children).toLowerCase().replace(/[^a-z]+/g,'-')}`}>{children}</span>

function requestAssignmentConflict(data,request,vehicle,driver) {
  const requestStart=toMinutes(request.preferredTime)
  const requestEnd=requestStart+120
  return data.trips.find(trip=>trip.date===request.travelDate&&ACTIVE_TRIP_STATUSES.includes(trip.status)&&((trip.vehicleId===vehicle.id)||(trip.driverId===driver.id))&&overlaps(requestStart,requestEnd,toMinutes(trip.departure),toMinutes(trip.arrival)<toMinutes(trip.departure)?toMinutes(trip.arrival)+1440:toMinutes(trip.arrival)))
}

function RequestCard({request,data,onUpdate,onAssign}) {
  const pickup=request.pickup?{...request.pickup,latitude:request.pickup.latitude,longitude:request.pickup.longitude}:null
  const destination=request.destination?{...request.destination,latitude:request.destination.latitude,longitude:request.destination.longitude}:null
  const [vehicleId,setVehicleId]=useState(request.assignedVehicleId||'')
  const [driverId,setDriverId]=useState(request.assignedDriverId||'')
  const [notes,setNotes]=useState(request.notes||'')
  const terminal=['REJECTED','COMPLETED','CANCELLED'].includes(request.status)
  return <article className="admin-custom-request"><header className="admin-custom-request-head"><div><strong>{request.id}</strong><Status>{request.status}</Status><span>{request.passengers} passenger(s)</span></div><span><CalendarDays size={13}/>{formatDate(request.travelDate)} · {request.preferredTime}</span></header><h2>{request.pickupLabel||request.pickup?.label} <i>→</i> {request.destinationLabel||request.destination?.label}</h2><p className="admin-request-requirements">{request.vehiclePreference||'No vehicle preference'}{request.specialRequirements?` · ${request.specialRequirements}`:''}</p><MapPicker pickup={pickup} destination={destination} readOnly height={205}/><label className="admin-notes-field">Operations notes<textarea rows="2" value={notes} onChange={event=>setNotes(event.target.value)} placeholder="Internal demo note"/></label><div className="admin-request-controls"><label>Available vehicle<select value={vehicleId} onChange={event=>setVehicleId(event.target.value)}><option value="">Select vehicle</option>{data.vehicles.map(vehicle=><option value={vehicle.id} key={vehicle.id} disabled={vehicle.status!=='Active'}>{vehicle.plate} · {vehicle.name} ({vehicle.capacity} seats · {vehicle.status})</option>)}</select></label><label>Available driver<select value={driverId} onChange={event=>setDriverId(event.target.value)}><option value="">Select driver</option>{data.drivers.map(driver=><option value={driver.id} key={driver.id} disabled={driver.status!=='On duty'}>{driver.name} · {driver.status}</option>)}</select></label></div><footer className="admin-request-actions"><span><ShieldCheck size={14}/> Demo availability checks; backend must enforce assignments.</span><div>{request.status==='PENDING'&&<><button type="button" className="button button-outline reject-button" onClick={()=>onUpdate(request,'REJECTED',notes)}><X size={14}/> Reject</button><button type="button" className="button button-outline" onClick={()=>onUpdate(request,'APPROVED',notes)}><Check size={14}/> Approve</button></>}{!terminal&&<button type="button" className="button button-primary" onClick={()=>onAssign(request,{vehicleId,driverId,notes})}><ArrowRight size={14}/> Assign and schedule</button>}</div></footer></article>
}

export default function CustomTripRequestsAdmin() {
  const {data,addRecord,updateRecord}=useAppData()
  const [filter,setFilter]=useState('PENDING')
  const [notice,setNotice]=useState('')
  const requests=(data.customTripRequests||[]).filter(request=>filter==='ALL'||request.status===filter).sort((a,b)=>b.createdAt.localeCompare(a.createdAt))
  const update=(request,status,notes)=>{updateRecord('customTripRequests',request.id,{status,notes});setNotice(`${request.id} marked ${status}.`)}
  const assign=(request,{vehicleId,driverId,notes})=>{
    const vehicle=data.vehicles.find(item=>item.id===vehicleId)
    const driver=data.drivers.find(item=>item.id===driverId)
    const driverAccount=data.users.find(user=>user.linkedProfileId===driverId&&user.role==='DRIVER')
    if(!vehicle||!driver)return setNotice('Select an available vehicle and driver before assigning.')
    if(vehicle.status!=='Active')return setNotice('Only active vehicles may be assigned.')
    if(driverAccount&&driverAccount.accountStatus!=='ACTIVE')return setNotice('The selected driver account is disabled.')
    if(driver.status!=='On duty')return setNotice('Only on-duty drivers may be assigned.')
    if(vehicle.capacity<Number(request.passengers))return setNotice(`Vehicle capacity (${vehicle.capacity}) is below the requested passenger count (${request.passengers}).`)
    if(requestAssignmentConflict(data,request,vehicle,driver))return setNotice('Vehicle or driver already has an overlapping trip assignment on that date.')
    const routeId=`CUSTOM-${request.id}`
    const tripId=`TR-${crypto.randomUUID().slice(0,8).toUpperCase()}`
    const start=request.preferredTime
    const [hour,minute]=start.split(':').map(Number)
    const arrival=new Date(2000,0,1,hour,minute+120).toTimeString().slice(0,5)
    const route={id:routeId,routeId,name:`${request.pickupLabel} → ${request.destinationLabel}`,origin:request.pickupLabel,destination:request.destinationLabel,distance:0,duration:'To be confirmed by operations',fare:0,status:'Active',serviceType:'CUSTOM',stops:[{stopOrder:1,name:request.pickupLabel,latitude:request.pickup?.latitude,longitude:request.pickup?.longitude,scheduledTime:start},{stopOrder:2,name:request.destinationLabel,latitude:request.destination?.latitude,longitude:request.destination?.longitude,scheduledTime:arrival}]}
    const trip={id:tripId,routeId,vehicleId,driverId,date:request.travelDate,departure:start,arrival,seats:vehicle.capacity,status:'Scheduled',serviceType:'CUSTOM',customRequestId:request.id,passengerCount:Number(request.passengers)}
    addRecord('routes',route);addRecord('trips',trip)
    updateRecord('customTripRequests',request.id,{status:'ASSIGNED',assignedVehicleId:vehicleId,assignedDriverId:driverId,assignedTripId:tripId,notes})
    setNotice(`${request.id} assigned. Scheduled demo trip ${tripId} created for driver ${driver.name}.`)
  }
  const pending=(data.customTripRequests||[]).filter(request=>request.status==='PENDING').length
  return <div className="page-content"><div className="page-heading"><div><div className="eyebrow">SERVICE REQUESTS · DEMO WORKFLOW</div><h1>Custom trip requests</h1><p>Review passenger requests, validate assignments, and schedule approved journeys.</p></div><span className="request-count">{pending} pending</span></div><div className="custom-admin-banner"><CircleAlert size={16}/><span>All request approvals and assignment checks are browser-side demo operations. The backend must perform authoritative, atomic driver/vehicle availability checks.</span></div>{notice&&<div className="notice-banner" role="status"><span>{notice}</span><button onClick={()=>setNotice('')} aria-label="Dismiss"><X size={16}/></button></div>}<div className="portal-tabs">{['PENDING','APPROVED','ASSIGNED','IN_PROGRESS','COMPLETED','REJECTED','ALL'].map(status=><button key={status} className={filter===status?'active':''} onClick={()=>setFilter(status==='ALL'?'ALL':status)}>{status.replace('_',' ')}<small>{status==='ALL'?(data.customTripRequests||[]).length:(data.customTripRequests||[]).filter(request=>request.status===status).length}</small></button>)}</div>{requests.length?<div className="custom-admin-list">{requests.map(request=><RequestCard key={request.id} request={request} data={data} onUpdate={update} onAssign={assign}/>)}</div>:<div className="portal-panel"><div className="portal-empty"><span><MapPin size={22}/></span><strong>No {filter==='ALL'?'custom trip':' '+filter.toLowerCase().replace('_',' ')} requests</strong><p>Passenger custom trip submissions will appear here for operations review.</p></div></div>}</div>
}
