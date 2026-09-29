import { useState } from 'react'
import { Badge, Card, EmptyState, ErrorState, PageHeader, Pagination, Select, Spinner, StatusBadge, Table, Tabs, type Column } from '@/components/ui'
import { adminApi } from '@/api/services'
import { useAsync } from '@/hooks/useAsync'
import type { Pago, Suscripcion, Venta } from '@/types'
import { fecha } from '@/utils/format'

type Tab = 'ventas' | 'pagos' | 'suscripciones'

export default function AdminVentas() {
  const [tab, setTab] = useState<Tab>('pagos')
  return (
    <>
      <PageHeader eyebrow="Administración" title="Ventas, pagos y suscripciones" subtitle="Transacciones Webpay Plus y estado de los planes." />
      <div className="mb-4"><Tabs value={tab} onChange={setTab} tabs={[{ value: 'pagos', label: 'Pagos' }, { value: 'ventas', label: 'Ventas' }, { value: 'suscripciones', label: 'Suscripciones' }]} /></div>
      {tab === 'pagos' && <Pagos />}
      {tab === 'ventas' && <Ventas />}
      {tab === 'suscripciones' && <Suscripciones />}
    </>
  )
}

function Pagos() {
  const [f, setF] = useState({ estado: '', page: 0 })
  const { data, loading, error, reload } = useAsync(() => adminApi.pagos({ ...f, size: 20 }), [JSON.stringify(f)])
  const cols: Column<Pago>[] = [
    { key: 'o', header: 'Orden', render: (p) => <span className="font-mono text-xs">{p.numeroOrden}</span> },
    { key: 'c', header: 'Cliente', render: (p) => <div><p>{p.clienteNombre ?? `Usuario ${p.usuarioId}`}</p><p className="text-xs text-ink-500">{p.clienteEmail}</p></div> },
    { key: 'pl', header: 'Plan', render: (p) => <StatusBadge estado={p.plan} /> },
    { key: 'm', header: 'Monto', render: (p) => <b>{p.montoFormateado}</b> },
    { key: 'e', header: 'Estado', render: (p) => <StatusBadge estado={p.estado} /> },
    { key: 't', header: 'Tarjeta', render: (p) => p.tarjeta ?? '—', hideOnMobile: true },
    { key: 'f', header: 'Fecha', render: (p) => fecha(p.fechaTransaccion ?? p.fecha, !!p.fechaTransaccion) },
  ]
  return (
    <Card>
      <div className="border-b border-ink-100 p-4"><Select aria-label="Estado" className="sm:w-56" value={f.estado} placeholder="Todos los estados"
        onChange={(e) => setF({ estado: e.target.value, page: 0 })} options={['PENDIENTE', 'APROBADO', 'RECHAZADO', 'ANULADO'].map((e) => ({ value: e, label: e }))} /></div>
      {loading && <Spinner />}{error && <ErrorState message={error.message} onRetry={reload} />}
      {data && <><Table columns={cols} rows={data.content} rowKey={(p) => p.id} empty={<EmptyState title="Sin pagos" />} />
        <Pagination page={data.page} totalPages={data.totalPages} totalElements={data.totalElements} onChange={(page) => setF({ ...f, page })} /></>}
    </Card>
  )
}

function Ventas() {
  const [page, setPage] = useState(0)
  const { data, loading, error, reload } = useAsync(() => adminApi.ventas(page, 20), [page])
  const cols: Column<Venta>[] = [
    { key: 'id', header: 'Venta', render: (v) => `#${v.id}` },
    { key: 'u', header: 'Usuario', render: (v) => `#${v.usuarioId}` },
    { key: 'p', header: 'Plan', render: (v) => <StatusBadge estado={v.plan} /> },
    { key: 'm', header: 'Monto', render: (v) => <b>{v.montoFormateado}</b> },
    { key: 'e', header: 'Pago', render: (v) => <StatusBadge estado={v.estadoPago} /> },
    { key: 's', header: 'Suscripción', render: (v) => <StatusBadge estado={v.estadoSuscripcion} /> },
    { key: 'f', header: 'Fecha', render: (v) => fecha(v.fecha) },
  ]
  return (
    <Card>
      {loading && <Spinner />}{error && <ErrorState message={error.message} onRetry={reload} />}
      {data && <><Table columns={cols} rows={data.content} rowKey={(v) => v.id} empty={<EmptyState title="Sin ventas" />} />
        <Pagination page={data.page} totalPages={data.totalPages} totalElements={data.totalElements} onChange={setPage} /></>}
    </Card>
  )
}

function Suscripciones() {
  const [f, setF] = useState({ estado: '', plan: '', page: 0 })
  const { data, loading, error, reload } = useAsync(() => adminApi.suscripciones({ ...f, size: 20 }), [JSON.stringify(f)])
  const cols: Column<Suscripcion>[] = [
    { key: 'c', header: 'Cliente', render: (s) => <div><p>{s.clienteNombre ?? `Usuario ${s.usuarioId}`}</p><p className="text-xs text-ink-500">{s.clienteEmail}</p></div> },
    { key: 'p', header: 'Plan', render: (s) => <StatusBadge estado={s.plan.nombre} /> },
    { key: 'e', header: 'Estado', render: (s) => <StatusBadge estado={s.estado} /> },
    { key: 'i', header: 'Inicio', render: (s) => fecha(s.fechaInicio) },
    { key: 'f', header: 'Vence', render: (s) => s.fechaFin ? <span>{fecha(s.fechaFin)} {s.diasRestantes !== undefined && s.estado === 'ACTIVA' && <Badge tono="blue">{s.diasRestantes} d</Badge>}</span> : '—' },
  ]
  return (
    <Card>
      <div className="flex flex-col gap-3 border-b border-ink-100 p-4 sm:flex-row">
        <Select aria-label="Plan" className="sm:w-48" value={f.plan} placeholder="Todos los planes" onChange={(e) => setF({ ...f, plan: e.target.value, page: 0 })} options={[{ value: 'ESTANDAR', label: 'Estándar' }, { value: 'PREMIUM', label: 'Premium' }]} />
        <Select aria-label="Estado" className="sm:w-48" value={f.estado} placeholder="Todos los estados" onChange={(e) => setF({ ...f, estado: e.target.value, page: 0 })} options={['PENDIENTE', 'ACTIVA', 'VENCIDA', 'CANCELADA'].map((e) => ({ value: e, label: e }))} />
      </div>
      {loading && <Spinner />}{error && <ErrorState message={error.message} onRetry={reload} />}
      {data && <><Table columns={cols} rows={data.content} rowKey={(s) => s.id} empty={<EmptyState title="Sin suscripciones" />} />
        <Pagination page={data.page} totalPages={data.totalPages} totalElements={data.totalElements} onChange={(page) => setF({ ...f, page })} /></>}
    </Card>
  )
}
