import { Bell, Menu, Search, ChevronDown } from 'lucide-react'
import { useLocation } from 'react-router-dom'

const titles = { '/': 'Dashboard', '/vehicles': 'Vehicles', '/drivers': 'Drivers', '/routes': 'Routes', '/trips': 'Trips', '/passengers': 'Passengers', '/bookings': 'Bookings', '/payments': 'Payments', '/maintenance': 'Maintenance', '/reviews': 'Reviews', '/announcements': 'Announcements', '/reports': 'Reports' }
export default function Navbar({ onMenu }) {
  const location = useLocation()
  const title = titles[location.pathname] || 'Dashboard'
  return <header className="topbar"><div className="topbar-left"><button className="menu-button" onClick={onMenu} aria-label="Open navigation"><Menu size={21} /></button><div className="breadcrumb">Workspace <span>/</span> <strong>{title}</strong></div></div><div className="topbar-actions"><label className="global-search"><Search size={16} /><input aria-label="Quick search" placeholder="Search anything..." /><kbd>⌘ K</kbd></label><button className="notification-button" aria-label="Notifications"><Bell size={19} /><i /></button><div className="topbar-divider" /><button className="profile-menu"><div className="avatar avatar-light">AS</div><span>Amara Silva</span><ChevronDown size={15} /></button></div></header>
}
