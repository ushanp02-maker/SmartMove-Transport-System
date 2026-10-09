import { useEffect, useState } from 'react'
import { request } from '../services/apiClient'
export default function ApiAnnouncementsAdmin(){
  const [items,setItems]=useState([]),[error,setError]=useState(''),[reload,setReload]=useState(0),[busy,setBusy]=useState(false)
  const [form,setForm]=useState({title:'',message:'',category:'GENERAL',priority:'NORMAL',targetAudience:'ALL',createdByUserId:''})
  useEffect(()=>{let active=true;request('/announcements').then(data=>{if(active){setItems(Array.isArray(data)?data:(data.content||[]));setError('')}}).catch(err=>{if(active)setError(err.message)});return()=>{active=false}},[reload])
  const save=async e=>{e.preventDefault();if(!Number.isSafeInteger(Number(form.createdByUserId))||Number(form.createdByUserId)<1)return setError('Enter your valid Oracle user ID');setBusy(true);setError('')
    try{await request('/announcements',{method:'POST',body:JSON.stringify({...form,createdByUserId:Number(form.createdByUserId)})});setForm({...form,title:'',message:''});setReload(n=>n+1)}catch(err){setError(err.message)}finally{setBusy(false)}
  }
  const publish=async item=>{const userId=window.prompt('Your Oracle admin user ID');if(!userId)return;if(!Number.isSafeInteger(Number(userId))||Number(userId)<1)return setError('Invalid user ID');setBusy(true);setError('');try{await request(`/announcements/${encodeURIComponent(item.id)}/publish`,{method:'PATCH',body:JSON.stringify({userId:Number(userId)})});setReload(n=>n+1)}catch(err){setError(err.message)}finally{setBusy(false)}}
  return <section className="portal-panel"><div className="portal-panel-head"><div><h1>Announcements</h1><p>Announcements are stored through the MongoDB-backed Spring Boot API.</p></div><button className="button button-outline" onClick={()=>setReload(n=>n+1)}>Refresh</button></div>
  {error&&<p className="auth-error" role="alert">{error}</p>}
  <form onSubmit={save}><div className="form-grid">{[['title','Title'],['message','Message'],['category','Category'],['priority','Priority'],['targetAudience','Target audience'],['createdByUserId','Creator Oracle user ID']].map(([key,label])=><label key={key}>{label}<input required={['title','message','createdByUserId'].includes(key)} value={form[key]} onChange={e=>setForm(old=>({...old,[key]:e.target.value}))}/></label>)}</div><button className="button button-primary" disabled={busy}>Create announcement</button></form>
  <div style={{overflowX:'auto'}}><table className="data-table"><thead><tr><th>ID</th><th>Title</th><th>Message</th><th>Status</th><th>Action</th></tr></thead><tbody>{items.map(item=><tr key={item.id}><td>{item.id}</td><td>{item.title}</td><td>{item.message}</td><td>{item.status}</td><td><button disabled={busy} className="button button-outline" onClick={()=>publish(item)}>Publish</button></td></tr>)}</tbody></table></div>{!items.length&&<p>No announcements yet.</p>}</section>
}
