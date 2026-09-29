import { Link } from 'react-router-dom'
import { Button, Logo, NotFoundState } from '@/components/ui'

export default function NotFound({ embedded }: { embedded?: boolean }) {
  if (embedded) return <NotFoundState message="La página que buscas no existe." />
  return (
    <div className="grid min-h-screen place-items-center bg-ink-50 px-4">
      <div className="text-center">
        <Logo />
        <p className="mt-10 text-7xl font-extrabold text-brand-600">404</p>
        <p className="mt-3 text-lg font-semibold">Página no encontrada</p>
        <p className="mt-1 text-sm text-ink-500">La dirección no existe o fue movida.</p>
        <Link to="/" className="mt-6 inline-block"><Button>Volver al inicio</Button></Link>
      </div>
    </div>
  )
}
