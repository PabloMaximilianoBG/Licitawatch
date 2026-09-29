import type { ReactNode } from 'react'
import { cn, etiquetaEstado } from '@/utils/format'

type Tono = 'gray' | 'blue' | 'green' | 'amber' | 'red' | 'violet' | 'indigo'

const tonos: Record<Tono, string> = {
  gray: 'bg-ink-100 text-ink-700 ring-ink-200',
  blue: 'bg-brand-50 text-brand-700 ring-brand-200',
  green: 'bg-emerald-50 text-emerald-700 ring-emerald-200',
  amber: 'bg-amber-50 text-amber-700 ring-amber-200',
  red: 'bg-red-50 text-red-700 ring-red-200',
  violet: 'bg-violet-50 text-violet-700 ring-violet-200',
  indigo: 'bg-indigo-50 text-indigo-700 ring-indigo-200',
}

export function Badge({ tono = 'gray', children, dot, className }: { tono?: Tono; children: ReactNode; dot?: boolean; className?: string }) {
  return (
    <span className={cn('inline-flex items-center gap-1.5 whitespace-nowrap rounded-full px-2.5 py-0.5 text-xs font-semibold ring-1 ring-inset', tonos[tono], className)}>
      {dot && <span className="h-1.5 w-1.5 rounded-full bg-current" aria-hidden />}
      {children}
    </span>
  )
}

const tonoEstado: Record<string, Tono> = {
  BORRADOR: 'gray', PUBLICADA: 'green', CERRADA: 'amber', ADJUDICADA: 'indigo', CANCELADA: 'red', SUSPENDIDA: 'red',
  ENVIADA: 'blue', EN_REVISION: 'amber', SELECCIONADA: 'green', RECHAZADA: 'red', RETIRADA: 'gray',
  PENDIENTE: 'amber', APROBADO: 'green', RECHAZADO: 'red', ANULADO: 'gray', ACTIVA: 'green', VENCIDA: 'gray',
  ENVIADO: 'green', FALLIDO: 'red', PREMIUM: 'violet', ESTANDAR: 'gray', PYME: 'blue', LICITADOR: 'indigo', ADMINISTRADOR: 'violet',
}

export function StatusBadge({ estado }: { estado: string }) {
  return <Badge tono={tonoEstado[estado] ?? 'gray'} dot>{etiquetaEstado[estado] ?? estado}</Badge>
}
