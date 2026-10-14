import { useEffect, useMemo, useState } from 'react'
import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { Activity, CalendarDays, Download, RefreshCw, TrendingUp, Database, Search } from 'lucide-react'
import { request } from '../services/apiClient'
import './ApiReports.css'

const reports = [
  { id: 'routes', label: 'Popular routes', description: 'Ticket bookings by route', metric: 'total_bookings', name: 'route_name' },
  { id: 'revenue', label: 'Ticket revenue', description: 'Sales and refunds in a selected period', dates: true, metric: 'gross_revenue' },
  { id: 'passenger-history', label: 'Passenger history', description: 'Journeys and booking records', passenger: true, metric: 'total_fare', name: 'route_name' },
  { id: 'maintenance-due', label: 'Maintenance due', description: 'Vehicles needing service', until: true, name: 'registration_number' },
  { id: 'drivers', label: 'Driver performance', description: 'Assignments and completed trips', metric: 'completed_trips', name: 'driver_name' },
  { id: 'bookings', label: 'Booking summary', description: 'Booking activity in a selected period', dates: true, metric: 'total_bookings' },
  { id: 'fleet', label: 'Fleet utilization', description: 'Trip assignments per vehicle', metric: 'assigned_trips', name: 'registration_number' },
  { id: 'daily-revenue', label: 'Daily revenue', description: 'Revenue and refunds by day', dates: true, metric: 'gross_revenue', name: 'report_date' },
  { id: 'maintenance-cost', label: 'Maintenance costs', description: 'Completed service expenses', dates: true, metric: 'total_actual_cost' },
]
const localDate = (date = new Date()) => [date.getFullYear(), String(date.getMonth() + 1).padStart(2, '0'), String(date.getDate()).padStart(2, '0')].join('-')
const titleCase = value => value.replaceAll('_', ' ').replace(/\b\w/g, c => c.toUpperCase())
const format = value => {
  if (value == null) return '—'
  if (typeof value === 'number') return new Intl.NumberFormat('en-LK', { maximumFractionDigits: 2 }).format(value)
  if (typeof value === 'object') return JSON.stringify(value)
  return String(value)
}
const csvCell = value => '"' + String(value ?? '').replaceAll('"', '""') + '"'

