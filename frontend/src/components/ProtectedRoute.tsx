import { Navigate, Outlet, useLocation } from 'react-router-dom'
import type { Rol } from '@/types'
import { rutaInicio, useAuth } from '@/store/auth'
import { ForbiddenState } from './ui'

/** Protected Routes: exige sesión válida y, opcionalmente, un rol. El backend vuelve a validar todo. */
export function ProtectedRoute({ roles }: { roles?: Rol[] }) {
  const { isAuthenticated, rol } = useAuth()
  const location = useLocation()
  if (!isAuthenticated()) {
    return <Navigate to={`/login?redirect=${encodeURIComponent(location.pathname + location.search)}`} replace />
  }
  if (roles && rol && !roles.includes(rol)) {
    return <div className="p-6"><ForbiddenState message={`Esta sección es para ${roles.map((r) => r.toLowerCase()).join(' / ')}.`} /></div>
  }
  return <Outlet />
}

export function RedirectDashboard() {
  const rol = useAuth((s) => s.rol)
  return <Navigate to={rutaInicio(rol)} replace />
}
