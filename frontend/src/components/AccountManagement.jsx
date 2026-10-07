import { useState } from 'react'
import { Search, Plus, Pencil, UserRoundCog, CircleAlert, X, ShieldCheck, UserPlus, RefreshCw, Trash2 } from 'lucide-react'
import { useAppData } from '../services/useAppData'

const blankDriver = { name:'',email:'',username:'',phone:'',license:'',experience:'0',status:'On duty',accountStatus:'ACTIVE' }
const blankAdmin = { name:'',email:'',username:'',phone:'',accountStatus:'ACTIVE' }
const validEmail = value => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)

function AccountForm({ kind, initial, onClose, onSave }) {
  const isDriver = kind === 'DRIVER'
  const [form, setForm] = useState(() => ({ ...(isDriver ? blankDriver : blankAdmin), ...initial, accountStatus: initial?.accountStatus || 'ACTIVE' }))
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const update = event => setForm(current => ({ ...current, [event.target.name]: event.target.value }))
  const submit = event => {
    event.preventDefault(); setError('')
    if (form.name.trim().length < 2 || !form.email.trim() || !form.username.trim() || !form.phone.trim()) return setError('Enter a full name and complete all required account fields.')
    if (!validEmail(form.email)) return setError('Enter a valid email address.')
    if (!/^[a-zA-Z0-9._-]{3,24}$/.test(form.username.trim())) return setError('Username must be 3–24 letters, numbers, dots, underscores or hyphens.')
    if (!/^\+?[0-9 ()-]{7,18}$/.test(form.phone.trim())) return setError('Enter a valid phone number.')
    if (isDriver && (!form.license.trim() || Number(form.experience) < 0 || !Number.isInteger(Number(form.experience)))) return setError('Enter a valid licence number and whole years of experience.')
    setBusy(true)
    const result = onSave({ ...form, name:form.name.trim(), email:form.email.trim().toLowerCase(), username:form.username.trim().toLowerCase(), phone:form.phone.trim(), ...(isDriver ? { experience:Number(form.experience), license:form.license.trim() } : {}) })
    if (result?.error) { setError(result.error); setBusy(false); return }
    setBusy(false)
  }
  const field = (name, label, type='text', props={}) => <label key={name}>{label}<input name={name} type={type} value={form[name] ?? ''} onChange={update} required={props.required !== false} min={props.min} step={props.step} autoComplete={props.autoComplete}/></label>
  return <div className="modal-backdrop" onMouseDown={event=>event.target===event.currentTarget&&onClose()}><section className="record-modal account-modal" role="dialog" aria-modal="true" aria-labelledby="account-modal-title"><div className="modal-heading"><div><span className="eyebrow">{initial?'ACCOUNT PROFILE':'DEMO ACCOUNT PROVISIONING'}</span><h2 id="account-modal-title">{initial?`Edit ${isDriver?'driver':'administrator'}`:`Add ${isDriver?'driver':'administrator'}`}</h2></div><button type="button" className="icon-button" onClick={onClose} aria-label="Close"><X size={19}/></button></div><div className="account-activation-note"><ShieldCheck size={16}/><span>Demo activation only. No password is generated or stored. The new account can use the documented demo sign-in flow.</span></div><form onSubmit={submit}><div className="form-grid">{field('name','Full name','text',{autoComplete:'name'})}{field('email','Email address','email',{autoComplete:'email'})}{field('username','Username','text',{autoComplete:'username'})}{field('phone','Phone number','tel',{autoComplete:'tel'})}{isDriver&&<>{field('license','Driving licence number')}{field('experience','Years of experience','number',{min:'0',step:'1'})}<label>Driver status<select name="status" value={form.status} onChange={update}><option>On duty</option><option>Off duty</option><option>On leave</option></select></label></>}</div><label className="account-status-field">Account status<select name="accountStatus" value={form.accountStatus} onChange={update}><option value="ACTIVE">Active</option><option value="DISABLED">Disabled</option></select></label>{error&&<p className="form-error" role="alert"><CircleAlert size={15}/>{error}</p>}<div className="modal-actions"><button type="button" className="button button-quiet" onClick={onClose}>Cancel</button><button className="button button-primary" disabled={busy}>{busy?'Saving account…':initial?'Save changes':isDriver?'Create driver account':'Create admin account'}</button></div></form></section></div>
}

