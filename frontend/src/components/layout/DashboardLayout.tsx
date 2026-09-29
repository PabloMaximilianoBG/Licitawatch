import { useState } from 'react'
import { Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '@/store/auth'
import { useSuscripcion } from '@/hooks/useSuscripcion'
import { toast } from '@/store/toast'
import { Sidebar } from './Sidebar'
import { Topbar } from './Topbar'

/** Estructura SaaS: Sidebar + Topbar + contenido principal. */
export function DashboardLayout() {
  const [abierto, setAbierto] = useState(false)
  const logout = useAuth((s) => s.logout)
  const nav = useNavigate()
  useSuscripcion()
  const salir = () => { logout(); toast.info('Sesión cerrada'); nav('/login') }
  return (
    <div className="min-h-screen">
      <Sidebar abierto={abierto} onCerrar={() => setAbierto(false)} onLogout={salir} />
      <div className="lg:pl-72">
        <Topbar onMenu={() => setAbierto(true)} onLogout={salir} />
        <main className="mx-auto w-full max-w-7xl px-4 py-6 sm:px-6 lg:px-8 lg:py-8 animate-fade-in">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
