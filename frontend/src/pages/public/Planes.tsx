import { Link, useNavigate } from 'react-router-dom'
import { Bot, Check, Crown, Eye, Headphones, Infinity as InfinityIcon, Minus, Zap } from 'lucide-react'
import { Badge, Button, ErrorState, Logo, Spinner } from '@/components/ui'
import { ventasApi } from '@/api/services'
import { useAsync } from '@/hooks/useAsync'
import { rutaInicio, useAuth } from '@/store/auth'
import { cn, num } from '@/utils/format'
import type { Plan } from '@/types'

const lim = (v: number | null, u: string) => (v === null ? 'Ilimitadas' : `${num(v)} ${u}/mes`)

export default function Planes() {
  const { isAuthenticated, rol, suscripcion } = useAuth()
  const nav = useNavigate()
  const { data: planes, loading, error, reload } = useAsync(ventasApi.planes)
  const logueado = isAuthenticated()
  const planActual = suscripcion?.plan.nombre

  const contratar = (p: Plan) => {
    if (!logueado) return nav(`/login?redirect=${encodeURIComponent(`/pago?plan=${p.id}`)}`)
    nav(`/pago?plan=${p.id}`)
  }

  return (
    <div className="min-h-screen bg-ink-50">
      <header className="bg-ink-950">
        <div className="mx-auto flex max-w-6xl items-center justify-between px-4 py-5 sm:px-8">
          <Link to="/"><Logo light /></Link>
          <Link to={logueado ? rutaInicio(rol) : '/login'}><Button size="sm" variant="outline-light">{logueado ? 'Volver a mi panel' : 'Iniciar sesión'}</Button></Link>
        </div>
        <div className="mx-auto max-w-3xl px-4 pb-24 pt-8 text-center text-white">
          <Badge tono="violet" className="!bg-violet-500/20 !text-violet-200 !ring-violet-400/30">Planes LicitaWatch</Badge>
          <h1 className="mt-4 text-3xl font-bold tracking-tight sm:text-5xl">Crece sin límites con Premium</h1>
          <p className="mt-4 text-ink-300">Publica y postula sin topes, gana visibilidad y usa LicitAsist, el asistente con IA que entiende tus licitaciones.</p>
        </div>
      </header>
      <main className="mx-auto -mt-16 max-w-5xl px-4 pb-16 sm:px-8">
        {loading && <Spinner />}
        {error && <ErrorState message={error.message} onRetry={reload} />}
        <div className="grid gap-6 md:grid-cols-2">
          {planes?.map((p) => {
            const premium = p.nombre === 'PREMIUM'
            const actual = planActual === p.nombre
            return (
              <article key={p.id} className={cn('relative flex flex-col rounded-3xl border bg-white p-7 shadow-card transition',
                premium ? 'border-transparent ring-2 ring-brand-500 shadow-glow' : 'border-ink-200')}>
                {premium && <span className="absolute -top-3 left-7 inline-flex items-center gap-1 rounded-full bg-brand-gradient px-3 py-1 text-xs font-semibold text-white"><Crown className="h-3.5 w-3.5" />Recomendado</span>}
                <div className="flex items-center justify-between">
                  <h2 className="text-xl font-bold">{p.etiqueta}</h2>
                  {actual && <Badge tono="green" dot>Tu plan actual</Badge>}
                </div>
                <p className="mt-1 text-sm text-ink-500">{p.descripcion}</p>
                <p className="mt-6 flex items-end gap-1">
                  <span className="text-4xl font-extrabold tracking-tight">{p.precio > 0 ? p.precioFormateado : 'Gratis'}</span>
                  {p.precio > 0 && <span className="pb-1 text-sm text-ink-500">CLP / {p.duracionDias} días</span>}
                </p>
                <ul className="mt-6 space-y-3 text-sm">
                  <Fila ok icon={<Zap />} texto={`Publicaciones: ${lim(p.limitePublicacionesMes, 'publicaciones')}`} />
                  <Fila ok icon={<InfinityIcon />} texto={`Postulaciones: ${lim(p.limitePostulacionesMes, 'postulaciones')}`} />
                  <Fila ok icon={<InfinityIcon />} texto={`Búsquedas con filtros: ${lim(p.limiteBusquedasMes, 'búsquedas')}`} />
                  <Fila ok={p.prioridadVisibilidad} icon={<Eye />} texto="Mayor visibilidad en resultados" />
                  <Fila ok icon={<Headphones />} texto={p.soporte === 'PRIORITARIO' ? 'Soporte prioritario' : 'Soporte estándar'} />
                  <Fila ok={p.accesoLicitasist} icon={<Bot />} texto="Acceso a LicitAsist (IA)" />
                </ul>
                <div className="mt-auto pt-8">
                  {premium ? (
                    rol === 'ADMINISTRADOR' ? <p className="text-center text-sm text-ink-500">El administrador ya tiene acceso a LicitAsist.</p>
                      : actual ? <Button full variant="secondary" disabled>Premium activo</Button>
                        : <Button full size="lg" onClick={() => contratar(p)} icon={<Crown className="h-5 w-5" />}>Contratar Premium</Button>
                  ) : (
                    <Button full variant="secondary" disabled={logueado} onClick={() => nav('/register')}>{logueado ? 'Incluido al registrarte' : 'Comenzar gratis'}</Button>
                  )}
                </div>
              </article>
            )
          })}
        </div>
        <p className="mt-8 text-center text-xs text-ink-500">Pagos procesados con Webpay Plus (Transbank). El plan Premium se activa solo cuando el backend confirma el pago con Webpay.</p>
      </main>
    </div>
  )
}

function Fila({ ok, texto, icon }: { ok: boolean; texto: string; icon: React.ReactNode }) {
  return (
    <li className={cn('flex items-center gap-3', !ok && 'text-ink-400 line-through')}>
      <span className={cn('grid h-6 w-6 place-items-center rounded-full [&>svg]:h-3.5 [&>svg]:w-3.5', ok ? 'bg-brand-50 text-brand-600' : 'bg-ink-100 text-ink-400')}>
        {ok ? icon : <Minus />}
      </span>
      {texto}
      {ok && <Check className="ml-auto h-4 w-4 text-emerald-500" aria-hidden />}
    </li>
  )
}
