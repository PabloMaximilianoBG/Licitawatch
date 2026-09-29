import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { ArrowLeft, Building2, CalendarClock, ClipboardList, FileText, MapPin, Send, Sparkles, Tag } from 'lucide-react'
import { Button, Card, CardHeader, ErrorState, NotFoundState, PageHeader, Spinner, StatusBadge } from '@/components/ui'
import { PostulacionModal } from '@/components/PostulacionModal'
import { licitacionesApi } from '@/api/services'
import { useAsync } from '@/hooks/useAsync'
import { useAuth } from '@/store/auth'
import { toast } from '@/store/toast'
import { clp, fecha } from '@/utils/format'

export default function LicitacionDetalle() {
  const { id } = useParams()
  const premium = useAuth((s) => s.suscripcion?.esPremium)
  const { data: l, loading, error, reload } = useAsync(() => licitacionesApi.obtener(id!), [id])
  const [modal, setModal] = useState(false)

  if (loading) return <Spinner />
  if (error) return error.status === 404 ? <NotFoundState message="La licitación no existe o no está disponible." /> : <ErrorState message={error.message} onRetry={reload} />
  if (!l) return null
  const abierta = l.estado === 'PUBLICADA' && (l.diasParaCierre ?? 0) >= 0

  return (
    <>
      <Link to="/dashboard/pyme/licitaciones" className="mb-4 inline-flex items-center gap-1.5 text-sm text-ink-500 hover:text-ink-900"><ArrowLeft className="h-4 w-4" />Volver a la búsqueda</Link>
      <PageHeader eyebrow={`Licitación #${l.id}`} title={l.titulo} subtitle={l.licitadorNombre}
        actions={<>
          {premium && <Link to={`/dashboard/asistente?licitacion=${l.id}`}><Button variant="secondary" icon={<Sparkles className="h-4 w-4" />}>Resumir con LicitAsist</Button></Link>}
          {l.miPostulacionId
            ? <Link to="/dashboard/pyme/postulaciones"><Button variant="secondary">Ver mi postulación</Button></Link>
            : abierta && <Button onClick={() => setModal(true)} icon={<Send className="h-4 w-4" />}>Postular</Button>}
        </>} />
      <div className="grid gap-6 lg:grid-cols-[1fr_320px]">
        <div className="space-y-6">
          <Card><CardHeader title="Descripción" icon={<FileText className="h-4 w-4" />} /><p className="whitespace-pre-line p-5 text-sm leading-relaxed text-ink-700">{l.descripcion}</p></Card>
          <Card><CardHeader title="Requisitos" icon={<ClipboardList className="h-4 w-4" />} /><p className="whitespace-pre-line p-5 text-sm leading-relaxed text-ink-700">{l.requisitos || 'El licitador no especificó requisitos adicionales.'}</p></Card>
          <Card><CardHeader title="Condiciones" icon={<Building2 className="h-4 w-4" />} /><p className="whitespace-pre-line p-5 text-sm leading-relaxed text-ink-700">{l.condiciones || 'Sin condiciones adicionales.'}</p></Card>
        </div>
        <aside className="space-y-4">
          <Card className="p-5">
            <div className="flex items-center justify-between"><StatusBadge estado={l.estado} />{l.miPostulacionEstado && <StatusBadge estado={l.miPostulacionEstado} />}</div>
            <p className="mt-5 text-xs uppercase tracking-wide text-ink-400">Monto estimado</p>
            <p className="text-3xl font-extrabold tracking-tight">{clp(l.montoEstimado)}</p>
            <dl className="mt-5 space-y-3 text-sm">
              <div className="flex items-center gap-2 text-ink-600"><CalendarClock className="h-4 w-4 text-ink-400" />Cierra el <b className="text-ink-900">{fecha(l.fechaCierre)}</b></div>
              <div className="flex items-center gap-2 text-ink-600"><Tag className="h-4 w-4 text-ink-400" />{l.categoria}</div>
              {l.region && <div className="flex items-center gap-2 text-ink-600"><MapPin className="h-4 w-4 text-ink-400" />{l.region}</div>}
            </dl>
            {abierta && !l.miPostulacionId && <Button className="mt-6" full onClick={() => setModal(true)}>Postular ahora</Button>}
            {!abierta && !l.miPostulacionId && <p className="mt-6 rounded-xl bg-ink-50 px-4 py-3 text-center text-sm text-ink-500">Esta licitación no recibe postulaciones.</p>}
          </Card>
        </aside>
      </div>
      {modal && <PostulacionModal open={modal} onClose={() => setModal(false)} titulo={l.titulo}
        onSubmit={async (f) => { await licitacionesApi.postular(l.id, f); toast.success('¡Postulación enviada! Te avisaremos por correo.'); reload() }} />}
    </>
  )
}
