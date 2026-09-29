import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { Ban, CheckCircle2, Clock, XCircle } from 'lucide-react'
import { Button, ErrorState, Logo, Spinner } from '@/components/ui'
import { Voucher } from '@/components/Voucher'
import { ventasApi } from '@/api/services'
import { apiError } from '@/api/client'
import { rutaInicio, useAuth } from '@/store/auth'
import type { Comprobante, Pago } from '@/types'

export default function PagoResultado() {
  const [params] = useSearchParams()
  const orden = params.get('orden')
  const errorRetorno = params.get('error')
  const { rol, setSuscripcion } = useAuth()
  const [pago, setPago] = useState<Pago | null>(null)
  const [voucher, setVoucher] = useState<Comprobante | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [cargando, setCargando] = useState(true)

  useEffect(() => {
    if (!orden) { setCargando(false); setError('No se recibió el número de orden del pago.'); return }
    ventasApi.pagoPorOrden(orden)
      .then(async (p) => {
        setPago(p)
        if (p.estado === 'APROBADO') setVoucher(await ventasApi.comprobante(p.id))
        ventasApi.suscripcion().then(setSuscripcion).catch(() => undefined)
      })
      .catch((e) => setError(apiError(e).message))
      .finally(() => setCargando(false))
  }, [orden, setSuscripcion])

  const cfg = pago && {
    APROBADO: { icon: <CheckCircle2 className="h-12 w-12 text-emerald-500" />, t: '¡Pago aprobado!', d: 'Tu plan Premium ya está activo. Te enviamos el comprobante por correo.' },
    RECHAZADO: { icon: <XCircle className="h-12 w-12 text-red-500" />, t: 'Pago rechazado', d: pago.mensaje },
    ANULADO: { icon: <Ban className="h-12 w-12 text-ink-400" />, t: 'Pago anulado', d: pago.mensaje },
    PENDIENTE: { icon: <Clock className="h-12 w-12 text-amber-500" />, t: 'Pago pendiente', d: pago.mensaje },
  }[pago.estado]

  return (
    <div className="min-h-screen bg-ink-50 px-4 py-8 sm:py-12">
      <div className="mx-auto max-w-lg">
        <div className="mb-8 text-center"><Logo /></div>
        {cargando && <Spinner label="Consultando el resultado del pago…" />}
        {!cargando && error && <ErrorState message={errorRetorno ? `Webpay devolvió un error (${errorRetorno}). ${error}` : error} />}
        {cfg && pago && (
          <div className="animate-fade-in">
            <div className="card mb-6 p-6 text-center">
              <div className="mx-auto w-fit">{cfg.icon}</div>
              <h1 className="mt-3 text-2xl font-bold">{cfg.t}</h1>
              <p className="mt-2 text-sm text-ink-600">{cfg.d}</p>
              <p className="mt-3 text-xs text-ink-400">Orden {pago.numeroOrden} · {pago.montoFormateado}</p>
            </div>
            {voucher && <Voucher c={voucher} />}
            <div className="mt-6 flex flex-col gap-2 sm:flex-row">
              <Link to={rutaInicio(rol)} className="flex-1"><Button full>Ir al dashboard</Button></Link>
              {pago.estado === 'APROBADO'
                ? <Link to="/dashboard/asistente" className="flex-1"><Button full variant="secondary">Probar LicitAsist</Button></Link>
                : <Link to="/planes" className="flex-1"><Button full variant="secondary">Intentar nuevamente</Button></Link>}
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
