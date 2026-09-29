import { forwardRef, useId, type SelectHTMLAttributes } from 'react'
import { ChevronDown } from 'lucide-react'
import { cn } from '@/utils/format'

interface Props extends SelectHTMLAttributes<HTMLSelectElement> {
  label?: string
  error?: string
  options: { value: string; label: string }[]
  placeholder?: string
}

export const Select = forwardRef<HTMLSelectElement, Props>(({ label, error, options, placeholder, className, id, ...rest }, ref) => {
  const autoId = useId()
  const sid = id ?? autoId
  return (
    <div className={className}>
      {label && <label htmlFor={sid} className="label">{label}{rest.required && <span className="text-red-500"> *</span>}</label>}
      <div className="relative">
        <select ref={ref} id={sid} aria-invalid={!!error} className={cn('field appearance-none pr-9', error && 'field-error')} {...rest}>
          {placeholder !== undefined && <option value="">{placeholder}</option>}
          {options.map((o) => <option key={o.value} value={o.value}>{o.label}</option>)}
        </select>
        <ChevronDown className="pointer-events-none absolute right-3 top-1/2 h-4 w-4 -translate-y-1/2 text-ink-400" />
      </div>
      {error && <p role="alert" className="mt-1.5 text-xs font-medium text-red-600">{error}</p>}
    </div>
  )
})
Select.displayName = 'Select'
