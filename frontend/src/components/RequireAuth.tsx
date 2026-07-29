import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

export function RequireAuth() {
  const { user, status } = useAuth()
  const location = useLocation()

  if (status === 'loading') {
    return (
      <div className="session-loading" role="status" aria-live="polite">
        <span className="session-spinner" />
        <strong>Verifying your secure session…</strong>
      </div>
    )
  }
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />
  if (!user.profileCompleted && location.pathname !== '/profile/setup') {
    return <Navigate to="/profile/setup" replace />
  }
  return <Outlet />
}
