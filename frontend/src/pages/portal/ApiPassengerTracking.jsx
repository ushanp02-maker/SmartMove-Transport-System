import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { listPassengerBookings } from '../../services/bookingService'
import { subscribeToTripLocation } from '../../services/trackingService'
import MapPicker from '../../components/MapPicker'
export default function ApiPassengerTracking(){
 const {tripId}=useParams()
 const [bookings,setBookings]=useState([]),[error,setError]=useState(''),[point,setPoint]=useState(null),[status,setStatus]=useState({status:'waiting'})
 useEffect(()=>{listPassengerBookings().then(setBookings).catch(e=>setError(e.message))},[])
 const owned=bookings.filter(b=>!['CANCELLED','REFUNDED'].includes(String(b.status).toUpperCase()))
 const selected=owned.find(b=>String(b.tripId)===String(tripId))
 useEffect(()=>{
  if(!selected)return
  return subscribeToTripLocation(selected.tripId,setPoint,setStatus)
 },[selected])
 return <section className="portal-panel"><h1>Track my journey</h1>{error&&<p role="alert">{error}</p>}{!selected&&<div><p>Select one of your bookings to view its live GPS status.</p>{owned.map(b=><p key={b.id}><Link to={'/passenger/tracking/'+b.tripId}>{b.bookingReference} · Trip #{b.tripId}</Link></p>)}</div>}{selected&&<><p>Trip #{selected.tripId} · {status.status}</p><p>{status.message||'Only GPS data submitted by the assigned driver is displayed.'}</p><MapPicker readOnly pickup={point?{latitude:point.latitude,longitude:point.longitude,label:'Last reported vehicle location'}:null} destination={null} height={390}/>{point&&<p>GPS: {point.latitude}, {point.longitude}</p>}</>}</section>
}
