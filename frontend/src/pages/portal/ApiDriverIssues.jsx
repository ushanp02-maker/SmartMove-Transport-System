import { useEffect, useState } from 'react'
import { request } from '../../services/apiClient'
const list=value=>Array.isArray(value)?value:value?.content||[]
export default function ApiDriverIssues(){
 const [trips,setTrips]=useState([]),[issues,setIssues]=useState([]),[tripId,setTripId]=useState(''),[description,setDescription]=useState(''),[priority,setPriority]=useState('HIGH'),[message,setMessage]=useState(''),[refresh,setRefresh]=useState(0)
 useEffect(()=>{
  let active=true
  Promise.all([request('/trip-status/driver/my-trips'),request('/driver-issues/mine')])
   .then(([assigned,reported])=>{if(active){setTrips(list(assigned));setIssues(list(reported));setMessage('')}})
   .catch(err=>{if(active)setMessage(err.message)})
  return()=>{active=false}
 },[refresh])
 const submit=async event=>{
  event.preventDefault()
  try{
   await request('/driver-issues',{method:'POST',body:JSON.stringify({tripId:Number(tripId),description,priority})})
   setDescription('');setTripId('');setRefresh(n=>n+1);setMessage('Issue reported to operations.')
  }catch(err){setMessage(err.message)}
 }
 return <section className="portal-panel"><h1>Report a vehicle issue</h1><p>Reports are stored in MongoDB and linked to your assigned trip and vehicle.</p><form onSubmit={submit}><label>Assigned trip<select required value={tripId} onChange={e=>setTripId(e.target.value)}><option value="">Select a trip</option>{trips.filter(t=>t.vehicleId).map(t=><option key={t.tripId} value={t.tripId}>Trip #{t.tripId} · Vehicle #{t.vehicleId}</option>)}</select></label><label>Priority<select value={priority} onChange={e=>setPriority(e.target.value)}>{['LOW','NORMAL','HIGH','URGENT'].map(x=><option key={x}>{x}</option>)}</select></label><label>Issue details<textarea required maxLength={1000} value={description} onChange={e=>setDescription(e.target.value)}/></label><button className="button button-primary">Report issue</button></form><p role="status">{message}</p><h2>My reports</h2>{issues.length?issues.map(issue=><article key={issue.id} className="portal-panel"><strong>{issue.priority} · {issue.status}</strong><p>Trip #{issue.tripId} · Vehicle #{issue.vehicleId}</p><p>{issue.description}</p></article>):<p>No reported issues yet.</p>}</section>
}
