import { Bell, Menu, Search, ChevronDown } from 'lucide-react'
import { useLocation } from 'react-router-dom'
import { useAppData } from '../services/useAppData'

const titles = { '/admin': 'Dashboard', '/admin/vehicles': 'Vehicles', '/admin/drivers': 'Drivers', '/admin/routes': 'Routes', '/admin/trips': 'Trips', '/admin/passengers': 'Passengers', '/admin/bookings': 'Bookings', '/admin/payments': 'Payments', '/admin/maintenance': 'Maintenance', '/admin/reviews': 'Reviews', '/admin/announcements': 'Announcements', '/admin/reports': 'Reports', '/admin/admin-management': 'Admin Management' }
export default function Navbar({ onMenu }) {
  const location = useLocation()
  const { data, currentUser } = useAppData()
  const title = titles[location.pathname] || 'Dashboard'
  const profile = data.admins.find(admin => admin.id === currentUser?.linkedProfileId)
  const name = profile?.name || 'Administrator'
  const initials = name.split(' ').map(part => part[0]).slice(0, 2).join('')
  return <header className="topbar"><div className="topbar-left"><button className="menu-button" onClick={onMenu} aria-label="Open navigation"><Menu size={21} /></button><div className="breadcrumb">Workspace <span>/</span> <strong>{title}</strong></div></div><div className="topbar-actions"><label className="global-search"><Search size={16} /><input aria-label="Quick search" placeholder="Search anything..." /><kbd>⌘ K</kbd></label><button className="notification-button" aria-label="Notifications"><Bell size={19} /><i /></button><div className="topbar-divider" /><button className="profile-menu"><div className="avatar avatar-light">{initials}</div><span>{name}</span><ChevronDown size={15} /></button></div></header>
}
