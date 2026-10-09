import { useEffect, useState } from 'react'
import { request } from '../../services/apiClient'
export default function ApiDriverVehicle(){
 const [items,setItems]=useState([]),[error,setError]=useState('')
 useEffect(()=>{
  let active=true
  const load=async()=>{
   const trips=await request('/trip-status/driver/my-trips')
   const ids=[...new Set(trips.map(t=>t.vehicleId).filter(Boolean))]
   const vehicles=await Promise.all(ids.map(id=>request('/vehicles/'+id)))
   if(active)setItems(vehicles)
  }
  load().catch(e=>{if(active)setError(e.message)})
  return()=>{active=false}
 },[])
 return <section className="portal-panel"><h1>My assigned vehicles</h1>{error&&<p role="alert">{error}</p>}{items.length?items.map((v,i)=><article key={v.id??i}><h3>{v.registrationNumber||v.name}</h3>{Object.entries(v).filter(([key])=>!key.toLowerCase().includes('document')).map(([key,value])=><p key={key}><strong>{key}</strong>: {String(value??'—')}</p>)}</article>):<p>No assigned vehicles.</p>}</section>
}
