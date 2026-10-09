import { useEffect, useState } from 'react'
import { request } from '../services/apiClient'

export default function ApiAccounts({ kind }) {
  const driver = kind === 'DRIVER'
  const [items,setItems] = useState([])
  const [error,setError] = useState('')
  const [notice,setNotice] = useState('')
  const [loading,setLoading] = useState(true)
  const [busy,setBusy] = useState(false)
  const [form,setForm] = useState({ username:'',email:'',password:'',role:'ADMIN',driverId:'' })
  const [refresh,setRefresh] = useState(0)
  useEffect(() => {
    let active=true
    request(`/admin/accounts/role/${driver?'DRIVER':'ADMIN'}`).then(async rows => {
      const all = driver ? rows : [...rows,...await request('/admin/accounts/role/SUPER_ADMIN')]
      if(active){setItems(all);setError('')}
    }).catch(e=>{if(active)setError(e.message)}).finally(()=>{if(active)setLoading(false)})
    return ()=>{active=false}
  },[driver,refresh])
  const update = e => setForm(v=>({...v,[e.target.name]:e.target.value}))
  const submit = async e => {
    e.preventDefault();setBusy(true);setError('');setNotice('')
    try {
      const payload = driver ? {driverId:Number(form.driverId),username:form.username,password:form.password} : {username:form.username,email:form.email,password:form.password,role:form.role}
      await request(driver?'/admin/accounts/driver':'/admin/accounts/admin',{method:'POST',body:JSON.stringify(payload)})
      setNotice('Account created in Oracle. Share the initial password securely with its owner.')
      setForm({username:'',email:'',password:'',role:'ADMIN',driverId:''})
      setLoading(true);setRefresh(n=>n+1)
    } catch(err){setError(err.message)}finally{setBusy(false)}
  }
  const changeStatus = async account => {
    const newStatus=account.accountStatus==='ACTIVE'?'DISABLED':'ACTIVE'
    if(!window.confirm(`Change ${account.username} to ${newStatus}?`))return
    try{await request(`/admin/accounts/${account.id}/status`,{method:'PATCH',body:JSON.stringify({newStatus})});setLoading(true);setRefresh(n=>n+1)}
    catch(err){setError(err.message)}
  }
  return <div className="page-content"><div className="page-heading"><div><h1>{driver?'Driver accounts':'Administrator accounts'}</h1><p>Secure account provisioning and access management backed by Oracle.</p></div></div>
    {error&&<p className="auth-error" role="alert">{error}</p>}{notice&&<p role="status">{notice}</p>}
    <section className="portal-panel"><h2>Create {driver?'driver login':'administrator account'}</h2>
      {driver&&<p>Create the driver profile first through your backend driver-management workflow. Enter its numeric ID here to link the login.</p>}
      <form className="form-grid" onSubmit={submit}>
        {driver&&<label>Existing driver ID<input type="number" min="1" name="driverId" value={form.driverId} onChange={update} required/></label>}
        <label>Username<input name="username" value={form.username} onChange={update} required minLength="3"/></label>
        {!driver&&<><label>Email<input type="email" name="email" value={form.email} onChange={update} required/></label><label>Role<select name="role" value={form.role} onChange={update}><option value="ADMIN">ADMIN</option><option value="SUPER_ADMIN">SUPER_ADMIN</option></select></label></>}
        <label>Initial password<input type="password" name="password" value={form.password} onChange={update} required minLength="8" autoComplete="new-password"/></label>
        <button className="button button-primary" disabled={busy}>{busy?'Creating...':'Create account'}</button>
      </form></section>
    <section className="portal-panel"><h2>Existing accounts</h2>{loading?<p>Loading accounts...</p>:items.length===0?<p>No matching accounts.</p>:
      <div className="table-scroll"><table><thead><tr><th>ID</th><th>Username</th><th>Email</th><th>Role</th><th>Status</th><th>Actions</th></tr></thead><tbody>{items.map(a=><tr key={a.id}><td>{a.id}</td><td>{a.username}</td><td>{a.email}</td><td>{a.role}</td><td>{a.accountStatus}</td><td><button className="button button-outline" onClick={()=>changeStatus(a)}>{a.accountStatus==='ACTIVE'?'Disable':'Enable'}</button></td></tr>)}</tbody></table></div>}
    </section></div>
}
