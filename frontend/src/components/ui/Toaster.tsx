import { CheckCircle2, Info, X, XCircle } from 'lucide-react'
import { useToasts } from '@/store/toast'
import { cn } from '@/utils/format'

export function Toaster() {
  const { items, remove } = useToasts()
  return (
    <div className="pointer-events-none fixed inset-x-0 bottom-4 z-[60] flex flex-col items-center gap-2 px-4 sm:inset-x-auto sm:right-4 sm:items-end" aria-live="polite">
      {items.map((t) => (
        <div key={t.id} role="status"
          className={cn('pointer-events-auto flex w-full max-w-sm items-start gap-3 rounded-xl border bg-white px-4 py-3 text-sm shadow-lg animate-slide-in',
            t.tipo === 'success' && 'border-emerald-200', t.tipo === 'error' && 'border-red-200', t.tipo === 'info' && 'border-brand-200')}>
          {t.tipo === 'success' ? <CheckCircle2 className="h-5 w-5 shrink-0 text-emerald-500" />
            : t.tipo === 'error' ? <XCircle className="h-5 w-5 shrink-0 text-red-500" /> : <Info className="h-5 w-5 shrink-0 text-brand-500" />}
          <p className="flex-1 text-ink-700">{t.mensaje}</p>
          <button onClick={() => remove(t.id)} aria-label="Cerrar aviso" className="text-ink-400 hover:text-ink-700"><X className="h-4 w-4" /></button>
        </div>
      ))}
    </div>
  )
}
