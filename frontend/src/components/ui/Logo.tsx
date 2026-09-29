import { cn } from '@/utils/format'

export function Logo({ light, className, size = 'md' }: { light?: boolean; className?: string; size?: 'sm' | 'md' | 'lg' }) {
  const s = { sm: 'h-7 w-7 text-sm', md: 'h-9 w-9 text-base', lg: 'h-11 w-11 text-lg' }[size]
  return (
    <span className={cn('inline-flex items-center gap-2.5', className)}>
      <span className={cn('relative grid place-items-center rounded-xl bg-brand-gradient font-extrabold text-white shadow-glow', s)} aria-hidden>
        <svg viewBox="0 0 24 24" className="h-[55%] w-[55%]" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
          <path d="M7 4v16h11" /><circle cx="17" cy="8" r="2.2" fill="currentColor" stroke="none" />
        </svg>
      </span>
      <span className={cn('font-bold tracking-tight', size === 'lg' ? 'text-2xl' : 'text-lg', light ? 'text-white' : 'text-ink-900')}>
        Licita<span className={light ? 'text-brand-300' : 'text-brand-600'}>Watch</span>
      </span>
    </span>
  )
}
