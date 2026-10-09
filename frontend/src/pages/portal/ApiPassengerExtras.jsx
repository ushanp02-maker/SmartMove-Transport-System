import { useEffect, useState } from 'react'
import { request } from '../../services/apiClient'
import { listPassengerBookings } from '../../services/bookingService'
const list = data => Array.isArray(data) ? data : data?.content || []
export function ApiPassengerAnnouncements(){
 const [items,setItems]=useState([]),[error,setError]=useState('')
 useEffect(()=>{request('/announcements/public').then(data=>setItems(list(data))).catch(e=>setError(e.message))},[])
 return <section className="portal-panel"><h1>Service announcements</h1>{error&&<p role="alert">{error}</p>}{items.length?items.map((a,i)=><article key={a.id??i}><h3>{a.title}</h3><p>{a.message}</p><small>{a.category} · {a.priority}</small><hr/></article>):<p>No published announcements.</p>}</section>
}
export function ApiPassengerReviews(){
 const [bookings,setBookings]=useState([]),[bookingId,setBookingId]=useState(''),[rating,setRating]=useState(5),[title,setTitle]=useState(''),[comment,setComment]=useState(''),[message,setMessage]=useState('')
 useEffect(()=>{listPassengerBookings().then(setBookings).catch(e=>setMessage(e.message))},[])
 const submit=async e=>{
  e.preventDefault()
  try{
   const passenger=await request('/users/me/passenger')
   await request('/feedback',{method:'POST',body:JSON.stringify({passengerId:passenger.passengerId,bookingId:Number(bookingId),rating:Number(rating),title,comment,category:'GENERAL'})})
   setMessage('Feedback submitted for review.');setTitle('');setComment('')
  }catch(err){setMessage(err.message)}
 }
 return <section className="portal-panel"><h1>Trip feedback</h1><p>Feedback is saved to MongoDB and may require moderation.</p><form onSubmit={submit}><label>Booking<select required value={bookingId} onChange={e=>setBookingId(e.target.value)}><option value="">Choose a booking</option>{bookings.map(b=><option key={b.id} value={b.id}>{b.bookingReference}</option>)}</select></label><label>Rating<select value={rating} onChange={e=>setRating(e.target.value)}>{[5,4,3,2,1].map(n=><option key={n} value={n}>{n} / 5</option>)}</select></label><label>Title<input required maxLength={150} value={title} onChange={e=>setTitle(e.target.value)}/></label><label>Comment<textarea required maxLength={3000} value={comment} onChange={e=>setComment(e.target.value)}/></label><button className="button button-primary">Submit feedback</button></form><p role="status">{message}</p></section>
}
