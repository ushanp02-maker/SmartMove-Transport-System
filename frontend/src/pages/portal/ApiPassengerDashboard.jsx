import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { listPassengerBookings } from '../../services/bookingService'
import { request } from '../../services/apiClient'
export default function ApiPassengerDashboard(){
 const [bookings,setBookings]=useState([]),[announcements,setAnnouncements]=useState([]),[error,setError]=useState('')
 useEffect(()=>{
  let active=true
  Promise.all([listPassengerBookings(),request('/announcements/public')]).then(([items,news])=>{
   if(active){setBookings(items);setAnnouncements(Array.isArray(news)?news:news?.content||[])}
  }).catch(e=>{if(active)setError(e.message)})
  return()=>{active=false}
 },[])
 const upcoming=bookings.filter(b=>!['CANCELLED','COMPLETED'].includes(String(b.status).toUpperCase()))
 return <div className="page-content"><div className="page-heading"><div><h1>My journeys</h1><p>Live booking records from Oracle and service alerts from MongoDB.</p></div></div>{error&&<p role="alert" className="auth-error">{error}</p>}<section className="portal-panel"><h2>Bookings: {bookings.length}</h2><p>Upcoming or active: {upcoming.length}</p><Link className="button button-primary" to="/passenger/search">Find a trip</Link><Link className="button button-outline" to="/passenger/bookings">My bookings</Link></section><section className="portal-panel"><h2>Recent bookings</h2>{bookings.slice(0,5).map(b=><article key={b.id}><strong>{b.bookingReference}</strong><p>{b.status} · {b.boardingStop} → {b.destinationStop}</p></article>)}</section><section className="portal-panel"><h2>Service notices</h2>{announcements.slice(0,3).map((a,i)=><article key={a.id??i}><strong>{a.title}</strong><p>{a.message}</p></article>)}</section></div>
}
