import { useState } from 'react'
import { Plus, Download, X, AlertTriangle } from 'lucide-react'
import { useAppData } from '../services/useAppData'
import { entityConfig } from '../data/mockData'
import DataTable from './DataTable'
import { formatLkr } from '../services/formatters'

const dependents = {
  vehicles: [['trips', 'vehicleId'], ['maintenance', 'vehicleId']], drivers: [['trips', 'driverId']], routes: [['trips', 'routeId']],
  trips: [['bookings', 'tripId'], ['reviews', 'tripId']], passengers: [['bookings', 'passengerId'], ['reviews', 'passengerId']], bookings: [['payments', 'bookingId']],
}
const optionsFor = (field, collections) => (collections[field.relation] || []).map(record => ({ value: record.id, label: field.relation === 'routes' ? record.name : `${record.id} · ${record.name || record.title || ''}` }))

function RecordModal({ config, collections, initial, onClose, onSave }) {
  const [values, setValues] = useState(() => Object.fromEntries(config.fields.map(field => [field.key, initial?.[field.key] ?? ''])))
  const [error, setError] = useState('')
  const submit = event => {
    event.preventDefault()
    for (const field of config.fields) {
      if (field.required && !values[field.key]) return setError(`${field.label} is required.`)
      if (field.type === 'email' && values[field.key] && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(values[field.key])) return setError('Enter a valid email address.')
      if (field.type === 'number' && values[field.key] && Number(values[field.key]) < 0) return setError(`${field.label} cannot be negative.`)
      if (field.relation && values[field.key] && !(collections[field.relation] || []).some(item => item.id === values[field.key])) return setError(`Select a valid ${field.label.toLowerCase()}.`)
    }
    onSave(Object.fromEntries(Object.entries(values).map(([key, value]) => {
      const field = config.fields.find(item => item.key === key)
      return [key, field?.type === 'number' && value !== '' ? Number(value) : value]
    })))
  }
  return <div className="modal-backdrop" onMouseDown={event => event.target === event.currentTarget && onClose()}><section className="record-modal" role="dialog" aria-modal="true" aria-labelledby="record-modal-title"><div className="modal-heading"><div><span className="eyebrow">{initial ? 'UPDATE RECORD' : 'NEW RECORD'}</span><h2 id="record-modal-title">{initial ? `Edit ${config.singular}` : `Add ${config.singular}`}</h2></div><button className="icon-button" onClick={onClose} aria-label="Close dialog"><X size={19} /></button></div><form onSubmit={submit}><div className="form-grid">{config.fields.map(field => <label key={field.key} className={field.key === 'message' || field.key === 'notes' || field.key === 'comment' ? 'form-span' : ''}>{field.label}{field.options ? <select value={values[field.key]} required={field.required} onChange={event => setValues({ ...values, [field.key]: event.target.value })}><option value="">Choose {field.label.toLowerCase()}</option>{field.options.map(option => <option key={option}>{option}</option>)}</select> : field.relation ? <select value={values[field.key]} required={field.required} onChange={event => setValues({ ...values, [field.key]: event.target.value })}><option value="">Choose {field.label.toLowerCase()}</option>{optionsFor(field, collections).map(option => <option key={option.value} value={option.value}>{option.label}</option>)}</select> : field.key === 'message' || field.key === 'notes' || field.key === 'comment' ? <textarea rows="3" required={field.required} value={values[field.key]} onChange={event => setValues({ ...values, [field.key]: event.target.value })} /> : <input type={field.type || 'text'} min={field.type === 'number' ? '0' : undefined} step={field.key === 'rating' ? '0.1' : undefined} required={field.required} value={values[field.key]} onChange={event => setValues({ ...values, [field.key]: event.target.value })} />}</label>)}</div>{error && <p className="form-error"><AlertTriangle size={15} />{error}</p>}<div className="modal-actions"><button type="button" className="button button-quiet" onClick={onClose}>Cancel</button><button className="button button-primary" type="submit">{initial ? 'Save changes' : `Create ${config.singular.toLowerCase()}`}</button></div></form></section></div>
}

function linkedCount(collections, key, recordId) {
  return (dependents[key] || []).reduce((total, [collection, field]) => total + collections[collection].filter(item => item[field] === recordId).length, 0)
}

