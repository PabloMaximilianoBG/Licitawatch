import { Link } from 'react-router-dom'
import { Building2, Crown, DollarSign, FileText, Gavel, Store, Users } from 'lucide-react'
import { Button, ChartCard, ErrorState, KpiCard, LoadingCards, PageHeader } from '@/components/ui'
import { BarrasMes } from '@/components/charts/BarrasMes'
import { EstadoDonut } from '@/components/charts/EstadoDonut'
import { LicitAsistPromo } from '@/components/LicitAsistPromo'
import { adminApi } from '@/api/services'
import { useAsync } from '@/hooks/useAsync'
import { clp, num } from '@/utils/format'

export default function AdminResumen() {
  const { data, loading, error, reload } = useAsync(() =>
    Promise.all([adminApi.statsUsuarios(), adminApi.statsLicitaciones(), adminApi.statsVentas()]).then(([u, l, v]) => ({ u, l, v })))
  return (
    <>
      <PageHeader eyebrow="Administración" title="Estadísticas de la plataforma" subtitle="Actividad de usuarios, licitaciones, ventas y suscripciones."
        actions={<Button variant="secondary" onClick={reload}>Actualizar</Button>} />
      {loading && <LoadingCards n={6} />}
      {error && <ErrorState message={error.message} onRetry={reload} />}
      {data && (
        <div className="space-y-6">
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
            <KpiCard label="Usuarios" value={num(data.u.totalUsuarios)} icon={<Users className="h-5 w-5" />} hint={`${data.u.nuevosUltimos30Dias} nuevos en 30 días · ${data.u.emailNoConfirmado} sin confirmar`} />
            <KpiCard label="Pymes / Licitadores" value={`${data.u.porRol.PYME ?? 0} / ${data.u.porRol.LICITADOR ?? 0}`} icon={<Store className="h-5 w-5" />} tono="violet" hint={`${data.u.inactivos} cuentas inactivas`} />
            <KpiCard label="Licitaciones" value={num(data.l.totalLicitaciones)} icon={<Gavel className="h-5 w-5" />} tono="amber" hint={`${data.l.licitacionesPorEstado.PUBLICADA ?? 0} publicadas · ${clp(data.l.montoTotalLicitado)} licitado`} />
            <KpiCard label="Postulaciones" value={num(data.l.totalPostulaciones)} icon={<FileText className="h-5 w-5" />} tono="green" hint={`${data.l.postulacionesPorEstado.SELECCIONADA ?? 0} seleccionadas`} />
            <KpiCard label="Ingresos aprobados" value={clp(data.v.ingresosAprobados)} icon={<DollarSign className="h-5 w-5" />} tono="green" hint={`${data.v.totalVentas} ventas registradas`} />
            <KpiCard label="Premium activas" value={data.v.premiumActivas} icon={<Crown className="h-5 w-5" />} tono="violet" />
            <KpiCard label="Pagos rechazados" value={data.v.pagosPorEstado.RECHAZADO ?? 0} icon={<Building2 className="h-5 w-5" />} tono="red" hint={`${data.v.pagosPorEstado.PENDIENTE ?? 0} pendientes · ${data.v.pagosPorEstado.ANULADO ?? 0} anulados`} />
            <LicitAsistPromo premium />
          </div>
          <div className="grid gap-6 lg:grid-cols-2">
            <ChartCard title="Registros de usuarios" subtitle="Últimos 6 meses"><BarrasMes data={data.u.registrosPorMes} /></ChartCard>
            <ChartCard title="Ingresos por mes" subtitle="Pagos aprobados (CLP)"><BarrasMes data={data.v.ingresosPorMes} dataKey="monto" color="#10b981" formato={clp} /></ChartCard>
            <ChartCard title="Licitaciones por estado"><EstadoDonut data={data.l.licitacionesPorEstado} /></ChartCard>
            <ChartCard title="Usuarios por rol"><EstadoDonut data={data.u.porRol} /></ChartCard>
            <ChartCard title="Publicaciones por mes"><BarrasMes data={data.l.publicacionesPorMes} color="#6366f1" /></ChartCard>
            <ChartCard title="Pagos por estado" action={<Link to="/dashboard/admin/ventas" className="text-sm font-semibold text-brand-600 hover:underline">Ver pagos</Link>}><EstadoDonut data={data.v.pagosPorEstado} /></ChartCard>
          </div>
        </div>
      )}
    </>
  )
}
