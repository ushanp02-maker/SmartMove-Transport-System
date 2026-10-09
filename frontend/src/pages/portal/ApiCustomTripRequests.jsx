import { useEffect, useState } from 'react'
import { request } from '../../services/apiClient'
export default function ApiCustomTripRequests(){
 const [form,setForm]=useState({pickupAddress:'',pickupLatitude:'',pickupLongitude:'',destinationAddress:'',destinationLatitude:'',destinationLongitude:'',requestedPickupTime:'',passengerCount:1,serviceType:'STANDARD',specialRequirements:''})
 const [rows,setRows]=useState([]),[error,setError]=useState(''),[refresh,setRefresh]=useState(0)
 useEffect(()=>{request('/on-demand/my-requests').then(setRows).catch(e=>setError(e.message))},[refresh])
 const submit=async e=>{e.preventDefault();try{await request('/on-demand/requests',{method:'POST',body:JSON.stringify({...form,pickupLatitude:Number(form.pickupLatitude),pickupLongitude:Number(form.pickupLongitude),destinationLatitude:Number(form.destinationLatitude),destinationLongitude:Number(form.destinationLongitude),passengerCount:Number(form.passengerCount)})});setRefresh(n=>n+1)}catch(err){setError(err.message)}}
 return <section className="portal-panel"><h1>Custom trip request</h1><form onSubmit={submit}>{Object.keys(form).map(key=><label key={key}>{key}<input required={key!=='specialRequirements'} type={key==='requestedPickupTime'?'datetime-local':'text'} value={form[key]} onChange={e=>setForm(old=>({...old,[key]:e.target.value}))}/></label>)}<button className="button button-primary">Submit</button></form>{error&&<p className="auth-error">{error}</p>}{rows.map(row=><p key={row.id}>#{row.id}: {row.status}</p>)}</section>
}
