import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { request } from '../../services/apiClient'
import { listPassengerBookings } from '../../services/bookingService'
import { formatLkr } from '../../services/formatters'

export function ApiPassengerProfile() {
  const [profile, setProfile] = useState(null)
  const [error, setError] = useState('')
  useEffect(() => {
    let active = true
    const load = async () => {
      const me = await request('/users/me/passenger')
      const data = await request('/passengers/' + me.passengerId)
      if (active) setProfile(data)
    }
    load().catch(e => { if (active) setError(e.message) })
    return () => { active = false }
  }, [])
  return <section className="portal-panel"><h1>My passenger profile</h1>{error && <p role="alert">{error}</p>}{profile && Object.entries(profile).map(([key, value]) => <p key={key}><strong>{key}</strong>: {String(value ?? '—')}</p>)}</section>
}

export function ApiPassengerPayments() {
  const [rows, setRows] = useState([])
  const [bookings, setBookings] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [filter, setFilter] = useState('ALL')
  const [refresh, setRefresh] = useState(0)
  useEffect(() => {
    let active = true
    setLoading(true)
    const load = async () => {
      const me = await request('/users/me/passenger')
      const [paymentsResult, bookingsResult] = await Promise.allSettled([
        request('/payments/passenger/' + me.passengerId),
        listPassengerBookings()
      ])
      if (!active) return
      if (paymentsResult.status === 'fulfilled') {
        const data = paymentsResult.value
        setRows(Array.isArray(data) ? data : data?.content || [])
      } else setError(paymentsResult.reason?.message || 'Unable to load payment records.')
      if (bookingsResult.status === 'fulfilled') setBookings(bookingsResult.value)
      else setError(prev => prev || bookingsResult.reason?.message || 'Unable to load bookings.')
    }
    load().finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [refresh])
  const visible = rows.filter(row => filter === 'ALL' || String(row.status).toUpperCase() === filter)
  const pendingBookings = bookings.filter(b => !['CANCELLED', 'REFUNDED'].includes(String(b.status).toUpperCase()) && !rows.some(p => String(p.bookingId ?? p.booking?.id) === String(b.id)))
  return <div className="portal-payments-page">
    <div className="portal-page-heading"><div><span>SMARTMOVE PASSENGER</span><h1>Payment records</h1><p>Track your trip fares and payments recorded by SmartMove.</p></div></div>
    <div className="portal-payment-summary">
      <div><small>Recorded payments</small><strong>{rows.length}</strong></div>
      <div><small>Completed payments</small><strong>{rows.filter(p => ['COMPLETED', 'PAID', 'SUCCESS'].includes(String(p.status).toUpperCase())).length}</strong></div>
      <div><small>Bookings awaiting a payment record</small><strong>{pendingBookings.length}</strong></div>
    </div>
    <section className="portal-panel">
      <div className="portal-payment-toolbar"><div><h2>Payment history</h2><p>Payments are recorded administratively. No online payment gateway is connected.</p></div><button className="button button-outline" onClick={() => { setError(''); setRefresh(n => n + 1) }}>Refresh</button></div>
      <div className="portal-payment-filters">{['ALL', 'PENDING', 'COMPLETED', 'REFUNDED', 'FAILED'].map(status => <button type="button" key={status} className={filter === status ? 'selected' : ''} onClick={() => setFilter(status)}>{status === 'ALL' ? 'All payments' : status}</button>)}</div>
      {error && <p className="auth-error" role="alert">{error}</p>}
      {loading ? <p>Loading your payment history...</p> : <div className="portal-payment-table-wrap"><table className="portal-payment-table"><thead><tr><th>Payment reference</th><th>Booking</th><th>Amount</th><th>Method</th><th>Status</th></tr></thead><tbody>{visible.map((item, i) => <tr key={item.id ?? i}><td><strong>{item.paymentReference || 'Payment #' + item.id}</strong></td><td>{item.bookingReference || item.booking?.bookingReference || item.bookingId || '—'}</td><td>{formatLkr(item.amount ?? item.totalAmount ?? 0)}</td><td>{item.paymentMethod || '—'}</td><td><span className={'portal-payment-status ' + String(item.status || '').toLowerCase()}>{item.status || 'UNKNOWN'}</span></td></tr>)}{!visible.length && <tr><td colSpan="5">No {filter === 'ALL' ? '' : filter.toLowerCase()} payment records yet.</td></tr>}</tbody></table></div>}
    </section>
    {!loading && pendingBookings.length > 0 && <section className="portal-panel"><h2>Bookings without payment records</h2><p>These are real booking records, not payments. An administrator must record the payment separately.</p><div className="portal-payment-table-wrap"><table className="portal-payment-table"><thead><tr><th>Booking reference</th><th>Journey</th><th>Fare</th><th>Booking status</th><th></th></tr></thead><tbody>{pendingBookings.map(b => <tr key={b.id}><td><strong>{b.bookingReference}</strong></td><td>{b.boardingStop || 'Pickup'} → {b.destinationStop || 'Destination'}</td><td>{formatLkr(b.totalFare)}</td><td><span className="portal-payment-status pending">NO PAYMENT RECORD</span></td><td><Link to={`/passenger/tickets?booking=${b.id}`}>View ticket</Link></td></tr>)}</tbody></table></div></section>}
  </div>
}
