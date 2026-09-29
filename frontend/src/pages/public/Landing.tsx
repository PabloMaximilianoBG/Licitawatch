import { Link } from 'react-router-dom'
import { ArrowRight, Bot, Building2, CreditCard, FileSearch, Gavel, ShieldCheck, Sparkles, Store } from 'lucide-react'
import { Button, Logo } from '@/components/ui'
import { rutaInicio, useAuth } from '@/store/auth'

export default function Landing() {
  const { isAuthenticated, rol } = useAuth()
  const logueado = isAuthenticated()
  return (
    <div className="relative min-h-screen overflow-hidden bg-ink-950 text-white">
      <div className="pointer-events-none absolute inset-0 bg-grid-dark [background-size:40px_40px]" />
      <div className="pointer-events-none absolute left-1/2 top-0 h-[520px] w-[900px] -translate-x-1/2 rounded-full bg-brand-600/25 blur-[120px]" />
      <header className="relative mx-auto flex max-w-7xl items-center justify-between px-4 py-5 sm:px-8">
        <Logo light />
        <nav className="flex items-center gap-2">
          <Link to="/planes" className="hidden rounded-lg px-3 py-2 text-sm text-ink-300 hover:text-white sm:block">Planes</Link>
          {logueado
            ? <Link to={rutaInicio(rol)}><Button size="sm">Ir a mi panel</Button></Link>
            : <>
              <Link to="/login" className="rounded-lg px-3 py-2 text-sm text-ink-300 hover:text-white">Iniciar sesión</Link>
              <Link to="/register"><Button size="sm">Crear cuenta</Button></Link>
            </>}
        </nav>
      </header>

      <section className="relative mx-auto max-w-7xl px-4 pb-20 pt-12 text-center sm:px-8 sm:pt-20">
        <span className="glass inline-flex items-center gap-2 rounded-full px-4 py-1.5 text-xs font-medium text-brand-200">
          <Sparkles className="h-3.5 w-3.5" />Nuevo: LicitAsist, asistente con IA sobre datos reales
        </span>
        <h1 className="mx-auto mt-6 max-w-4xl text-4xl font-bold leading-[1.1] tracking-tight sm:text-6xl">
          La plataforma privada para <span className="bg-gradient-to-r from-brand-300 via-brand-400 to-indigo-300 bg-clip-text text-transparent">gestionar licitaciones</span>
        </h1>
        <p className="mx-auto mt-6 max-w-2xl text-base text-ink-300 sm:text-lg">
          Los Licitadores publican y administran sus procesos. Las Pymes buscan, filtran y postulan. Todo centralizado, sin planillas ni correos sueltos.
        </p>
        <div className="mt-10 flex flex-col justify-center gap-3 sm:flex-row">
          <Link to="/register/pyme"><Button size="lg" full icon={<Store className="h-5 w-5" />}>Soy Pyme</Button></Link>
          <Link to="/register/licitador"><Button size="lg" variant="outline-light" full icon={<Gavel className="h-5 w-5" />}>Soy Licitador</Button></Link>
        </div>

        <div className="mx-auto mt-16 grid max-w-5xl gap-4 text-left sm:grid-cols-2 lg:grid-cols-4">
          {[
            { i: <Building2 />, t: 'Licitadores publican', d: 'Crean, editan y cierran licitaciones con condiciones y plazos.' },
            { i: <FileSearch />, t: 'Pymes postulan', d: 'Búsqueda con filtros por categoría, región, monto y cierre.' },
            { i: <CreditCard />, t: 'Planes propios', d: 'Estándar gratuito y Premium con pago Webpay Plus.' },
            { i: <Bot />, t: 'LicitAsist IA', d: 'Respuestas con datos reales de la plataforma vía Groq.' },
          ].map((f) => (
            <div key={f.t} className="glass rounded-2xl p-5 transition hover:bg-white/10">
              <div className="mb-3 inline-flex rounded-xl bg-brand-500/15 p-2.5 text-brand-300 [&>svg]:h-5 [&>svg]:w-5">{f.i}</div>
              <p className="font-semibold">{f.t}</p>
              <p className="mt-1 text-sm text-ink-400">{f.d}</p>
            </div>
          ))}
        </div>

        <div className="glass mx-auto mt-16 max-w-3xl rounded-2xl p-6 text-left">
          <p className="text-xs font-semibold uppercase tracking-widest text-brand-300">Flujo principal</p>
          <ol className="mt-4 grid gap-4 sm:grid-cols-4">
            {['Licitador publica', 'Pyme busca y postula', 'Licitador decide', 'Ambos reciben correo'].map((p, n) => (
              <li key={p} className="flex items-center gap-3 text-sm text-ink-200">
                <span className="grid h-7 w-7 shrink-0 place-items-center rounded-full bg-brand-600 text-xs font-bold">{n + 1}</span>{p}
              </li>
            ))}
          </ol>
        </div>
      </section>

      <footer className="relative border-t border-white/10 py-6 text-center text-xs text-ink-500">
        <span className="inline-flex items-center gap-1.5"><ShieldCheck className="h-3.5 w-3.5" />JWT · RBAC · Webpay Plus · PostgreSQL</span>
        <span className="mx-2">·</span>
        <Link to="/planes" className="inline-flex items-center gap-1 hover:text-white">Ver planes <ArrowRight className="h-3 w-3" /></Link>
      </footer>
    </div>
  )
}
