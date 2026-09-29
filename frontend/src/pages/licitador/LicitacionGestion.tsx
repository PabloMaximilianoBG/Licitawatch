import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ArrowLeft, Award, CalendarClock, Eye, Lock, Mail, Pencil, RotateCcw, Send, Sparkles, Trash2, XCircle } from 'lucide-react'
import {
  Badge, Button, Card, CardHeader, ConfirmDialog, EmptyState, ErrorState, Modal, NotFoundState, PageHeader, Spinner,
  StatusBadge, Textarea,
} from '@/components/ui'
import { licitacionesApi, postulacionesApi } from '@/api/services'
import { apiError } from '@/api/client'
import { useAsync } from '@/hooks/useAsync'
import { useAuth } from '@/store/auth'
import { toast } from '@/store/toast'
import type { Postulacion } from '@/types'
import { clp, fecha } from '@/utils/format'

type Accion = { tipo: 'estado'; estado: string; titulo: string; mensaje: string; danger?: boolean } | { tipo: 'eliminar' }

export default function LicitacionGestion() {
  const { id } = useParams()
  const nav = useNavigate()
  const premium = useAuth((s) => s.suscripcion?.esPremium)
  const lic = useAsync(() => licitacionesApi.obtener(id!), [id])
  const posts = useAsync(() => licitacionesApi.postulaciones(id!), [id])
  const [accion, setAccion] = useState<Accion | null>(null)
  const [procesando, setProcesando] = useState(false)
  const [detalle, setDetalle] = useState<Postulacion | null>(null)
  const [seleccion, setSeleccion] = useState<{ p: Postulacion; tipo: 'seleccionar' | 'rechazar' } | null>(null)
  const [comentario, setComentario] = useState('')

  if (lic.loading) return <Spinner />
  if (lic.error) return lic.error.status === 404 ? <NotFoundState /> : <ErrorState message={lic.error.message} onRetry={lic.reload} />
  const l = lic.data!
  const recargar = () => { lic.reload(); posts.reload() }

  const ejecutar = async () => {
    if (!accion) return
    setProcesando(true)
    try {
      if (accion.tipo === 'eliminar') {
        await licitacionesApi.eliminar(l.id)
        toast.success('Licitación eliminada')
        nav('/dashboard/licitador/licitaciones')
        return
      }
      await licitacionesApi.cambiarEstado(l.id, accion.estado)
      toast.success('Estado actualizado')
      setAccion(null)
      recargar()
    } catch (e) { toast.error(apiError(e).message) } finally { setProcesando(false) }
  }

  const decidir = async () => {
    if (!seleccion) return
    setProcesando(true)
    try {
      if (seleccion.tipo === 'seleccionar') {
        await postulacionesApi.seleccionar(seleccion.p.id, comentario || undefined)
        toast.success('Postulante seleccionado. Notificamos a todas las Pymes por correo.')
      } else {
        await postulacionesApi.evaluar(seleccion.p.id, 'RECHAZADA', comentario || undefined)
        toast.success('Postulación rechazada y notificada')
      }
      setSeleccion(null)
      setComentario('')
      recargar()
    } catch (e) { toast.error(apiError(e).message) } finally { setProcesando(false) }
  }

  const enRevision = async (p: Postulacion) => {
    try { await postulacionesApi.evaluar(p.id, 'EN_REVISION'); toast.success('Marcada en revisión'); posts.reload() }
    catch (e) { toast.error(apiError(e).message) }
  }

  const adjudicable = l.estado === 'PUBLICADA' || l.estado === 'CERRADA'
  const lista = posts.data ?? []
  const mejorMonto = lista.filter((p) => ['ENVIADA', 'EN_REVISION', 'SELECCIONADA'].includes(p.estado)).reduce((m, p) => Math.min(m, p.montoOfertado), Infinity)

  return (
    <>
      <Link to="/dashboard/licitador/licitaciones" className="mb-4 inline-flex items-center gap-1.5 text-sm text-ink-500 hover:text-ink-900"><ArrowLeft className="h-4 w-4" />Mis licitaciones</Link>
      <PageHeader eyebrow={`Licitación #${l.id}`} title={l.titulo}
        subtitle={<span className="inline-flex flex-wrap items-center gap-2"><StatusBadge estado={l.estado} />{clp(l.montoEstimado)} · <CalendarClock className="h-4 w-4" />Cierra {fecha(l.fechaCierre)}</span>}
        actions={<>
          {premium && <Link to={`/dashboard/asistente?licitacion=${l.id}`}><Button variant="secondary" icon={<Sparkles className="h-4 w-4" />}>Analizar con LicitAsist</Button></Link>}
          {['BORRADOR', 'PUBLICADA'].includes(l.estado) && <Link to={`/dashboard/licitador/licitaciones/${l.id}/editar`}><Button variant="secondary" icon={<Pencil className="h-4 w-4" />}>Editar</Button></Link>}
          {l.estado === 'BORRADOR' && <Button icon={<Send className="h-4 w-4" />} onClick={() => setAccion({ tipo: 'estado', estado: 'PUBLICADA', titulo: 'Publicar licitación', mensaje: 'La licitación quedará visible para todas las Pymes.' })}>Publicar</Button>}
          {l.estado === 'PUBLICADA' && <Button variant="secondary" icon={<Lock className="h-4 w-4" />} onClick={() => setAccion({ tipo: 'estado', estado: 'CERRADA', titulo: 'Cerrar licitación', mensaje: 'No se recibirán más postulaciones. Podrás seleccionar un postulante.' })}>Cerrar</Button>}
          {l.estado === 'CERRADA' && <Button variant="secondary" icon={<RotateCcw className="h-4 w-4" />} onClick={() => setAccion({ tipo: 'estado', estado: 'PUBLICADA', titulo: 'Reabrir licitación', mensaje: 'Volverá a recibir postulaciones hasta la fecha de cierre.' })}>Reabrir</Button>}
          {['BORRADOR', 'PUBLICADA', 'CERRADA'].includes(l.estado) && <Button variant="ghost" icon={<XCircle className="h-4 w-4" />} onClick={() => setAccion({ tipo: 'estado', estado: 'CANCELADA', titulo: 'Cancelar licitación', mensaje: 'Las postulaciones activas serán rechazadas y notificadas. Esta acción no se puede deshacer.', danger: true })}>Cancelar</Button>}
          {!l.cantidadPostulaciones && <Button variant="ghost" className="text-red-600" icon={<Trash2 className="h-4 w-4" />} onClick={() => setAccion({ tipo: 'eliminar' })}>Eliminar</Button>}
        </>} />

      <div className="grid gap-6 lg:grid-cols-[1fr_340px]">
        <Card>
          <CardHeader title="Postulaciones recibidas" subtitle={`${lista.length} postulantes`} />
          {posts.loading && <Spinner />}
          {posts.error && <ErrorState message={posts.error.message} onRetry={posts.reload} />}
          {posts.data && lista.length === 0 && <EmptyState title="Aún no hay postulaciones" message={l.estado === 'PUBLICADA' ? 'Las Pymes verán tu licitación y podrán postular hasta la fecha de cierre.' : 'Publica la licitación para recibir postulaciones.'} />}
          <ul className="divide-y divide-ink-100">
            {lista.map((p) => (
              <li key={p.id} className="p-5">
                <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                  <div className="min-w-0">
                    <div className="flex flex-wrap items-center gap-2">
                      <p className="font-semibold text-ink-900">{p.pymeNombre ?? `Pyme #${p.pymeId}`}</p>
                      <StatusBadge estado={p.estado} />
                      {p.montoOfertado === mejorMonto && lista.length > 1 && <Badge tono="green">Mejor oferta</Badge>}
                    </div>
                    <p className="mt-0.5 text-xs text-ink-500">RUT {p.pymeRut} · postuló el {fecha(p.fechaPostulacion)}</p>
                    <p className="mt-2 line-clamp-2 text-sm text-ink-600">{p.propuesta}</p>
                  </div>
                  <div className="shrink-0 text-left sm:text-right">
                    <p className="text-lg font-bold">{clp(p.montoOfertado)}</p>
                    <p className="text-xs text-ink-500">{p.plazoEntregaDias} días de entrega</p>
                  </div>
                </div>
                <div className="mt-3 flex flex-wrap gap-2">
                  <Button size="sm" variant="secondary" icon={<Eye className="h-3.5 w-3.5" />} onClick={() => setDetalle(p)}>Ver propuesta</Button>
                  {p.estado === 'ENVIADA' && adjudicable && <Button size="sm" variant="ghost" onClick={() => enRevision(p)}>Marcar en revisión</Button>}
                  {['ENVIADA', 'EN_REVISION'].includes(p.estado) && adjudicable && <>
                    <Button size="sm" variant="success" icon={<Award className="h-3.5 w-3.5" />} onClick={() => setSeleccion({ p, tipo: 'seleccionar' })}>Seleccionar</Button>
                    <Button size="sm" variant="ghost" className="text-red-600" onClick={() => setSeleccion({ p, tipo: 'rechazar' })}>Rechazar</Button>
                  </>}
                  {p.estado === 'SELECCIONADA' && p.pymeEmail && <a href={`mailto:${p.pymeEmail}`}><Button size="sm" variant="secondary" icon={<Mail className="h-3.5 w-3.5" />}>Contactar</Button></a>}
                </div>
              </li>
            ))}
          </ul>
        </Card>
        <aside className="space-y-4">
          <Card><CardHeader title="Descripción" /><p className="whitespace-pre-line p-5 text-sm text-ink-700">{l.descripcion}</p></Card>
          <Card><CardHeader title="Requisitos" /><p className="whitespace-pre-line p-5 text-sm text-ink-700">{l.requisitos || '—'}</p></Card>
          <Card><CardHeader title="Condiciones" /><p className="whitespace-pre-line p-5 text-sm text-ink-700">{l.condiciones || '—'}</p></Card>
        </aside>
      </div>

      <ConfirmDialog open={!!accion} onClose={() => setAccion(null)} onConfirm={ejecutar} loading={procesando}
        danger={accion?.tipo === 'eliminar' || (accion?.tipo === 'estado' && accion.danger)}
        title={accion?.tipo === 'eliminar' ? 'Eliminar licitación' : accion?.titulo ?? ''}
        message={accion?.tipo === 'eliminar' ? 'La licitación se eliminará definitivamente.' : accion?.mensaje ?? ''} />

      <Modal open={!!detalle} onClose={() => setDetalle(null)} title={detalle?.pymeNombre ?? 'Propuesta'} description={detalle && `${clp(detalle.montoOfertado)} · ${detalle.plazoEntregaDias} días`} size="lg">
        {detalle && <div className="space-y-4 text-sm">
          <p className="whitespace-pre-line leading-relaxed text-ink-700">{detalle.propuesta}</p>
          <div className="rounded-xl bg-ink-50 p-4 text-xs text-ink-600">RUT {detalle.pymeRut} · {detalle.pymeEmail}</div>
        </div>}
      </Modal>

      <Modal open={!!seleccion} onClose={() => setSeleccion(null)}
        title={seleccion?.tipo === 'seleccionar' ? 'Seleccionar postulante' : 'Rechazar postulación'}
        description={seleccion?.p.pymeNombre}
        footer={<><Button variant="secondary" onClick={() => setSeleccion(null)}>Cancelar</Button>
          <Button variant={seleccion?.tipo === 'seleccionar' ? 'success' : 'danger'} loading={procesando} onClick={decidir}>{seleccion?.tipo === 'seleccionar' ? 'Adjudicar' : 'Rechazar'}</Button></>}>
        <p className="mb-4 text-sm text-ink-600">{seleccion?.tipo === 'seleccionar'
          ? 'La licitación quedará ADJUDICADA, las demás postulaciones activas serán rechazadas y ambas partes recibirán un correo.'
          : 'La Pyme recibirá un correo con el resultado.'}</p>
        <Textarea label="Comentario (opcional)" rows={3} value={comentario} onChange={(e) => setComentario(e.target.value)} maxLength={2000} />
      </Modal>
    </>
  )
}
