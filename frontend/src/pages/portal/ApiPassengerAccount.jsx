import { useEffect, useState } from 'react'
import { request } from '../../services/apiClient'
export function ApiPassengerProfile(){
 const [profile,setProfile]=useState(null),[error,setError]=useState('')
 useEffect(()=>{let active=true;const load=async()=>{const me=await request('/users/me/passenger');const data=await request('/passengers/'+me.passengerId);if(active)setProfile(data)};load().catch(e=>{if(active)setError(e.message)});return()=>{active=false}},[])
 return <section className="portal-panel"><h1>My passenger profile</h1>{error&&<p role="alert">{error}</p>}{profile&&Object.entries(profile).map(([key,value])=><p key={key}><strong>{key}</strong>: {String(value??'—')}</p>)}</section>
}
export function ApiPassengerPayments(){
 const [rows,setRows]=useState([]),[error,setError]=useState('')
 useEffect(()=>{let active=true;const load=async()=>{const me=await request('/users/me/passenger');const data=await request('/payments/passenger/'+me.passengerId);if(active)setRows(Array.isArray(data)?data:data?.content||[])};load().catch(e=>{if(active)setError(e.message)});return()=>{active=false}},[])
 return <section className="portal-panel"><h1>Payment records</h1><p>Administrative payment statuses only. No payment gateway is connected.</p>{error&&<p role="alert">{error}</p>}{rows.length?rows.map((item,i)=><article key={item.id??i}><strong>{item.paymentReference||'Payment #'+item.id} · {item.status}</strong><p>Amount: {item.amount??item.totalAmount??'—'}</p></article>):<p>No payment records.</p>}</section>
}
