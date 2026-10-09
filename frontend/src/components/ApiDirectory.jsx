import { useEffect, useState } from 'react'
import { request } from '../services/apiClient'
const paths={passengers:'/passengers',bookings:'/bookings'}
const format=value=>value==null?'—':typeof value==='object'?JSON.stringify(value):String(value)
export default function ApiDirectory({entity}){
  const [rows,setRows]=useState([]),[error,setError]=useState(''),[reload,setReload]=useState(0),[search,setSearch]=useState('')
  useEffect(()=>{let active=true;request(paths[entity]).then(value=>{if(active){setRows(Array.isArray(value)?value:value?.content||[]);setError('')}}).catch(err=>{if(active)setError(err.message)});return()=>{active=false}},[entity,reload])
  const visible=rows.filter(row=>JSON.stringify(row).toLowerCase().includes(search.toLowerCase()))
  const keys=[...new Set(visible.flatMap(row=>Object.keys(row)))].slice(0,12)
  return <section className="portal-panel"><div className="portal-panel-head"><div><h1>{entity==='passengers'?'Passenger directory':'Booking management'}</h1><p>Records retrieved directly from Oracle via Spring Boot.</p></div><button className="button button-outline" onClick={()=>setReload(n=>n+1)}>Refresh</button></div><label>Search records<input value={search} onChange={e=>setSearch(e.target.value)} placeholder="Search database records"/></label>{error&&<p className="auth-error" role="alert">{error}</p>}<div style={{overflowX:'auto'}}><table className="data-table"><thead><tr>{keys.map(key=><th key={key}>{key}</th>)}</tr></thead><tbody>{visible.map((row,i)=><tr key={row.id??i}>{keys.map(key=><td key={key}>{format(row[key])}</td>)}</tr>)}</tbody></table></div>{!visible.length&&<p>No matching records.</p>}</section>
}
