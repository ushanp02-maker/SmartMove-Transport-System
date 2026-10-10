import { useEffect, useState } from 'react'
import EntityPage from '../components/EntityPage'
import ApiAdminEntity from '../components/ApiAdminEntity'
import { isApiMode, request } from '../services/apiClient'

function LivePayments() {
  const [bookings, setBookings] = useState([])
  const [payments, setPayments] = useState([])
  const [version, setVersion] = useState(0)
  const [busyId, setBusyId] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let active = true
    setLoading(true)
    Promise.all([request('/bookings'), request('/payments')])
      .then(([bookingRows, paymentRows]) => {
        if (!active) return
        setBookings(Array.isArray(bookingRows) ? bookingRows : bookingRows?.content || [])
        setPayments(Array.isArray(paymentRows) ? paymentRows : paymentRows?.content || [])
        setError('')
      })
      .catch(err => { if (active) setError(err.message) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [version])

  // Historical pending bookings can predate automatic payment creation.
  // Never create another payment if a pending or completed one already exists.
  const outstanding = bookings.filter(booking =>
    String(booking.status).toUpperCase() === 'PENDING' &&
    !payments.some(payment =>
      String(payment.bookingId) === String(booking.id) &&
      ['PENDING', 'COMPLETED'].includes(String(payment.status).toUpperCase())
    )
  )

  const createPending = async booking => {
    setBusyId(booking.id)
    setError('')
    try {
      await request('/payments', {
        method: 'POST',
        body: JSON.stringify({
          bookingId: booking.id,
          paymentMethod: 'SIMULATED',
          notes: 'Pending payment for existing booking',
        }),
      })
      setVersion(value => value + 1)
    } catch (err) {
      setError(err.message)
    } finally {
      setBusyId(null)
    }
  }

  return <>
    <ApiAdminEntity key={version} entity="payments" />
    <section className="sm-admin-page" style={{ marginTop: 24 }}>
      <div className="sm-page-heading">
        <div>
          <span className="sm-breadcrumb">Payments / Reconciliation</span>
          <h2>Bookings awaiting payment records</h2>
          <p>Older pending bookings that do not yet have an active payment record.</p>
        </div>
        <button type="button" className="button button-outline" onClick={() => setVersion(value => value + 1)}>Refresh</button>
      </div>
      {error && <p className="auth-error" role="alert">{error}</p>}
      {loading ? <p>Checking pending bookings...</p> : outstanding.length === 0
        ? <p>All pending bookings have payment records.</p>
        : <div className="sm-table-card sm-table-scroll">
          <table className="data-table sm-refined-table">
            <thead><tr><th>Booking ID</th><th>Passenger</th><th>Reference</th><th>Fare (LKR)</th><th>Action</th></tr></thead>
            <tbody>{outstanding.map(booking =>
              <tr key={booking.id}>
                <td>{booking.id}</td>
                <td>{booking.passengerName || booking.passengerId}</td>
                <td>{booking.bookingReference}</td>
                <td>{Number(booking.totalFare || 0).toFixed(2)}</td>
                <td><button type="button" className="button button-primary" disabled={busyId !== null} onClick={() => createPending(booking)}>
                  {busyId === booking.id ? 'Creating...' : 'Create pending payment'}
                </button></td>
              </tr>
            )}</tbody>
          </table>
        </div>}
    </section>
  </>
}

export default function Payments() {
  return isApiMode() ? <LivePayments /> : <EntityPage entity="payments" />
}
