import { Link } from 'react-router-dom'
import { CalendarClock, MapPin, Sparkles, Tag, Users } from 'lucide-react'
import { StatusBadge } from '@/components/ui'
import type { Licitacion } from '@/types'
import { clp, cn, fecha } from '@/utils/format'

export function LicitacionCard({ l, to }: { l: Licitacion; to: string }) {
  const urgente = l.estado === 'PUBLICADA' && (l.diasParaCierre ?? 99) <= 7
  return (
    <Link to={to} className={cn('card group flex flex-col p-5 transition hover:-translate-y-0.5 hover:border-brand-300 hover:shadow-glow',
      l.destacada && 'ring-1 ring-violet-200')}>
      <div className="flex items-start justify-between gap-3">
        <div className="flex flex-wrap items-center gap-2">
          <StatusBadge estado={l.estado} />
          {l.destacada && <span className="inline-flex items-center gap-1 rounded-full bg-violet-50 px-2 py-0.5 text-[11px] font-semibold text-violet-700"><Sparkles className="h-3 w-3" />Destacada</span>}
        </div>
        <span className="text-xs text-ink-400">#{l.id}</span>
      </div>
      <h3 className="mt-3 line-clamp-2 font-semibold text-ink-900 group-hover:text-brand-700">{l.titulo}</h3>
      {l.licitadorNombre && <p className="mt-1 text-xs text-ink-500">{l.licitadorNombre}</p>}
      <p className="mt-2 line-clamp-2 text-sm text-ink-500">{l.descripcion}</p>
      <div className="mt-4 flex flex-wrap gap-x-4 gap-y-1.5 text-xs text-ink-500">
        <span className="inline-flex items-center gap-1"><Tag className="h-3.5 w-3.5" />{l.categoria}</span>
        {l.region && <span className="inline-flex items-center gap-1"><MapPin className="h-3.5 w-3.5" />{l.region}</span>}
        {l.cantidadPostulaciones !== undefined && <span className="inline-flex items-center gap-1"><Users className="h-3.5 w-3.5" />{l.cantidadPostulaciones} postulaciones</span>}
      </div>
      <div className="mt-auto flex items-end justify-between border-t border-ink-100 pt-4">
        <div>
          <p className="text-[11px] uppercase tracking-wide text-ink-400">Monto estimado</p>
          <p className="font-bold text-ink-900">{clp(l.montoEstimado)}</p>
        </div>
        <div className={cn('text-right text-xs', urgente ? 'font-semibold text-amber-600' : 'text-ink-500')}>
          <p className="inline-flex items-center gap-1"><CalendarClock className="h-3.5 w-3.5" />Cierra {fecha(l.fechaCierre)}</p>
          {l.estado === 'PUBLICADA' && l.diasParaCierre !== undefined && <p>{l.diasParaCierre <= 0 ? 'Cierra hoy' : `en ${l.diasParaCierre} días`}</p>}
        </div>
      </div>
      {l.miPostulacionEstado && <div className="mt-3 rounded-lg bg-brand-50 px-3 py-1.5 text-xs font-medium text-brand-700">Ya postulaste · <StatusBadge estado={l.miPostulacionEstado} /></div>}
    </Link>
  )
}
