import { Link } from 'react-router-dom'
import { Crown, Sparkles } from 'lucide-react'
import { Button } from '@/components/ui'

export function LicitAsistPromo({ premium }: { premium: boolean }) {
  return (
    <div className="relative overflow-hidden rounded-2xl bg-ink-950 p-6 text-white">
      <div className="absolute inset-0 bg-grid-dark [background-size:24px_24px]" />
      <div className="absolute -right-10 -top-10 h-40 w-40 rounded-full bg-brand-600/40 blur-3xl" />
      <div className="relative">
        <span className="inline-flex items-center gap-1.5 rounded-full bg-white/10 px-2.5 py-1 text-xs font-semibold text-brand-200"><Sparkles className="h-3.5 w-3.5" />LicitAsist · IA</span>
        <p className="mt-3 text-lg font-semibold">{premium ? 'Tu asistente está listo' : 'Pregúntale a LicitAsist'}</p>
        <p className="mt-1 text-sm text-ink-300">{premium ? 'Consulta licitaciones, requisitos y postulaciones con datos reales.' : 'Disponible en el plan Premium: respuestas instantáneas sobre tus licitaciones.'}</p>
        <Link to={premium ? '/dashboard/asistente' : '/planes'} className="mt-4 inline-block">
          <Button size="sm" icon={premium ? <Sparkles className="h-4 w-4" /> : <Crown className="h-4 w-4" />}>{premium ? 'Abrir LicitAsist' : 'Ver Premium'}</Button>
        </Link>
      </div>
    </div>
  )
}