export default function AccountManagement({ kind }) {
  const isDriver = kind === 'DRIVER'
  const { data, currentUser, createProvisionedAccount, updateAccountAndProfile, deleteProvisionedDriver } = useAppData()
  const [query, setQuery] = useState('')
  const [modal, setModal] = useState(null)
  const [notice, setNotice] = useState('')
  const accounts = data.users.filter(user=>isDriver ? user.role==='DRIVER' : user.role==='ADMIN'||user.role==='SUPER_ADMIN').map(user=>({ ...user, profile:data[isDriver?'drivers':'admins'].find(profile=>profile.id===user.linkedProfileId) })).filter(user=>user.profile)
  const filtered = accounts.filter(account=>[account.profile.name,account.email,account.username,account.linkedProfileId,account.profile.license].some(value=>String(value||'').toLowerCase().includes(query.toLowerCase())))
  const openEdit=account=>setModal({ account, profile:account.profile })
  const save=values=>{
    if (data.users.some(account=>account.userId!==modal?.account?.userId&&(account.email.toLowerCase()===values.email.toLowerCase()||account.username.toLowerCase()===values.username.toLowerCase()))) return {error:'That email or username is already assigned to another passenger, driver or administrator.'}
    if (isDriver&&data.drivers.some(driver=>driver.id!==modal?.account?.linkedProfileId&&driver.license?.toLowerCase()===values.license.toLowerCase())) return {error:'That licence number is already assigned to a driver.'}
    if (modal?.account) {
      if(values.accountStatus==='DISABLED'&&modal.account.userId===currentUser?.userId)return {error:'You cannot disable the account you are currently using.'}
      if(values.accountStatus==='DISABLED'&&modal.account.role==='SUPER_ADMIN'&&data.users.filter(user=>user.role==='SUPER_ADMIN'&&user.accountStatus==='ACTIVE').length<=1)return {error:'The last active SUPER_ADMIN cannot be disabled.'}
      const accountChanges={email:values.email,username:values.username,accountStatus:values.accountStatus}
      const result=updateAccountAndProfile(modal.account.userId,modal.account.role,values,accountChanges)
      if(result?.error)return result
      setNotice(`${isDriver?'Driver':'Administrator'} account updated.`)
    } else {
      const result=createProvisionedAccount({role:isDriver?'DRIVER':'ADMIN',profile:values,accountStatus:values.accountStatus})
      if (result.error) return result
      setNotice(`${isDriver?'Driver':'Administrator'} account created. Username: ${values.username}. ${values.accountStatus==='ACTIVE'?'Demo sign-in is available; no password was generated or stored.':'The account is disabled until reactivated; no password was generated or stored.'}`)
    }
    setModal(null)
    return null
  }
  const toggle=account=>{
    const next=account.accountStatus==='ACTIVE'?'DISABLED':'ACTIVE'
    if(account.userId===currentUser?.userId&&next==='DISABLED'){setNotice('You cannot disable the account you are currently using.');return}
    if(account.role==='SUPER_ADMIN'&&next==='DISABLED'){
      const activeCount=data.users.filter(user=>user.role==='SUPER_ADMIN'&&user.accountStatus==='ACTIVE').length
      if(activeCount<=1){setNotice('The last active SUPER_ADMIN cannot be disabled.');return}
    }
    const label=isDriver?'driver account':'administrator account'
    if(!window.confirm(`${next==='DISABLED'?'Disable':'Reactivate'} this ${label}? ${next==='DISABLED'?'It will no longer be able to sign in.':'It will be able to sign in again.'}`))return
    updateAccountAndProfile(account.userId,account.role,{accountStatus:next},{accountStatus:next})
    setNotice(`${account.profile.name}'s account is now ${next.toLowerCase()}.`)
  }
  const removeDriver=account=>{
    if(!window.confirm(`Delete ${account.profile.name}'s unassigned demo driver profile and login account?`))return
    const result=deleteProvisionedDriver(account.userId)
    setNotice(result?.error||'Unassigned driver profile and demo login account deleted.')
  }
  return <div className="page-content"><div className="page-heading"><div><div className="eyebrow">SMARTMOVE ACCESS CONTROL · DEMO</div><h1>{isDriver?'Drivers':'Admin Management'}</h1><p>{isDriver?'Manage driver profiles and provision unified demo accounts.':'Create and manage administrator accounts and account access.'}</p></div><button className="button button-primary" onClick={()=>setModal({account:null,profile:null})}><Plus size={17}/>{isDriver?'Add Driver':'Add Administrator'}</button></div>
    {!isDriver&&<div className="info-banner"><ShieldCheck size={15}/> SUPER_ADMIN-only demo section. Admin management permissions must be enforced by the future backend.</div>}
    {isDriver&&<div className="account-activation-note page-activation"><ShieldCheck size={16}/><span>New drivers are provisioned by an administrator, never public registration. New accounts are demo-activated without creating or storing a password.</span></div>}
    {notice&&<div className="notice-banner" role="status"><span>{notice}</span><button onClick={()=>setNotice('')} aria-label="Dismiss"><X size={16}/></button></div>}
    <div className="account-summary-stats"><div><span>{isDriver?'Driver profiles':'Administrator accounts'}</span><strong>{accounts.length}</strong></div><div><span>Active accounts</span><strong>{accounts.filter(account=>account.accountStatus==='ACTIVE').length}</strong></div><div><span>Disabled accounts</span><strong>{accounts.filter(account=>account.accountStatus!=='ACTIVE').length}</strong></div></div>
    <section className="table-card account-table-card"><div className="table-toolbar"><label className="search-box"><Search size={16}/><input aria-label="Search accounts" placeholder={`Search ${isDriver?'drivers':'administrators'} by name, email or username...`} value={query} onChange={event=>setQuery(event.target.value)}/></label><span className="record-count">{filtered.length} account{filtered.length===1?'':'s'}</span></div><div className="table-scroll"><table><thead><tr><th>{isDriver?'DRIVER':'ADMINISTRATOR'}</th><th>EMAIL</th><th>USERNAME</th><th>PHONE</th>{isDriver&&<><th>LICENCE</th><th>EXPERIENCE</th><th>DRIVER STATUS</th></>}<th>ROLE</th><th>ACCOUNT STATUS</th><th className="actions-heading">ACTIONS</th></tr></thead><tbody>{filtered.map(account=><tr key={account.userId}><td><strong className="account-name-cell">{account.profile.name}</strong><small className="account-id-cell">{account.linkedProfileId}</small></td><td>{account.email}</td><td><span className="table-id">{account.username}</span></td><td>{account.profile.phone||'—'}</td>{isDriver&&<><td>{account.profile.license||'—'}</td><td>{account.profile.experience??0} years</td><td>{account.profile.status||'—'}</td></>}<td><span className={`role-chip ${account.role==='SUPER_ADMIN'?'super-admin':''}`}>{account.role.replace('_',' ')}</span></td><td><span className={`status-badge ${account.accountStatus==='ACTIVE'?'status-active':'status-unavailable'}`}>{account.accountStatus==='ACTIVE'?'Active':'Disabled'}</span></td><td><div className="row-actions"><button type="button" className="icon-button" onClick={()=>openEdit(account)} aria-label={`Edit ${account.profile.name}`} title="Edit account"><Pencil size={15}/></button><button type="button" className={`account-toggle ${account.accountStatus==='ACTIVE'?'disable':'enable'}`} onClick={()=>toggle(account)}>{account.accountStatus==='ACTIVE'?<><X size={13}/>Disable</>:<><RefreshCw size={13}/>Enable</>}</button>{isDriver&&<button type="button" className="icon-button danger-icon" onClick={()=>removeDriver(account)} aria-label={`Delete unassigned driver ${account.profile.name}`} title="Delete unassigned driver"><Trash2 size={15}/></button>}</div></td></tr>)}</tbody></table>{filtered.length===0&&<div className="empty-state"><div className="empty-icon"><UserRoundCog size={20}/></div><strong>{query?'No matching accounts':'No accounts yet'}</strong><p>{query?'Try another name, email or username.':'Create an account to see it listed here.'}</p><button className="button button-primary" onClick={()=>setModal({account:null,profile:null})}><UserPlus size={15}/>Add account</button></div>}</div><div className="table-footer">Account profile fields and role links are held in the shared local demo state.<span>No password or secret is stored.</span></div></section>
    {modal&&<AccountForm kind={kind} initial={modal.profile?{...modal.profile,email:modal.account.email,username:modal.account.username,accountStatus:modal.account.accountStatus}:null} onClose={()=>setModal(null)} onSave={save}/>}
  </div>
}
