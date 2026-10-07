import { Navigate, useLocation } from 'react-router-dom'
import { useAppData } from '../../services/useAppData'

export default function RouteGuard({ role, children, superAdminOnly = false }) {
  const { session, currentUser } = useAppData()
  const location = useLocation()
  if (!session) return <Navigate to={`/login?next=${encodeURIComponent(location.pathname + location.search)}`} replace />
  if (!currentUser || currentUser.accountStatus !== 'ACTIVE') return <Navigate to="/login" replace />
  const isAdminRole = currentUser.role === 'ADMIN' || currentUser.role === 'SUPER_ADMIN'
  const roleMatches = role === 'admin' ? isAdminRole : currentUser.role === role.toUpperCase()
  if (!roleMatches) {
    const destination = isAdminRole ? '/admin' : currentUser.role === 'DRIVER' ? '/driver' : '/passenger'
    return <Navigate to={destination} replace />
  }
  if (superAdminOnly && currentUser.role !== 'SUPER_ADMIN') return <Navigate to="/admin" replace />
  return children
}
