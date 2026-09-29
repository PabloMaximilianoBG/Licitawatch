import { Link } from 'react-router-dom'
import { Bot, CalendarClock, Crown, Receipt, ShieldCheck } from 'lucide-react'
import { Badge, Button, Card, CardHeader, ErrorState, PageHeader, Spinner, StatusBadge } from '@/components/ui'
import { UsoPlanCard } from '@/components/UsoPlanCard'
import { licitacionesApi, ventasApi } from '@/api/services'
import { useAsync } from '@/hooks/useAsync'
import { useAuth } from '@/store/auth'
import { fecha } from '@/utils/format'

export default function Suscripcion() {
  const { rol, setSuscripcion } = useAuth()
  const { data, loading, error, reload } = useAsync(() => Promise.all([ventasApi.suscripcion(), licitacionesApi.uso()]).then(([s, u]) => { setSuscripcion(s); return { s, u } }))
  if (loading) return <Spinner />
  if (error) return <ErrorState message={error.message} onRetry={reload} />
  const { s, u } = data!
  return (
    <>
      <PageHeader eyebrow="Cuenta" title="Suscripción" subtitle="Tu plan, beneficios y consumo del mes."
        actions={<Link to="/dashboard/pagos"><Button variant="secondary" icon={<Receipt className="h-4 w-4" />}>Historial de pagos</Button></Link>} />
      <div className="grid gap-6 lg:grid-cols-[1.3fr_1fr]">
        <Card className="overflow-hidden">
          <div className={s.esPremium ? 'bg-gradient-to-br from-brand-600 via-indigo-600 to-violet-600 p-6 text-white' : 'bg-ink-950 p-6 text-white'}>
            <div className="flex items-start justify-between">
              <div>
                <p className="text-xs uppercase tracking-widest text-white/70">Plan actual</p>
                <p className="mt-1 flex items-center gap-2 text-3xl font-bold">{s.esPremium && <Crown className="h-7 w-7" />}{s.plan.etiqueta}</p>
              </div>
              <StatusBadge estado={s.estado} />
            </div>
            {s.esPremium && s.fechaFin && <p className="mt-4 inline-flex items-center gap-2 text-sm text-white/90"><CalendarClock className="h-4 w-4" />Vigente hasta el {fecha(s.fechaFin)} · {s.diasRestantes} días restantes</p>}
            {!s.esPremium && <p className="mt-4 text-sm text-ink-300">Plan gratuito con límites mensuales.</p>}
          </div>
          <div className="p-6">
            <p className="mb-3 text-sm font-semibold">Beneficios incluidos</p>
            <ul className="grid gap-2 sm:grid-cols-2">
              {s.plan.beneficios.map((b) => <li key={b} className="flex items-center gap-2 text-sm text-ink-700"><ShieldCheck className="h-4 w-4 text-emerald-500" />{b}</li>)}
            </ul>
            <div className="mt-6 flex items-center gap-3 rounded-xl border border-ink-200 p-4">
              <Bot className="h-5 w-5 text-brand-600" />
              <p className="flex-1 text-sm">LicitAsist</p>
              {s.accesoLicitasist ? <Badge tono="green" dot>Habilitado</Badge> : <Badge tono="gray">Requiere Premium</Badge>}
            </div>
            {!s.esPremium && <Link to="/planes" className="mt-6 block"><Button full size="lg" icon={<Crown className="h-5 w-5" />}>Contratar Premium · $24.990</Button></Link>}
          </div>
        </Card>
        <div className="space-y-6">
          {(rol === 'PYME' || rol === 'LICITADOR') && <UsoPlanCard uso={u} rol={rol} />}
          <Card>
            <CardHeader title="¿Cómo se activa Premium?" />
            <ol className="space-y-2 p-5 text-sm text-ink-600">
              <li>1. Eliges Premium y pagas en Webpay Plus.</li>
              <li>2. Nuestro backend confirma la transacción directamente con Transbank.</li>
              <li>3. Solo entonces se activa tu plan y recibes el comprobante por correo.</li>
            </ol>
          </Card>
        </div>
      </div>
    </>
  )
}
