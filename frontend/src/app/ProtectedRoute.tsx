import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '@/features/auth/useAuth'

export function ProtectedRoute() {
  const { user, loading } = useAuth()
  const location = useLocation()

  if (loading) {
    return (
      <main className="auth-loading" aria-live="polite">
        <span className="brand-mark" aria-hidden="true">
          i
        </span>
        <p>Verificando sua sessão…</p>
      </main>
    )
  }
  if (!user)
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  return <Outlet />
}
