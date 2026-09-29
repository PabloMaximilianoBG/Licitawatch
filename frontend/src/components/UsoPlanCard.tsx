import { Link } from 'react-router-dom'
import { Crown } from 'lucide-react'
import { Button, Card, CardHeader } from '@/components/ui'
import type { UsoPlan } from '@/types'
import { cn } from '@/utils/format'

function Barra({ label, usado, limite }: { label: string; usado: number; limite: number | null }) {
  const pct = limite === null ? 100 : Math.min(100, Math.round((usado / Math.max(limite, 1)) * 100))
  return (
    <div>
      <div className="mb-1.5 flex justify-between text-xs"><span className="text-ink-600">{label}</span>
        <span className="font-semibold text-ink-900">{usado} / {limite === null ? '∞' : limite}</span></div>
      <div className="h-2 overflow-hidden rounded-full bg-ink-100" role="progressbar" aria-valuenow={pct} aria-valuemin={0} aria-valuemax={100} aria-label={label}>
        <div className={cn('h-full rounded-full transition-all', limite === null ? 'bg-gradient-to-r from-violet-500 to-brand-500' : pct >= 100 ? 'bg-red-500' : pct >= 70 ? 'bg-amber-500' : 'bg-brand-500')}
          style={{ width: `${pct}%` }} />
      </div>
    </div>
  )
}

export function UsoPlanCard({ uso, rol }: { uso: UsoPlan; rol: 'PYME' | 'LICITADOR' }) {
  const premium = uso.plan === 'PREMIUM'
  return (
    <Card>
      <CardHeader title="Uso de tu plan este mes" subtitle={premium ? 'Plan Premium · sin límites' : 'Plan Estándar'}
        icon={<Crown className="h-4 w-4" />}
        action={!premium && <Link to="/planes"><Button size="sm">Mejorar</Button></Link>} />
      <div className="space-y-4 p-5">
        {rol === 'LICITADOR' && <Barra label="Publicaciones" usado={uso.publicacionesMes} limite={uso.limitePublicacionesMes} />}
        {rol === 'PYME' && <>
          <Barra label="Postulaciones" usado={uso.postulacionesMes} limite={uso.limitePostulacionesMes} />
          <Barra label="Búsquedas con filtros" usado={uso.busquedasMes} limite={uso.limiteBusquedasMes} />
        </>}
      </div>
    </Card>
  )
}
