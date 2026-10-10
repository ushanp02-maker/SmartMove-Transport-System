import { useEffect, useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router-dom'
import { request } from '../../services/apiClient'
import { listUpcomingTrips, getTrip } from '../../services/tripService'
import { createBooking, getBookingQuote, getSeatAvailability, listPassengerBookings, cancelBooking } from '../../services/bookingService'
import { useAppData } from '../../services/useAppData'
import { formatLkr } from '../../services/formatters'
import RouteMapPreview from '../../components/RouteMapPreview'
import './smartmove-journey.css'

const dateOf = value => String(value || '').slice(0, 10)
const timeOf = value => String(value || '').slice(11, 16)
const errorText = error => error?.message || 'Request failed.'
const linkStops = (tripId, boardId, destinationId) => `/passenger/trip/${tripId}?${new URLSearchParams({ boardingStopId: boardId, destinationStopId: destinationId })}`
const routeStops = async routeId => {
  const details = await request(`/routes/${encodeURIComponent(routeId)}/details`)
  return { route: details.route, stops: [...(details.stops || [])].sort((a,b) => a.stopOrder - b.stopOrder) }
}
const title = (heading, detail) => <div className="portal-page-heading"><div><span>SMARTMOVE PASSENGER</span><h1>{heading}</h1><p>{detail}</p></div></div>
const message = (text, retry) => <div className="portal-panel"><p role="status">{text}</p>{retry && <button type="button" className="button button-outline" onClick={retry}>Try again</button>}</div>
const journey = booking => `${booking.boardingStop || 'Boarding'} → ${booking.destinationStop || 'Destination'}`

export function ApiSearchTrips() {
  const [params, setParams] = useSearchParams()
  const [tripType, setTripType] = useState(params.get('type') === 'STAFF' ? 'STAFF' : 'STANDARD')
  const [trips, setTrips] = useState([])
  const [routes, setRoutes] = useState({})
  const [from, setFrom] = useState(params.get('from') || '')
  const [to, setTo] = useState(params.get('to') || '')
  const [date, setDate] = useState(params.get('date') || '')
  const [seats, setSeats] = useState(Number(params.get('passengers') || 1))
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [refresh, setRefresh] = useState(0)
  const [selectedTripId, setSelectedTripId] = useState(null)
  const [sort, setSort] = useState('earliest')
  useEffect(() => {
    let active = true
    Promise.all([listUpcomingTrips(), request('/routes/active')]).then(async ([items, activeRoutes]) => {
      const routeIds = [...new Set(items.map(t => t.routeId))]
      const details = await Promise.all(routeIds.map(id => routeStops(id).then(data => [id, data])))
      if (active) {
        setTrips(items)
        setRoutes(Object.fromEntries(details.filter(([, data]) => activeRoutes.some(r => String(r.id) === String(data.route?.id)))))
        setError('')
      }
    }).catch(err => { if (active) setError(errorText(err)) }).finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [refresh])
  const matches = trips.flatMap(trip => {
    const details = routes[trip.routeId]
    if (!details || (tripType === 'STAFF' ? String(details.route?.serviceType).toUpperCase() !== 'STAFF' : !['STANDARD','COMMUTER'].includes(String(details.route?.serviceType || 'COMMUTER').toUpperCase()))) return []
    if (date && dateOf(trip.departureTime) !== date) return []
    const stops = details.stops
    const includes = (stop, value) => !value.trim() || stop.stopName.toLowerCase().includes(value.trim().toLowerCase())
    const board = stops.find(s => includes(s, from) && stops.some(d => d.stopOrder > s.stopOrder && includes(d, to)))
    const destination = stops.find(s => board && s.stopOrder > board.stopOrder && includes(s, to))
    return board && destination ? [{ trip, board, destination }] : []
  })
  const orderedMatches = [...matches].sort((a,b) => sort === 'latest' ? String(b.trip.departureTime).localeCompare(String(a.trip.departureTime)) : String(a.trip.departureTime).localeCompare(String(b.trip.departureTime)))
  const selected = orderedMatches.find(item => String(item.trip.id) === String(selectedTripId)) || orderedMatches[0]
  const selectedDetails = selected ? routes[selected.trip.routeId] : null
  return <div className="sm-search-page">
    <div className="sm-search-heading"><div>{title('Find and book your trip', 'Choose the trip type, select your route and travel date, and book your seat.')}</div><aside><strong>Different trip types</strong><p>Standard trips are open for all passengers. Staff transport is for company staff only. Custom trips let you request a personalised trip.</p></aside></div>
    <div className="sm-type-selector"><button type="button" className={tripType === 'STANDARD' ? 'selected' : ''} onClick={() => {setTripType('STANDARD');setSelectedTripId(null)}}><span className="sm-type-icon">▣</span><span><strong>Standard Trips</strong><small>Scheduled routes for all passengers</small></span><span className="sm-type-check">{tripType==='STANDARD'?'✓':'›'}</span></button><button type="button" className={tripType === 'STAFF' ? 'selected' : ''} onClick={() => {setTripType('STAFF');setSelectedTripId(null)}}><span className="sm-type-icon">♟</span><span><strong>Staff Transport</strong><small>Routes for company staff</small></span><span className="sm-type-check">{tripType==='STAFF'?'✓':'›'}</span></button><Link to="/passenger/custom-trips"><span className="sm-type-icon">➤</span><span><strong>Custom Trip</strong><small>Request a personalised trip</small></span><span className="sm-type-check">›</span></Link></div>
    <form className="sm-search-form sm-surface" onSubmit={e => {e.preventDefault();setSelectedTripId(null);setParams({from,to,date,passengers:String(seats),type:tripType})}}><h3>⌕ &nbsp; Search {tripType==='STAFF'?'staff transport':'standard trips'}</h3><div className="sm-search-fields"><label>From<input value={from} onChange={e=>setFrom(e.target.value)} placeholder="e.g. Maharagama"/></label><label>To<input value={to} onChange={e=>setTo(e.target.value)} placeholder="e.g. Nugegoda"/></label><label>Travel date<input type="date" value={date} onChange={e=>setDate(e.target.value)}/></label><label>Number of passengers<select value={seats} onChange={e=>setSeats(Number(e.target.value))}>{[1,2,3,4,5,6].map(n=><option key={n}>{n}</option>)}</select></label><button className="button button-primary">⌕ &nbsp; Search trips</button></div></form>
    {loading ? message('Loading upcoming trips...') : error ? message(error,()=>{setLoading(true);setRefresh(n=>n+1)}) : orderedMatches.length===0 ? message('No matching upcoming trips for this category. Check your stops or travel date.') :
    <div className="sm-results-layout"><section className="sm-surface sm-results"><div className="sm-results-title"><div><h2>Available {tripType==='STAFF'?'Staff':'Standard'} Trips</h2><p>{orderedMatches.length} matching upcoming trip{orderedMatches.length===1?'':'s'}</p></div><label>Sort by <select value={sort} onChange={e=>setSort(e.target.value)}><option value="earliest">Departure time (earliest)</option><option value="latest">Departure time (latest)</option></select></label></div><div className="sm-trip-list">{orderedMatches.map(({trip,board,destination})=><article key={trip.id} className={'sm-trip-option '+(selected?.trip.id===trip.id?'active':'')} onClick={()=>setSelectedTripId(trip.id)}><div className="sm-trip-times"><strong>{timeOf(trip.departureTime)}</strong><span>│</span><small>{timeOf(trip.arrivalTime)}</small></div><div className="sm-trip-info"><h3>{board.stopName} → {destination.stopName}</h3><p>{trip.routeName||'Scheduled route'}</p><small>{routes[trip.routeId]?.stops?.map(stop=>stop.stopName).join(', ')}</small></div><div className="sm-trip-fare"><strong>{trip.fare!=null?formatLkr(trip.fare):'Fare at checkout'}</strong><small>{trip.seatingCapacity!=null?trip.seatingCapacity+' total seats':'Check availability'}</small></div><Link className="button button-primary" to={`${linkStops(trip.id,board.id,destination.id)}&seats=${seats}`}>View seats</Link></article>)}</div></section><aside className="sm-surface sm-trip-detail">{selectedDetails&&<><div className="sm-trip-map"><RouteMapPreview route={selectedDetails.route} stops={selectedDetails.stops} height={260}/></div><div className="sm-detail-body"><h2>Trip details <span>{tripType}</span></h2><div className="sm-detail-stop"><strong>{timeOf(selected.trip.departureTime)} &nbsp; {selected.board.stopName} (Pickup)</strong><strong>{timeOf(selected.trip.arrivalTime)} &nbsp; {selected.destination.stopName} (Drop-off)</strong></div><div className="sm-detail-metrics"><span>Route<strong>{selected.trip.routeName||'Scheduled route'}</strong></span><span>Departure<strong>{dateOf(selected.trip.departureTime)}</strong></span><span>Seats<strong>{selected.trip.seatingCapacity??'Check availability'}</strong></span></div><Link className="button button-primary sm-detail-book" to={`${linkStops(selected.trip.id,selected.board.id,selected.destination.id)}&seats=${seats}`}>View seats and book</Link></div></>}</aside></div>}
  </div>
}

function useJourney(tripId, params) {
  const [state, setState] = useState({ loading: true, error: '', trip: null, details: null, board: null, destination: null, availability: null })
  const boardId = params.get('boardingStopId')
  const destinationId = params.get('destinationStopId')
  const [refresh, setRefresh] = useState(0)
  useEffect(() => {
    let active = true
    Promise.all([getTrip(tripId)]).then(async ([trip]) => {
      const details = await routeStops(trip.routeId)
      const board = details.stops.find(s => String(s.id) === String(boardId)) || (!boardId ? details.stops[0] : null)
      const destination = details.stops.find(s => String(s.id) === String(destinationId)) || (!destinationId ? details.stops.at(-1) : null)
      if (!board || !destination || board.stopOrder >= destination.stopOrder) throw new Error('Choose valid boarding and destination stops.')
      const availability = await getSeatAvailability({ tripId, boardingStopId: board.id, destinationStopId: destination.id })
      if (active) setState({ loading: false, error: '', trip, details, board, destination, availability })
    }).catch(err => { if (active) setState(prev => ({ ...prev, loading: false, error: errorText(err) })) })
    return () => { active = false }
  }, [tripId, boardId, destinationId, refresh])
  return [state, () => { setState(prev => ({ ...prev, loading: true })); setRefresh(n => n + 1) }]
}

export function ApiPassengerTripDetails() {
  const { tripId } = useParams()
  const [params] = useSearchParams()
  const [state, retry] = useJourney(tripId, params)
  if (state.loading) return message('Loading journey details...')
  if (state.error) return message(state.error, retry)
  const { trip, board, destination, availability, details } = state
  return <>{title(`${board.stopName} to ${destination.stopName}`, 'Live route and seat information')}
    <section className="portal-panel"><h2>{trip.routeName}</h2><p>Departure: {dateOf(trip.departureTime)} at {timeOf(trip.departureTime)}</p><p>Arrival: {dateOf(trip.arrivalTime)} at {timeOf(trip.arrivalTime)}</p><p>Vehicle: {trip.vehicleRegistration || 'Pending'}</p><p>Seats available for this segment: <strong>{availability.availableSeats}</strong></p>
      <RouteMapPreview route={details.route} stops={details.stops} height={300}/>
      <Link className="button button-primary" to={`/passenger/book/${trip.id}?${new URLSearchParams({ boardingStopId: board.id, destinationStopId: destination.id, seats: params.get('seats') || '1' })}`}>Continue to booking</Link>
    </section></>
}

export function ApiBookTicket() {
  const { tripId } = useParams()
  const [params] = useSearchParams()
  const { currentUser } = useAppData()
  const [state, retry] = useJourney(tripId, params)
  const [seats, setSeats] = useState(Math.max(1, Math.min(6, Number(params.get('seats')) || 1)))
  const [quote, setQuote] = useState(null)
  const [quoteError, setQuoteError] = useState('')
  const [saving, setSaving] = useState(false)
  const [saved, setSaved] = useState(null)
  const [error, setError] = useState('')
  useEffect(() => {
    if (!state.board || !state.destination) return
    let active = true
    getBookingQuote({ tripId, boardingStopId: state.board.id, destinationStopId: state.destination.id, seatCount: seats })
      .then(q => { if (active) { setQuote(q); setQuoteError('') } })
      .catch(err => { if (active) { setQuote(null); setQuoteError(errorText(err)) } })
    return () => { active = false }
  }, [tripId, state.board, state.destination, seats])
  if (state.loading) return message('Checking availability...')
  if (state.error) return message(state.error, retry)
  const { trip, board, destination, availability, details } = state
  if (saved) return <>{title('Booking created', 'Your reservation was saved in Oracle. No payment has been processed.')}<section className="portal-panel"><h2>Reference: {saved.bookingReference}</h2><p>Status: {saved.status}</p><p>{saved.seatCount} seat(s) · {formatLkr(saved.totalFare)}</p><Link className="button button-primary" to={`/passenger/tickets?booking=${saved.id}`}>View booking details</Link></section></>
  const submit = async e => {
    e.preventDefault()
    if (!currentUser || currentUser.role !== 'PASSENGER') return setError('Please sign in with a passenger account.')
    setSaving(true); setError('')
    try {
      const passenger = await request('/users/me/passenger')
      const booking = await createBooking({ passengerId: passenger.passengerId, tripId: Number(tripId), boardingStopId: board.id, destinationStopId: destination.id, seatCount: seats })
      setSaved(booking)
    } catch (err) { setError(errorText(err)); retry() }
    finally { setSaving(false) }
  }
  return <>{title('Reserve your seats', `${board.stopName} → ${destination.stopName} · ${dateOf(trip.departureTime)}`)}
    <form className="booking-form portal-panel" onSubmit={submit}><h2>Booking summary</h2><RouteMapPreview route={details.route} stops={details.stops} height={260}/>
      <p>Seats available: {availability.availableSeats}</p>
      <label>Number of seats<select value={seats} onChange={e => setSeats(Number(e.target.value))}>{[1,2,3,4,5,6].filter(n => n <= availability.availableSeats).map(n => <option key={n}>{n}</option>)}</select></label>
      {quote && <p>Fare per seat: {formatLkr(quote.farePerSeat)} · Total: <strong>{formatLkr(quote.totalFare)}</strong></p>}
      {quoteError && <p className="auth-error" role="alert">{quoteError}</p>}
      {error && <p className="auth-error" role="alert">{error}</p>}
      <p>No online payment is processed. The backend controls booking status and seat allocation.</p>
      <button className="button button-primary" disabled={saving || !quote || seats > availability.availableSeats || availability.availableSeats < 1}>{saving ? 'Creating booking...' : 'Confirm reservation'}</button>
    </form></>
}

function useBookings() {
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [refresh, setRefresh] = useState(0)
  useEffect(() => {
    let active = true
    listPassengerBookings().then(records => { if (active) { setItems(records); setError('') } })
      .catch(err => { if (active) setError(errorText(err)) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [refresh])
  return { items, loading, error, retry: () => { setLoading(true); setRefresh(n => n + 1) } }
}

export function ApiMyBookings() {
  const { items, loading, error, retry } = useBookings()
  const [cancelling, setCancelling] = useState(null)
  const [actionError, setActionError] = useState('')
  const cancel = async booking => {
    if (!window.confirm(`Cancel booking ${booking.bookingReference}?`)) return
    setCancelling(booking.id); setActionError('')
    try { await cancelBooking(booking.id); retry() } catch (err) { setActionError(errorText(err)) }
    finally { setCancelling(null) }
  }
  return <>{title('My bookings', 'Your Oracle-backed reservation history')}
    {loading ? message('Loading bookings...') : error ? message(error, retry) : items.length === 0 ? message('You have no bookings yet.') :
      <div className="booking-record-list">{items.map(b => <article className="booking-record" key={b.id}>
        <div className="booking-record-main"><span className="booking-reference">{b.bookingReference} · {b.status}</span><strong>{journey(b)}</strong><span>{dateOf(b.departureTime)} · {timeOf(b.departureTime)} · {b.seatCount} seat(s)</span></div>
        <div className="booking-record-side"><strong>{formatLkr(b.totalFare)}</strong><Link to={`/passenger/tickets?booking=${b.id}`}>View ticket</Link>
          {String(b.status).toUpperCase() === 'PENDING' && <button type="button" disabled={cancelling === b.id} onClick={() => cancel(b)}>Cancel booking</button>}
        </div>
      </article>)}</div>}
    {actionError && <p className="auth-error" role="alert">{actionError}</p>}
    <Link className="button button-outline" to="/passenger/search">Find another trip</Link>
  </>
}

export function ApiMyTickets() {
  const { items, loading, error, retry } = useBookings()
  const [params] = useSearchParams()
  const { currentUser } = useAppData()
  const selected = items.filter(b => !['CANCELLED'].includes(String(b.status).toUpperCase()) && (!params.get('booking') || String(b.id) === params.get('booking') || b.bookingReference === params.get('booking')))
  return <>{title('My tickets', 'Booking references and travel details from Oracle')}
    <button className="button button-outline" onClick={() => window.print()}>Print tickets</button>
    {loading ? message('Loading tickets...') : error ? message(error, retry) : selected.length === 0 ? message('No active booking records found.') :
      <div className="ticket-grid">{selected.map(b => <article className="ticket-card" key={b.id}>
        <div className="ticket-top"><strong>SMARTMOVE</strong><span>{b.status}</span></div>
        <div className="ticket-main"><small>BOOKING REFERENCE</small><h2>{b.bookingReference}</h2><h3>{journey(b)}</h3><p>{dateOf(b.departureTime)} · {timeOf(b.departureTime)}</p><p>Route: {b.routeName}</p></div>
        <div className="ticket-bottom"><span><small>PASSENGER</small><b>{b.passengerName || currentUser?.username}</b></span><span><small>SEATS</small><b>{b.seatCount}</b></span><span><small>FARE</small><b>{formatLkr(b.totalFare)}</b></span></div>
        <p className="ticket-driver-note">Reservation status: {b.status}. Payment is managed separately; this is not proof of payment.</p>
      </article>)}</div>}
  </>
}
