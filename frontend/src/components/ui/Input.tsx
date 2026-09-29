import { forwardRef, useId, useState, type InputHTMLAttributes, type ReactNode } from 'react'
import { Eye, EyeOff } from 'lucide-react'
import { cn } from '@/utils/format'

interface Props extends InputHTMLAttributes<HTMLInputElement> {
  label?: string
  error?: string
  hint?: string
  icon?: ReactNode
}

export const Input = forwardRef<HTMLInputElement, Props>(({ label, error, hint, icon, className, type, id, ...rest }, ref) => {
  const autoId = useId()
  const inputId = id ?? autoId
  const [ver, setVer] = useState(false)
  const esPassword = type === 'password'
  return (
    <div className={className}>
      {label && <label htmlFor={inputId} className="label">{label}{rest.required && <span className="text-red-500"> *</span>}</label>}
      <div className="relative">
        {icon && <span className="pointer-events-none absolute left-3 top-1/2 -translate-y-1/2 text-ink-400">{icon}</span>}
        <input
          ref={ref}
          id={inputId}
          type={esPassword && ver ? 'text' : type}
          aria-invalid={!!error}
          aria-describedby={error ? `${inputId}-error` : hint ? `${inputId}-hint` : undefined}
          className={cn('field', icon && 'pl-10', esPassword && 'pr-10', error && 'field-error')}
          {...rest}
        />
        {esPassword && (
          <button type="button" onClick={() => setVer((v) => !v)} aria-label={ver ? 'Ocultar contraseña' : 'Mostrar contraseña'}
            className="absolute right-2 top-1/2 -translate-y-1/2 rounded-lg p-1.5 text-ink-400 hover:text-ink-700">
            {ver ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
          </button>
        )}
      </div>
      {error ? <p id={`${inputId}-error`} role="alert" className="mt-1.5 text-xs font-medium text-red-600">{error}</p>
        : hint ? <p id={`${inputId}-hint`} className="mt-1.5 text-xs text-ink-500">{hint}</p> : null}
    </div>
  )
})
Input.displayName = 'Input'
