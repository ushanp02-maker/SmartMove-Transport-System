import { useEffect, useState } from 'react'
import { request } from '../../services/apiClient'
const defaults={organizationName:'',contactPerson:'',contactPhone:'',contactEmail:'',origin:'',destination:'',requestedDate:'',pickupTime:'08:00',returnTime:'',passengerCount:1,journeyType:'ONE_WAY',frequency:'ONE_TIME',serviceEndDate:'',specialRequirements:''}
export default function ApiStaffTransport(){
 const [form,setForm]=useState(defaults),[rows,setRows]=useState([]),[error,setError]=useState(''),[notice,setNotice]=useState(''),[reload,setReload]=useState(0)
 useEffect(()=>{let active=true;request('/staff-transport/my-requests').then(value=>{if(active)setRows(Array.isArray(value)?value:value?.content||[])}).catch(err=>{if(active)setError(err.message)});return()=>{active=false}},[reload])
 const submit=async event=>{
  event.preventDefault();setError('');setNotice('')
  try {
   const body={...form,passengerCount:Number(form.passengerCount)}
   for(const key of ['returnTime','serviceEndDate','contactEmail','contactPhone','contactPerson','specialRequirements'])if(!body[key])body[key]=null
   const result=await request('/staff-transport/requests',{method:'POST',body:JSON.stringify(body)})
   setNotice('Staff transport request submitted: #'+result.id);setForm(defaults);setReload(n=>n+1)
  }catch(err){setError(err.message)}
 }
 return <section className="portal-panel"><h1>Staff transport request</h1><p>Operations must review and approve each request before assignment.</p><form onSubmit={submit}><div className="form-grid">{Object.keys(defaults).map(key=><label key={key}>{key}<input type={key==='requestedDate'||key==='serviceEndDate'?'date':key==='pickupTime'||key==='returnTime'?'time':key==='passengerCount'?'number':'text'} required={['organizationName','origin','destination','requestedDate','pickupTime','passengerCount'].includes(key)} value={form[key]} onChange={e=>setForm(old=>({...old,[key]:e.target.value}))}/></label>)}</div><button className="button button-primary">Submit request</button></form>{error&&<p className="auth-error" role="alert">{error}</p>}{notice&&<p role="status">{notice}</p>}<h2>My requests</h2>{rows.map((row,i)=><article key={row.id??i}><strong>#{row.id} · {row.status}</strong><p>{row.origin} → {row.destination}</p></article>)}</section>
}
