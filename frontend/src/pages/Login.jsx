import { useState } from 'react'
import { Link, Navigate, useNavigate, useSearchParams } from 'react-router-dom'
import { ArrowLeft, ArrowRight, BusFront, Eye, EyeOff, ShieldCheck, UserRound, UsersRound } from 'lucide-react'
import { useAppData } from '../services/useAppData'

export default function Login() {
  const { data, session, signIn } = useAppData()
  const [params] = useSearchParams()
  const navigate = useNavigate()
  const [role, setRole] = useState(params.get('role') || 'passenger')
  const [identity, setIdentity] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const allowSwitch = params.get('switch') === '1'
  const next = params.get('next') || (role==='admin'?'/admin':role==='driver'?'/driver':'/passenger')
  if (session && !allowSwitch) return <Navigate to={session.role==='admin'?'/admin':session.role==='driver'?'/driver':'/passenger'} replace/>
  const submit = event => {
    event.preventDefault(); setError('')
    if (!identity.trim() || !password) return setError('Enter the required demo fields to continue.')
    setBusy(true)
    window.setTimeout(() => {
      if (role === 'passenger') {
        const found = data.passengers.find(person => person.email.toLowerCase() === identity.trim().toLowerCase())
        if (!found) { setError('We could not find this demo passenger. Register a passenger profile or use a sample listed below.'); setBusy(false); return }
        signIn('passenger', found.id)
      } else if (role === 'driver') {
        const found = data.drivers.find(person => person.id === identity || person.name.toLowerCase() === identity.toLowerCase())
        if (!found) { setError('Choose an existing provisioned demo driver. Driver accounts cannot self-register.'); setBusy(false); return }
        signIn('driver', found.id)
      } else {
        if (!['admin@smartmove.lk','admin'].includes(identity.toLowerCase())) { setError('Use the demo admin identity shown below.'); setBusy(false); return }
        signIn('admin', 'admin-demo')
      }
      navigate(next, { replace: true }); setBusy(false)
    }, 240)
  }
  return <main className="auth-page"><aside className="auth-art"><div className="auth-art-image"/><Link className="auth-brand" to="/"><span><BusFront size={21}/></span>smart<span>move</span></Link><div className="auth-art-copy"><span>THE ISLAND IS YOURS</span><h1>There’s a<br/>beautiful road<br/><em>ahead.</em></h1><p>Good journeys begin with one simple step.</p></div><div className="auth-art-foot">SRI LANKA · MADE FOR THE JOURNEY</div></aside><section className="auth-main"><Link className="auth-back" to="/"><ArrowLeft size={15}/> Back to SmartMove</Link><div className="auth-box"><div className="auth-mobile-logo"><span><BusFront size={20}/></span> smartmove</div><span className="auth-kicker">WELCOME BACK</span><h2>Let’s get you<br/>on your way.</h2><p className="auth-subtitle">Sign in to your SmartMove demo workspace.</p><div className="role-tabs" role="tablist" aria-label="Choose demo role">{[{id:'passenger',label:'Passenger',icon:UserRound},{id:'driver',label:'Driver',icon:BusFront},{id:'admin',label:'Admin',icon:ShieldCheck}].map(item=><button type="button" role="tab" aria-selected={role===item.id} className={role===item.id?'selected':''} key={item.id} onClick={()=>{setRole(item.id);setIdentity('');setError('')}}><item.icon size={15}/>{item.label}</button>)}</div><form className="auth-form" onSubmit={submit}><label>{role==='driver'?'Demo driver':'Email address'}{role==='driver'?<select value={identity} onChange={event=>setIdentity(event.target.value)} required><option value="">Choose driver profile</option>{data.drivers.map(driver=><option key={driver.id} value={driver.id}>{driver.name} · {driver.id}</option>)}</select>:<input type="text" autoComplete="username" placeholder={role==='admin'?'admin@smartmove.lk':'you@example.com'} value={identity} onChange={event=>setIdentity(event.target.value)} required/>}</label><label>Demo password<div className="password-field"><input type={showPassword?'text':'password'} autoComplete="current-password" placeholder="Enter any demo text" value={password} onChange={event=>setPassword(event.target.value)} required/><button type="button" onClick={()=>setShowPassword(!showPassword)} aria-label={showPassword?'Hide password':'Show password'}>{showPassword?<EyeOff size={17}/>:<Eye size={17}/>}</button></div></label>{error&&<div className="auth-error" role="alert">{error}</div>}<button className="auth-submit" disabled={busy}>{busy?'Opening your workspace…':<>Sign in <ArrowRight size={16}/></>}</button></form><div className="demo-disclaimer"><ShieldCheck size={16}/><p><strong>Demo access, not secure authentication.</strong> Password text is only checked for presence and is never saved. Any non-empty value works with a listed demo identity.</p></div>{role==='passenger'&&<div className="auth-signup">New to SmartMove? <Link to={`/register${params.get('next')?`?next=${encodeURIComponent(params.get('next'))}`:''}`}>Create a passenger account</Link></div>}{role==='driver'&&<p className="driver-provision-note"><UsersRound size={15}/> Driver profiles are provisioned by an administrator. Self-registration is not available.</p>}<div className="auth-demo-users"><span>DEMO IDENTITIES</span>{role==='passenger'?<button type="button" onClick={()=>setIdentity(data.passengers[0]?.email||'')}>{data.passengers[0]?.email} <small>Sample passenger</small></button>:role==='driver'?<p>Select one of the provisioned drivers above.</p>:<button type="button" onClick={()=>setIdentity('admin@smartmove.lk')}>admin@smartmove.lk <small>Sample administrator</small></button>}</div></div><footer className="auth-footer">© 2026 SmartMove Transport Solutions <span>Demo experience · Not a security boundary</span></footer></section></main>
}
