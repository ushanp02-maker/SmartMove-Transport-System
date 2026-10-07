import { NavLink } from 'react-router-dom'
import { LayoutDashboard, BusFront, Users, Route, Navigation, ContactRound, TicketCheck, Wallet, Wrench, Star, Megaphone, ChartNoAxesCombined, X } from 'lucide-react'

const links = [
  { label: 'Dashboard', to: '/', icon: LayoutDashboard },
  { label: 'FLEET OPERATIONS', section: true },
  { label: 'Vehicles', to: '/vehicles', icon: BusFront }, { label: 'Drivers', to: '/drivers', icon: Users }, { label: 'Routes', to: '/routes', icon: Route }, { label: 'Trips', to: '/trips', icon: Navigation },
  { label: 'CUSTOMER & SALES', section: true },
  { label: 'Passengers', to: '/passengers', icon: ContactRound }, { label: 'Bookings', to: '/bookings', icon: TicketCheck }, { label: 'Payments', to: '/payments', icon: Wallet },
  { label: 'SERVICE & INSIGHTS', section: true },
  { label: 'Maintenance', to: '/maintenance', icon: Wrench }, { label: 'Reviews', to: '/reviews', icon: Star }, { label: 'Announcements', to: '/announcements', icon: Megaphone }, { label: 'Reports', to: '/reports', icon: ChartNoAxesCombined },
]

export default function Sidebar({ open, onClose }) {
  return <><div className={`sidebar-scrim ${open ? 'visible' : ''}`} onClick={onClose} /><aside className={`sidebar ${open ? 'sidebar-open' : ''}`}><div className="brand"><div className="brand-mark"><BusFront size={21} /></div><div><strong>SmartMove</strong><span>TRANSPORT SOLUTIONS</span></div><button className="mobile-close" onClick={onClose} aria-label="Close navigation"><X size={19} /></button></div><div className="workspace-pill"><span className="workspace-dot" /> Operations workspace <span className="chevron">⌄</span></div><nav className="side-nav">{links.map((item, index) => item.section ? <div className="nav-section" key={item.label}>{item.label}</div> : <NavLink onClick={onClose} key={item.to} to={item.to} end={index === 0} className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}><item.icon size={18} strokeWidth={1.8} /><span>{item.label}</span>{item.label === 'Reviews' && <span className="nav-count">3</span>}</NavLink>)}</nav><div className="sidebar-bottom"><div className="help-card"><div className="help-icon">✦</div><strong>Need a hand?</strong><p>Our team is here to help you keep things moving.</p><button type="button" onClick={() => window.alert('Support is available at support@smartmove.lk')}>Contact support <span>↗</span></button></div><div className="sidebar-user"><div className="avatar avatar-green">AS</div><div><strong>Amara Silva</strong><span>Administrator</span></div><span className="more-dots">···</span></div></div></aside></>
}
