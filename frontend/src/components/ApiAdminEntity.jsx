import { useEffect, useState } from 'react'
import { request } from '../services/apiClient'
import RouteMapPreview from './RouteMapPreview'

const definitions = {
  vehicles: { title: 'Fleet management', path: '/vehicles', fields: [['registrationNumber','Registration'],['name','Vehicle name'],['vehicleType','Vehicle type'],['seatingCapacity','Seats','number'],['currentMileage','Mileage','number'],['nextServiceDate','Next service','date'],['manufactureYear','Manufacture year','number']] },
  routes: { title: 'Route management', path: '/routes', fields: [['name','Route name'],['origin','Origin'],['destination','Destination'],['distanceKm','Distance (km)','number'],['durationMinutes','Duration (minutes)','number'],['baseFare','Base fare (LKR)','number'],['serviceType','Service type']] },
  trips: { title: 'Trip scheduling', path: '/trips', fields: [['routeId','Route ID','number'],['vehicleId','Vehicle ID','number'],['driverId','Driver ID','number'],['departureTime','Departure','datetime-local'],['arrivalTime','Arrival','datetime-local'],['fare','Fare (LKR)','number']] },
  maintenance: { title: 'Vehicle maintenance', path: '/maintenance', fields: [['vehicleId','Vehicle ID','number'],['maintenanceType','Maintenance type'],['description','Description'],['priority','Priority'],['scheduledDate','Scheduled date','date'],['serviceProvider','Service provider'],['estimatedCost','Estimated cost','number'],['odometerReading','Odometer','number'],['notes','Notes'],['reportedByUserId','Reporter user ID','number']], updateFields: ['maintenanceType','description','priority','scheduledDate','serviceProvider','estimatedCost','odometerReading','notes'] },
  payments: { title: 'Payment records', path: '/payments', fields: [['bookingId','Booking ID','number'],['paymentMethod','Payment method'],['notes','Notes']], createOnly: true },
}
const label = value => String(value ?? '—')
const pretty = value => typeof value === 'object' && value !== null ? JSON.stringify(value) : label(value)
const fieldValue = (value, type) => value === '' ? null : type === 'number' ? Number(value) : value
const formatInitial = (value, type) => type === 'datetime-local' && value ? String(value).slice(0,16) : (value ?? '')
const errorText = err => err?.message || 'Request failed.'
export default function ApiAdminEntity({ entity }) {
  const config = definitions[entity]
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [actionError, setActionError] = useState('')
  const [editing, setEditing] = useState(null)
  const [values, setValues] = useState({})
  const [saving, setSaving] = useState(false)
  const [version, setVersion] = useState(0)
  const [search, setSearch] = useState('')
  const [previewRoute, setPreviewRoute] = useState(null)
  const [previewStops, setPreviewStops] = useState([])
  const [statusFilter, setStatusFilter] = useState('ALL')
  const [page, setPage] = useState(1)
  const [options, setOptions] = useState({ routes: [], vehicles: [], drivers: [] })
  useEffect(() => { if (entity !== 'trips') return; let alive = true; Promise.allSettled(['/routes','/vehicles','/drivers'].map(path => request(path))).then(results => { if (alive) setOptions({ routes: results[0].status === 'fulfilled' ? results[0].value : [], vehicles: results[1].status === 'fulfilled' ? results[1].value : [], drivers: results[2].status === 'fulfilled' ? results[2].value : [] }) }); return () => { alive = false } }, [entity])
  useEffect(() => {
    let active = true
    request(config.path).then(records => { if (active) { setItems(records); setError('') } })
      .catch(err => { if (active) setError(errorText(err)) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [config.path, version])
  const showRoute = async record => { setPreviewRoute(record); setPreviewStops([]); try { const data = await request(`/routes/${encodeURIComponent(record.id)}/details`); setPreviewRoute(data.route || record); setPreviewStops(data.stops || []) } catch { /* Fall back to route landmarks. */ } }
  const open = record => {
    setEditing(record || {})
    setActionError('')
    setValues(Object.fromEntries(config.fields.map(([key,,type]) => [key, formatInitial(record?.[key], type)])))
  }
  const selectedRoute = entity === 'trips' ? (Array.isArray(options.routes) ? options.routes : []).find(x => String(x.id) === String(values.routeId)) : null
  const submit = async event => {
    event.preventDefault()
    setSaving(true); setActionError('')
    try {
      const updating = editing.id != null
      const fields = config.fields.filter(([key]) => !updating || !config.updateFields || config.updateFields.includes(key))
      const payload = Object.fromEntries(fields.map(([key,,type]) => [key, fieldValue(values[key],type)]))
      await request(updating ? `${config.path}/${encodeURIComponent(editing.id)}` : config.path, {
        method: updating ? 'PUT' : 'POST', body: JSON.stringify(payload),
      })
      setEditing(null)
      setLoading(true)
      setVersion(n => n + 1)
    } catch (err) { setActionError(errorText(err)) }
    finally { setSaving(false) }
  }
  const paymentAction = async (record, action) => {
    const id = record.id
    let body
    if (action === 'fail') { const reason = window.prompt('Reason for payment failure'); if (reason === null) return; body = { reason } }
    if (action === 'refund') { const amount = window.prompt('Refund amount in LKR'); if (!amount) return; if (!(Number(amount) > 0)) return setActionError('Enter a positive refund amount.'); const reason = window.prompt('Refund reason') ; if (reason === null) return; body = { amount: Number(amount), reason } }
    if (action === 'complete' && !window.confirm('Mark this payment as completed? No payment gateway is involved.')) return
    setSaving(true); setActionError('')
    try { await request(`/payments/${encodeURIComponent(id)}/${action}`, { method: 'PATCH', ...(body ? { body: JSON.stringify(body) } : {}) }); setLoading(true); setVersion(n => n + 1) }
    catch (err) { setActionError(errorText(err)) }
    finally { setSaving(false) }
  }
  const list = Array.isArray(items) ? items : items?.content || []
  const filtered = list.filter(item => {
    const text = JSON.stringify(item).toLowerCase()
    const status = String(item.status || '').toUpperCase()
    return text.includes(search.toLowerCase()) && (statusFilter === 'ALL' || status === statusFilter)
  })
  const statuses = [...new Set(list.map(item => String(item.status || '').toUpperCase()).filter(Boolean))]
  const pageSize = 7
  const pages = Math.max(1, Math.ceil(filtered.length / pageSize))
  const currentPage = Math.min(page, pages)
  const shown = filtered.slice((currentPage - 1) * pageSize, currentPage * pageSize)
  const titles = { routes: 'Routes', trips: 'Trip scheduling', vehicles: 'Vehicles', maintenance: 'Vehicle maintenance', payments: 'Payments' }
  const summary = entity === 'routes' ? [
    ['Total routes', list.length], ['Commuter routes', list.filter(x => /COMMUTER/i.test(x.serviceType || '')).length], ['Staff routes', list.filter(x => /STAFF/i.test(x.serviceType || '')).length], ['Active routes', list.filter(x => !/INACTIVE/i.test(x.status || '')).length]
  ] : entity === 'trips' ? [
    ['Total trips', list.length], ['Scheduled', list.filter(x => /SCHEDULED/i.test(x.status || '')).length], ['Ongoing', list.filter(x => /ONGOING|IN_PROGRESS/i.test(x.status || '')).length], ['Completed', list.filter(x => /COMPLETED/i.test(x.status || '')).length]
  ] : entity === 'vehicles' ? [
    ['Total vehicles', list.length], ['Total seats', list.reduce((n,x)=>n+(Number(x.seatingCapacity)||0),0)], ['Available', list.filter(x=>/AVAILABLE/i.test(x.status||'')).length], ['In service', list.filter(x=>/MAINTENANCE|SERVICE/i.test(x.status||'')).length]
  ] : [['Total records',list.length],['Visible records',filtered.length],['Statuses',statuses.length],['Current page',currentPage]]
  const displayField = (record, key) => {
    if (entity === 'trips' && ['routeId','vehicleId','driverId'].includes(key)) {
      const collection = key === 'routeId' ? options.routes : key === 'vehicleId' ? options.vehicles : options.drivers
      const item = (Array.isArray(collection) ? collection : []).find(x => String(x.id) === String(record[key]))
      return item ? (item.name || item.fullName || item.registrationNumber || item.username || String(record[key])) + ' (#' + record[key] + ')' : pretty(record[key])
    }
    return pretty(record[key])
  }
  const renderInput = ([key,title,type]) => {
    const choices = entity === 'trips' && (key === 'routeId' ? options.routes : key === 'vehicleId' ? options.vehicles : key === 'driverId' ? options.drivers : null)
    return <label key={key}>{title}{choices ? <select value={values[key] ?? ''} onChange={event => setValues(old => ({ ...old, [key]: event.target.value }))}><option value="">Select {title.toLowerCase()}</option>{(Array.isArray(choices) ? choices : []).map(item => <option key={item.id} value={item.id}>{item.name || item.fullName || item.registrationNumber || item.username || title} — ID {item.id}</option>)}</select> : <input type={type || 'text'} value={values[key] ?? ''} onChange={event => setValues(old => ({ ...old, [key]: event.target.value }))} step={type === 'number' ? 'any' : undefined} />}</label>
  }
  if (!config) return <p>Unknown admin module.</p>
  return <section className="sm-admin-page">
    <div className="sm-page-heading"><div><span className="sm-breadcrumb">Dashboard / {titles[entity]}</span><h1>{titles[entity]}</h1><p>{entity === 'routes' ? 'Manage commuter and staff routes with stops and details.' : entity === 'trips' ? 'Manage scheduled trips, drivers and vehicles.' : 'Manage records and operational details.'}</p></div><button type="button" className="button button-primary" onClick={() => open(null)}>+ {entity === 'routes' ? 'Add Route' : entity === 'trips' ? 'Add Trip' : entity === 'vehicles' ? 'Add Vehicle' : 'Add Record'}</button></div>
    <div className="sm-summary-grid">{summary.map(([name,value],i) => <div className="sm-summary-card" key={name}><span className={'sm-summary-icon sm-summary-icon-'+i}>{['▦','◉','◇','✓'][i]}</span><div><strong>{value}</strong><small>{name}</small></div></div>)}</div>
    <div className="sm-table-card"><div className="sm-table-toolbar"><div className="sm-search-wrap"><span>⌕</span><input aria-label="Search records" placeholder={'Search '+titles[entity].toLowerCase()+'...'} value={search} onChange={e=>{setSearch(e.target.value);setPage(1)}} /></div><div className="sm-toolbar-actions">{statuses.length > 0 && <select aria-label="Filter status" value={statusFilter} onChange={e=>{setStatusFilter(e.target.value);setPage(1)}}><option value="ALL">All statuses</option>{statuses.map(x=><option key={x} value={x}>{x}</option>)}</select>}<button type="button" className="button button-outline" onClick={()=>{setLoading(true);setVersion(n=>n+1)}}>Refresh</button></div></div>
    {actionError && !editing && <p className="auth-error" role="alert">{actionError}</p>}
    {loading ? <p className="sm-empty">Loading records...</p> : error ? <p className="auth-error" role="alert">{error} <button type="button" onClick={() => {setLoading(true);setVersion(n=>n+1)}}>Retry</button></p> : filtered.length === 0 ? <p className="sm-empty">{list.length ? 'No matching records.' : 'No records in the database yet.'}</p> :
    <div className="sm-table-scroll"><table className="data-table sm-refined-table"><thead><tr><th>ID</th>{config.fields.slice(0,5).map(([key,title]) => <th key={key}>{title}</th>)}<th>Actions</th></tr></thead><tbody>{shown.map(record => <tr key={record.id}><td>{record.id}</td>{config.fields.slice(0,5).map(([key]) => <td key={key}>{displayField(record,key)}</td>)}<td className="sm-actions">{entity === 'routes' && <button className="button button-outline" type="button" onClick={() => showRoute(record)}>Map</button>}{entity === 'trips' && <button className="button button-outline" type="button" onClick={() => { const route = (Array.isArray(options.routes) ? options.routes : []).find(x => String(x.id) === String(record.routeId)); if (route) showRoute(route) }}>Map</button>}{!config.createOnly && <button className="button button-outline" type="button" onClick={() => open(record)}>Edit</button>}{entity === 'payments' && ['complete','fail','refund'].map(action => <button key={action} className="button button-outline" type="button" disabled={saving} onClick={() => paymentAction(record, action)}>{action}</button>)}</td></tr>)}</tbody></table></div>}
    <div className="sm-table-footer"><span>Showing {filtered.length ? (currentPage-1)*pageSize+1 : 0} to {Math.min(currentPage*pageSize,filtered.length)} of {filtered.length} records</span><div className="sm-pagination"><button type="button" disabled={currentPage===1} onClick={()=>setPage(n=>Math.max(1,n-1))}>‹</button><span>{currentPage} / {pages}</span><button type="button" disabled={currentPage===pages} onClick={()=>setPage(n=>Math.min(pages,n+1))}>›</button></div></div></div>
    {previewRoute && <div className="modal-backdrop"><section className="record-modal sm-refined-modal" role="dialog" aria-modal="true" aria-label="Route map preview"><div className="modal-heading"><h2>Route map</h2><button type="button" className="button button-outline" onClick={()=>setPreviewRoute(null)}>Close</button></div><RouteMapPreview route={previewRoute} stops={previewStops}/></section></div>}
    {editing && <div className="modal-backdrop"><section className="record-modal sm-refined-modal" role="dialog" aria-modal="true" aria-label="Manage database record"><div className="modal-heading"><div><span className="sm-breadcrumb">{titles[entity]} / {editing.id == null ? 'Create' : 'Edit'}</span><h2>{editing.id == null ? 'Create' : 'Update'} {entity === 'trips' ? 'Trip' : entity === 'routes' ? 'Route' : entity === 'vehicles' ? 'Vehicle' : entity}</h2></div><button className="button button-outline" onClick={() => setEditing(null)}>Close</button></div><p className="sm-modal-intro">Complete the details below to {editing.id == null ? 'create a new record' : 'update this record'}.</p><form onSubmit={submit}><div className="form-grid">{config.fields.filter(([key]) => editing.id == null || !config.updateFields || config.updateFields.includes(key)).map(renderInput)}</div>{entity === 'trips' && <RouteMapPreview route={selectedRoute} height={240}/ >}{entity === 'routes' && <RouteMapPreview route={{name:values.name,origin:values.origin,destination:values.destination}} height={240}/ >}{actionError && <p className="auth-error" role="alert">{actionError}</p>}<div className="sm-form-footer"><button type="button" className="button button-outline" onClick={()=>setEditing(null)}>Cancel</button><button className="button button-primary" disabled={saving}>{saving ? 'Saving...' : 'Save to Oracle'}</button></div></form></section></div>}
  </section>
}
