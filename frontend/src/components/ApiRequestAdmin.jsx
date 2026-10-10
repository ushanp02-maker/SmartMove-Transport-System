import { useEffect, useState } from 'react'
import { request } from '../services/apiClient'

const services = {
  staff: { title: 'Staff transport requests', base: '/staff-transport' },
  custom: { title: 'On-demand trip requests', base: '/on-demand' },
}
const rowsOf = data => Array.isArray(data) ? data : (data?.content || [])
const show = value => value == null ? '—' : typeof value === 'object' ? JSON.stringify(value) : String(value)
export default function ApiRequestAdmin({ kind }) {
  const config = services[kind]
  const [rows, setRows] = useState([])
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [refresh, setRefresh] = useState(0)
  const [filter, setFilter] = useState('ALL')
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(1)
  const [trips, setTrips] = useState([])
  const [assignment, setAssignment] = useState(null)
  const [selectedTripId, setSelectedTripId] = useState('')
  const [tripError, setTripError] = useState('')
  useEffect(() => {
    let live = true
    request(`${config.base}/admin/requests`).then(data => { if (live) { setRows(rowsOf(data)); setError('') } })
      .catch(err => { if (live) setError(err.message) })
    return () => { live = false }
  }, [config.base, refresh])
  const openAssignment = async item => {
    setAssignment(item); setSelectedTripId(''); setTripError(''); setBusy(true)
    try {
      const data = await request('/trips')
      setTrips(rowsOf(data))
    } catch (err) { setTripError(err.message) }
    finally { setBusy(false) }
  }
  const assignedIds = new Set(rows.map(item => String(item.assignedTripId ?? item.tripId ?? '')).filter(Boolean))
  const availableTrips = trips.filter(trip => {
    if (String(trip.status || '').toUpperCase() !== 'SCHEDULED' || !trip.departureTime || new Date(trip.departureTime).getTime() <= Date.now() || assignedIds.has(String(trip.id))) return false
    if (!assignment) return true
    const seats = Number(assignment.passengerCount ?? assignment.numberOfPassengers ?? 1)
    if (Number(trip.seatingCapacity) < seats) return false
    if (kind === 'custom') {
      const requested = assignment.requestedPickupTime ?? assignment.pickupTime ?? assignment.travelDate
      if (requested && !Number.isNaN(Date.parse(requested)) && Math.abs(new Date(trip.departureTime).getTime() - new Date(requested).getTime()) > 30 * 60000) return false
    } else {
      const requestedDate = assignment.requestedDate
      const pickupTime = assignment.pickupTime
      if (requestedDate && String(trip.departureTime).slice(0,10) !== String(requestedDate).slice(0,10)) return false
      if (pickupTime && String(trip.departureTime).slice(11,16) !== String(pickupTime).slice(0,5)) return false
      if (assignment.origin && trip.origin && assignment.origin.trim().toLowerCase() !== trip.origin.trim().toLowerCase()) return false
      if (assignment.destination && trip.destination && assignment.destination.trim().toLowerCase() !== trip.destination.trim().toLowerCase()) return false
    }
    return true
  })
  const act = async (item, action, chosenTripId) => {
    const id = item.id ?? item.requestId
    if (id == null) return setError('Request ID missing from API response.')
    let body
    if (action === 'approve') {
      const fare = window.prompt('Approved fare in LKR (leave blank for unchanged)', '')
      if (fare === null) return
      if (fare.trim() && (!Number.isFinite(Number(fare)) || Number(fare) < 0)) return setError('Enter a valid fare.')
      body = { reviewNotes: '', approvedFare: fare.trim() ? Number(fare) : null }
    } else if (action === 'reject') {
      const reason = window.prompt('Reason for rejection')
      if (!reason?.trim()) return
      body = { reviewNotes: reason.trim() }
    } else if (action === 'assign') {
      if (!Number.isSafeInteger(Number(chosenTripId)) || Number(chosenTripId) < 1) return setError('Select an available trip.')
      body = { tripId: Number(chosenTripId) }
    } else if (action === 'cancel') {
      const reason = window.prompt('Cancellation reason')
      if (!reason?.trim()) return
      body = { reason: reason.trim() }
    }
    setBusy(true); setError('')
    try {
      await request(`${config.base}/admin/requests/${encodeURIComponent(id)}/${action}`, { method: 'PATCH', ...(body ? { body: JSON.stringify(body) } : {}) })
      setAssignment(null); setSelectedTripId(''); setRefresh(n => n + 1)
    } catch (err) { setError(err.message) }
    finally { setBusy(false) }
  }
  const visible = rows.filter(item => (filter === 'ALL' || item.status === filter) && JSON.stringify(item).toLowerCase().includes(search.toLowerCase()))
  const pageSize = 7, pages = Math.max(1,Math.ceil(visible.length/pageSize)), currentPage = Math.min(page,pages)
  const shown = visible.slice((currentPage-1)*pageSize,currentPage*pageSize)
  const count = status => rows.filter(item => item.status === status).length
  const summary = [['Total requests',rows.length],['Pending approval',count('PENDING')],['Approved',count('APPROVED')],['Rejected',count('REJECTED')]]
  const detail = (item,keys) => keys.map(key => item[key]).find(value=>value!=null&&value!=='') || '—'
  return <section className="sm-admin-page">
    <div className="sm-page-heading"><div><span className="sm-breadcrumb">Dashboard / {config.title}</span><h1>{config.title}</h1><p>{kind === 'staff' ? 'Manage employee transport requests and approvals.' : 'Manage passenger on-demand trip requests and assignments.'}</p></div><button className="button button-outline" onClick={()=>setRefresh(n=>n+1)}>Refresh</button></div>
    <div className="sm-summary-grid">{summary.map(([label,value],i)=><div className="sm-summary-card" key={label}><span className={'sm-summary-icon sm-summary-icon-'+i}>{['▦','◉','✓','◇'][i]}</span><div><strong>{value}</strong><small>{label}</small></div></div>)}</div>
    <div className="sm-table-card"><div className="sm-table-toolbar"><div className="sm-search-wrap"><span>⌕</span><input aria-label="Search requests" placeholder="Search requests..." value={search} onChange={e=>{setSearch(e.target.value);setPage(1)}}/></div><div className="sm-toolbar-actions"><select aria-label="Filter requests" value={filter} onChange={e=>{setFilter(e.target.value);setPage(1)}}>{['ALL','PENDING','APPROVED','ASSIGNED','IN_PROGRESS','COMPLETED','REJECTED','CANCELLED'].map(status=><option key={status}>{status}</option>)}</select></div></div>
    {error&&<p className="auth-error" role="alert">{error}</p>}
    <div className="sm-table-scroll"><table className="data-table sm-refined-table"><thead><tr><th>ID</th><th>{kind==='staff'?'Employee':'Passenger'}</th><th>Pickup location</th><th>Destination</th><th>Requested date</th><th>Status</th><th>Actions</th></tr></thead><tbody>{shown.map(item=>{const id=item.id??item.requestId;return <tr key={id}><td>{id}</td><td>{detail(item,['passengerName','employeeName','staffName','name','passengerId','employeeId'])}</td><td>{detail(item,['pickupAddress','pickupLocation','origin','pickup'])}</td><td>{detail(item,['destinationAddress','destination','dropoffLocation'])}</td><td>{detail(item,['requestedDate','travelDate','tripDate','createdAt'])}</td><td><span className={'sm-status-pill sm-status-'+String(item.status||'').toLowerCase()}>{show(item.status)}</span></td><td><div className="sm-actions">{(item.status==='PENDING'?['approve','reject']:item.status==='APPROVED'?['assign','cancel']:item.status==='ASSIGNED'?['complete','cancel']:[]).map(action=><button type="button" className="button button-outline" disabled={busy} key={action} onClick={()=>action === 'assign' ? openAssignment(item) : act(item,action)}>{action}</button>)}<details className="sm-row-details"><summary>View</summary><div className="sm-detail-popover">{Object.entries(item).filter(([key])=>!['id','requestId'].includes(key)).map(([key,value])=><p key={key}><strong>{key}:</strong> {show(value)}</p>)}</div></details></div></td></tr>})}</tbody></table></div>
    {assignment && <div className="modal-backdrop"><section className="record-modal sm-refined-modal" role="dialog" aria-modal="true" aria-label="Assign scheduled trip"><div className="modal-heading"><div><span className="sm-breadcrumb">Transport requests / Assignment</span><h2>Assign a scheduled trip</h2></div><button type="button" className="button button-outline" onClick={()=>setAssignment(null)}>Close</button></div><p className="sm-modal-intro">Choose an eligible scheduled trip for request #{assignment.id ?? assignment.requestId}. Trips must meet the request's time, capacity and route requirements.</p>{tripError && <p className="auth-error" role="alert">{tripError}</p>}<label>Available scheduled trips<select value={selectedTripId} onChange={e=>setSelectedTripId(e.target.value)}><option value="">Select an eligible trip</option>{availableTrips.map(trip=><option key={trip.id} value={trip.id}>#{trip.id} — {trip.routeName || trip.origin + ' → ' + trip.destination} — {String(trip.departureTime).replace('T',' ')} — {trip.vehicleRegistration || 'Vehicle #'+trip.vehicleId} — {trip.driverName || 'Driver #'+trip.driverId}</option>)}</select></label>{!busy && !availableTrips.length && <p className="sm-empty">No eligible scheduled trips found. Create a trip with the matching route, date, time, and enough seats in Trip scheduling first.</p>}<div className="sm-form-footer"><button type="button" className="button button-outline" onClick={()=>setAssignment(null)}>Cancel</button><button type="button" className="button button-primary" disabled={busy || !selectedTripId} onClick={()=>act(assignment,'assign',selectedTripId)}>{busy ? 'Assigning...' : 'Assign selected trip'}</button></div></section></div>}
    {!visible.length&&<p className="sm-empty">No requests match this filter.</p>}
    <div className="sm-table-footer"><span>Showing {visible.length?(currentPage-1)*pageSize+1:0} to {Math.min(currentPage*pageSize,visible.length)} of {visible.length} requests</span><div className="sm-pagination"><button disabled={currentPage===1} onClick={()=>setPage(n=>Math.max(1,n-1))}>‹</button><span>{currentPage} / {pages}</span><button disabled={currentPage===pages} onClick={()=>setPage(n=>Math.min(pages,n+1))}>›</button></div></div></div>
  </section>
}
