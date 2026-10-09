import { useState } from 'react'
import { Link, Navigate, useNavigate, useSearchParams } from 'react-router-dom'
import { ArrowLeft, ArrowRight, BusFront, Check, Eye, EyeOff, UserPlus } from 'lucide-react'
import { useAppData } from '../services/useAppData'
import { isApiMode } from '../services/apiClient'

export default function Register() {
  const { session, currentUser, data, createPassengerAccount, registerRealPassenger } = useAppData()
  const apiMode = isApiMode()
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const [values, setValues] = useState({name:'',email:'',username:'',phone:'',password:'',confirm:''})
  const [show, setShow] = useState(false)
  const [error, setError] = useState('')
  const [busy,setBusy] = useState(false)
  const requestedNext = params.get('next') || ''
  const next = requestedNext.startsWith('/passenger/book/') || requestedNext.startsWith('/passenger/custom-trips') || requestedNext.startsWith('/passenger/tracking') ? requestedNext : '/passenger'
  if (session && currentUser && !busy) return <Navigate to={currentUser.role==='SUPER_ADMIN'||currentUser.role==='ADMIN'?'/admin':currentUser.role==='DRIVER'?'/driver':'/passenger'} replace/>
  const change=event=>setValues({...values,[event.target.name]:event.target.value})
  const submit = async event => {
    event.preventDefault()
    if (busy) return
    setError('')
    if (values.name.trim().length < 2) return setError('Enter your full name.')
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(values.email)) return setError('Enter a valid email address.')
    if (!/^[a-zA-Z0-9._-]{3,24}$/.test(values.username.trim())) return setError('Username must be 3–24 characters: letters, numbers, dot, underscore or hyphen.')
    if (!/^\+?[0-9 ()-]{7,18}$/.test(values.phone)) return setError('Enter a valid phone number.')
    if (values.password.length < 8) return setError('Use a password of at least 8 characters.')
    if (values.password !== values.confirm) return setError('Your passwords do not match.')
    if (!apiMode && data.users.some(user =>
      user.email.toLowerCase() === values.email.trim().toLowerCase() ||
      user.username.toLowerCase() === values.username.trim().toLowerCase()
    )) return setError('That email or username is already registered.')
    setBusy(true)
    try {
      const result = apiMode
        ? await registerRealPassenger({
            fullName: values.name,
            username: values.username,
            email: values.email,
            phone: values.phone,
            password: values.password,
            city: 'Colombo',
          })
        : createPassengerAccount(values)
      if (result.error) {
        setError(result.error)
        return
      }
      navigate(apiMode
        ? `/login?next=${encodeURIComponent(next)}`
        : next, { replace: true })
    } catch (error) {
      setError(error.message || 'Registration failed.')
    } finally {
      setBusy(false)
    }
  }

  return <main className="auth-page"><aside className="auth-art"><div className="auth-art-image"/><Link className="auth-brand" to="/"><span><BusFront size={21}/></span>smart<span>move</span></Link><div className="auth-art-copy"><span>A SEAT FOR EVERY STORY</span><h1>More island.<br/>More moments.<br/><em>Your way.</em></h1><p>Create an account and keep your journeys together.</p></div><div className="auth-art-foot">PASSENGER MEMBERSHIP</div></aside><section className="auth-main"><Link className="auth-back" to="/"><ArrowLeft size={15}/> Back to SmartMove</Link><div className="auth-box register-box"><div className="auth-mobile-logo"><span><BusFront size={20}/></span> smartmove</div><span className="auth-kicker">A BETTER WAY TO GO</span><h2>Let’s make it<br/>official.</h2><p className="auth-subtitle">Create a passenger profile to save a seat.</p><div className="account-rule"><Check size={16}/><span>Passenger profiles can self-register. Driver and administrator access is provisioned separately.</span></div><form className="auth-form" onSubmit={submit}><label>Full name<input name="name" autoComplete="name" placeholder="Your name" value={values.name} onChange={change} required/></label><label>Email address<input type="email" name="email" autoComplete="email" placeholder="you@example.com" value={values.email} onChange={change} required/></label><label>Username<input name="username" autoComplete="username" placeholder="Choose a username" value={values.username} onChange={change} required/></label><label>Phone number<input type="tel" name="phone" autoComplete="tel" placeholder="+94 77 123 4567" value={values.phone} onChange={change} required/></label><label>Password<div className="password-field"><input type={show?'text':'password'} name="password" autoComplete="new-password" placeholder="At least 8 characters" value={values.password} onChange={change} required minLength={8}/><button type="button" onClick={()=>setShow(!show)} aria-label={show?'Hide password':'Show password'}>{show?<EyeOff size={17}/>:<Eye size={17}/>}</button></div></label><label>Confirm password<input type={show?'text':'password'} name="confirm" autoComplete="new-password" placeholder="Type it once more" value={values.confirm} onChange={change} required/></label>{error&&<div className="auth-error" role="alert">{error}</div>}<button className="auth-submit" disabled={busy}>{busy?'Creating profile…':<>Create passenger account <ArrowRight size={16}/></>}</button></form><div className="demo-disclaimer"><UserPlus size={16}/><p>{apiMode ? "Your passenger account will be registered in SmartMove. Sign in after registration to continue." : "Demo profile only. Passwords are not verified in demo mode."}</p></div><div className="auth-signup">Already have a profile? <Link to={`/login${requestedNext?`?next=${encodeURIComponent(requestedNext)}`:''}`}>Sign in</Link></div></div><footer className="auth-footer">© 2026 SmartMove Transport Solutions <span>{apiMode ? "SmartMove passenger registration" : "Demo experience"}</span></footer></section></main>
}
