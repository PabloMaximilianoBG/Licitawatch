import { useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { ArrowLeft, Bot, Crown, Lock, ShieldCheck } from 'lucide-react'
import { Button, ErrorState, Logo, Spinner } from '@/components/ui'
import { ventasApi } from '@/api/services'
import { apiError } from '@/api/client'
import { useAsync } from '@/hooks/useAsync'
import { useAuth } from '@/store/auth'
import { etiquetaEstado } from '@/utils/format'

/** Resumen de compra y redirección a Webpay Plus. Premium lo activa SOLO el backend tras confirmar con Transbank. */
export default function Pago() {
  const [params] = useSearchParams()
  const planId = params.get('plan')
  const usuario = useAuth((s) => s.usuario)
  const { data: plan, loading, error, reload } = useAsync(() => planId ? ventasApi.plan(planId) : ventasApi.planes().then((p) => p.find((x) => x.nombre === 'PREMIUM')!), [planId])
  const [procesando, setProcesando] = useState(false)
  const [fallo, setFallo] = useState<string | null>(null)

  const pagar = async () => {
    if (!plan) return
    setProcesando(true)
    setFallo(null)
    try {
      const venta = await ventasApi.crearVenta(plan.id) // 1) MS-Ventas crea venta pendiente
      const tx = await ventasApi.crearTransaccion(venta.id) // 2) se crea la transacción Webpay
      // 3) Webpay exige un POST de formulario con token_ws hacia la URL entregada por Transbank
      const form = document.createElement('form')
      form.method = 'POST'
      form.action = tx.url
      const input = document.createElement('input')
      input.type = 'hidden'
      input.name = 'token_ws'
      input.value = tx.token
      form.appendChild(input)
      document.body.appendChild(form)
      form.submit()
    } catch (e) {
      setFallo(apiError(e).message)
      setProcesando(false)
    }
  }

  return (
    <div className="min-h-screen bg-ink-50">
      <header className="border-b border-ink-200 bg-white">
        <div className="mx-auto flex max-w-5xl items-center justify-between px-4 py-4 sm:px-8">
          <Logo />
          <span className="inline-flex items-center gap-1.5 text-xs font-medium text-ink-500"><Lock className="h-3.5 w-3.5" />Pago seguro</span>
        </div>
      </header>
      <main className="mx-auto max-w-5xl px-4 py-8 sm:px-8">
        <Link to="/planes" className="mb-6 inline-flex items-center gap-1.5 text-sm text-ink-500 hover:text-ink-900"><ArrowLeft className="h-4 w-4" />Volver a planes</Link>
        {loading && <Spinner />}
        {error && <ErrorState message={error.message} onRetry={reload} />}
        {plan && (
          <div className="grid gap-6 lg:grid-cols-[1.4fr_1fr]">
            <section className="card min-w-0 p-6 sm:p-8">
              <h1 className="text-2xl font-bold">Confirma tu compra</h1>
              <p className="mt-1 text-sm text-ink-500">Revisa el detalle antes de continuar a Webpay Plus.</p>
              <div className="mt-6 flex flex-wrap items-center gap-4 rounded-2xl bg-gradient-to-br from-brand-600 to-indigo-600 p-5 text-white">
                <div className="rounded-xl bg-white/15 p-3"><Crown className="h-6 w-6" /></div>
                <div className="min-w-0 flex-1 basis-40">
                  <p className="font-semibold">Plan {plan.etiqueta}</p>
                  <p className="text-sm text-white/80">{plan.duracionDias} días · {plan.descripcion}</p>
                </div>
                <p className="shrink-0 text-2xl font-extrabold">{plan.precioFormateado}</p>
              </div>
              <ul className="mt-6 grid gap-2 text-sm sm:grid-cols-2">
                {plan.beneficios.map((b) => <li key={b} className="flex items-start gap-2 text-ink-700"><ShieldCheck className="mt-0.5 h-4 w-4 shrink-0 text-emerald-500" />{b}</li>)}
              </ul>
              <div className="mt-6 rounded-xl border border-brand-100 bg-brand-50/60 p-4 text-sm text-ink-700">
                <p className="flex items-center gap-2 font-semibold text-brand-700"><Bot className="h-4 w-4" />Incluye LicitAsist</p>
                <p className="mt-1">Asistente con IA que consulta los datos reales de la plataforma para ayudarte a {usuario?.rol === 'LICITADOR' ? 'redactar licitaciones y analizar postulantes' : 'encontrar licitaciones y preparar postulaciones'}.</p>
              </div>
            </section>
            <aside className="card h-fit min-w-0 p-6">
              <h2 className="font-semibold">Información de la compra</h2>
              <dl className="mt-4 space-y-3 text-sm">
                <div className="flex justify-between"><dt className="text-ink-500">Cliente</dt><dd className="font-medium">{usuario?.nombre}</dd></div>
                <div className="flex justify-between gap-3"><dt className="text-ink-500">Email</dt><dd className="min-w-0 break-all text-right font-medium">{usuario?.email}</dd></div>
                <div className="flex justify-between"><dt className="text-ink-500">Perfil</dt><dd className="font-medium">{usuario && etiquetaEstado[usuario.rol]}</dd></div>
                <div className="flex justify-between"><dt className="text-ink-500">Plan</dt><dd className="font-medium">{plan.etiqueta}</dd></div>
                <div className="flex justify-between border-t border-ink-100 pt-3 text-base"><dt className="font-semibold">Total</dt><dd className="font-extrabold">{plan.precioFormateado}</dd></div>
              </dl>
              {fallo && <p role="alert" className="mt-4 rounded-xl bg-red-50 px-4 py-3 text-sm text-red-700">{fallo}</p>}
              <Button className="mt-6" full size="lg" onClick={pagar} loading={procesando} disabled={plan.precio <= 0}>
                {procesando ? 'Conectando con Webpay…' : 'Pagar con Webpay Plus'}
              </Button>
              <p className="mt-3 text-center text-xs text-ink-500">Serás redirigido al formulario seguro de Transbank. Ambiente sandbox (sin cobros reales).</p>
            </aside>
          </div>
        )}
      </main>
    </div>
  )
}
