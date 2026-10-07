import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ArrowRight, CalendarDays, Check, CircleAlert, Clock3, MapPin, Navigation, ShieldCheck, BusFront, FileText } from 'lucide-react'
import MapPicker from '../../components/MapPicker'
import { useAppData } from '../../services/useAppData'
import { formatDate } from '../../services/formatters'
import { CUSTOM_TRIP_STATUSES } from '../../services/customTripService'
import { subscribeToTripLocation } from '../../services/trackingService'

const today = () => new Date().toISOString().slice(0, 10)
const Status = ({children}) => <span className={`portal-status ${String(children).toLowerCase().replace(/[^a-z]+/g,'-')}`}>{children}</span>
const coarse = point => point ? { latitude: Math.round(point.latitude * 100) / 100, longitude: Math.round(point.longitude * 100) / 100, label: point.label } : null

function PageTitle({title,subtitle,kicker='YOUR TRANSPORT SERVICES',action}) { return <div className="portal-page-heading"><div><span>{kicker}</span><h1>{title}</h1><p>{subtitle}</p></div>{action}</div> }
function CustomRequest({request,data}) {
  const trip=data.trips.find(item=>item.id===request.assignedTripId)
  const vehicle=data.vehicles.find(item=>item.id===request.assignedVehicleId)
  const driver=data.drivers.find(item=>item.id===request.assignedDriverId)
  return <article className="custom-request-card"><div className="custom-request-heading"><div><span className="custom-request-id">{request.id}</span><Status>{request.status}</Status></div><span className="custom-request-date"><CalendarDays size={13}/>{formatDate(request.travelDate)}</span></div><h3>{request.pickup?.label||request.pickupLabel||'Pickup location'} <i>→</i> {request.destination?.label||request.destinationLabel||'Destination'}</h3><div className="custom-request-details"><span><Clock3 size={13}/>{request.preferredTime} · {request.passengers} passenger(s)</span><span><BusFront size={13}/>{request.vehiclePreference||'No vehicle preference'}</span></div>{request.specialRequirements&&<p className="custom-requirements">{request.specialRequirements}</p>}{request.notes&&<p className="custom-requirements"><strong>Operations:</strong> {request.notes}</p>}{trip&&<div className="custom-assignment"><Check size={14}/><span>Assigned {vehicle?.plate||trip.vehicleId} · {driver?.name||trip.driverId} · trip {trip.id}</span>{trip.status==='In progress'&&<Link to={`/passenger/track/${trip.id}`}>Track trip <ArrowRight size={13}/></Link>}</div>}</article>
}

