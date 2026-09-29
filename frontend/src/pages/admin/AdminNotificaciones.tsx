import { useState } from 'react'
import { Card, EmptyState, ErrorState, PageHeader, Pagination, Select, Spinner, StatusBadge, Table, type Column } from '@/components/ui'
import { adminApi } from '@/api/services'
import { useAsync } from '@/hooks/useAsync'
import type { Notificacion } from '@/types'
import { fecha } from '@/utils/format'

const TIPOS = ['CONFIRMACION_REGISTRO', 'LICITACION_PUBLICADA', 'POSTULACION_RECIBIDA', 'POSTULACION_ENVIADA', 'RESULTADO_POSTULACION',
  'LICITACION_ADJUDICADA', 'PAGO_CONFIRMADO', 'PAGO_RECHAZADO', 'GENERAL']

export default function AdminNotificaciones() {
  const [f, setF] = useState({ tipo: '', estado: '', page: 0 })
  const { data, loading, error, reload } = useAsync(() => adminApi.notificaciones({ ...f, size: 20 }), [JSON.stringify(f)])
  const cols: Column<Notificacion>[] = [
    { key: 'a', header: 'Asunto', render: (n) => <div><p className="font-medium text-ink-900">{n.asunto}</p><p className="text-xs text-ink-500">{n.tipo}</p></div> },
    { key: 'd', header: 'Destinatario', render: (n) => n.destinatario },
    { key: 'e', header: 'Envío', render: (n) => <span title={n.detalleError}><StatusBadge estado={n.estado} /></span> },
    { key: 'f', header: 'Fecha', render: (n) => fecha(n.creadoEn, true) },
  ]
  return (
    <>
      <PageHeader eyebrow="Administración" title="Notificaciones" subtitle="Historial de correos enviados por la plataforma (SMTP)." />
      <Card>
        <div className="flex flex-col gap-3 border-b border-ink-100 p-4 sm:flex-row">
          <Select aria-label="Tipo" className="sm:w-72" value={f.tipo} placeholder="Todos los tipos" onChange={(e) => setF({ ...f, tipo: e.target.value, page: 0 })} options={TIPOS.map((t) => ({ value: t, label: t }))} />
          <Select aria-label="Estado" className="sm:w-48" value={f.estado} placeholder="Todos" onChange={(e) => setF({ ...f, estado: e.target.value, page: 0 })} options={[{ value: 'ENVIADO', label: 'Enviado' }, { value: 'FALLIDO', label: 'Fallido' }]} />
        </div>
        {loading && <Spinner />}{error && <ErrorState message={error.message} onRetry={reload} />}
        {data && <><Table columns={cols} rows={data.content} rowKey={(n) => n.id} empty={<EmptyState title="Sin notificaciones" />} />
          <Pagination page={data.page} totalPages={data.totalPages} totalElements={data.totalElements} onChange={(page) => setF({ ...f, page })} /></>}
      </Card>
    </>
  )
}
