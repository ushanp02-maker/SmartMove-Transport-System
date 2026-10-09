import { useEffect, useState } from 'react'
import { request } from '../services/apiClient'
const reportTypes = [['dashboard','Operations overview'],['routes','Route performance'],['drivers','Driver performance'],['passengers','Passenger activity'],['fleet','Fleet utilization'],['maintenance','Maintenance costs'],['bookings','Booking summary'],['revenue','Revenue summary']]
const dated = new Set(['maintenance','bookings','revenue'])
const today = () => new Date().toLocaleDateString('en-CA')
const display = value => value == null ? '—' : typeof value === 'object' ? JSON.stringify(value) : String(value)
export default function ApiReports() {
  const [type,setType] = useState('dashboard')
  const [start,setStart] = useState(`${new Date().getFullYear()}-01-01`)
  const [end,setEnd] = useState(today())
  const [data,setData] = useState(null)
  const [error,setError] = useState('')
  const [loading,setLoading] = useState(false)
  const [refresh,setRefresh] = useState(0)
  useEffect(() => {
    if (dated.has(type) && (!start || !end || start > end)) return
    let live = true
    const query = dated.has(type) ? `?startDate=${encodeURIComponent(start)}&endDate=${encodeURIComponent(end)}` : ''
    request(`/reports/${type}${query}`).then(value => { if (live) setData(value) }).catch(err => { if (live) {setData(null);setError(err.message)} }).finally(() => { if (live) setLoading(false) })
    return () => { live = false }
  }, [type,start,end,refresh])
  const rows = Array.isArray(data) ? data : data ? [data] : []
  const keys = [...new Set(rows.flatMap(row => Object.keys(row || {})))].slice(0,14)
  const exportCsv = () => {
    if (!rows.length) return
    const escape = value => `"${display(value).replaceAll('"','""')}"`
    const csv = [keys.map(escape).join(','), ...rows.map(row => keys.map(key => escape(row[key])).join(','))].join('\r\n')
    const url = URL.createObjectURL(new Blob([csv],{type:'text/csv;charset=utf-8'}))
    const link = document.createElement('a'); link.href=url;link.download=`smartmove-${type}.csv`;link.click();URL.revokeObjectURL(url)
  }
  return <section className="portal-panel"><div className="portal-panel-head"><div><h1>Operations reports</h1><p>Actual figures returned by the Spring Boot reporting service, not illustrative demo metrics.</p></div><div style={{display:'flex',gap:8}}><button className="button button-outline" onClick={() => setRefresh(n=>n+1)}>Refresh</button><button className="button button-primary" onClick={exportCsv} disabled={!rows.length}>Export CSV</button></div></div>
    <div className="form-grid"><label>Report<select value={type} onChange={e => setType(e.target.value)}>{reportTypes.map(([value,label]) => <option value={value} key={value}>{label}</option>)}</select></label>{dated.has(type)&&<><label>Start date<input type="date" value={start} onChange={e => setStart(e.target.value)}/></label><label>End date<input type="date" value={end} onChange={e => setEnd(e.target.value)}/></label></>}</div>
    {(dated.has(type) && (!start || !end || start > end) ? 'Choose a valid date range.' : error) && <p className="auth-error" role="alert">{dated.has(type) && (!start || !end || start > end) ? 'Choose a valid date range.' : error}</p>}{loading ? <p>Loading live report...</p> : !rows.length ? <p>No report records.</p> : <div style={{overflowX:'auto'}}><table className="data-table"><thead><tr>{keys.map(key => <th key={key}>{key}</th>)}</tr></thead><tbody>{rows.map((row,i) => <tr key={i}>{keys.map(key => <td key={key}>{display(row[key])}</td>)}</tr>)}</tbody></table></div>}
  </section>
}
