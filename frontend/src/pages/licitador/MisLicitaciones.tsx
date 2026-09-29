import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { FilePlus2, Pencil } from 'lucide-react'
import { Button, Card, EmptyState, ErrorState, PageHeader, Spinner, StatusBadge, Table, Tabs, type Column } from '@/components/ui'
import { licitacionesApi } from '@/api/services'
import { useAsync } from '@/hooks/useAsync'
import type { EstadoLicitacion, Licitacion } from '@/types'
import { clp, fecha } from '@/utils/format'

type Filtro = 'TODAS' | EstadoLicitacion

export default function MisLicitaciones() {
  const nav = useNavigate()
  const { data, loading, error, reload } = useAsync(licitacionesApi.mias)
  const [filtro, setFiltro] = useState<Filtro>('TODAS')
  const lista = (data ?? []).filter((l) => filtro === 'TODAS' || l.estado === filtro)
  const columnas: Column<Licitacion>[] = [
    { key: 'titulo', header: 'Licitación', render: (l) => <div><p className="font-semibold text-ink-900">{l.titulo}</p><p className="text-xs text-ink-400">#{l.id} · {l.categoria}</p></div> },
    { key: 'estado', header: 'Estado', render: (l) => <StatusBadge estado={l.estado} /> },
    { key: 'monto', header: 'Monto', render: (l) => clp(l.montoEstimado) },
    { key: 'cierre', header: 'Cierre', render: (l) => fecha(l.fechaCierre) },
    { key: 'post', header: 'Postulaciones', render: (l) => <span className="font-semibold">{l.cantidadPostulaciones ?? 0}</span> },
    { key: 'acc', header: '', hideOnMobile: true, className: 'text-right', render: (l) => ['BORRADOR', 'PUBLICADA'].includes(l.estado) &&
      <Button size="sm" variant="ghost" icon={<Pencil className="h-3.5 w-3.5" />} onClick={(e) => { e.stopPropagation(); nav(`/dashboard/licitador/licitaciones/${l.id}/editar`) }}>Editar</Button> },
  ]
  const estados: Filtro[] = ['TODAS', 'BORRADOR', 'PUBLICADA', 'CERRADA', 'ADJUDICADA', 'CANCELADA']
  return (
    <>
      <PageHeader eyebrow="Gestión" title="Mis licitaciones" subtitle="Administra estados, condiciones y postulantes."
        actions={<Link to="/dashboard/licitador/licitaciones/nueva"><Button icon={<FilePlus2 className="h-4 w-4" />}>Crear licitación</Button></Link>} />
      <div className="mb-4"><Tabs value={filtro} onChange={setFiltro}
        tabs={estados.map((e) => ({ value: e, label: e === 'TODAS' ? 'Todas' : e.charAt(0) + e.slice(1).toLowerCase(), count: (data ?? []).filter((l) => e === 'TODAS' || l.estado === e).length }))} /></div>
      <Card>
        {loading && <Spinner />}
        {error && <ErrorState message={error.message} onRetry={reload} />}
        {data && <Table columns={columnas} rows={lista} rowKey={(l) => l.id} onRowClick={(l) => nav(`/dashboard/licitador/licitaciones/${l.id}`)}
          empty={<EmptyState title="No hay licitaciones aquí" message="Crea una licitación para publicarla a las Pymes." />} />}
      </Card>
    </>
  )
}
