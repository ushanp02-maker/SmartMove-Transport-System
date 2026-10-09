import { useEffect, useState } from 'react'
import { request } from '../services/apiClient'

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
  useEffect(() => {
    let active = true
    request(config.path).then(records => { if (active) { setItems(records); setError('') } })
      .catch(err => { if (active) setError(errorText(err)) })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [config.path, version])
  const open = record => {
    setEditing(record || {})
    setActionError('')
    setValues(Object.fromEntries(config.fields.map(([key,,type]) => [key, formatInitial(record?.[key], type)])))
  }
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
  if (!config) return <p>Unknown admin module.</p>
  return <section className="portal-panel">
    <div className="portal-panel-head"><div><h1>{config.title}</h1><p>Records loaded directly from the Spring Boot and Oracle API.</p></div><button type="button" className="button button-primary" onClick={() => open(null)}>Add record</button></div>
    {loading ? <p>Loading records...</p> : error ? <p className="auth-error" role="alert">{error} <button type="button" onClick={() => {setLoading(true);setVersion(n=>n+1)}}>Retry</button></p> : items.length === 0 ? <p>No records in the database.</p> :
      <div style={{ overflowX: 'auto' }}><table className="data-table"><thead><tr><th>ID</th>{config.fields.slice(0,5).map(([key,title]) => <th key={key}>{title}</th>)}<th>Actions</th></tr></thead><tbody>{items.map(record => <tr key={record.id}><td>{record.id}</td>{config.fields.slice(0,5).map(([key]) => <td key={key}>{pretty(record[key])}</td>)}<td>{!config.createOnly && <button className="button button-outline" type="button" onClick={() => open(record)}>Edit</button>}</td></tr>)}</tbody></table></div>}
    {editing && <div className="modal-backdrop"><section className="record-modal" role="dialog" aria-modal="true" aria-label="Manage database record">
      <div className="modal-heading"><h2>{editing.id == null ? 'Create' : 'Update'} {entity}</h2><button className="button button-outline" onClick={() => setEditing(null)}>Close</button></div>
      <form onSubmit={submit}><div className="form-grid">{config.fields.filter(([key]) => editing.id == null || !config.updateFields || config.updateFields.includes(key)).map(([key,title,type]) =>
        <label key={key}>{title}<input type={type || 'text'} value={values[key] ?? ''} onChange={event => setValues(old => ({ ...old, [key]: event.target.value }))} step={type === 'number' ? 'any' : undefined} /></label>
      )}</div>{actionError && <p className="auth-error" role="alert">{actionError}</p>}<button className="button button-primary" disabled={saving}>{saving ? 'Saving...' : 'Save to Oracle'}</button></form>
    </section></div>}
  </section>
}
