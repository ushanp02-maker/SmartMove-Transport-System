import { useEffect, useState } from 'react'
import { request } from '../services/apiClient'
import RouteMapPreview from './RouteMapPreview'

const empty = { stopName: '', stopOrder: '', latitude: '', longitude: '', distanceFromStartKm: '', minutesFromStart: '' }
const numberOrNull = value => value === '' || value == null ? null : Number(value)

/** Manage real ordered route stops persisted in Oracle, not just route labels. */
export default function RouteStopsManager({ route, onClose }) {
  const [stops, setStops] = useState([])
  const [routeInfo, setRouteInfo] = useState(route)
  const [form, setForm] = useState(empty)
  const [selected, setSelected] = useState(null)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [loading, setLoading] = useState(true)
  const load = async () => {
    setLoading(true)
    try {
      const details = await request('/routes/' + route.id + '/details')
      setStops(details.stops || [])
      setRouteInfo(details.route || route)
      setError('')
    } catch (e) { setError(e.message) }
    finally { setLoading(false) }
  }
  useEffect(() => { load() }, [route.id])
  const edit = stop => {
    setSelected(stop?.id ?? null)
    setForm(stop ? Object.fromEntries(Object.keys(empty).map(key => [key, stop[key] ?? ''])) : { ...empty, stopOrder: stops.length ? Math.max(...stops.map(s => Number(s.stopOrder))) + 1 : 0 })
    setError('')
  }
  const save = async event => {
    event.preventDefault()
    setBusy(true); setError('')
    try {
      const body = Object.fromEntries(Object.keys(empty).map(key => [key, key === 'stopName' ? form[key].trim() : numberOrNull(form[key])]))
      if (!body.stopName) throw new Error('Enter a stop name.')
      if (body.stopOrder == null || body.stopOrder < 0 || !Number.isInteger(body.stopOrder)) throw new Error('Stop order must be a non-negative whole number.')
      if (stops.some(s => s.id !== selected && Number(s.stopOrder) === body.stopOrder)) throw new Error('That stop order is already used. Choose another order.')
      await request('/routes/' + route.id + '/stops' + (selected != null ? '/' + selected : ''), { method: selected != null ? 'PUT' : 'POST', body: JSON.stringify(body) })
      setSelected(null); setForm(empty); await load()
    } catch (e) { setError(e.message) }
    finally { setBusy(false) }
  }
  const remove = async stop => {
    if (!window.confirm('Delete stop "' + stop.stopName + '"? Existing bookings may prevent removal.')) return
    setBusy(true); setError('')
    try { await request('/routes/' + route.id + '/stops/' + stop.id, { method: 'DELETE' }); await load() }
    catch (e) { setError(e.message + ' Deactivate the route first if it is active.') }
    finally { setBusy(false) }
  }
  return <div className="modal-backdrop"><section className="record-modal sm-refined-modal" role="dialog" aria-modal="true" aria-label="Manage route stops" style={{maxWidth:1000,maxHeight:'90vh',overflowY:'auto'}}>
    <div className="modal-heading"><div><span className="sm-breadcrumb">Routes / Stops</span><h2>Build route: {routeInfo.name}</h2><p>Add every place where passengers can board or leave, in travel order.</p></div><button type="button" className="button button-outline" onClick={onClose}>Close</button></div>
    <RouteMapPreview route={routeInfo} stops={stops} height={280}/>
    <h3>Ordered stops</h3>
    {loading ? <p>Loading stops...</p> : <div className="sm-table-scroll"><table className="data-table sm-refined-table"><thead><tr><th>Order</th><th>Stop</th><th>Km from start</th><th>Minutes from start</th><th>Actions</th></tr></thead><tbody>{[...stops].sort((a,b)=>a.stopOrder-b.stopOrder).map(stop=><tr key={stop.id}><td>{stop.stopOrder}</td><td>{stop.stopName}</td><td>{stop.distanceFromStartKm ?? '—'}</td><td>{stop.minutesFromStart ?? '—'}</td><td className="sm-actions"><button type="button" className="button button-outline" onClick={()=>edit(stop)}>Edit</button><button type="button" className="button button-outline" disabled={busy} onClick={()=>remove(stop)}>Delete</button></td></tr>)}</tbody></table></div>}
    <h3>{selected == null ? 'Add a stop' : 'Edit stop'}</h3>
    <form onSubmit={save}><div className="form-grid">
      {Object.entries({stopName:'Stop name',stopOrder:'Order along route',latitude:'Latitude (optional)',longitude:'Longitude (optional)',distanceFromStartKm:'Km from starting point',minutesFromStart:'Minutes from starting point'}).map(([key,label])=><label key={key}>{label}<input required={key==='stopName'||key==='stopOrder'} type={key==='stopName'?'text':'number'} step={key==='stopOrder'||key==='minutesFromStart'?'1':'any'} value={form[key]} onChange={e=>setForm(old=>({...old,[key]:e.target.value}))}/></label>)}
    </div>{error && <p className="auth-error" role="alert">{error}</p>}<div className="sm-form-footer"><button type="button" className="button button-outline" onClick={()=>edit(null)}>Clear</button><button className="button button-primary" disabled={busy}>{busy?'Saving...':selected==null?'Add stop':'Save stop'}</button></div></form>
    <p className="sm-modal-intro">Stops must be in increasing travel order. Existing route endpoints are included automatically. To insert a stop, assign it an unused order and adjust other orders as needed. Routes with historical bookings may restrict stop deletion.</p>
  </section></div>
}
