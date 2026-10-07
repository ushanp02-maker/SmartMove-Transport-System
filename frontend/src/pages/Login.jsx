import { useState } from 'react'
import { Link, Navigate, useNavigate, useSearchParams } from 'react-router-dom'
import { ArrowLeft, ArrowRight, BusFront, Eye, EyeOff, ShieldCheck } from 'lucide-react'
import { useAppData } from '../services/useAppData'

const accountHome = role => role === 'PASSENGER' ? '/passenger' : role === 'DRIVER' ? '/driver' : '/admin'

export default function Login() {
  const { session, currentUser, demoAuthenticate } = useAppData()
  const [params] = useSearchParams()
  const navigate = useNavigate()
  const [identity, setIdentity] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [demoConsent, setDemoConsent] = useState(false)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const allowSwitch = params.get('switch') === '1'
  const next = params.get('next') || ''

  if (session && currentUser?.accountStatus === 'ACTIVE' && !allowSwitch && !busy) {
    return <Navigate to={accountHome(currentUser.role)} replace />
  }

  const submit = event => {
    event.preventDefault()
    setError('')
    if (!identity.trim() || !password) return setError('Enter your email or username and password text to continue.')
    if (!demoConsent) return setError('Confirm the demo authentication notice before continuing.')
    setBusy(true)
    window.setTimeout(() => {
      const result = demoAuthenticate(identity, true)
      if (result.error) {
        setError(result.error)
        setBusy(false)
        return
      }
      const account = result.account
      const destination = account.role === 'PASSENGER' && next.startsWith('/passenger/') ? next : accountHome(account.role)
      navigate(destination, { replace: true })
    }, 180)
  }

  return <main className="auth-page">
    <aside className="auth-art">
      <div className="auth-art-image" />
      <Link className="auth-brand" to="/"><span><BusFront size={21}/></span>smart<span>move</span></Link>
      <div className="auth-art-copy"><span>THE ISLAND IS YOURS</span><h1>There’s a<br/>beautiful road<br/><em>ahead.</em></h1><p>Good journeys begin with one simple step.</p></div>
      <div className="auth-art-foot">SRI LANKA · MADE FOR THE JOURNEY</div>
    </aside>
    <section className="auth-main">
      <Link className="auth-back" to="/"><ArrowLeft size={15}/> Back to SmartMove</Link>
      <div className="auth-box">
        <div className="auth-mobile-logo"><span><BusFront size={20}/></span> smartmove</div>
        <span className="auth-kicker">WELCOME TO SMARTMOVE</span>
        <h2>Let’s get you<br/>on your way.</h2>
        <p className="auth-subtitle">Sign in with your account email or username.</p>
        {allowSwitch && session && <div className="account-rule"><ShieldCheck size={15}/><span>Switch demo account. Your next sign-in will use the account you enter below.</span></div>}
        <form className="auth-form" onSubmit={submit}>
          <label>Email address or username<input type="text" autoComplete="username" placeholder="you@example.com or username" value={identity} onChange={event=>setIdentity(event.target.value)} required/></label>
          <label>Password<div className="password-field"><input type={showPassword?'text':'password'} autoComplete="current-password" placeholder="Demo sign-in does not verify passwords" value={password} onChange={event=>setPassword(event.target.value)} required/><button type="button" onClick={()=>setShowPassword(!showPassword)} aria-label={showPassword?'Hide password':'Show password'}>{showPassword?<EyeOff size={17}/>:<Eye size={17}/>}</button></div></label>
          <div className="auth-form-links"><button type="button" onClick={()=>setError('Password reset is unavailable in demo mode. No passwords are stored.')}>Forgot password?</button><small>Unavailable in demo mode</small></div>
          <label className="demo-consent"><input type="checkbox" checked={demoConsent} onChange={event=>setDemoConsent(event.target.checked)}/><span>I understand this is demo authentication. The password field is required for the form but is <strong>not checked or saved</strong>.</span></label>
          {error&&<div className="auth-error" role="alert">{error}</div>}
          <button className="auth-submit" disabled={busy}>{busy?'Checking demo account…':<>Sign in <ArrowRight size={16}/></>}</button>
        </form>
        <div className="demo-disclaimer"><ShieldCheck size={16}/><p><strong>Demo access, not secure authentication.</strong> Role is resolved from the linked account. Backend password-hash verification and authorization are not connected.</p></div>
        <div className="auth-signup">New to SmartMove? <Link to={`/register${next?`?next=${encodeURIComponent(next)}`:''}`}>Create a passenger account</Link></div>
      </div>
      <footer className="auth-footer">© 2026 SmartMove Transport Solutions <span>Demo experience · Not a security boundary</span></footer>
    </section>
  </main>
}
