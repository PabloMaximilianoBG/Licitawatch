import { forwardRef, useId, type TextareaHTMLAttributes } from 'react'
import { cn } from '@/utils/format'

interface Props extends TextareaHTMLAttributes<HTMLTextAreaElement> { label?: string; error?: string; hint?: string }

export const Textarea = forwardRef<HTMLTextAreaElement, Props>(({ label, error, hint, className, id, ...rest }, ref) => {
  const autoId = useId()
  const tid = id ?? autoId
  return (
    <div className={className}>
      {label && <label htmlFor={tid} className="label">{label}{rest.required && <span className="text-red-500"> *</span>}</label>}
      <textarea ref={ref} id={tid} aria-invalid={!!error} className={cn('field min-h-[96px] resize-y', error && 'field-error')} {...rest} />
      {error ? <p role="alert" className="mt-1.5 text-xs font-medium text-red-600">{error}</p>
        : hint ? <p className="mt-1.5 text-xs text-ink-500">{hint}</p> : null}
    </div>
  )
})
Textarea.displayName = 'Textarea'
