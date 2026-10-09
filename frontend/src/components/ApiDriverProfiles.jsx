import { useState } from 'react'
import { request } from '../services/apiClient'
export default function ApiDriverProfiles() {
 const [form,setForm]=useState({name:'',email:'',phone:'',licenseNumber:'',licenseExpiry:'',experienceYears:0})
 const [notice,setNotice]=useState('')
 const submit=async event=>{
  event.preventDefault()
  try {
   const result=await request('/drivers',{method:'POST',body:JSON.stringify({...form,experienceYears:Number(form.experienceYears)})})
   setNotice('Driver profile ID: '+result.id)
  } catch(err) { setNotice(err.message) }
 }
 return <section className="portal-panel"><h2>Create driver profile</h2><form onSubmit={submit}>{Object.keys(form).map(key=><label key={key}>{key}<input required={key!=='phone'} type={key==='licenseExpiry'?'date':key==='experienceYears'?'number':'text'} value={form[key]} onChange={e=>setForm(old=>({...old,[key]:e.target.value}))}/></label>)}<button className="button button-primary">Save driver profile</button></form><p>{notice}</p></section>
}
