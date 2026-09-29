import { cn } from '@/utils/format'

export function Tabs<T extends string>({ tabs, value, onChange }: { tabs: { value: T; label: string; count?: number }[]; value: T; onChange: (v: T) => void }) {
  return (
    <div role="tablist" className="scrollbar-thin -mx-1 flex gap-1 overflow-x-auto px-1">
      {tabs.map((t) => (
        <button key={t.value} role="tab" aria-selected={value === t.value} onClick={() => onChange(t.value)}
          className={cn('whitespace-nowrap rounded-lg px-3.5 py-2 text-sm font-medium transition',
            value === t.value ? 'bg-ink-900 text-white shadow-sm' : 'text-ink-600 hover:bg-ink-100')}>
          {t.label}{t.count !== undefined && <span className={cn('ml-1.5 rounded-full px-1.5 text-xs', value === t.value ? 'bg-white/20' : 'bg-ink-100')}>{t.count}</span>}
        </button>
      ))}
    </div>
  )
}
