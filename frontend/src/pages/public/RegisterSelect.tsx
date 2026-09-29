import { Link } from 'react-router-dom'
import { ArrowRight, Gavel, Store } from 'lucide-react'
import { AuthLayout } from '@/components/layout/AuthLayout'

export default function RegisterSelect() {
  const opciones = [
    { to: '/register/pyme', icon: <Store className="h-6 w-6" />, titulo: 'Registrarme como Pyme', texto: 'Busca, filtra y postula a licitaciones privadas.', datos: 'RUT y nombre de contacto' },
    { to: '/register/licitador', icon: <Gavel className="h-6 w-6" />, titulo: 'Registrarme como Licitador', texto: 'Publica y administra tus licitaciones, revisa postulantes y adjudica.', datos: 'RUT y razón social' },
  ]
  return (
    <AuthLayout title="Crea tu cuenta" subtitle={<>Elige tu perfil. ¿Ya tienes cuenta? <Link to="/login" className="font-semibold text-brand-600 hover:underline">Inicia sesión</Link></>}>
      <div className="space-y-4">
        {opciones.map((o) => (
          <Link key={o.to} to={o.to} className="group flex items-center gap-4 rounded-2xl border border-ink-200 p-5 transition hover:border-brand-400 hover:shadow-glow focus-visible:ring-2">
            <div className="rounded-xl bg-brand-gradient p-3 text-white">{o.icon}</div>
            <div className="flex-1">
              <p className="font-semibold text-ink-900">{o.titulo}</p>
              <p className="mt-0.5 text-sm text-ink-500">{o.texto}</p>
              <p className="mt-2 text-xs text-ink-400">Te pediremos: email, contraseña, {o.datos}</p>
            </div>
            <ArrowRight className="h-5 w-5 text-ink-300 transition group-hover:translate-x-1 group-hover:text-brand-600" />
          </Link>
        ))}
      </div>
    </AuthLayout>
  )
}