export default function ApiReports() {
  const [type, setType] = useState('routes')
  const [start, setStart] = useState(localDate(new Date(new Date().getFullYear(), 0, 1)))
  const [end, setEnd] = useState(localDate())
  const [passengerId, setPassengerId] = useState('')
  const [rows, setRows] = useState([])
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [refresh, setRefresh] = useState(0)
  const [filter, setFilter] = useState('')
  const selected = reports.find(item => item.id === type)
  const invalid = (selected.dates && (!start || !end || start > end)) || (selected.until && !end) || (selected.passenger && (!/^\d+$/.test(passengerId) || Number(passengerId) <= 0))
  useEffect(() => {
    if (invalid) { return }
    let active = true
    const params = new URLSearchParams()
    if (selected.dates) { params.set('startDate', start); params.set('endDate', end) }
    if (selected.until) params.set('endDate', end)
    if (selected.passenger) params.set('passengerId', passengerId)
    const query = params.size ? '?' + params.toString() : ''
    // Do not show stale rows while changing reports.
    request('/reports/plsql/' + type + query)
      .then(result => { if (active) setRows(Array.isArray(result) ? result : []) })
      .catch(err => { if (active) { setRows([]); setError(err.message || 'Report unavailable') } })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [type, start, end, passengerId, refresh, invalid, selected.dates, selected.until, selected.passenger])
  const visible = useMemo(() => rows.filter(row => Object.values(row).some(value => String(value ?? '').toLowerCase().includes(filter.toLowerCase()))), [rows, filter])
  const columns = useMemo(() => [...new Set(rows.flatMap(row => Object.keys(row)))], [rows])
  const chart = useMemo(() => selected.metric && selected.name ? visible.slice(0, 12).map((row, i) => ({
    label: String(row[selected.name] ?? 'Record ' + (i + 1)).slice(0, 19),
    value: Number(row[selected.metric] || 0),
  })) : [], [visible, selected])
  const total = selected.metric ? rows.reduce((sum, row) => sum + Number(row[selected.metric] || 0), 0) : rows.length
  const exportCsv = () => {
    if (!visible.length) return
    const content = [columns.map(csvCell).join(','), ...visible.map(row => columns.map(key => csvCell(row[key])).join(','))].join('\r\n')
    const url = URL.createObjectURL(new Blob(['\uFEFF' + content], { type: 'text/csv;charset=utf-8' }))
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = 'smartmove-plsql-' + type + '.csv'
    anchor.click()
    URL.revokeObjectURL(url)
  }
  const changeType = value => { setType(value); setRows([]); setError(''); setFilter(''); setLoading(true) }
  const reload = () => { setRows([]); setError(''); setLoading(true); setRefresh(n => n + 1) }
  return <div className="page-content sm-reports">
    <div className="page-heading sm-report-heading">
      <div><div className="eyebrow">SMARTMOVE OPERATIONS / ORACLE ANALYTICS</div><h1>Business reports</h1><p>Live insights generated by Oracle PL/SQL procedures.</p></div>
      <div className="heading-actions"><button className="button button-outline" onClick={reload} disabled={loading || invalid}><RefreshCw size={15}/> Refresh</button><button className="button button-primary" onClick={exportCsv} disabled={!visible.length}><Download size={15}/> Export CSV</button></div>
    </div>
    <div className="sm-report-intro"><span className="sm-report-intro-icon"><Database size={23}/></span><div><strong>Oracle-powered reporting</strong><p>Data comes from the SM_REPORTS package through the Spring Boot API. No illustrative figures are used.</p></div><span className="sm-report-live"><span/> PL/SQL DATA</span></div>
    <div className="sm-report-layout">
      <aside className="panel sm-report-menu"><div className="sm-report-menu-title">REPORT LIBRARY <span>{reports.length}</span></div>{reports.map(report => <button key={report.id} className={'sm-report-menu-item' + (report.id === type ? ' active' : '')} onClick={() => changeType(report.id)} aria-pressed={report.id === type}><span>{report.label}<small>{report.description}</small></span></button>)}</aside>
      <div className="sm-report-main">
        <section className="panel sm-report-panel"><div className="sm-report-section-title"><div><h2>{selected.label}</h2><p>{selected.description}</p></div><span className="sm-report-source"><Activity size={13}/> Oracle</span></div>
          <div className="sm-report-filters">
            {selected.dates && <label><CalendarDays size={14}/> From <input aria-label="Start date" type="date" value={start} onChange={e => setStart(e.target.value)}/></label>}
            {(selected.dates || selected.until) && <label><CalendarDays size={14}/> {selected.until ? 'Due by' : 'To'} <input aria-label="End date" type="date" value={end} onChange={e => setEnd(e.target.value)}/></label>}
            {selected.passenger && <label>Passenger ID <input aria-label="Passenger ID" type="number" min="1" placeholder="Enter Oracle passenger ID" value={passengerId} onChange={e => setPassengerId(e.target.value)}/></label>}
          </div>
          {invalid ? <div className="sm-report-notice">Enter {selected.passenger ? 'a valid passenger ID' : 'a valid date range'} to run this report.</div> : error ? <div className="sm-report-error" role="alert">{error}<p>Check the backend connection and confirm the SM_REPORTS package is installed in Oracle.</p></div> : loading ? <div className="sm-report-notice" role="status">Loading Oracle report…</div> : <><div className="sm-report-metrics"><div><span>Returned records</span><strong>{format(rows.length)}</strong></div><div><span>{selected.metric ? titleCase(selected.metric) : 'Report status'}</span><strong>{selected.metric ? format(total) : 'Ready'}</strong></div><div><span>Data source</span><strong>Oracle PL/SQL</strong></div></div>{chart.length > 0 && <div className="sm-report-chart"><div className="sm-report-chart-heading"><TrendingUp size={16}/><strong>{titleCase(selected.metric)} by {titleCase(selected.name)}</strong></div><ResponsiveContainer width="100%" height={245}><BarChart data={chart} margin={{top:12,right:12,left:0,bottom:10}}><CartesianGrid vertical={false} stroke="#edf0eb"/><XAxis dataKey="label" tick={{fontSize:10,fill:'#718175'}} axisLine={false} tickLine={false} interval={0} angle={-15} textAnchor="end" height={55}/><YAxis tick={{fontSize:10,fill:'#718175'}} axisLine={false} tickLine={false}/><Tooltip formatter={value=>format(value)}/><Bar dataKey="value" fill="#73977e" radius={[5,5,0,0]} maxBarSize={48}/></BarChart></ResponsiveContainer></div>}</>}
        </section>
        <section className="panel sm-report-table-panel"><div className="sm-report-section-title"><div><h2>Detailed records</h2><p>{visible.length} matching records</p></div><label className="sm-report-search"><Search size={15}/><input aria-label="Filter report rows" placeholder="Filter results..." value={filter} onChange={e => setFilter(e.target.value)}/></label></div><div className="sm-report-table-wrap">{!invalid && !loading && !error && visible.length ? <table className="data-table"><thead><tr>{columns.map(key => <th key={key}>{titleCase(key)}</th>)}</tr></thead><tbody>{visible.map((row, i) => <tr key={i}>{columns.map(key => <td key={key}>{format(row[key])}</td>)}</tr>)}</tbody></table> : <div className="sm-report-notice">{loading ? 'Retrieving records…' : error ? 'Report could not be loaded.' : invalid ? 'Complete the filters above.' : 'No records found for this selection.'}</div>}</div></section>
      </div>
    </div>
  </div>
}
