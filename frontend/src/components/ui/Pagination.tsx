import { ChevronLeft, ChevronRight } from 'lucide-react'
import { Button } from './Button'

export function Pagination({ page, totalPages, totalElements, onChange }: { page: number; totalPages: number; totalElements?: number; onChange: (p: number) => void }) {
  if (totalPages <= 1) return null
  return (
    <nav className="flex items-center justify-between gap-3 border-t border-ink-100 px-5 py-3" aria-label="Paginación">
      <p className="text-xs text-ink-500">Página <b>{page + 1}</b> de <b>{totalPages}</b>{totalElements !== undefined && <> · {totalElements} resultados</>}</p>
      <div className="flex gap-2">
        <Button size="sm" variant="secondary" disabled={page === 0} onClick={() => onChange(page - 1)} icon={<ChevronLeft className="h-4 w-4" />} aria-label="Anterior">Anterior</Button>
        <Button size="sm" variant="secondary" disabled={page + 1 >= totalPages} onClick={() => onChange(page + 1)} aria-label="Siguiente">Siguiente<ChevronRight className="h-4 w-4" /></Button>
      </div>
    </nav>
  )
}
