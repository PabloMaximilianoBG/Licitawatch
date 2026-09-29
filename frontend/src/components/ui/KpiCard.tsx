import type { ReactNode } from 'react'
import { cn } from '@/utils/format'

export function KpiCard({ label, value, icon, hint, tono = 'brand' }: { label: string; value: ReactNode; icon: ReactNode; hint?: ReactNode; tono?: 'brand' | 'green' | 'amber' | 'violet' | 'red' }) {
  const t = {
    brand: 'from-brand-500/15 to-indigo-500/10 text-brand-600',
    green: 'from-emerald-500/15 to-teal-500/10 text-emerald-600',
    amber: 'from-amber-500/15 to-orange-500/10 text-amber-600',
    violet: 'from-violet-500/15 to-fuchsia-500/10 text-violet-600',
    red: 'from-red-500/15 to-rose-500/10 text-red-600',
  }[tono]
  return (
    <div className="card group relative overflow-hidden p-5 transition hover:-translate-y-0.5 hover:shadow-lg">
      <div className="flex items-start justify-between">
        <div>
          <p className="text-xs font-semibold uppercase tracking-wide text-ink-500">{label}</p>
          <p className="mt-2 text-3xl font-bold tracking-tight text-ink-900">{value}</p>
        </div>
        <div className={cn('rounded-xl bg-gradient-to-br p-2.5', t)}>{icon}</div>
      </div>
      {hint && <p className="mt-3 text-xs text-ink-500">{hint}</p>}
    </div>
  )
}
