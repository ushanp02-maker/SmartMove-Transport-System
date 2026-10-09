import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { request } from '../../services/apiClient'
const list=data=>Array.isArray(data)?data:data?.content||[]
export function ApiDriverSchedule(){
 const [rows,setRows]=useState([]),[error,setError]=useState('')
 useEffect(()=>{request('/trip-status/driver/my-trips').then(setRows).catch(e=>setError(e.message))},[])
 return <section className="portal-panel"><h1>My schedule</h1>{error&&<p role="alert">{error}</p>}{rows.map(t=><article key={t.tripId}><Link to={'/driver/trip/'+t.tripId}>Trip #{t.tripId}</Link><p>{t.scheduledDeparture} → {t.scheduledArrival} · {t.status}</p></article>)}</section>
}
export function ApiDriverProfile(){
 const [data,setData]=useState(null),[error,setError]=useState('')
 useEffect(()=>{request('/users/me').then(setData).catch(e=>setError(e.message))},[])
 return <section className="portal-panel"><h1>Driver account</h1>{error&&<p role="alert">{error}</p>}{data&&Object.entries(data).filter(([key])=>!key.toLowerCase().includes('password')).map(([key,value])=><p key={key}><strong>{key}</strong>: {String(value??'—')}</p>)}</section>
}
export function ApiDriverAnnouncements(){
 const [rows,setRows]=useState([]),[error,setError]=useState('')
 useEffect(()=>{request('/announcements/public').then(data=>setRows(list(data))).catch(e=>setError(e.message))},[])
 return <section className="portal-panel"><h1>Announcements</h1>{error&&<p role="alert">{error}</p>}{rows.map((item,i)=><article key={item.id??i}><h3>{item.title}</h3><p>{item.message}</p></article>)}</section>
}
