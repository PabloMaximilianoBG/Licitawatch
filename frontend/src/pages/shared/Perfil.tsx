import { useState, type FormEvent } from 'react'
import { KeyRound, Save } from 'lucide-react'
import { Button, Card, CardHeader, ErrorState, Input, PageHeader, Spinner, StatusBadge } from '@/components/ui'
import { usuariosApi } from '@/api/services'
import { apiError } from '@/api/client'
import { useAsync } from '@/hooks/useAsync'
import { useAuth } from '@/store/auth'
import { toast } from '@/store/toast'
import { PASSWORD_REGEX, fecha } from '@/utils/format'

export default function Perfil() {
  const setUsuario = useAuth((s) => s.setUsuario)
  const { data: u, loading, error, reload, setData } = useAsync(usuariosApi.me)
  const [campo, setCampo] = useState<string | null>(null)
  const [guardando, setGuardando] = useState(false)
  const [pw, setPw] = useState({ passwordActual: '', passwordNueva: '', confirmPassword: '' })
  const [pwErr, setPwErr] = useState<Record<string, string>>({})
  const [cambiando, setCambiando] = useState(false)

  if (loading) return <Spinner />
  if (error || !u) return <ErrorState message={error?.message} onRetry={reload} />
  const clave = u.rol === 'PYME' ? 'nombreContacto' : u.rol === 'LICITADOR' ? 'razonSocial' : 'area'
  const valor = campo ?? (u[clave] ?? '')

  const guardar = async (e: FormEvent) => {
    e.preventDefault()
    setGuardando(true)
    try { const r = await usuariosApi.actualizar({ [clave]: valor }); setData(r); setUsuario(r); setCampo(null); toast.success('Perfil actualizado') }
    catch (x) { toast.error(apiError(x).message) } finally { setGuardando(false) }
  }
  const cambiar = async (e: FormEvent) => {
    e.preventDefault()
    const er: Record<string, string> = {}
    if (!PASSWORD_REGEX.test(pw.passwordNueva)) er.passwordNueva = 'Mínimo 8 caracteres con mayúscula, minúscula y número'
    if (pw.passwordNueva !== pw.confirmPassword) er.confirmPassword = 'No coincide'
    setPwErr(er)
    if (Object.keys(er).length) return
    setCambiando(true)
    try { toast.success((await usuariosApi.cambiarPassword(pw)).mensaje); setPw({ passwordActual: '', passwordNueva: '', confirmPassword: '' }) }
    catch (x) { const a = apiError(x); setPwErr(a.errores ?? (a.code === 'PASSWORD_ACTUAL_INCORRECTA' ? { passwordActual: a.message } : { passwordNueva: a.message })) }
    finally { setCambiando(false) }
  }

  return (
    <>
      <PageHeader eyebrow="Cuenta" title="Mi perfil" subtitle="Datos de tu cuenta y seguridad." />
      <div className="grid gap-6 lg:grid-cols-2">
        <Card>
          <CardHeader title="Datos del perfil" action={<StatusBadge estado={u.rol} />} />
          <form onSubmit={guardar} className="space-y-4 p-5">
            <Input label="Email" value={u.email} disabled hint="El email identifica tu cuenta y no se puede cambiar aquí." />
            {u.rut && <Input label="RUT" value={u.rut} disabled />}
            <Input label={u.rol === 'PYME' ? 'Nombre de contacto' : u.rol === 'LICITADOR' ? 'Razón social' : 'Área'} value={valor} onChange={(e) => setCampo(e.target.value)} minLength={2} required />
            <div className="flex items-center justify-between text-xs text-ink-500"><span>Miembro desde {fecha(u.creadoEn)}</span><span>Último acceso {fecha(u.ultimoLogin, true)}</span></div>
            <Button type="submit" loading={guardando} disabled={campo === null} icon={<Save className="h-4 w-4" />}>Guardar cambios</Button>
          </form>
        </Card>
        <Card>
          <CardHeader title="Cambiar contraseña" icon={<KeyRound className="h-4 w-4" />} />
          <form onSubmit={cambiar} className="space-y-4 p-5" noValidate>
            <Input label="Contraseña actual" type="password" autoComplete="current-password" value={pw.passwordActual} onChange={(e) => setPw({ ...pw, passwordActual: e.target.value })} error={pwErr.passwordActual} required />
            <Input label="Nueva contraseña" type="password" autoComplete="new-password" value={pw.passwordNueva} onChange={(e) => setPw({ ...pw, passwordNueva: e.target.value })} error={pwErr.passwordNueva} required />
            <Input label="Confirmar nueva contraseña" type="password" autoComplete="new-password" value={pw.confirmPassword} onChange={(e) => setPw({ ...pw, confirmPassword: e.target.value })} error={pwErr.confirmPassword} required />
            <Button type="submit" variant="secondary" loading={cambiando}>Actualizar contraseña</Button>
          </form>
        </Card>
      </div>
    </>
  )
}
