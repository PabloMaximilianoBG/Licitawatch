import { useNavigate } from 'react-router-dom'
import { Receipt } from 'lucide-react'
import { Button, Card, EmptyState, ErrorState, PageHeader, Spinner, StatusBadge, Table, type Column } from '@/components/ui'
import { ventasApi } from '@/api/services'
import { useAsync } from '@/hooks/useAsync'
import type { Pago } from '@/types'
import { etiquetaEstado, fecha } from '@/utils/format'

export default function HistorialPagos() {
  const nav = useNavigate()
  const { data, loading, error, reload } = useAsync(ventasApi.historial)
  const cols: Column<Pago>[] = [
    { key: 'f', header: 'Fecha', render: (p) => fecha(p.fechaTransaccion ?? p.fecha, !!p.fechaTransaccion) },
    { key: 'p', header: 'Plan', render: (p) => etiquetaEstado[p.plan] ?? p.plan },
    { key: 'm', header: 'Monto', render: (p) => <b>{p.montoFormateado}</b> },
    { key: 'e', header: 'Estado', render: (p) => <StatusBadge estado={p.estado} /> },
    { key: 'o', header: 'N° de orden', render: (p) => <span className="font-mono text-xs">{p.numeroOrden}</span> },
    { key: 't', header: 'ID de transacción', hideOnMobile: true, render: (p) => <span className="block max-w-[140px] truncate font-mono text-xs" title={p.idTransaccion}>{p.idTransaccion ?? '—'}</span> },
    { key: 'a', header: '', className: 'text-right', render: (p) => p.comprobanteDisponible
      ? <Button size="sm" variant="secondary" icon={<Receipt className="h-3.5 w-3.5" />} onClick={() => nav(`/dashboard/pagos/${p.id}/comprobante`)}>Ver comprobante</Button>
      : <span className="text-xs text-ink-400">Sin comprobante</span> },
  ]
  return (
    <>
      <PageHeader eyebrow="Cuenta · Suscripción" title="Historial de pagos" subtitle="Todas tus transacciones con Webpay Plus." />
      <Card>
        {loading && <Spinner />}
        {error && <ErrorState message={error.message} onRetry={reload} />}
        {data && <Table columns={cols} rows={data} rowKey={(p) => p.id} empty={<EmptyState title="Aún no tienes pagos" message="Cuando contrates Premium verás aquí tus comprobantes." />} />}
      </Card>
    </>
  )
}
