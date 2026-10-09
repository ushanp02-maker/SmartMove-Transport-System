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
  const visible = rows.filter(item => filter === 'ALL' || item.status === filter)
  return <section className="portal-panel">
    <div className="portal-panel-head"><div><h1>{config.title}</h1><p>Live Spring Boot requests; approvals and assignments are saved to Oracle.</p></div><button className="button button-outline" onClick={() => setRefresh(n => n + 1)}>Refresh</button></div>
    {error && <p className="auth-error" role="alert">{error}</p>}
    <label>Filter by status <select value={filter} onChange={e => setFilter(e.target.value)}>{['ALL','PENDING','APPROVED','ASSIGNED','IN_PROGRESS','COMPLETED','REJECTED','CANCELLED'].map(status => <option key={status}>{status}</option>)}</select></label>
    <div style={{overflowX:'auto'}}><table className="data-table"><thead><tr><th>ID</th><th>Status</th><th>Request details</th><th>Actions</th></tr></thead><tbody>{visible.map(item => {
      const id = item.id ?? item.requestId
      return <tr key={id}><td>{id}</td><td>{show(item.status)}</td><td>{Object.entries(item).filter(([key]) => !['id','requestId','status'].includes(key)).slice(0,8).map(([key,value]) => <div key={key}><strong>{key}:</strong> {show(value)}</div>)}</td><td><div style={{display:'flex',gap:8,flexWrap:'wrap'}}>{(item.status === 'PENDING' ? ['approve','reject'] : item.status === 'APPROVED' ? ['assign','cancel'] : item.status === 'ASSIGNED' ? ['complete','cancel'] : []).map(action => <button type="button" className="button button-outline" disabled={busy} key={action} onClick={() => act(item,action)}>{action}</button>)}</div></td></tr>
    })}</tbody></table></div>
    {!visible.length && <p>No requests match this filter.</p>}
  </section>
}
