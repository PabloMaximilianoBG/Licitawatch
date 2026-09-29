import { useState } from 'react'
import { Button, Input, Modal, Textarea } from '@/components/ui'
import { apiError } from '@/api/client'
import type { Postulacion } from '@/types'

export interface PostulacionForm { propuesta: string; montoOfertado: number; plazoEntregaDias: number }

export function PostulacionModal({ open, onClose, titulo, inicial, onSubmit }: {
  open: boolean; onClose: () => void; titulo: string; inicial?: Postulacion; onSubmit: (f: PostulacionForm) => Promise<void>
}) {
  const [f, setF] = useState({ propuesta: inicial?.propuesta ?? '', monto: inicial ? String(inicial.montoOfertado) : '', plazo: inicial ? String(inicial.plazoEntregaDias) : '' })
  const [err, setErr] = useState<Record<string, string>>({})
  const [enviando, setEnviando] = useState(false)
  const enviar = async () => {
    const e: Record<string, string> = {}
    if (f.propuesta.trim().length < 20) e.propuesta = 'Describe tu propuesta (mínimo 20 caracteres)'
    if (!(Number(f.monto) > 0)) e.montoOfertado = 'Ingresa un monto mayor a 0'
    if (!(Number(f.plazo) >= 1)) e.plazoEntregaDias = 'Ingresa el plazo en días'
    setErr(e)
    if (Object.keys(e).length) return
    setEnviando(true)
    try {
      await onSubmit({ propuesta: f.propuesta.trim(), montoOfertado: Number(f.monto), plazoEntregaDias: Number(f.plazo) })
      onClose()
    } catch (x) {
      const a = apiError(x)
      setErr(a.errores ?? { general: a.message })
    } finally {
      setEnviando(false)
    }
  }
  return (
    <Modal open={open} onClose={onClose} title={inicial ? 'Modificar postulación' : 'Postular a la licitación'} description={titulo} size="lg"
      footer={<><Button variant="secondary" onClick={onClose}>Cancelar</Button><Button onClick={enviar} loading={enviando}>{inicial ? 'Guardar cambios' : 'Enviar postulación'}</Button></>}>
      <div className="space-y-4">
        <Textarea label="Propuesta técnica y económica" rows={7} value={f.propuesta} onChange={(e) => setF({ ...f, propuesta: e.target.value })}
          error={err.propuesta} hint="Describe experiencia, equipo, metodología y cómo cumples los requisitos." maxLength={5000} required />
        <div className="grid gap-4 sm:grid-cols-2">
          <Input label="Monto ofertado (CLP)" type="number" min={1} value={f.monto} onChange={(e) => setF({ ...f, monto: e.target.value })} error={err.montoOfertado} required />
          <Input label="Plazo de entrega (días)" type="number" min={1} max={3650} value={f.plazo} onChange={(e) => setF({ ...f, plazo: e.target.value })} error={err.plazoEntregaDias} required />
        </div>
        {err.general && <p role="alert" className="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-700">{err.general}</p>}
      </div>
    </Modal>
  )
}
