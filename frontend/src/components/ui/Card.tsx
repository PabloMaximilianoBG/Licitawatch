import type { HTMLAttributes, ReactNode } from 'react'
import { cn } from '@/utils/format'

export function Card({ className, ...rest }: HTMLAttributes<HTMLDivElement>) {
  return <div className={cn('card', className)} {...rest} />
}

export function CardHeader({ title, subtitle, action, icon }: { title: ReactNode; subtitle?: ReactNode; action?: ReactNode; icon?: ReactNode }) {
  return (
    <div className="flex items-start justify-between gap-4 border-b border-ink-100 px-5 py-4">
      <div className="flex items-start gap-3">
        {icon && <div className="mt-0.5 rounded-lg bg-brand-50 p-2 text-brand-600">{icon}</div>}
        <div>
          <h3 className="font-semibold text-ink-900">{title}</h3>
          {subtitle && <p className="mt-0.5 text-sm text-ink-500">{subtitle}</p>}
        </div>
      </div>
      {action}
    </div>
  )
}
