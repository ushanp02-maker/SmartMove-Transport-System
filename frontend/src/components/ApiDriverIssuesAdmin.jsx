import { useEffect, useState } from 'react'
import { request } from '../services/apiClient'
export default function ApiDriverIssuesAdmin(){
 const [items,setItems]=useState([]),[error,setError]=useState(''),[refresh,setRefresh]=useState(0)
 useEffect(()=>{let active=true;request('/driver-issues/admin').then(data=>{if(active)setItems(data)}).catch(err=>{if(active)setError(err.message)});return()=>{active=false}},[refresh])
 const change=async(issue,status)=>{
  try{await request('/driver-issues/admin/'+encodeURIComponent(issue.id)+'/status',{method:'PATCH',body:JSON.stringify({status})});setRefresh(n=>n+1)}catch(err){setError(err.message)}
 }
 return <section className="portal-panel"><h2>Driver issue reports</h2><p>Reports submitted by assigned drivers, stored in MongoDB.</p>{error&&<p role="alert">{error}</p>}{items.length?items.map(issue=><article key={issue.id} className="portal-panel"><strong>{issue.priority} · {issue.status}</strong><p>Trip #{issue.tripId} · Vehicle #{issue.vehicleId} · Driver #{issue.driverId}</p><p>{issue.description}</p><div style={{display:'flex',gap:8}}>{['OPEN','IN_PROGRESS','RESOLVED'].filter(x=>x!==issue.status).map(status=><button key={status} className="button button-outline" onClick={()=>change(issue,status)}>{status.replace('_',' ')}</button>)}</div></article>):<p>No issues reported.</p>}</section>
}
