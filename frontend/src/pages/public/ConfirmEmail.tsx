import { useEffect, useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { CheckCircle2, Loader2, XCircle } from 'lucide-react'
import { AuthLayout } from '@/components/layout/AuthLayout'
import { Button, Input } from '@/components/ui'
import { authApi } from '@/api/services'
import { apiError } from '@/api/client'
import { toast } from '@/store/toast'

export default function ConfirmEmail() {
  const [params] = useSearchParams()
  const token = params.get('token')
  const [estado, setEstado] = useState<'cargando' | 'ok' | 'error'>(token ? 'cargando' : 'error')
  const [mensaje, setMensaje] = useState(token ? '' : 'El enlace no contiene un token de confirmación.')
  const [code, setCode] = useState<string>()
  const [email, setEmail] = useState('')
  const enviado = useRef(false)

  useEffect(() => {
    if (!token || enviado.current) return
    enviado.current = true // evita doble envío en StrictMode (el token es de un solo uso)
    authApi.confirmarEmail(token)
      .then((r) => { setEstado('ok'); setMensaje(r.mensaje) })
      .catch((e) => {
        const a = apiError(e)
        setCode(a.code)
        setEstado(a.code === 'EMAIL_YA_CONFIRMADO' ? 'ok' : 'error')
        setMensaje(a.message)
      })
  }, [token])

  return (
    <AuthLayout title="Confirmación de correo">
      <div className="rounded-2xl border border-ink-200 p-8 text-center">
        {estado === 'cargando' && <><Loader2 className="mx-auto h-10 w-10 animate-spin text-brand-500" /><p className="mt-4 text-sm text-ink-600">Validando tu enlace…</p></>}
        {estado === 'ok' && <>
          <CheckCircle2 className="mx-auto h-12 w-12 text-emerald-500" />
          <p className="mt-4 font-semibold text-ink-900">{mensaje}</p>
          <Link to="/login" className="mt-6 block"><Button full>Iniciar sesión</Button></Link>
        </>}
        {estado === 'error' && <>
          <XCircle className="mx-auto h-12 w-12 text-red-500" />
          <p className="mt-4 font-semibold text-ink-900">{mensaje}</p>
          {(code === 'TOKEN_EXPIRADO' || code === 'TOKEN_USADO' || code === 'TOKEN_INVALIDO' || !token) && (
            <form className="mt-6 space-y-3 text-left" onSubmit={(e) => { e.preventDefault(); authApi.reenviar(email).then((r) => toast.success(r.mensaje)).catch((er) => toast.error(apiError(er).message)) }}>
              <Input label="Solicita un nuevo enlace" type="email" placeholder="tu@email.cl" value={email} onChange={(e) => setEmail(e.target.value)} required />
              <Button type="submit" full variant="secondary">Reenviar confirmación</Button>
            </form>
          )}
        </>}
      </div>
    </AuthLayout>
  )
}
