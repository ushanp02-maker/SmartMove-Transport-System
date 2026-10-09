import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { request } from '../services/apiClient'
const render=(value)=>value==null?'—':typeof value==='object'?JSON.stringify(value):String(value)
export default function ApiDashboard(){
 const [data,setData]=useState(null),[error,setError]=useState(''),[refresh,setRefresh]=useState(0)
 useEffect(()=>{let active=true;request('/reports/dashboard').then(value=>{if(active){setData(value);setError('')}}).catch(e=>{if(active)setError(e.message)});return()=>{active=false}},[refresh])
 return <div className="page-content dashboard-page"><div className="page-heading"><div><h1>Operations overview</h1><p>Live statistics from Spring Boot and Oracle, not demo records.</p></div><button className="button button-outline" onClick={()=>setRefresh(n=>n+1)}>Refresh</button></div>{error&&<p role="alert" className="auth-error">{error}</p>}{!data?<p>Loading dashboard...</p>:<div className="stat-grid">{Object.entries(data).map(([key,value])=><section className="portal-panel" key={key}><h3>{key.replace(/([A-Z])/g,' $1')}</h3><strong>{render(value)}</strong></section>)}</div>}<div style={{display:'flex',gap:12,marginTop:20,flexWrap:'wrap'}}><Link className="button button-primary" to="/admin/trips">Manage trips</Link><Link className="button button-outline" to="/admin/reports">View reports</Link><Link className="button button-outline" to="/admin/live-fleet">Live fleet</Link></div></div>
}
