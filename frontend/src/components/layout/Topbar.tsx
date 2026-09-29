import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Bell, CheckCheck, ChevronDown, LogOut, Menu, User, Wallet } from 'lucide-react'
import { notificacionesApi } from '@/api/services'
import { useAuth } from '@/store/auth'
import type { Notificacion } from '@/types'
import { cn, etiquetaEstado, haceCuanto } from '@/utils/format'

function useClickFuera(cb: () => void) {
  const ref = useRef<HTMLDivElement>(null)
  useEffect(() => {
    const h = (e: MouseEvent) => { if (ref.current && !ref.current.contains(e.target as Node)) cb() }
    document.addEventListener('mousedown', h)
    return () => document.removeEventListener('mousedown', h)
  }, [cb])
  return ref
}

export function Topbar({ onMenu, onLogout }: { onMenu: () => void; onLogout: () => void }) {
  const { usuario, rol } = useAuth()
  const nav = useNavigate()
  const [notifAbierto, setNotifAbierto] = useState(false)
  const [perfilAbierto, setPerfilAbierto] = useState(false)
  const [noLeidas, setNoLeidas] = useState(0)
  const [lista, setLista] = useState<Notificacion[]>([])
  const refNotif = useClickFuera(() => setNotifAbierto(false))
  const refPerfil = useClickFuera(() => setPerfilAbierto(false))

  const cargar = () => {
    notificacionesApi.noLeidas().then((r) => setNoLeidas(r.noLeidas)).catch(() => undefined)
  }
  useEffect(() => {
    cargar()
    const t = setInterval(cargar, 60_000)
    return () => clearInterval(t)
  }, [])

  const abrirNotif = () => {
    setNotifAbierto((v) => !v)
    if (!notifAbierto) notificacionesApi.mias(0, 6).then((p) => setLista(p.content)).catch(() => undefined)
  }
  const marcarTodas = async () => {
    await notificacionesApi.marcarTodas()
    setNoLeidas(0)
    setLista((l) => l.map((n) => ({ ...n, leida: true })))
  }
  const iniciales = (usuario?.nombre ?? '?').split(' ').map((p) => p[0]).slice(0, 2).join('').toUpperCase()

  return (
    <header className="sticky top-0 z-20 flex h-16 items-center gap-3 border-b border-ink-200/70 bg-white/80 px-4 backdrop-blur-xl sm:px-6">
      <button onClick={onMenu} className="rounded-lg p-2 text-ink-600 hover:bg-ink-100 lg:hidden" aria-label="Abrir menú"><Menu className="h-5 w-5" /></button>
      <div className="flex-1" />
      <div className="relative" ref={refNotif}>
        <button onClick={abrirNotif} className="relative rounded-xl p-2.5 text-ink-600 transition hover:bg-ink-100" aria-label={`Notificaciones (${noLeidas} sin leer)`}>
          <Bell className="h-5 w-5" />
          {noLeidas > 0 && <span className="absolute right-1.5 top-1.5 grid h-4 min-w-4 place-items-center rounded-full bg-red-500 px-1 text-[10px] font-bold text-white">{noLeidas > 9 ? '9+' : noLeidas}</span>}
        </button>
        {notifAbierto && (
          <div className="absolute right-0 mt-2 w-[min(92vw,380px)] overflow-hidden rounded-2xl border border-ink-200 bg-white shadow-xl animate-fade-in">
            <div className="flex items-center justify-between border-b border-ink-100 px-4 py-3">
              <p className="text-sm font-semibold">Notificaciones</p>
              {noLeidas > 0 && <button onClick={marcarTodas} className="flex items-center gap-1 text-xs font-medium text-brand-600 hover:underline"><CheckCheck className="h-3.5 w-3.5" />Marcar leídas</button>}
            </div>
            <ul className="max-h-96 divide-y divide-ink-100 overflow-y-auto">
              {lista.length === 0 && <li className="px-4 py-8 text-center text-sm text-ink-500">Sin notificaciones</li>}
              {lista.map((n) => (
                <li key={n.id} className={cn('px-4 py-3 text-sm', !n.leida && 'bg-brand-50/50')}>
                  <div className="flex items-start gap-2">
                    {!n.leida && <span className="mt-1.5 h-2 w-2 shrink-0 rounded-full bg-brand-500" />}
                    <div className="min-w-0">
                      <p className="truncate font-medium text-ink-900">{n.asunto}</p>
                      {n.resumen && <p className="mt-0.5 line-clamp-2 text-xs text-ink-500">{n.resumen}</p>}
                      <p className="mt-1 text-[11px] text-ink-400">{haceCuanto(n.creadoEn)} · {n.estado === 'ENVIADO' ? 'enviado por email' : 'email no enviado'}</p>
                    </div>
                  </div>
                </li>
              ))}
            </ul>
            <Link to="/dashboard/notificaciones" onClick={() => setNotifAbierto(false)} className="block border-t border-ink-100 px-4 py-2.5 text-center text-xs font-semibold text-brand-600 hover:bg-ink-50">Ver todas</Link>
          </div>
        )}
      </div>
      <div className="relative" ref={refPerfil}>
        <button onClick={() => setPerfilAbierto((v) => !v)} className="flex items-center gap-2.5 rounded-xl py-1.5 pl-1.5 pr-2.5 transition hover:bg-ink-100" aria-haspopup="menu" aria-expanded={perfilAbierto}>
          <span className="grid h-8 w-8 place-items-center rounded-lg bg-brand-gradient text-xs font-bold text-white">{iniciales}</span>
          <span className="hidden text-left sm:block">
            <span className="block max-w-[160px] truncate text-sm font-semibold text-ink-900">{usuario?.nombre}</span>
            <span className="block text-[11px] text-ink-500">{rol && etiquetaEstado[rol]}</span>
          </span>
          <ChevronDown className="hidden h-4 w-4 text-ink-400 sm:block" />
        </button>
        {perfilAbierto && (
          <div role="menu" className="absolute right-0 mt-2 w-56 overflow-hidden rounded-xl border border-ink-200 bg-white py-1 shadow-xl animate-fade-in">
            <p className="truncate border-b border-ink-100 px-4 py-2.5 text-xs text-ink-500">{usuario?.email}</p>
            <button role="menuitem" onClick={() => { setPerfilAbierto(false); nav('/dashboard/perfil') }} className="flex w-full items-center gap-2 px-4 py-2 text-sm hover:bg-ink-50"><User className="h-4 w-4" />Mi perfil</button>
            {rol !== 'ADMINISTRADOR' && <button role="menuitem" onClick={() => { setPerfilAbierto(false); nav('/dashboard/suscripcion') }} className="flex w-full items-center gap-2 px-4 py-2 text-sm hover:bg-ink-50"><Wallet className="h-4 w-4" />Suscripción</button>}
            <button role="menuitem" onClick={onLogout} className="flex w-full items-center gap-2 px-4 py-2 text-sm text-red-600 hover:bg-red-50"><LogOut className="h-4 w-4" />Cerrar sesión</button>
          </div>
        )}
      </div>
    </header>
  )
}
