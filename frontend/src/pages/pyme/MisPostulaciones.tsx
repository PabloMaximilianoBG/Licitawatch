import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Mail, Pencil, Undo2 } from 'lucide-react'
import { Button, Card, ConfirmDialog, EmptyState, ErrorState, LoadingCards, PageHeader, StatusBadge, Tabs } from '@/components/ui'
import { PostulacionModal } from '@/components/PostulacionModal'
import { postulacionesApi } from '@/api/services'
import { apiError } from '@/api/client'
import { useAsync } from '@/hooks/useAsync'
import { toast } from '@/store/toast'
import type { Postulacion } from '@/types'
import { clp, fecha } from '@/utils/format'

type Filtro = 'TODAS' | 'ACTIVAS' | 'SELECCIONADA' | 'RECHAZADA'

export default function MisPostulaciones() {
  const { data, loading, error, reload } = useAsync(postulacionesApi.mias)
  const [filtro, setFiltro] = useState<Filtro>('TODAS')
  const [editar, setEditar] = useState<Postulacion | null>(null)
  const [retirar, setRetirar] = useState<Postulacion | null>(null)
  const [procesando, setProcesando] = useState(false)
  const lista = (data ?? []).filter((p) => filtro === 'TODAS' || (filtro === 'ACTIVAS' ? ['ENVIADA', 'EN_REVISION'].includes(p.estado) : p.estado === filtro))
  const cuenta = (f: Filtro) => (data ?? []).filter((p) => f === 'TODAS' || (f === 'ACTIVAS' ? ['ENVIADA', 'EN_REVISION'].includes(p.estado) : p.estado === f)).length

  const confirmarRetiro = async () => {
    if (!retirar) return
    setProcesando(true)
    try { await postulacionesApi.retirar(retirar.id); toast.success('Postulación retirada'); setRetirar(null); reload() }
    catch (e) { toast.error(apiError(e).message) } finally { setProcesando(false) }
  }

  return (
    <>
      <PageHeader eyebrow="Seguimiento" title="Mis postulaciones" subtitle="Revisa el estado de cada propuesta enviada." />
      <div className="mb-4"><Tabs value={filtro} onChange={setFiltro} tabs={(['TODAS', 'ACTIVAS', 'SELECCIONADA', 'RECHAZADA'] as Filtro[]).map((v) => ({
        value: v, label: { TODAS: 'Todas', ACTIVAS: 'En curso', SELECCIONADA: 'Seleccionadas', RECHAZADA: 'Rechazadas' }[v], count: cuenta(v) }))} /></div>
      {loading && <LoadingCards />}
      {error && <ErrorState message={error.message} onRetry={reload} />}
      {data && lista.length === 0 && <Card><EmptyState title="Sin postulaciones" message="Cuando postules a una licitación la verás aquí."
        action={<Link to="/dashboard/pyme/licitaciones"><Button>Buscar licitaciones</Button></Link>} /></Card>}
      <div className="space-y-3">
        {lista.map((p) => (
          <Card key={p.id} className="p-5">
            <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
              <div className="min-w-0">
                <div className="flex flex-wrap items-center gap-2"><StatusBadge estado={p.estado} /><span className="text-xs text-ink-400">Postulada el {fecha(p.fechaPostulacion)}</span></div>
                <Link to={`/dashboard/pyme/licitaciones/${p.licitacionId}`} className="mt-2 block font-semibold text-ink-900 hover:text-brand-700">{p.licitacionTitulo}</Link>
                <p className="mt-1 line-clamp-2 text-sm text-ink-500">{p.propuesta}</p>
                {p.comentarioLicitador && <p className="mt-2 rounded-lg bg-ink-50 px-3 py-2 text-xs text-ink-600"><b>Comentario del licitador:</b> {p.comentarioLicitador}</p>}
                {p.estado === 'SELECCIONADA' && p.licitadorEmail && (
                  <a href={`mailto:${p.licitadorEmail}`} className="mt-3 inline-flex items-center gap-2 rounded-lg bg-emerald-50 px-3 py-2 text-xs font-semibold text-emerald-700">
                    <Mail className="h-4 w-4" />Contacta a {p.licitadorNombre}: {p.licitadorEmail}
                  </a>
                )}
              </div>
              <div className="flex shrink-0 flex-row items-center gap-4 sm:flex-col sm:items-end">
                <div className="text-right"><p className="text-xs text-ink-400">Monto ofertado</p><p className="font-bold">{clp(p.montoOfertado)}</p><p className="text-xs text-ink-500">{p.plazoEntregaDias} días</p></div>
                <div className="flex gap-2">
                  {p.estado === 'ENVIADA' && p.licitacionEstado === 'PUBLICADA' && <Button size="sm" variant="secondary" icon={<Pencil className="h-3.5 w-3.5" />} onClick={() => setEditar(p)}>Editar</Button>}
                  {['ENVIADA', 'EN_REVISION'].includes(p.estado) && <Button size="sm" variant="ghost" icon={<Undo2 className="h-3.5 w-3.5" />} onClick={() => setRetirar(p)}>Retirar</Button>}
                </div>
              </div>
            </div>
          </Card>
        ))}
      </div>
      {editar && <PostulacionModal open onClose={() => setEditar(null)} titulo={editar.licitacionTitulo} inicial={editar}
        onSubmit={async (f) => { await postulacionesApi.actualizar(editar.id, f); toast.success('Postulación actualizada'); reload() }} />}
      <ConfirmDialog open={!!retirar} onClose={() => setRetirar(null)} onConfirm={confirmarRetiro} loading={procesando} danger
        title="Retirar postulación" message={`¿Seguro que quieres retirar tu postulación a "${retirar?.licitacionTitulo}"? No podrás volver a postular a esta licitación.`} confirmText="Retirar" />
    </>
  )
}
