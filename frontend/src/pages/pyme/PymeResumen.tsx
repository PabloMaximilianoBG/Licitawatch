import { Link } from 'react-router-dom'
import { CalendarClock, FileSearch, Send, Trophy } from 'lucide-react'
import { Button, Card, CardHeader, ChartCard, EmptyState, ErrorState, KpiCard, LoadingCards, PageHeader } from '@/components/ui'
import { LicitacionCard } from '@/components/LicitacionCard'
import { UsoPlanCard } from '@/components/UsoPlanCard'
import { LicitAsistPromo } from '@/components/LicitAsistPromo'
import { EstadoDonut } from '@/components/charts/EstadoDonut'
import { licitacionesApi } from '@/api/services'
import { useAsync } from '@/hooks/useAsync'
import { useAuth } from '@/store/auth'

export default function PymeResumen() {
  const { usuario, suscripcion } = useAuth()
  const { data, loading, error, reload } = useAsync(licitacionesApi.resumen)
  return (
    <>
      <PageHeader eyebrow="Panel Pyme" title={`Hola, ${usuario?.nombre?.split(' ')[0] ?? ''} 👋`} subtitle="Estas son tus oportunidades de negocio hoy."
        actions={<Link to="/dashboard/pyme/licitaciones"><Button icon={<FileSearch className="h-4 w-4" />}>Buscar licitaciones</Button></Link>} />
      {loading && <LoadingCards n={4} />}
      {error && <ErrorState message={error.message} onRetry={reload} />}
      {data && (
        <div className="space-y-6">
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <KpiCard label="Licitaciones disponibles" value={data.kpis.licitacionesDisponibles} icon={<FileSearch className="h-5 w-5" />} />
            <KpiCard label="Cierran esta semana" value={data.kpis.cierranEstaSemana} icon={<CalendarClock className="h-5 w-5" />} tono="amber" />
            <KpiCard label="Mis postulaciones" value={data.kpis.misPostulaciones} icon={<Send className="h-5 w-5" />} tono="violet" />
            <KpiCard label="Seleccionadas" value={data.kpis.seleccionadas} icon={<Trophy className="h-5 w-5" />} tono="green" />
          </div>
          <div className="grid gap-6 lg:grid-cols-3">
            <Card className="lg:col-span-2">
              <CardHeader title="Próximas a cerrar" subtitle="Licitaciones publicadas ordenadas por fecha de cierre"
                action={<Link to="/dashboard/pyme/licitaciones" className="text-sm font-semibold text-brand-600 hover:underline">Ver todas</Link>} />
              <div className="grid gap-4 p-5 sm:grid-cols-2">
                {data.proximasACerrar.length === 0 && <div className="sm:col-span-2"><EmptyState title="No hay licitaciones abiertas" message="Vuelve pronto: los licitadores publican nuevas oportunidades constantemente." /></div>}
                {data.proximasACerrar.slice(0, 4).map((l) => <LicitacionCard key={l.id} l={l} to={`/dashboard/pyme/licitaciones/${l.id}`} />)}
              </div>
            </Card>
            <div className="space-y-6">
              {data.uso && <UsoPlanCard uso={data.uso} rol="PYME" />}
              <LicitAsistPromo premium={!!suscripcion?.esPremium} />
            </div>
          </div>
          <ChartCard title="Estado de mis postulaciones">{data.postulacionesPorEstado && <EstadoDonut data={data.postulacionesPorEstado} />}</ChartCard>
        </div>
      )}
    </>
  )
}
