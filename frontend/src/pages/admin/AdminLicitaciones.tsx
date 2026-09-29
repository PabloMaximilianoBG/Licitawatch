import { useState } from 'react'
import { Ban, PlayCircle, Trash2 } from 'lucide-react'
import { Button, Card, ConfirmDialog, EmptyState, ErrorState, Input, Modal, PageHeader, Pagination, SearchBar, Select, Spinner, StatusBadge, Table, type Column } from '@/components/ui'
import { adminApi } from '@/api/services'
import { apiError } from '@/api/client'
import { useAsync } from '@/hooks/useAsync'
import { toast } from '@/store/toast'
import type { Licitacion } from '@/types'
import { clp, fecha } from '@/utils/format'

export default function AdminLicitaciones() {
  const [f, setF] = useState({ q: '', estado: '', page: 0 })
  const { data, loading, error, reload } = useAsync(() => adminApi.licitaciones({ ...f, size: 15 }), [JSON.stringify(f)])
  const [suspender, setSuspender] = useState<Licitacion | null>(null)
  const [motivo, setMotivo] = useState('')
  const [eliminar, setEliminar] = useState<Licitacion | null>(null)
  const [procesando, setProcesando] = useState(false)

  const moderar = async (l: Licitacion, accion: 'SUSPENDER' | 'REACTIVAR', m?: string) => {
    setProcesando(true)
    try { await adminApi.moderar(l.id, accion, m); toast.success(accion === 'SUSPENDER' ? 'Licitación suspendida' : 'Licitación reactivada'); setSuspender(null); setMotivo(''); reload() }
    catch (e) { toast.error(apiError(e).message) } finally { setProcesando(false) }
  }
  const borrar = async () => {
    if (!eliminar) return
    setProcesando(true)
    try { await adminApi.eliminarLicitacion(eliminar.id); toast.success('Licitación eliminada'); setEliminar(null); reload() }
    catch (e) { toast.error(apiError(e).message) } finally { setProcesando(false) }
  }

  const columnas: Column<Licitacion>[] = [
    { key: 't', header: 'Licitación', render: (l) => <div><p className="font-semibold text-ink-900">{l.titulo}</p><p className="text-xs text-ink-500">#{l.id} · {l.licitadorNombre ?? `Licitador ${l.licitadorId}`}</p></div> },
    { key: 'e', header: 'Estado', render: (l) => <StatusBadge estado={l.estado} /> },
    { key: 'm', header: 'Monto', render: (l) => clp(l.montoEstimado) },
    { key: 'c', header: 'Cierre', render: (l) => fecha(l.fechaCierre) },
    { key: 'p', header: 'Postulaciones', render: (l) => l.cantidadPostulaciones ?? 0 },
    { key: 'a', header: 'Moderación', className: 'text-right', render: (l) => (
      <div className="flex justify-end gap-1">
        {l.estado === 'SUSPENDIDA'
          ? <Button size="sm" variant="ghost" className="text-emerald-600" icon={<PlayCircle className="h-4 w-4" />} onClick={() => moderar(l, 'REACTIVAR')}>Reactivar</Button>
          : !['ADJUDICADA', 'CANCELADA'].includes(l.estado) && <Button size="sm" variant="ghost" className="text-amber-600" icon={<Ban className="h-4 w-4" />} onClick={() => setSuspender(l)}>Suspender</Button>}
        <Button size="sm" variant="ghost" className="text-red-600" aria-label="Eliminar" onClick={() => setEliminar(l)}><Trash2 className="h-4 w-4" /></Button>
      </div>) },
  ]
  return (
    <>
      <PageHeader eyebrow="Administración" title="Licitaciones" subtitle="Consulta y modera todas las licitaciones de la plataforma." />
      <Card>
        <div className="flex flex-col gap-3 border-b border-ink-100 p-4 lg:flex-row">
          <SearchBar value={f.q} placeholder="Buscar licitaciones" onSearch={(q) => setF({ ...f, q, page: 0 })} />
          <Select aria-label="Estado" className="lg:w-52" value={f.estado} placeholder="Todos los estados" onChange={(e) => setF({ ...f, estado: e.target.value, page: 0 })}
            options={['BORRADOR', 'PUBLICADA', 'CERRADA', 'ADJUDICADA', 'CANCELADA', 'SUSPENDIDA'].map((e) => ({ value: e, label: e }))} />
        </div>
        {loading && <Spinner />}
        {error && <ErrorState message={error.message} onRetry={reload} />}
        {data && <>
          <Table columns={columnas} rows={data.content} rowKey={(l) => l.id} empty={<EmptyState title="Sin licitaciones" />} />
          <Pagination page={data.page} totalPages={data.totalPages} totalElements={data.totalElements} onChange={(page) => setF({ ...f, page })} />
        </>}
      </Card>
      <Modal open={!!suspender} onClose={() => setSuspender(null)} title="Suspender licitación" description={suspender?.titulo}
        footer={<><Button variant="secondary" onClick={() => setSuspender(null)}>Cancelar</Button><Button variant="danger" loading={procesando} onClick={() => suspender && moderar(suspender, 'SUSPENDER', motivo)}>Suspender</Button></>}>
        <p className="mb-4 text-sm text-ink-600">Dejará de ser visible para las Pymes. El licitador recibirá un correo con el motivo.</p>
        <Input label="Motivo" value={motivo} onChange={(e) => setMotivo(e.target.value)} maxLength={500} />
      </Modal>
      <ConfirmDialog open={!!eliminar} onClose={() => setEliminar(null)} onConfirm={borrar} loading={procesando} danger title="Eliminar licitación"
        message={`Se eliminará "${eliminar?.titulo}" junto con sus postulaciones. Esta acción no se puede deshacer.`} confirmText="Eliminar" />
    </>
  )
}
