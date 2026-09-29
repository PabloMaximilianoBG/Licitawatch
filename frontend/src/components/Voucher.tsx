import { useState } from 'react'
import { BadgeCheck, Download, Mail, Printer } from 'lucide-react'
import { Button, Logo } from '@/components/ui'
import { ventasApi } from '@/api/services'
import { apiError } from '@/api/client'
import { toast } from '@/store/toast'
import type { Comprobante } from '@/types'

/** Comprobante visual tipo voucher comercial (sec. 19). */
export function Voucher({ c, acciones = true }: { c: Comprobante; acciones?: boolean }) {
  const [descargando, setDescargando] = useState(false)
  const [enviando, setEnviando] = useState(false)
  const fila = (k: string, v?: string | number | null) => v !== undefined && v !== null && v !== '' && (
    <div className="flex justify-between gap-4 border-b border-dashed border-ink-200 py-2 text-sm last:border-0">
      <dt className="text-ink-500">{k}</dt><dd className="break-all text-right font-semibold text-ink-900">{v}</dd>
    </div>
  )
  const pdf = async () => {
    setDescargando(true)
    try {
      const blob = await ventasApi.comprobantePdf(c.pagoId)
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `comprobante-${c.numeroOrden}.pdf`
      a.click()
      URL.revokeObjectURL(url)
    } catch (e) { toast.error(apiError(e).message) } finally { setDescargando(false) }
  }
  const reenviar = async () => {
    setEnviando(true)
    try { toast.success((await ventasApi.reenviarComprobante(c.pagoId)).mensaje) }
    catch (e) { toast.error(apiError(e).message) } finally { setEnviando(false) }
  }
  return (
    <div className="mx-auto w-full max-w-md">
      <div className="overflow-hidden rounded-3xl bg-white shadow-xl ring-1 ring-ink-200 print:shadow-none">
        <div className="relative bg-ink-950 px-6 pb-8 pt-6 text-white">
          <div className="absolute inset-0 bg-grid-dark [background-size:24px_24px]" />
          <div className="relative flex items-center justify-between">
            <Logo light size="sm" />
            <span className="text-[10px] font-semibold uppercase tracking-[.2em] text-brand-300">Comprobante de pago</span>
          </div>
          <div className="relative mt-6 text-center">
            <span className="inline-flex items-center gap-1.5 rounded-full bg-emerald-500/15 px-3 py-1 text-xs font-bold text-emerald-300 ring-1 ring-emerald-400/30">
              <BadgeCheck className="h-4 w-4" />Estado: {c.estado}
            </span>
            <p className="mt-4 text-4xl font-extrabold tracking-tight">{c.montoFormateado}</p>
            <p className="mt-1 text-sm text-ink-300">Plan {c.plan} · {c.fecha} {c.hora}</p>
          </div>
        </div>
        <div className="relative -mt-3 h-6 bg-[radial-gradient(circle_at_12px_0,transparent_11px,white_12px)] [background-size:24px_24px]" aria-hidden />
        <dl className="px-6 pb-6">
          <p className="mb-1 text-[11px] font-bold uppercase tracking-widest text-brand-600">Cliente</p>
          {fila('Razón social / Nombre', c.clienteNombre)}
          {fila('RUT', c.clienteRut)}
          {fila('Email', c.clienteEmail)}
          {fila('Perfil', c.perfil)}
          <p className="mb-1 mt-5 text-[11px] font-bold uppercase tracking-widest text-brand-600">Transacción</p>
          {fila('Plan', c.plan)}
          {fila('Monto', c.montoFormateado)}
          {fila('Fecha', c.fecha)}
          {fila('Hora', c.hora)}
          {fila('Número de orden', c.numeroOrden)}
          {fila('ID de transacción', c.idTransaccion)}
          {fila('Código de autorización', c.codigoAutorizacion)}
          {fila('Pasarela', c.pasarela)}
          {fila('Tarjeta', c.tarjeta)}
          {fila('Tipo de pago', c.tipoPago)}
        </dl>
      </div>
      {acciones && (
        <div className="mt-4 grid grid-cols-3 gap-2 print:hidden">
          <Button variant="secondary" size="sm" onClick={pdf} loading={descargando} icon={<Download className="h-4 w-4" />}>PDF</Button>
          <Button variant="secondary" size="sm" onClick={reenviar} loading={enviando} icon={<Mail className="h-4 w-4" />}>Enviar</Button>
          <Button variant="secondary" size="sm" onClick={() => window.print()} icon={<Printer className="h-4 w-4" />}>Imprimir</Button>
        </div>
      )}
    </div>
  )
}
