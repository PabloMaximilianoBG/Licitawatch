import type { ReactNode } from 'react'
import { SlidersHorizontal } from 'lucide-react'
import { Button } from './Button'

export function FilterPanel({ children, onApply, onReset, activos }: { children: ReactNode; onApply: () => void; onReset: () => void; activos?: number }) {
  return (
    <details className="card group" open={typeof window !== 'undefined' && window.innerWidth >= 1024}>
      <summary className="flex cursor-pointer list-none items-center justify-between px-5 py-3.5">
        <span className="flex items-center gap-2 text-sm font-semibold text-ink-800">
          <SlidersHorizontal className="h-4 w-4 text-brand-600" />Filtros
          {!!activos && <span className="rounded-full bg-brand-600 px-2 py-0.5 text-[10px] text-white">{activos}</span>}
        </span>
        <span className="text-xs text-ink-400 group-open:hidden">Mostrar</span>
        <span className="hidden text-xs text-ink-400 group-open:inline">Ocultar</span>
      </summary>
      <div className="space-y-4 border-t border-ink-100 px-5 py-4">
        {children}
        <div className="flex gap-2 pt-1">
          <Button size="sm" onClick={onApply} full>Aplicar</Button>
          <Button size="sm" variant="secondary" onClick={onReset}>Limpiar</Button>
        </div>
      </div>
    </details>
  )
}
