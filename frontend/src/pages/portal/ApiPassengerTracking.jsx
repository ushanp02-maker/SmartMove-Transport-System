import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { BusFront, Clock3, MapPin, Navigation, Radio, Route, Ticket } from 'lucide-react'
import { listPassengerBookings } from '../../services/bookingService'
import { subscribeToTripLocation } from '../../services/trackingService'
import MapPicker from '../../components/MapPicker'
import './PassengerPolish.css'

export default function ApiPassengerTracking() {
  const { tripId } = useParams()
  const [bookings, setBookings] = useState([])
  const [error, setError] = useState('')
  const [point, setPoint] = useState(null)
  const [status, setStatus] = useState({ status: 'waiting' })
  useEffect(() => { listPassengerBookings().then(setBookings).catch(e => setError(e.message)) }, [])
  const owned = bookings.filter(b => !['CANCELLED', 'REFUNDED'].includes(String(b.status).toUpperCase()))
  const selected = owned.find(b => String(b.tripId) === String(tripId))
  useEffect(() => {
    if (!selected) return
    return subscribeToTripLocation(selected.tripId, setPoint, setStatus)
  }, [selected?.tripId])
  return <div className="sp-page">
    <header className="sp-heading sp-tracking-hero"><div><span className="sp-eyebrow">YOUR JOURNEY, AT A GLANCE</span><h1>Track my journey</h1><p>View the live location and progress of your selected booking.</p></div><span className="sp-hero-icon"><Navigation size={43}/></span></header>
    <section className="sp-surface sp-tracking-panel">
      <div className="sp-booking-picker"><span className="sp-booking-icon"><BusFront size={22}/></span><div className="sp-booking-field"><label>Select your booking</label><p>Choose the journey you want to follow</p><div className="sp-tracking-select">{owned.length ? owned.map(b => <Link className={selected?.id === b.id ? 'selected' : ''} key={b.id} to={'/passenger/tracking/' + b.tripId}><Ticket size={16}/>{b.bookingReference} · Trip #{b.tripId}</Link>) : <span>No active bookings available.</span>}</div></div></div>
      {error && <p className="sp-error" role="alert">{error}</p>}
      {selected ? <div className="sp-tracking-grid"><div className="sp-trip-progress"><h2><Route size={18}/> Trip progress</h2><div className="sp-timeline"><div className="sp-timeline-stop"><span className="sp-timeline-dot active"/><div><strong>Booking confirmed route</strong><small>Trip #{selected.tripId}</small></div></div><div className="sp-timeline-stop"><span className={'sp-timeline-dot ' + (point ? 'active' : '')}/><div><strong>{point ? 'Driver location received' : 'Awaiting driver location'}</strong><small>{point ? 'GPS update available' : 'No GPS update received yet'}</small></div></div><div className="sp-timeline-stop"><span className="sp-timeline-dot"/><div><strong>Journey destination</strong><small>Arrival is not yet verified</small></div></div></div><div className="sp-tracking-state"><Radio size={17}/><span>{status.message || 'Location appears when the assigned driver shares GPS updates.'}</span></div></div><div className="sp-live-map"><MapPicker readOnly pickup={point ? { latitude: point.latitude, longitude: point.longitude, label: 'Last reported vehicle location' } : null} destination={null} height={365}/><div className="sp-map-caption"><span><MapPin size={16}/>{point ? 'Last reported vehicle location' : 'Waiting for a GPS signal'}</span><span><Clock3 size={15}/>{point ? `${Number(point.latitude).toFixed(5)}, ${Number(point.longitude).toFixed(5)}` : 'Not yet available'}</span></div></div></div> : <div className="sp-empty"><Navigation size={28}/><h3>Select a booking to start tracking</h3><p>Live GPS information is shown only after the assigned driver sends an update.</p></div>}
    </section>
  </div>
}
