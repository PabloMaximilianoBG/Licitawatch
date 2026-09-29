import { Link } from 'react-router-dom'
import { Archive, FilePlus2, Gavel, Inbox, Radio } from 'lucide-react'
import { Button, Card, CardHeader, ChartCard, EmptyState, ErrorState, KpiCard, LoadingCards, PageHeader } from '@/components/ui'
import { LicitacionCard } from '@/components/LicitacionCard'
import { UsoPlanCard } from '@/components/UsoPlanCard'
import { LicitAsistPromo } from '@/components/LicitAsistPromo'
import { EstadoDonut } from '@/components/charts/EstadoDonut'
import { licitacionesApi } from '@/api/services'
import { useAsync } from '@/hooks/useAsync'
import { useAuth } from '@/store/auth'

export default function LicitadorResumen() {
  const { usuario, suscripcion } = useAuth()
  const { data, loading, error, reload } = useAsync(licitacionesApi.resumen)
  return (
    <>
      <PageHeader eyebrow="Panel Licitador" title={usuario?.nombre ?? 'Resumen'} subtitle="Gestiona tus procesos de licitación y revisa a tus postulantes."
        actions={<Link to="/dashboard/licitador/licitaciones/nueva"><Button icon={<FilePlus2 className="h-4 w-4" />}>Crear licitación</Button></Link>} />
      {loading && <LoadingCards n={4} />}
      {error && <ErrorState message={error.message} onRetry={reload} />}
      {data && (
        <div className="space-y-6">
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <KpiCard label="Licitaciones creadas" value={data.kpis.licitacionesCreadas} icon={<Gavel className="h-5 w-5" />} hint={`${data.kpis.borradores} en borrador`} />
            <KpiCard label="Activas" value={data.kpis.activas} icon={<Radio className="h-5 w-5" />} tono="green" />
            <KpiCard label="Cerradas / adjudicadas" value={data.kpis.cerradas} icon={<Archive className="h-5 w-5" />} tono="amber" />
            <KpiCard label="Postulaciones recibidas" value={data.kpis.postulacionesRecibidas} icon={<Inbox className="h-5 w-5" />} tono="violet" />
          </div>
          <div className="grid gap-6 lg:grid-cols-2">
            <ChartCard title="Mis licitaciones por estado">{data.licitacionesPorEstado && <EstadoDonut data={data.licitacionesPorEstado} />}</ChartCard>
            <ChartCard title="Postulaciones recibidas por estado">{data.postulacionesPorEstado && <EstadoDonut data={data.postulacionesPorEstado} />}</ChartCard>
          </div>
          <div className="grid gap-6 lg:grid-cols-3">
            <Card className="lg:col-span-2">
              <CardHeader title="Mis licitaciones activas" subtitle="Próximas a cerrar"
                action={<Link to="/dashboard/licitador/licitaciones" className="text-sm font-semibold text-brand-600 hover:underline">Ver todas</Link>} />
              <div className="grid gap-4 p-5 sm:grid-cols-2">
                {data.proximasACerrar.length === 0 && <div className="sm:col-span-2"><EmptyState title="Sin licitaciones publicadas" message="Publica tu primera licitación para empezar a recibir postulaciones."
                  action={<Link to="/dashboard/licitador/licitaciones/nueva"><Button>Crear licitación</Button></Link>} /></div>}
                {data.proximasACerrar.slice(0, 4).map((l) => <LicitacionCard key={l.id} l={l} to={`/dashboard/licitador/licitaciones/${l.id}`} />)}
              </div>
            </Card>
            <div className="space-y-6">
              {data.uso && <UsoPlanCard uso={data.uso} rol="LICITADOR" />}
              <LicitAsistPromo premium={!!suscripcion?.esPremium} />
            </div>
          </div>
        </div>
      )}
    </>
  )
}
