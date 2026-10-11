import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { BusFront, Navigation, Route, RefreshCw } from 'lucide-react'
import { listPassengerBookings } from '../../services/bookingService'
import { request } from '../../services/apiClient'
import './PassengerPolish.css'

export default function ApiPassengerTracking() {
  const { tripId } = useParams()
  const navigate = useNavigate()
  const [bookings,setBookings] = useState([])
  const [progress,setProgress] = useState(null)
  const [error,setError] = useState('')
  useEffect(()=>{listPassengerBookings().then(setBookings).catch(e=>setError(e.message))},[])
  const owned=bookings.filter(b=>!['CANCELLED','REFUNDED'].includes(String(b.status).toUpperCase()))
  const selected=owned.find(b=>String(b.tripId)===String(tripId))
  useEffect(()=>{
    if(!selected){setProgress(null);return}
    let active=true
    const poll=()=>request(`/trip-checkpoints/${encodeURIComponent(selected.tripId)}`)
      .then(value=>{if(active){setProgress(value);setError('')}})
      .catch(e=>{if(active)setError(e.message)})
    poll()
    const timer=window.setInterval(poll,10000)
    return()=>{active=false;window.clearInterval(timer)}
  },[selected?.tripId])
  return <div className="sp-page">
    <header className="sp-heading sp-tracking-hero"><div><span className="sp-eyebrow">YOUR JOURNEY, AT A GLANCE</span><h1>Track my journey</h1><p>Follow driver-confirmed stops and trip status. No GPS tracking is used.</p></div><span className="sp-hero-icon"><Navigation size={43}/></span></header>
    <section className="sp-surface sp-tracking-panel">
      <div className="sp-booking-picker"><span className="sp-booking-icon"><BusFront size={22}/></span><div className="sp-booking-field">
        <label htmlFor="tracking-booking">Select your booking</label><p>Choose the journey you want to follow</p>
        <div className="sp-tracking-select"><select id="tracking-booking" value={selected?String(selected.tripId):''}
          onChange={event=>{setProgress(null);navigate('/passenger/tracking/'+event.target.value)}} disabled={!owned.length}
          style={{width:'100%',padding:'12px 14px',borderRadius:10,border:'1px solid #cadbd3',background:'#fff',color:'#173f31',font:'inherit'}}>
          <option value="" disabled>{owned.length?'Choose your booked trip':'No active bookings available'}</option>
          {owned.map(b=><option key={b.id} value={String(b.tripId)}>{b.bookingReference} · Trip #{b.tripId}</option>)}
        </select></div>
      </div></div>
      {error&&<p className="sp-error" role="alert">{error}</p>}
      {selected?<div className="sp-tracking-grid">
        <div className="sp-trip-progress" style={{gridColumn:'1 / -1'}}>
          <h2><Route size={18}/> Trip progress — Trip #{selected.tripId}</h2>
          <p>Trip status: <strong>{progress?.tripStatus||'Loading…'}</strong></p>
          <p>Updates are reported by the driver when they reach each stop. This page refreshes automatically every 10 seconds.</p>
          <div className="sp-timeline">
            {(progress?.stops||[]).map((stop,index)=>{
              const reached=(progress.confirmed||[]).find(c=>c.stopId===stop.id)
              return <div className="sp-timeline-stop" key={stop.id}>
                <span className={'sp-timeline-dot '+(reached?'active':'')}/>
                <div><strong>{stop.name}</strong><small>{reached?'Reached · '+String(reached.confirmedAt).replace('T',' '):index===(progress.confirmed||[]).length?'Next stop':'Not reached yet'}</small></div>
              </div>
            })}
          </div>
          <div className="sp-tracking-state"><RefreshCw size={17}/><span>{progress?.confirmed?.length||0} of {progress?.stops?.length||0} stops confirmed</span></div>
        </div>
      </div>:<div className="sp-empty"><Navigation size={28}/><h3>Select a booking to follow its progress</h3><p>Driver-confirmed stop updates appear here automatically.</p></div>}
    </section>
  </div>
}
