import { useState } from 'react'
import { NavLink, Outlet, Link } from 'react-router-dom'
import { BusFront, LayoutDashboard, Search, Ticket, Wallet, Star, Megaphone, UserRound, CalendarDays, ClipboardList, CircleAlert, Menu, X, LogOut, ChevronDown, Route, Clock3 } from 'lucide-react'
import { useAppData } from '../../services/useAppData'

const passengerLinks = [
  { label: 'Overview', to: '/passenger', icon: LayoutDashboard, end: true },
  { label: 'Search trips', to: '/passenger/search', icon: Search },
  { label: 'My bookings', to: '/passenger/bookings', icon: Ticket },
  { label: 'My tickets', to: '/passenger/tickets', icon: ClipboardList },
  { label: 'Payments', to: '/passenger/payments', icon: Wallet },
  { label: 'Reviews', to: '/passenger/reviews', icon: Star },
  { label: 'Announcements', to: '/passenger/announcements', icon: Megaphone },
  { label: 'My profile', to: '/passenger/profile', icon: UserRound },
]
const driverLinks = [
  { label: 'Overview', to: '/driver', icon: LayoutDashboard, end: true },
  { label: 'My trips', to: '/driver/trips', icon: Route },
  { label: 'Trip status', to: '/driver/trip-status', icon: Clock3 },
  { label: 'Schedule', to: '/driver/schedule', icon: CalendarDays },
  { label: 'My vehicle', to: '/driver/vehicle', icon: BusFront },
  { label: 'Report an issue', to: '/driver/issues', icon: CircleAlert },
  { label: 'Announcements', to: '/driver/announcements', icon: Megaphone },
  { label: 'My profile', to: '/driver/profile', icon: UserRound },
]

export function PortalShell({ role, children }) {
  const [open, setOpen] = useState(false)
  const { data, currentUser, signOut } = useAppData()
  const driver = data.drivers.find(item => item.id === currentUser?.linkedProfileId) || data.drivers[0]
  const passenger = data.passengers.find(item => item.id === currentUser?.linkedProfileId) || data.passengers[0]
  const user = role === 'driver' ? driver : passenger
  const links = role === 'driver' ? driverLinks : passengerLinks
  const logout = () => { signOut(); window.location.assign('/') }
  return <div className="portal-shell">
    <div className={`portal-scrim ${open ? 'open' : ''}`} onClick={() => setOpen(false)} />
    <aside className={`portal-sidebar ${open ? 'open' : ''}`}>
      <div className="portal-brand"><Link className="portal-brand-link" to="/"><span className="portal-brand-icon"><BusFront size={21}/></span><span><strong>SmartMove</strong><small>{role === 'driver' ? 'DRIVER PORTAL' : 'PASSENGER PORTAL'}</small></span></Link><button className="portal-close" onClick={() => setOpen(false)} aria-label="Close menu"><X size={18}/></button></div>
      <div className="portal-nav-label">YOUR WORKSPACE</div><nav className="portal-nav">{links.map(item => <NavLink onClick={() => setOpen(false)} key={item.to} to={item.to} end={item.end} className={({isActive}) => `portal-link ${isActive ? 'active' : ''}`}><item.icon size={18}/>{item.label}</NavLink>)}</nav>
      <div className="portal-sidebar-bottom"><div className="portal-tip"><span>✦ A smoother trip starts here</span><small>{role === 'driver' ? 'Safe journeys begin with good preparation.' : 'Your next Sri Lankan adventure is waiting.'}</small></div><div className="portal-user"><span className="portal-avatar">{user?.name?.split(' ').map(part => part[0]).slice(0,2).join('') || 'SM'}</span><span className="portal-user-meta"><strong>{user?.name || 'Demo user'}</strong><small>{role === 'driver' ? 'Driver account' : 'Passenger account'}</small></span><button onClick={logout} title="Sign out" aria-label="Sign out"><LogOut size={16}/></button></div></div>
    </aside>
    <section className="portal-main"><header className="portal-topbar"><div className="portal-top-left"><button className="portal-menu" onClick={() => setOpen(true)} aria-label="Open menu"><Menu size={21}/></button><span className="portal-breadcrumb">SmartMove <i>/</i> {role === 'driver' ? 'Driver workspace' : 'Passenger workspace'}</span></div><div className="portal-top-right"><span className="demo-pill"><i/> Demo mode</span><Link className="role-switch" to="/login?switch=1">Switch role <ChevronDown size={14}/></Link><span className="portal-avatar small">{user?.name?.split(' ').map(part => part[0]).slice(0,2).join('') || 'SM'}</span></div></header><main className="portal-content">{children || <Outlet/>}</main><footer className="portal-footer"><span>SmartMove Transport Solutions</span><span>Demo workspace · data saved in this browser</span></footer></section>
  </div>
}
