import type { ReactNode } from 'react'
import { AlertCircle, Inbox, Loader2, Lock, SearchX, ShieldAlert } from 'lucide-react'
import { Link } from 'react-router-dom'
import { Button } from './Button'

export function Spinner({ label = 'Cargando…' }: { label?: string }) {
  return (
    <div role="status" className="flex items-center justify-center gap-3 py-16 text-sm text-ink-500">
      <Loader2 className="h-5 w-5 animate-spin text-brand-500" />{label}
    </div>
  )
}

export function Skeleton({ className = 'h-4 w-full' }: { className?: string }) {
  return <div className={`animate-pulse rounded-lg bg-ink-100 ${className}`} aria-hidden />
}

export function LoadingCards({ n = 3 }: { n?: number }) {
  return (
    <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3" role="status" aria-label="Cargando">
      {Array.from({ length: n }).map((_, i) => (
        <div key={i} className="card space-y-3 p-5"><Skeleton className="h-4 w-1/3" /><Skeleton className="h-5 w-3/4" /><Skeleton className="h-4 w-full" /><Skeleton className="h-4 w-2/3" /></div>
      ))}
    </div>
  )
}

function Estado({ icon, title, message, action, tono = 'text-ink-400 bg-ink-100' }: { icon: ReactNode; title: string; message?: ReactNode; action?: ReactNode; tono?: string }) {
  return (
    <div className="flex flex-col items-center justify-center px-6 py-14 text-center animate-fade-in">
      <div className={`mb-4 rounded-2xl p-3.5 ${tono}`}>{icon}</div>
      <h3 className="text-base font-semibold text-ink-900">{title}</h3>
      {message && <p className="mt-1.5 max-w-md text-sm text-ink-500">{message}</p>}
      {action && <div className="mt-5">{action}</div>}
    </div>
  )
}

export const EmptyState = ({ title = 'Sin resultados', message, action, icon }: { title?: string; message?: ReactNode; action?: ReactNode; icon?: ReactNode }) =>
  <Estado icon={icon ?? <Inbox className="h-6 w-6" />} title={title} message={message} action={action} />

export const ErrorState = ({ message, onRetry }: { message?: string; onRetry?: () => void }) =>
  <Estado icon={<AlertCircle className="h-6 w-6" />} tono="bg-red-50 text-red-500" title="No pudimos cargar la información"
    message={message ?? 'Ocurrió un error inesperado.'} action={onRetry && <Button variant="secondary" onClick={onRetry}>Reintentar</Button>} />

export const ForbiddenState = ({ message }: { message?: string }) =>
  <Estado icon={<ShieldAlert className="h-6 w-6" />} tono="bg-amber-50 text-amber-600" title="Acceso restringido"
    message={message ?? 'Tu perfil no tiene permisos para ver esta sección.'} />

export const UnauthorizedState = () =>
  <Estado icon={<Lock className="h-6 w-6" />} title="Sesión requerida" message="Inicia sesión para continuar."
    action={<Link to="/login"><Button>Iniciar sesión</Button></Link>} />

export const NotFoundState = ({ message }: { message?: string }) =>
  <Estado icon={<SearchX className="h-6 w-6" />} title="No encontrado" message={message ?? 'El recurso que buscas no existe o fue eliminado.'} />
