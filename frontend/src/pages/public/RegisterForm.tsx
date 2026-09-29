import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { Building2, IdCard, Lock, Mail, MailCheck, User } from 'lucide-react'
import { AuthLayout } from '@/components/layout/AuthLayout'
import { Button, Input } from '@/components/ui'
import { authApi } from '@/api/services'
import { apiError } from '@/api/client'
import { toast } from '@/store/toast'
import { EMAIL_REGEX, PASSWORD_REGEX, formatearRut, rutValido } from '@/utils/format'

/** Formulario específico por perfil: Pyme pide nombre de contacto; Licitador pide razón social (sec. 8). */
export function RegisterForm({ tipo }: { tipo: 'PYME' | 'LICITADOR' }) {
  const esPyme = tipo === 'PYME'
  const campoNombre = esPyme ? 'nombreContacto' : 'razonSocial'
  const [f, setF] = useState({ email: '', password: '', confirmPassword: '', rut: '', nombre: '' })
  const [errores, setErrores] = useState<Record<string, string>>({})
  const [cargando, setCargando] = useState(false)
  const [listo, setListo] = useState<{ email: string; correoEnviado: boolean; mensaje: string } | null>(null)
  const set = (k: keyof typeof f) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setF((p) => ({ ...p, [k]: k === 'rut' ? formatearRut(e.target.value) : e.target.value }))

  const validar = () => {
    const e: Record<string, string> = {}
    if (!EMAIL_REGEX.test(f.email)) e.email = 'Email inválido'
    if (!PASSWORD_REGEX.test(f.password)) e.password = 'Mínimo 8 caracteres, con mayúscula, minúscula y número'
    if (f.password !== f.confirmPassword) e.confirmPassword = 'Las contraseñas no coinciden'
    if (!rutValido(f.rut)) e.rut = 'RUT inválido (revisa el dígito verificador)'
    if (f.nombre.trim().length < 3) e[campoNombre] = esPyme ? 'Ingresa el nombre de contacto' : 'Ingresa la razón social'
    setErrores(e)
    return Object.keys(e).length === 0
  }

  const enviar = async (ev: FormEvent) => {
    ev.preventDefault()
    if (!validar()) return
    setCargando(true)
    try {
      const base = { email: f.email.trim(), password: f.password, confirmPassword: f.confirmPassword, rut: f.rut }
      const r = esPyme
        ? await authApi.registrarPyme({ ...base, nombreContacto: f.nombre.trim() })
        : await authApi.registrarLicitador({ ...base, razonSocial: f.nombre.trim() })
      setListo({ email: r.email, correoEnviado: r.correoEnviado, mensaje: r.mensaje })
    } catch (err) {
      const a = apiError(err)
      if (a.errores) setErrores(a.errores)
      else if (a.code === 'EMAIL_DUPLICADO') setErrores({ email: a.message })
      else if (a.code === 'RUT_DUPLICADO' || a.code === 'RUT_INVALIDO') setErrores({ rut: a.message })
      else if (a.code === 'PASSWORD_NO_COINCIDE') setErrores({ confirmPassword: a.message })
      else toast.error(a.message)
    } finally {
      setCargando(false)
    }
  }

  if (listo) {
    return (
      <AuthLayout title="¡Revisa tu correo!">
        <div className="rounded-2xl border border-brand-100 bg-brand-50/50 p-6 text-center">
          <div className="mx-auto mb-4 grid h-14 w-14 place-items-center rounded-2xl bg-brand-gradient text-white"><MailCheck className="h-7 w-7" /></div>
          <p className="text-sm text-ink-700">{listo.mensaje}</p>
          <p className="mt-2 text-sm font-semibold text-ink-900">{listo.email}</p>
          <p className="mt-4 text-xs text-ink-500">Presiona el botón “Confirmar correo” del mensaje. Recién entonces podrás iniciar sesión.</p>
          {!listo.correoEnviado && (
            <Button className="mt-4" variant="secondary" onClick={() => authApi.reenviar(listo.email).then((r) => toast.success(r.mensaje))}>Reenviar correo</Button>
          )}
        </div>
        <Link to="/login" className="mt-6 block"><Button full variant="secondary">Ir a iniciar sesión</Button></Link>
      </AuthLayout>
    )
  }

  return (
    <AuthLayout title={esPyme ? 'Registro de Pyme' : 'Registro de Licitador'}
      subtitle={<>{esPyme ? '¿Publicas licitaciones?' : '¿Eres una Pyme que postula?'} <Link to={esPyme ? '/register/licitador' : '/register/pyme'} className="font-semibold text-brand-600 hover:underline">Regístrate como {esPyme ? 'Licitador' : 'Pyme'}</Link></>}>
      <form onSubmit={enviar} className="space-y-4" noValidate>
        <Input label="Email" type="email" autoComplete="email" value={f.email} onChange={set('email')} error={errores.email} icon={<Mail className="h-4 w-4" />} required />
        <Input label="RUT" value={f.rut} onChange={set('rut')} error={errores.rut} placeholder="76.543.210-3" icon={<IdCard className="h-4 w-4" />} inputMode="text" required />
        <Input label={esPyme ? 'Nombre de contacto' : 'Razón social'} value={f.nombre} onChange={set('nombre')} error={errores[campoNombre]}
          placeholder={esPyme ? 'Ej: Carla Pérez' : 'Ej: Constructora Andes SpA'} icon={esPyme ? <User className="h-4 w-4" /> : <Building2 className="h-4 w-4" />} required />
        <div className="grid gap-4 sm:grid-cols-2">
          <Input label="Contraseña" type="password" autoComplete="new-password" value={f.password} onChange={set('password')} error={errores.password} icon={<Lock className="h-4 w-4" />} required />
          <Input label="Confirmar contraseña" type="password" autoComplete="new-password" value={f.confirmPassword} onChange={set('confirmPassword')} error={errores.confirmPassword} icon={<Lock className="h-4 w-4" />} required />
        </div>
        <p className="text-xs text-ink-500">Mínimo 8 caracteres, con al menos una mayúscula, una minúscula y un número.</p>
        <Button type="submit" size="lg" full loading={cargando}>Crear cuenta</Button>
        <p className="text-center text-sm text-ink-500">¿Ya tienes cuenta? <Link to="/login" className="font-semibold text-brand-600 hover:underline">Inicia sesión</Link></p>
      </form>
    </AuthLayout>
  )
}