export function CustomTripRequests() {
  const {data,session,addRecord}=useAppData()
  const passenger=data.passengers.find(item=>item.id===session?.userId)
  const [pickup,setPickup]=useState(null)
  const [destination,setDestination]=useState(null)
  const [pickTarget,setPickTarget]=useState('pickup')
  const [date,setDate]=useState('')
  const [time,setTime]=useState('08:00')
  const [passengers,setPassengers]=useState(1)
  const [vehiclePreference,setVehiclePreference]=useState('No preference')
  const [specialRequirements,setSpecialRequirements]=useState('')
  const [error,setError]=useState('')
  const [notice,setNotice]=useState('')
  const requests=(data.customTripRequests||[]).filter(request=>request.passengerId===passenger?.id).sort((a,b)=>b.createdAt.localeCompare(a.createdAt))
  const submit=event=>{
    event.preventDefault();setError('');setNotice('')
    if(!pickup||!destination)return setError('Choose both pickup and destination on the map or from the supported place search.')
    if(pickup.latitude===destination.latitude&&pickup.longitude===destination.longitude)return setError('Choose two different locations.')
    if(!date||date<today())return setError('Choose today or a future travel date.')
    if(passengers<1||passengers>48)return setError('Passenger count must be between 1 and 48.')
    const id=`CR-${crypto.randomUUID().slice(0,8).toUpperCase()}`
    // Demo localStorage stores only rounded (~1 km) map points, never precise GPS coordinates.
    addRecord('customTripRequests',{id,passengerId:passenger.id,pickup:coarse(pickup),destination:coarse(destination),pickupLabel:pickup.label,destinationLabel:destination.label,travelDate:date,preferredTime:time,passengers:Number(passengers),vehiclePreference,specialRequirements:specialRequirements.trim(),status:'PENDING',createdAt:new Date().toISOString(),assignedVehicleId:null,assignedDriverId:null,assignedTripId:null,notes:''})
    setNotice(`Request ${id} submitted. It is pending administrator review and is not a confirmed booking.`)
    setPickup(null);setDestination(null);setDate('');setTime('08:00');setPassengers(1);setSpecialRequirements('')
  }
  return <><PageTitle title="Custom trips." subtitle="Request a vehicle for a journey that doesn’t fit a regular timetable." kicker="PLAN A JOURNEY YOUR WAY"/><div className="custom-trip-policy"><FileText size={17}/><span><strong>Request first, confirmation later.</strong> Custom journeys are reviewed by operations. This form does not confirm a trip or collect payment.</span></div><div className="custom-trip-layout"><form className="custom-trip-form portal-panel" onSubmit={submit}><h2>Tell us about your journey</h2><p>Choose approximate pickup and destination points. Precise GPS is not saved with demo requests.</p><MapPicker pickup={pickup} destination={destination} onPickupChange={setPickup} onDestinationChange={setDestination} pickTarget={pickTarget} onTargetChange={setPickTarget} height={300}/><div className="custom-trip-fields"><label>Travel date<input type="date" min={today()} value={date} onChange={event=>setDate(event.target.value)} required/></label><label>Preferred departure time<input type="time" value={time} onChange={event=>setTime(event.target.value)} required/></label><label>Passengers<input type="number" min="1" max="48" value={passengers} onChange={event=>setPassengers(event.target.value)} required/></label><label>Vehicle preference<select value={vehiclePreference} onChange={event=>setVehiclePreference(event.target.value)}><option>No preference</option><option>Van</option><option>Mini Coach</option><option>Coach</option></select></label><label className="custom-span">Special requirements<textarea rows="3" maxLength="500" value={specialRequirements} onChange={event=>setSpecialRequirements(event.target.value)} placeholder="Accessibility, luggage or other requirements (optional)"/></label></div>{error&&<p className="form-error-text" role="alert">{error}</p>}{notice&&<div className="success-notice" role="status"><Check size={15}/>{notice}</div>}<button className="button button-primary"><Navigation size={15}/> Submit trip request</button></form><aside className="custom-process-card"><span className="section-kicker">WHAT HAPPENS NEXT</span><h2>A thoughtful trip<br/>takes a little planning.</h2><ol><li><span>01</span><div><strong>Your request</strong><small>We keep your trip in PENDING until reviewed.</small></div></li><li><span>02</span><div><strong>Operations review</strong><small>An admin checks capacity, vehicle and driver schedules.</small></div></li><li><span>03</span><div><strong>Assignment & approval</strong><small>Vehicle/driver assignment updates status to ASSIGNED.</small></div></li></ol><div className="custom-process-foot"><ShieldCheck size={15}/> Approval and availability checks are demo UI logic. A backend must enforce them atomically.</div></aside></div><section className="portal-panel my-custom-requests"><div className="portal-panel-head"><div><h2>My custom trip requests</h2><p>Follow each request through review and assignment.</p></div><span className="request-count">{requests.length} request(s)</span></div>{requests.length?<div className="custom-request-list">{requests.map(request=><CustomRequest key={request.id} request={request} data={data}/>)}</div>:<div className="portal-empty compact"><span><FileText size={20}/></span><strong>No custom requests yet</strong><p>Your submitted requests and their review status appear here.</p></div>}<div className="custom-status-legend">Request lifecycle: {CUSTOM_TRIP_STATUSES.map(status=><Status key={status}>{status}</Status>)}</div></section></>
}

