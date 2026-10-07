import { Navigate, useLocation } from 'react-router-dom'
import { useAppData } from '../../services/useAppData'

export default function RouteGuard({ role, children }) {
  const { session } = useAppData()
  const location = useLocation()
  if (!session) return <Navigate to={`/login?next=${encodeURIComponent(location.pathname + location.search)}`} replace />
  if (session.role !== role) {
    const destination = session.role === 'admin' ? '/admin' : session.role === 'driver' ? '/driver' : '/passenger'
    return <Navigate to={destination} replace />
  }
  return children
}
