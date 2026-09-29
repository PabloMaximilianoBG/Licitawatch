import { NavLink } from 'react-router-dom'
import { Crown, LogOut, X } from 'lucide-react'
import { Logo } from '@/components/ui'
import { useAuth } from '@/store/auth'
import { cn, etiquetaEstado } from '@/utils/format'
import { navegacion } from './navegacion'

export function Sidebar({ abierto, onCerrar, onLogout }: { abierto: boolean; onCerrar: () => void; onLogout: () => void }) {
  const { rol, usuario, suscripcion } = useAuth()
  if (!rol) return null
  const premium = suscripcion?.esPremium
  return (
    <>
      <div onClick={onCerrar} className={cn('fixed inset-0 z-30 bg-ink-950/60 backdrop-blur-sm transition lg:hidden', abierto ? 'opacity-100' : 'pointer-events-none opacity-0')} />
      <aside
        className={cn('fixed inset-y-0 left-0 z-40 flex w-72 flex-col bg-ink-950 text-ink-300 transition-transform duration-200 lg:translate-x-0',
          abierto ? 'translate-x-0' : '-translate-x-full')}
        aria-label="Navegación principal">
        <div className="pointer-events-none absolute inset-0 bg-grid-dark [background-size:28px_28px] opacity-60" />
        <div className="pointer-events-none absolute -left-20 top-0 h-64 w-64 rounded-full bg-brand-600/20 blur-3xl" />
        <div className="relative flex h-16 items-center justify-between px-5">
          <Logo light />
          <button onClick={onCerrar} className="rounded-lg p-1.5 text-ink-400 hover:bg-white/10 lg:hidden" aria-label="Cerrar menú"><X className="h-5 w-5" /></button>
        </div>
        <div className="relative mx-4 mb-4 rounded-xl border border-white/10 bg-white/5 p-3">
          <p className="truncate text-sm font-semibold text-white">{usuario?.nombre}</p>
          <div className="mt-1 flex items-center gap-2 text-xs">
            <span className="text-ink-400">{etiquetaEstado[rol]}</span>
            {rol !== 'ADMINISTRADOR' && (premium
              ? <span className="inline-flex items-center gap-1 rounded-full bg-violet-500/20 px-2 py-0.5 font-semibold text-violet-200"><Crown className="h-3 w-3" />Premium</span>
              : <span className="rounded-full bg-white/10 px-2 py-0.5 text-ink-300">Estándar</span>)}
          </div>
        </div>
        <nav className="scrollbar-thin relative flex-1 space-y-6 overflow-y-auto px-3 pb-4">
          {navegacion[rol].map((g) => (
            <div key={g.titulo}>
              <p className="mb-2 px-3 text-[11px] font-semibold uppercase tracking-widest text-ink-500">{g.titulo}</p>
              <ul className="space-y-0.5">
                {g.items.map((it) => (
                  <li key={it.to}>
                    <NavLink to={it.to} end={it.end} onClick={onCerrar}
                      className={({ isActive }) => cn('group flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition',
                        isActive ? 'bg-brand-600/20 text-white ring-1 ring-inset ring-brand-400/30' : 'hover:bg-white/5 hover:text-white')}>
                      <span className="text-ink-400 group-hover:text-brand-300 group-[.active]:text-brand-300">{it.icon}</span>
                      <span className="flex-1">{it.label}</span>
                      {it.premium && !premium && <Crown className="h-3.5 w-3.5 text-violet-300" aria-label="Requiere Premium" />}
                    </NavLink>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </nav>
        {rol !== 'ADMINISTRADOR' && !premium && (
          <NavLink to="/planes" onClick={onCerrar} className="relative mx-4 mb-3 block overflow-hidden rounded-xl bg-gradient-to-br from-brand-600 to-violet-600 p-4 text-white">
            <Crown className="mb-2 h-5 w-5" />
            <p className="text-sm font-semibold">Pásate a Premium</p>
            <p className="mt-0.5 text-xs text-white/80">Sin límites + LicitAsist con IA por $24.990</p>
          </NavLink>
        )}
        <button onClick={onLogout} className="relative mx-3 mb-4 flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium text-ink-400 hover:bg-white/5 hover:text-white">
          <LogOut className="h-[18px] w-[18px]" />Cerrar sesión
        </button>
      </aside>
    </>
  )
}
