import { useState, type FormEvent } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { AlertCircle, Lock, Mail } from 'lucide-react'
import { AuthLayout } from '@/components/layout/AuthLayout'
import { Button, Input } from '@/components/ui'
import { authApi, ventasApi } from '@/api/services'
import { apiError } from '@/api/client'
import { rutaInicio, useAuth } from '@/store/auth'
import { toast } from '@/store/toast'
import { EMAIL_REGEX } from '@/utils/format'

export default function Login() {
  const [params] = useSearchParams()
  const nav = useNavigate()
  const { login, setSuscripcion } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<{ code?: string; message: string } | null>(null)
  const [cargando, setCargando] = useState(false)
  const [reenviando, setReenviando] = useState(false)

  const enviar = async (e: FormEvent) => {
    e.preventDefault()
    if (!EMAIL_REGEX.test(email)) return setError({ message: 'Ingresa un email válido' })
    if (!password) return setError({ message: 'Ingresa tu contraseña' })
    setCargando(true)
    setError(null)
    try {
      const r = await authApi.login(email.trim(), password)
      login(r.token, r.expiraEn, r.usuario)
      ventasApi.suscripcion().then(setSuscripcion).catch(() => undefined)
      toast.success(`¡Hola, ${r.usuario.nombre}!`)
      const redirect = params.get('redirect')
      nav(redirect && redirect.startsWith('/') ? redirect : rutaInicio(r.usuario.rol), { replace: true })
    } catch (err) {
      setError(apiError(err))
    } finally {
      setCargando(false)
    }
  }

  const reenviar = async () => {
    setReenviando(true)
    try {
      const r = await authApi.reenviar(email.trim())
      toast.success(r.mensaje)
    } catch (err) {
      toast.error(apiError(err).message)
    } finally {
      setReenviando(false)
    }
  }

  return (
    <AuthLayout title="Inicia sesión" subtitle={<>¿No tienes cuenta? <Link to="/register" className="font-semibold text-brand-600 hover:underline">Regístrate</Link></>}>
      {params.get('expirada') && <div className="mb-5 rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-800">Tu sesión expiró. Vuelve a iniciar sesión.</div>}
      <form onSubmit={enviar} className="space-y-4" noValidate>
        <Input label="Email" type="email" autoComplete="email" value={email} onChange={(e) => setEmail(e.target.value)} icon={<Mail className="h-4 w-4" />} required />
        <Input label="Contraseña" type="password" autoComplete="current-password" value={password} onChange={(e) => setPassword(e.target.value)} icon={<Lock className="h-4 w-4" />} required />
        {error && (
          <div role="alert" className="flex gap-2.5 rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
            <AlertCircle className="mt-0.5 h-4 w-4 shrink-0" />
            <div>
              <p>{error.message}</p>
              {error.code === 'EMAIL_NO_CONFIRMADO' && (
                <button type="button" onClick={reenviar} disabled={reenviando} className="mt-1 font-semibold underline">
                  {reenviando ? 'Enviando…' : 'Reenviar correo de confirmación'}
                </button>
              )}
            </div>
          </div>
        )}
        <Button type="submit" size="lg" full loading={cargando}>Ingresar</Button>
      </form>
    </AuthLayout>
  )
}
