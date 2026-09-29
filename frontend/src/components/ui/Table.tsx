import type { ReactNode } from 'react'
import { cn } from '@/utils/format'

export interface Column<T> {
  key: string
  header: ReactNode
  render: (row: T) => ReactNode
  className?: string
  hideOnMobile?: boolean
}

/** Tabla en desktop; en mobile se transforma en tarjetas apiladas (no solo se achica). */
export function Table<T>({ columns, rows, rowKey, onRowClick, empty }: {
  columns: Column<T>[]; rows: T[]; rowKey: (r: T) => string | number; onRowClick?: (r: T) => void; empty?: ReactNode
}) {
  if (!rows.length && empty) return <>{empty}</>
  return (
    <>
      <div className="hidden overflow-x-auto md:block">
        <table className="w-full text-left text-sm">
          <thead>
            <tr className="border-b border-ink-100 text-xs uppercase tracking-wide text-ink-500">
              {columns.map((c) => <th key={c.key} scope="col" className={cn('px-5 py-3 font-semibold', c.className)}>{c.header}</th>)}
            </tr>
          </thead>
          <tbody className="divide-y divide-ink-100">
            {rows.map((r) => (
              <tr key={rowKey(r)} onClick={onRowClick ? () => onRowClick(r) : undefined}
                className={cn('transition-colors hover:bg-ink-50/70', onRowClick && 'cursor-pointer')}>
                {columns.map((c) => <td key={c.key} className={cn('px-5 py-3.5 align-middle text-ink-700', c.className)}>{c.render(r)}</td>)}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <ul className="divide-y divide-ink-100 md:hidden">
        {rows.map((r) => (
          <li key={rowKey(r)} onClick={onRowClick ? () => onRowClick(r) : undefined} className={cn('space-y-1.5 px-4 py-3.5', onRowClick && 'cursor-pointer active:bg-ink-50')}>
            {columns.filter((c) => !c.hideOnMobile).map((c) => (
              <div key={c.key} className="flex items-start justify-between gap-3 text-sm">
                <span className="shrink-0 text-xs font-medium uppercase tracking-wide text-ink-400">{c.header}</span>
                <span className="min-w-0 text-right text-ink-800 [overflow-wrap:anywhere]">{c.render(r)}</span>
              </div>
            ))}
          </li>
        ))}
      </ul>
    </>
  )
}