export default function EntityPage({ entity }) {
  const config = entityConfig[entity]
  const { data, addRecord, updateRecord, deleteRecord } = useAppData()
  const [modalRecord, setModalRecord] = useState(null)
  const [modalOpen, setModalOpen] = useState(false)
  const [notice, setNotice] = useState('')
  const rows = data[entity] || []
  const openAdd = () => { setModalRecord(null); setModalOpen(true) }
  const openEdit = record => { setModalRecord(record); setModalOpen(true) }
  const remove = record => {
    const count = linkedCount(data, entity, record.id)
    if (count) { setNotice(`This ${config.singular.toLowerCase()} is linked to ${count} record${count === 1 ? '' : 's'}. Remove or reassign those records first.`); return }
    if (window.confirm(`Delete ${record.id}? This action cannot be undone.`)) { deleteRecord(entity, record.id); setNotice('Record deleted.') }
  }
  const save = values => { if (modalRecord) updateRecord(entity, modalRecord.id, values); else addRecord(entity, values); setModalOpen(false); setNotice(`${config.singular} ${modalRecord ? 'updated' : 'created'} successfully.`) }
  const metrics = entity === 'vehicles' ? [['Fleet vehicles', rows.length], ['Active', rows.filter(x => x.status === 'Active').length], ['In service', rows.filter(x => x.status === 'In service').length]] : entity === 'trips' ? [['Total trips', rows.length], ['Scheduled', rows.filter(x => x.status === 'Scheduled').length], ['In progress', rows.filter(x => x.status === 'In progress').length]] : entity === 'bookings' ? [['Total bookings', rows.length], ['Confirmed', rows.filter(x => x.status === 'Confirmed').length], ['Booking value', formatLkr(rows.reduce((sum, x) => sum + Number(x.amount || 0), 0))]] : entity === 'payments' ? [['Payment records', rows.length], ['Paid', rows.filter(x => x.status === 'Paid').length], ['Recorded total', formatLkr(rows.filter(x => x.status === 'Paid').reduce((sum, x) => sum + Number(x.amount || 0), 0))]] : entity === 'drivers' ? [['Driver profiles', rows.length], ['On duty', rows.filter(x => x.status === 'On duty').length], ['Avg. rating', `${(rows.reduce((sum, x) => sum + Number(x.rating || 0), 0) / (rows.length || 1)).toFixed(1)} ★`]] : entity === 'routes' ? [['Network routes', rows.length], ['Active routes', rows.filter(x => x.status === 'Active').length], ['Destinations', new Set(rows.map(x => x.destination)).size]] : entity === 'passengers' ? [['Passengers', rows.length], ['New this month', rows.filter(x => x.joined?.startsWith('2025-10')).length], ['Cities served', new Set(rows.map(x => x.city)).size]] : entity === 'maintenance' ? [['Work orders', rows.length], ['In progress', rows.filter(x => x.status === 'In progress').length], ['Scheduled', rows.filter(x => x.status === 'Scheduled').length]] : entity === 'reviews' ? [['Reviews', rows.length], ['Average rating', `${(rows.reduce((sum, x) => sum + Number(x.rating || 0), 0) / (rows.length || 1)).toFixed(1)} ★`], ['Needs attention', rows.filter(x => x.status === 'Needs review').length]] : [['Total announcements', rows.length], ['Published', rows.filter(x => x.status === 'Published').length], ['Scheduled', rows.filter(x => x.status === 'Scheduled').length]]
  return <div className="page-content"><div className="page-heading"><div><div className="eyebrow">SMARTMOVE OPERATIONS</div><h1>{config.title}</h1><p>{config.description}</p></div><div className="heading-actions"><button className="button button-outline" onClick={() => window.alert('Export is a demo-only placeholder.') }><Download size={16} /> Export</button><button className="button button-primary" onClick={openAdd}><Plus size={17} /> Add {config.singular}</button></div></div>
    <div className="mini-stats">{metrics.map(([label, value], index) => <div key={label} className="mini-stat"><span>{label}</span><strong>{value}</strong>{index === 0 && <small>Updated just now</small>}</div>)}</div>
    {notice && <div className="notice-banner"><span>{notice}</span><button onClick={() => setNotice('')} aria-label="Dismiss message"><X size={16} /></button></div>}
    {entity === 'payments' && <div className="info-banner">Payment records are for administrative tracking only. No payment processing is performed.</div>}
    <div className="table-title-row"><div><h2>{entity === 'announcements' ? 'All announcements' : `All ${config.title.toLowerCase()}`}</h2><p>{entity === 'trips' ? 'Trip assignments are linked to fleet, routes and drivers.' : `Manage and review your ${config.title.toLowerCase()} records.`}</p></div>{entity === 'trips' && <span className="table-note">Trip relationships are protected</span>}</div>
    <DataTable data={rows} columns={config.columns} collections={data} onEdit={openEdit} onDelete={remove} />
    {modalOpen && <RecordModal config={config} collections={data} initial={modalRecord} onClose={() => setModalOpen(false)} onSave={save} />}
  </div>
}