export function PassengerTripTracking() {
  const {tripId}=useParams()
  const {data,session}=useAppData()
  const passengerId=session?.userId
  const ownedTripIds=[...new Set([...(data.bookings||[]).filter(booking=>booking.passengerId===passengerId&&booking.status!=='Cancelled').map(booking=>booking.tripId),...(data.staffBookings||[]).filter(booking=>booking.passengerId===passengerId&&booking.status!=='Cancelled').map(booking=>booking.tripId),...(data.customTripRequests||[]).filter(request=>request.passengerId===passengerId&&request.assignedTripId).map(request=>request.assignedTripId)])]
  const ownedTrips=data.trips.filter(item=>ownedTripIds.includes(item.id))
  const activeTrip=ownedTrips.find(item=>['In progress','IN_PROGRESS'].includes(item.status))
  const selectedTripId=tripId||activeTrip?.id
  const regularBooking=data.bookings.find(booking=>booking.tripId===selectedTripId&&booking.passengerId===passengerId&&booking.status!=='Cancelled')
  const staffBooking=(data.staffBookings||[]).find(booking=>booking.tripId===selectedTripId&&booking.passengerId===passengerId&&booking.status!=='Cancelled')
  const customRequest=(data.customTripRequests||[]).find(request=>request.passengerId===passengerId&&request.assignedTripId===selectedTripId)
  const trip=data.trips.find(item=>item.id===selectedTripId)
  const authorized=Boolean(regularBooking||staffBooking||customRequest)
  const [location,setLocation]=useState(null)
  const [liveState,setLiveState]=useState({status:'unavailable',message:'Live vehicle data is not connected in Demo mode. No simulated vehicle position is shown.'})
  const isActiveTrip=['In progress','IN_PROGRESS'].includes(trip?.status)
  useEffect(()=>{
    if(!authorized||!selectedTripId||!isActiveTrip)return undefined
    return subscribeToTripLocation(selectedTripId,setLocation,setLiveState)
  },[authorized,selectedTripId,isActiveTrip])
  useEffect(()=>{
    if(!location?.recordedAt)return undefined
    const timer=window.setInterval(()=>{if(Date.now()-new Date(location.recordedAt).getTime()>60000)setLiveState({status:'stale',message:'The last reported vehicle location is more than one minute old.'})},10000)
    return ()=>window.clearInterval(timer)
  },[location])
  if(!authorized)return <><PageTitle title="Track a trip." subtitle="Choose one of your own bookings or approved requests. Location availability depends on a connected service." kicker="PASSENGER TRIP TRACKING"/>{ownedTrips.length?<div className="booking-record-list">{ownedTrips.map(item=>{const route=data.routes.find(entry=>entry.id===item.routeId)||data.staffRoutes.find(entry=>entry.routeId===item.routeId);return <article className="booking-record" key={item.id}><div className="booking-record-icon"><MapPin size={18}/></div><div className="booking-record-main"><span className="booking-reference">{item.id} <Status>{item.status}</Status></span><strong>{route?.origin||item.pickupLabel} → {route?.destination||item.destinationLabel}</strong><span>{formatDate(item.date)} · {item.departure} · {item.serviceType||'COMMUTER'}</span></div><div className="booking-record-side"><Link to={`/passenger/tracking/${item.id}`}>Open tracking <ArrowRight size={14}/></Link></div></article>})}</div>:<div className="portal-empty"><span><ShieldCheck size={21}/></span><strong>No trips available to track</strong><p>Tracking is available only for your own booked trips or assigned custom requests.</p><Link className="button button-outline" to="/passenger/bookings">View my bookings</Link></div>}</>
  const info=trip?data.routes.find(route=>route.id===trip.routeId)||data.staffRoutes.find(route=>route.routeId===trip.routeId):null
  const point=location?{latitude:location.latitude,longitude:location.longitude,label:'Reported vehicle location'}:null
  const displayedLiveState=authorized&&isActiveTrip?liveState:{status:'unavailable',message:'Location is available only for a trip you booked while it is IN_PROGRESS.'}
  return <><PageTitle title="Track your trip." subtitle="Vehicle location appears only when an authorized live source supplies it." kicker="YOUR ACTIVE JOURNEY"/><div className={`tracking-status-banner tracking-${displayedLiveState.status}`}><span className="tracking-pulse"/><div><strong>{displayedLiveState.status==='active'?'Live location received':displayedLiveState.status==='stale'?'Location may be stale':'Live location unavailable'}</strong><small>{displayedLiveState.message}</small></div><Status>{trip?.status||customRequest?.status||'Unknown'}</Status></div><div className="passenger-tracking-layout"><div className="portal-panel"><div className="portal-panel-head"><div><h2>{info?.origin||customRequest?.pickupLabel||'Pickup'} → {info?.destination||customRequest?.destinationLabel||'Destination'}</h2><p>{trip?`${trip.id} · ${trip.date} · ${trip.departure}`:'Custom trip request · awaiting schedule'}</p></div></div><MapPicker pickup={point} destination={null} routeStops={info?.stops||[]} readOnly height={390}/><div className="tracking-last-seen"><MapPin size={15}/>{location?`Last reported ${new Date(location.recordedAt).toLocaleString()} · accuracy ${Math.round(location.accuracy)} m`:'Route landmarks only. A vehicle marker appears only after an authorized backend/WebSocket update.'}</div></div><aside className="trip-fare-card tracking-trip-facts"><span>TRIP DETAILS</span><strong>{data.vehicles.find(vehicle=>vehicle.id===trip?.vehicleId)?.plate||'Vehicle not assigned'}</strong><small>Registration</small><hr/><div><span>Driver</span><b>{data.drivers.find(driver=>driver.id===trip?.driverId)?.name||'Not assigned'}</b></div><div><span>Service</span><b>{trip?.serviceType||customRequest?.serviceType||'COMMUTER'}</b></div><div><span>Last update</span><b>{location?new Date(location.recordedAt).toLocaleTimeString():'—'}</b></div><div className="tracking-demo-note"><CircleAlert size={14}/>No estimated arrival is shown without reliable backend timing.</div></aside></div></>
}
