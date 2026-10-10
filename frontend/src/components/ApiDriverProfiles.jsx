import { useEffect, useState } from 'react'
import { request } from '../services/apiClient'
const blank={name:'',email:'',phone:'',licenseNumber:'',licenseExpiry:'',experienceYears:0}
export default function ApiDriverProfiles(){
 const [form,setForm]=useState(blank),[editing,setEditing]=useState(null),[rows,setRows]=useState([]),[reload,setReload]=useState(0),[notice,setNotice]=useState('')
 useEffect(()=>{let active=true;request('/drivers').then(data=>{if(active)setRows(data)}).catch(e=>{if(active)setNotice(e.message)});return()=>{active=false}},[reload])
 const submit=async event=>{
  event.preventDefault()
  try{
   const payload={...form,experienceYears:Number(form.experienceYears)}
   const result=await request(editing?'/drivers/'+editing:'/drivers',{method:editing?'PUT':'POST',body:JSON.stringify(payload)})
   setNotice('Driver profile #'+result.id+' saved. Link this ID to a driver account below.')
   setForm(blank);setEditing(null);setReload(n=>n+1)
  }catch(err){setNotice(err.message)}
 }
 const remove=async row=>{
  if(!window.confirm('Delete driver '+row.name+'? Only unassigned drivers without linked accounts can be removed.'))return
  try{await request('/admin/delete/drivers/'+row.id,{method:'DELETE'});setNotice('Driver removed.');setReload(n=>n+1)}catch(err){setNotice(err.message)}
 }
 const changeStatus=async(row,status)=>{
  try{await request('/drivers/'+row.id+'/status',{method:'PATCH',body:JSON.stringify({newStatus:status})});setReload(n=>n+1)}catch(err){setNotice(err.message)}
 }
 return <section className="portal-panel"><h2>Driver profiles</h2><form onSubmit={submit}><div className="form-grid">{Object.keys(blank).map(key=><label key={key}>{key}<input required={['name','email','licenseNumber','licenseExpiry'].includes(key)} type={key==='licenseExpiry'?'date':key==='experienceYears'?'number':key==='email'?'email':'text'} value={form[key]??''} onChange={e=>setForm(old=>({...old,[key]:e.target.value}))}/></label>)}</div><button className="button button-primary">{editing?'Update profile':'Create profile'}</button>{editing&&<button type="button" className="button button-outline" onClick={()=>{setEditing(null);setForm(blank)}}>Cancel edit</button>}</form><p role="status">{notice}</p><div style={{overflowX:'auto'}}><table className="data-table"><thead><tr><th>ID</th><th>Name</th><th>License</th><th>Status</th><th>Actions</th></tr></thead><tbody>{rows.map(row=><tr key={row.id}><td>{row.id}</td><td>{row.name}</td><td>{row.licenseNumber}</td><td>{row.status}</td><td><button onClick={()=>{setEditing(row.id);setForm({name:row.name,email:row.email||'',phone:row.phone||'',licenseNumber:row.licenseNumber,licenseExpiry:row.licenseExpiry||'',experienceYears:row.experienceYears||0})}}>Edit</button><button onClick={()=>changeStatus(row,row.status==='AVAILABLE'?'UNAVAILABLE':'AVAILABLE')}>{row.status==='AVAILABLE'?'Mark unavailable':'Mark available'}</button><button type="button" onClick={()=>remove(row)}>Delete</button></td></tr>)}</tbody></table></div></section>
}
