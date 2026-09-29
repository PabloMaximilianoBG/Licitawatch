import { useEffect, useState, type FormEvent } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ArrowLeft, Save, Send } from 'lucide-react'
import { Button, Card, ErrorState, Input, PageHeader, Select, Spinner, Textarea } from '@/components/ui'
import { licitacionesApi } from '@/api/services'
import { apiError } from '@/api/client'
import { toast } from '@/store/toast'
import type { LicitacionInput } from '@/types'
import { CATEGORIAS, REGIONES } from '@/utils/format'

const vacio: LicitacionInput = { titulo: '', descripcion: '', requisitos: '', condiciones: '', categoria: '', region: '', montoEstimado: '', fechaCierre: '', publicar: false }

export default function LicitacionForm() {
  const { id } = useParams()
  const edicion = !!id
  const nav = useNavigate()
  const [f, setF] = useState<LicitacionInput>(vacio)
  const [estado, setEstado] = useState<string>('BORRADOR')
  const [cargando, setCargando] = useState(edicion)
  const [errorCarga, setErrorCarga] = useState<string | null>(null)
  const [errores, setErrores] = useState<Record<string, string>>({})
  const [guardando, setGuardando] = useState<'borrador' | 'publicar' | null>(null)

  useEffect(() => {
    if (!id) return
    licitacionesApi.obtener(id).then((l) => {
      setEstado(l.estado)
      setF({ titulo: l.titulo, descripcion: l.descripcion, requisitos: l.requisitos ?? '', condiciones: l.condiciones ?? '', categoria: l.categoria,
        region: l.region ?? '', montoEstimado: l.montoEstimado, fechaCierre: l.fechaCierre, publicar: false })
    }).catch((e) => setErrorCarga(apiError(e).message)).finally(() => setCargando(false))
  }, [id])

  const set = (k: keyof LicitacionInput) => (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>) =>
    setF((p) => ({ ...p, [k]: k === 'montoEstimado' ? (e.target.value === '' ? '' : Number(e.target.value)) : e.target.value }))

  const validar = () => {
    const e: Record<string, string> = {}
    if (f.titulo.trim().length < 5) e.titulo = 'El título debe tener al menos 5 caracteres'
    if (f.descripcion.trim().length < 20) e.descripcion = 'Describe la licitación (mínimo 20 caracteres)'
    if (!f.categoria) e.categoria = 'Selecciona una categoría'
    if (!(Number(f.montoEstimado) > 0)) e.montoEstimado = 'Ingresa un monto mayor a 0'
    if (!f.fechaCierre || new Date(`${f.fechaCierre}T00:00:00`) <= new Date()) e.fechaCierre = 'La fecha de cierre debe ser futura'
    setErrores(e)
    return !Object.keys(e).length
  }

  const guardar = async (publicar: boolean, ev?: FormEvent) => {
    ev?.preventDefault()
    if (!validar()) return
    setGuardando(publicar ? 'publicar' : 'borrador')
    try {
      const body = { ...f, publicar }
      const l = edicion ? await licitacionesApi.actualizar(Number(id), body) : await licitacionesApi.crear(body)
      toast.success(publicar ? 'Licitación publicada. Te enviamos un correo de confirmación.' : 'Licitación guardada')
      nav(`/dashboard/licitador/licitaciones/${l.id}`)
    } catch (e) {
      const a = apiError(e)
      if (a.errores) setErrores(a.errores)
      toast.error(a.message)
    } finally {
      setGuardando(null)
    }
  }

  if (cargando) return <Spinner />
  if (errorCarga) return <ErrorState message={errorCarga} />
  const minFecha = new Date(Date.now() + 86400000).toISOString().slice(0, 10)

  return (
    <>
      <Link to="/dashboard/licitador/licitaciones" className="mb-4 inline-flex items-center gap-1.5 text-sm text-ink-500 hover:text-ink-900"><ArrowLeft className="h-4 w-4" />Mis licitaciones</Link>
      <PageHeader eyebrow={edicion ? `Editar #${id}` : 'Nueva licitación'} title={edicion ? 'Editar licitación' : 'Crear licitación'}
        subtitle="Define el alcance, los requisitos, las condiciones y la fecha de cierre." />
      <form onSubmit={(e) => guardar(false, e)} className="grid gap-6 lg:grid-cols-[1fr_320px]" noValidate>
        <Card className="space-y-5 p-6">
          <Input label="Título" value={f.titulo} onChange={set('titulo')} error={errores.titulo} maxLength={200} placeholder="Ej: Servicio de aseo industrial para planta" required />
          <Textarea label="Descripción" rows={6} value={f.descripcion} onChange={set('descripcion')} error={errores.descripcion} maxLength={5000} required
            hint="Explica qué necesitas, el alcance y los entregables esperados." />
          <Textarea label="Requisitos" rows={4} value={f.requisitos} onChange={set('requisitos')} error={errores.requisitos} maxLength={5000}
            hint="Experiencia, certificaciones, documentos o garantías que deben presentar las Pymes." />
          <Textarea label="Condiciones" rows={4} value={f.condiciones} onChange={set('condiciones')} error={errores.condiciones} maxLength={5000}
            hint="Forma de pago, plazos, multas u otras condiciones comerciales." />
        </Card>
        <aside className="space-y-6">
          <Card className="space-y-4 p-6">
            <Select label="Categoría" value={f.categoria} onChange={set('categoria')} error={errores.categoria} placeholder="Selecciona…" options={CATEGORIAS.map((c) => ({ value: c, label: c }))} required />
            <Select label="Región" value={f.region} onChange={set('region')} placeholder="Todo Chile" options={REGIONES.map((c) => ({ value: c, label: c }))} />
            <Input label="Monto estimado (CLP)" type="number" min={1} value={f.montoEstimado} onChange={set('montoEstimado')} error={errores.montoEstimado} required />
            <Input label="Fecha de cierre" type="date" min={minFecha} value={f.fechaCierre} onChange={set('fechaCierre')} error={errores.fechaCierre} required />
          </Card>
          <Card className="space-y-3 p-6">
            {estado === 'BORRADOR' && <Button type="button" full onClick={() => guardar(true)} loading={guardando === 'publicar'} icon={<Send className="h-4 w-4" />}>Publicar ahora</Button>}
            <Button type="submit" full variant="secondary" loading={guardando === 'borrador'} icon={<Save className="h-4 w-4" />}>
              {estado === 'BORRADOR' ? 'Guardar como borrador' : 'Guardar cambios'}
            </Button>
            <p className="text-xs text-ink-500">Al publicar, las Pymes podrán verla y postular. El plan Estándar permite 3 publicaciones al mes.</p>
          </Card>
        </aside>
      </form>
    </>
  )
}
