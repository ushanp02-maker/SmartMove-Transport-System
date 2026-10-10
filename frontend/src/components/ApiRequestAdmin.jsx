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
  useEffect(() => {
    let live = true
    request(`${config.base}/admin/requests`).then(data => { if (live) { setRows(rowsOf(data)); setError('') } })
      .catch(err => { if (live) setError(err.message) })
    return () => { live = false }
  }, [config.base, refresh])
  const act = async (item, action) => {
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
      const tripId = window.prompt('Existing Oracle trip ID to assign')
      if (!tripId) return
      if (!Number.isSafeInteger(Number(tripId)) || Number(tripId) < 1) return setError('Enter a valid trip ID.')
      body = { tripId: Number(tripId) }
    } else if (action === 'cancel') {
      const reason = window.prompt('Cancellation reason')
      if (!reason?.trim()) return
      body = { reason: reason.trim() }
    }
    setBusy(true); setError('')
    try {
      await request(`${config.base}/admin/requests/${encodeURIComponent(id)}/${action}`, { method: 'PATCH', ...(body ? { body: JSON.stringify(body) } : {}) })
      setRefresh(n => n + 1)
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
    <div className="sm-table-scroll"><table className="data-table sm-refined-table"><thead><tr><th>ID</th><th>{kind==='staff'?'Employee':'Passenger'}</th><th>Pickup location</th><th>Destination</th><th>Requested date</th><th>Status</th><th>Actions</th></tr></thead><tbody>{shown.map(item=>{const id=item.id??item.requestId;return <tr key={id}><td>{id}</td><td>{detail(item,['passengerName','employeeName','staffName','name','passengerId','employeeId'])}</td><td>{detail(item,['pickupAddress','pickupLocation','origin','pickup'])}</td><td>{detail(item,['destinationAddress','destination','dropoffLocation'])}</td><td>{detail(item,['requestedDate','travelDate','tripDate','createdAt'])}</td><td><span className={'sm-status-pill sm-status-'+String(item.status||'').toLowerCase()}>{show(item.status)}</span></td><td><div className="sm-actions">{(item.status==='PENDING'?['approve','reject']:item.status==='APPROVED'?['assign','cancel']:item.status==='ASSIGNED'?['complete','cancel']:[]).map(action=><button type="button" className="button button-outline" disabled={busy} key={action} onClick={()=>act(item,action)}>{action}</button>)}<details className="sm-row-details"><summary>View</summary><div className="sm-detail-popover">{Object.entries(item).filter(([key])=>!['id','requestId'].includes(key)).map(([key,value])=><p key={key}><strong>{key}:</strong> {show(value)}</p>)}</div></details></div></td></tr>})}</tbody></table></div>
    {!visible.length&&<p className="sm-empty">No requests match this filter.</p>}
    <div className="sm-table-footer"><span>Showing {visible.length?(currentPage-1)*pageSize+1:0} to {Math.min(currentPage*pageSize,visible.length)} of {visible.length} requests</span><div className="sm-pagination"><button disabled={currentPage===1} onClick={()=>setPage(n=>Math.max(1,n-1))}>‹</button><span>{currentPage} / {pages}</span><button disabled={currentPage===pages} onClick={()=>setPage(n=>Math.min(pages,n+1))}>›</button></div></div></div>
  </section>
}
