import { Link, useParams, useSearchParams } from 'react-router-dom'
import { ArrowRight, BusFront, ShieldCheck, Users } from 'lucide-react'
import { useAppData } from '../../services/useAppData'
import { formatDate, formatLkr } from '../../services/formatters'
import { matchRouteStops, resolveBookingStops } from '../../services/routeService'
import MapPicker from '../../components/MapPicker'

const today = () => {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
}
const booked = (data, id) => (data.bookings || [])
  .filter(booking => booking.tripId === id && booking.status !== 'Cancelled')
  .reduce((sum, booking) => sum + Number(booking.seats || 0), 0)

export default function TripDetailsPage() {
  const { tripId } = useParams()
  const [params] = useSearchParams()
  const { data } = useAppData()
  const trip = data.trips.find(item => item.id === tripId)

  if (!trip) return <div className="portal-empty"><span><BusFront size={22}/></span><strong>Journey not found</strong><p>This trip is not available in this browser's demo data. Use the same localhost port where you created it.</p><Link className="button button-primary" to="/passenger/search">Browse trips</Link></div>

  const route = data.routes.find(item => item.id === trip.routeId)
  if (!route) return <div className="portal-empty"><strong>Route not found</strong><p>The scheduled trip has no matching route.</p><Link className="button button-primary" to="/passenger/search">Browse trips</Link></div>

  const vehicle = data.vehicles.find(item => item.id === trip.vehicleId)
  const driver = data.drivers.find(item => item.id === trip.driverId)
  const stops = (route.stops || []).slice().sort((a, b) => a.stopOrder - b.stopOrder)
  const hasStopSelection = params.has('boardingStopId') || params.has('destinationStopId')
  const selection = hasStopSelection
    ? resolveBookingStops(route, {
        boardingStopId: params.get('boardingStopId'),
        destinationStopId: params.get('destinationStopId'),
      })
    : matchRouteStops(route)

  if (!selection) return <div className="portal-empty"><strong>Invalid boarding or destination stop</strong><p>Please search again and select a valid journey.</p><Link className="button button-primary" to="/passenger/search">Search journeys</Link></div>

  const { boardStop, alightStop } = selection
  const boardingTime = boardStop.scheduledTime || trip.departure
  const destinationTime = alightStop.scheduledTime || trip.arrival
  const staffBooked = (data.staffBookings || [])
    .filter(booking => booking.tripId === trip.id && booking.status !== 'Cancelled')
    .reduce((sum, booking) => sum + Number(booking.seats || 0), 0)
  const remaining = Math.max(0, Number(trip.seats || 0) - booked(data, trip.id) - staffBooked)
  const eligible = trip.status === 'Scheduled' && trip.date >= today() && remaining > 0 && trip.serviceType !== 'STAFF' && trip.serviceType !== 'CUSTOM'
  const stopParams = new URLSearchParams({
    boardingStopId: String(boardStop.stopId || boardStop.id || `${route.id}-STOP-${boardStop.stopOrder}`),
    destinationStopId: String(alightStop.stopId || alightStop.id || `${route.id}-STOP-${alightStop.stopOrder}`),
  })
  const bookingUrl = `/passenger/book/${trip.id}?${stopParams.toString()}`

  return <>
    <Link className="portal-back-link" to="/passenger/search">← Back to route search</Link>
    <div className="trip-detail-hero">
      <span>YOUR SELECTED JOURNEY · DEMO SCHEDULE</span>
      <h1>{boardStop.name}<i>to</i>{alightStop.name}</h1>
      <p>Full bus route: {route.origin} → {route.destination} · {formatDate(trip.date)}</p>
    </div>
    <div className="trip-detail-grid">
      <div className="trip-detail-main">
        <section className="portal-panel">
          <div className="portal-panel-head"><div><h2>Your boarding and destination</h2><p>The selected stops are preserved when you proceed to booking.</p></div></div>
          <div className="detail-timeline">
            <div><span className="timeline-dot start"/><small>BOARD AT</small><strong>{boardStop.name}</strong><p>{boardingTime || 'Time not supplied'}</p></div>
            <i/>
            <div><span className="timeline-dot end"/><small>GET OFF AT</small><strong>{alightStop.name}</strong><p>{destinationTime || 'Time not supplied'}</p></div>
          </div>
        </section>
        <section className="portal-panel">
          <div className="portal-panel-head"><div><h2>Full route and stop times</h2><p>All stops served by this bus, including those outside your selected journey.</p></div></div>
          <ol className="passenger-route-stop-list">
            {stops.length ? stops.map(stop => <li key={stop.stopOrder}>
              <span className="route-stop-index">{stop.stopOrder}</span>
              <span><strong>{stop.name}{stop.stopOrder === boardStop.stopOrder ? ' · Your boarding stop' : stop.stopOrder === alightStop.stopOrder ? ' · Your destination' : ''}</strong><small>{stop.scheduledTime || 'Scheduled time not supplied'}</small></span>
            </li>) : <li><span className="route-stop-index">•</span><span><strong>{route.origin} → {route.destination}</strong><small>No intermediate stop timetable supplied.</small></span></li>}
          </ol>
        </section>
        <section className="portal-panel">
          <div className="portal-panel-head"><div><h2>Route map</h2><p>Demo route stop landmarks, not live vehicle tracking.</p></div></div>
          <MapPicker routeStops={stops} readOnly showCurrentLocation={false} height={300}/>
        </section>
        <section className="portal-panel">
          <div className="portal-panel-head"><div><h2>Assigned service</h2><p>Vehicle and driver linked to this scheduled trip.</p></div></div>
          <div className="trip-detail-facts">
            <div><BusFront size={17}/><span><small>VEHICLE</small><strong>{vehicle?.name || 'Not assigned'}</strong><em>{vehicle?.plate} · {vehicle?.type}</em></span></div>
            <div><Users size={17}/><span><small>DRIVER</small><strong>{driver?.name || 'Not assigned'}</strong><em>Profile data from the demo schedule</em></span></div>
            <div><Users size={17}/><span><small>SEATS</small><strong>{remaining} available</strong><em>{trip.seats} total seats</em></span></div>
          </div>
        </section>
      </div>
      <aside className="trip-fare-card">
        <span>YOUR JOURNEY</span>
        <strong>{formatLkr(route.fare)}</strong>
        <small>Full-route demo fare per seat; segment pricing not available</small>
        <hr/>
        <div><span>Boarding</span><b>{boardStop.name}</b></div>
        <div><span>Destination</span><b>{alightStop.name}</b></div>
        <div><span>Travel date</span><b>{formatDate(trip.date)}</b></div>
        <div><span>Boarding time</span><b>{boardingTime || 'Not supplied'}</b></div>
        <div><span>Destination time</span><b>{destinationTime || 'Not supplied'}</b></div>
        <div><span>Available seats</span><b>{remaining}</b></div>
        {eligible ? <Link className="button button-primary" to={bookingUrl}>Book this trip <ArrowRight size={15}/></Link> : <span className="trip-unavailable-note">This scheduled trip is not currently bookable.</span>}
        <p><ShieldCheck size={14}/> Demo booking only. No payment is collected.</p>
      </aside>
    </div>
    <p className="portal-demo-caption">Route and schedule data are demo records. Backend validation and live seat locking are not connected.</p>
  </>
}
