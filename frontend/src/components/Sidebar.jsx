import { NavLink } from 'react-router-dom'
import { LayoutDashboard, BusFront, Users, Route, Navigation, ContactRound, TicketCheck, Wallet, Wrench, Star, Megaphone, ChartNoAxesCombined, X, UserRoundCog, LogOut } from 'lucide-react'
import { useAppData } from '../services/useAppData'

const links = [
  { label: 'Dashboard', to: '/admin', icon: LayoutDashboard },
  { label: 'FLEET OPERATIONS', section: true },
  { label: 'Vehicles', to: '/admin/vehicles', icon: BusFront }, { label: 'Drivers', to: '/admin/drivers', icon: Users }, { label: 'Routes', to: '/admin/routes', icon: Route }, { label: 'Trips', to: '/admin/trips', icon: Navigation },
  { label: 'CUSTOMER & SALES', section: true },
  { label: 'Passengers', to: '/admin/passengers', icon: ContactRound }, { label: 'Bookings', to: '/admin/bookings', icon: TicketCheck }, { label: 'Payments', to: '/admin/payments', icon: Wallet },
  { label: 'SERVICE & INSIGHTS', section: true },
  { label: 'Maintenance', to: '/admin/maintenance', icon: Wrench }, { label: 'Reviews', to: '/admin/reviews', icon: Star }, { label: 'Announcements', to: '/admin/announcements', icon: Megaphone }, { label: 'Reports', to: '/admin/reports', icon: ChartNoAxesCombined },
]

export default function Sidebar({ open, onClose }) {
  const { currentUser, data, signOut } = useAppData()
  const admin = data.admins.find(profile => profile.id === currentUser?.linkedProfileId)
  const showAdminManagement = currentUser?.role === 'SUPER_ADMIN'
  const navLinks = showAdminManagement ? [...links, { label: 'ACCESS MANAGEMENT', section: true }, { label: 'Admin Management', to: '/admin/admin-management', icon: UserRoundCog }] : links
  return <><div className={`sidebar-scrim ${open ? 'visible' : ''}`} onClick={onClose} /><aside className={`sidebar ${open ? 'sidebar-open' : ''}`}><div className="brand"><div className="brand-mark"><BusFront size={21} /></div><div><strong>SmartMove</strong><span>TRANSPORT SOLUTIONS</span></div><button className="mobile-close" onClick={onClose} aria-label="Close navigation"><X size={19} /></button></div><div className="workspace-pill"><span className="workspace-dot" /> Operations workspace <span className="chevron">⌄</span></div><nav className="side-nav">{navLinks.map(item => item.section ? <div className="nav-section" key={item.label}>{item.label}</div> : <NavLink onClick={onClose} key={item.to} to={item.to} end={item.to === '/admin'} className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}><item.icon size={18} strokeWidth={1.8} /><span>{item.label}</span>{item.label === 'Reviews' && <span className="nav-count">3</span>}</NavLink>)}</nav><div className="sidebar-bottom"><div className="help-card"><div className="help-icon">✦</div><strong>Need a hand?</strong><p>Our team is here to help you keep things moving.</p><button type="button" onClick={() => window.alert('Support is available at support@smartmove.lk')}>Contact support <span>↗</span></button></div><div className="sidebar-user"><div className="avatar avatar-green">{admin?.name?.split(' ').map(part=>part[0]).slice(0,2).join('')||'AS'}</div><div><strong>{admin?.name||'Administrator'}</strong><span>{currentUser?.role?.replace('_',' ')||'Administrator'}</span></div><button type="button" className="admin-signout" onClick={()=>{signOut();window.location.assign('/')}} aria-label="Sign out" title="Sign out"><LogOut size={15}/></button></div></div></aside></>
}
