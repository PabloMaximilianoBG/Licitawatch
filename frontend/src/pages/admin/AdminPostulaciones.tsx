import { useState } from 'react'
import { Card, EmptyState, ErrorState, Input, PageHeader, Pagination, Select, Spinner, StatusBadge, Table, type Column } from '@/components/ui'
import { adminApi } from '@/api/services'
import { useAsync } from '@/hooks/useAsync'
import type { Postulacion } from '@/types'
import { clp, fecha } from '@/utils/format'

export default function AdminPostulaciones() {
  const [f, setF] = useState({ estado: '', licitacionId: '', page: 0 })
  const { data, loading, error, reload } = useAsync(() => adminApi.postulaciones({ ...f, size: 20 }), [JSON.stringify(f)])
  const columnas: Column<Postulacion>[] = [
    { key: 'l', header: 'Licitación', render: (p) => <div><p className="font-semibold text-ink-900">{p.licitacionTitulo}</p><p className="text-xs text-ink-500">#{p.licitacionId} · <StatusBadge estado={p.licitacionEstado} /></p></div> },
    { key: 'p', header: 'Pyme', render: (p) => <div><p>{p.pymeNombre ?? `Pyme ${p.pymeId}`}</p><p className="text-xs text-ink-500">{p.pymeRut}</p></div> },
    { key: 'e', header: 'Estado', render: (p) => <StatusBadge estado={p.estado} /> },
    { key: 'm', header: 'Monto ofertado', render: (p) => clp(p.montoOfertado) },
    { key: 'f', header: 'Fecha', render: (p) => fecha(p.fechaPostulacion) },
  ]
  return (
    <>
      <PageHeader eyebrow="Administración" title="Postulaciones" subtitle="Todas las postulaciones de la plataforma." />
      <Card>
        <div className="flex flex-col gap-3 border-b border-ink-100 p-4 sm:flex-row">
          <Select aria-label="Estado" className="sm:w-56" value={f.estado} placeholder="Todos los estados" onChange={(e) => setF({ ...f, estado: e.target.value, page: 0 })}
            options={['ENVIADA', 'EN_REVISION', 'SELECCIONADA', 'RECHAZADA', 'RETIRADA'].map((e) => ({ value: e, label: e }))} />
          <Input aria-label="ID licitación" className="sm:w-48" type="number" placeholder="ID de licitación" value={f.licitacionId} onChange={(e) => setF({ ...f, licitacionId: e.target.value, page: 0 })} />
        </div>
        {loading && <Spinner />}
        {error && <ErrorState message={error.message} onRetry={reload} />}
        {data && <>
          <Table columns={columnas} rows={data.content} rowKey={(p) => p.id} empty={<EmptyState title="Sin postulaciones" />} />
          <Pagination page={data.page} totalPages={data.totalPages} totalElements={data.totalElements} onChange={(page) => setF({ ...f, page })} />
        </>}
      </Card>
    </>
  )
}
