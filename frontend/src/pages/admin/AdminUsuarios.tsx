import { useState } from 'react'
import { Pencil, Plus, Power, Send, Shield } from 'lucide-react'
import {
  Badge, Button, Card, ConfirmDialog, EmptyState, ErrorState, Input, Modal, PageHeader, Pagination, SearchBar, Select,
  Spinner, StatusBadge, Table, type Column,
} from '@/components/ui'
import { adminApi, type AdminUsuarioInput } from '@/api/services'
import { apiError } from '@/api/client'
import { useAsync } from '@/hooks/useAsync'
import { useAuth } from '@/store/auth'
import { toast } from '@/store/toast'
import type { Rol, Usuario } from '@/types'
import { fecha, formatearRut } from '@/utils/format'

type Modo = { tipo: 'crear' } | { tipo: 'editar'; u: Usuario } | { tipo: 'rol'; u: Usuario } | { tipo: 'notificar'; u: Usuario }

export default function AdminUsuarios() {
  const yo = useAuth((s) => s.usuario)
  const [filtros, setFiltros] = useState({ q: '', rol: '', activo: '', page: 0 })
  const { data, loading, error, reload } = useAsync(() => adminApi.usuarios({ ...filtros, size: 15 }), [JSON.stringify(filtros)])
  const roles = useAsync(adminApi.roles)
  const [modo, setModo] = useState<Modo | null>(null)
  const [toggle, setToggle] = useState<Usuario | null>(null)
  const [procesando, setProcesando] = useState(false)

  const cambiarEstado = async () => {
    if (!toggle) return
    setProcesando(true)
    try { await adminApi.estadoUsuario(toggle.id, !toggle.activo); toast.success(toggle.activo ? 'Usuario desactivado' : 'Usuario activado'); setToggle(null); reload() }
    catch (e) { toast.error(apiError(e).message) } finally { setProcesando(false) }
  }

  const columnas: Column<Usuario>[] = [
    { key: 'u', header: 'Usuario', render: (u) => <div><p className="font-semibold text-ink-900">{u.nombre}</p><p className="text-xs text-ink-500">{u.email}</p></div> },
    { key: 'rol', header: 'Rol', render: (u) => <StatusBadge estado={u.rol} /> },
    { key: 'rut', header: 'RUT / Área', render: (u) => u.rut ?? u.area ?? '—' },
    { key: 'estado', header: 'Estado', render: (u) => <div className="flex flex-wrap justify-end gap-1 md:justify-start">
      <Badge tono={u.activo ? 'green' : 'red'} dot>{u.activo ? 'Activo' : 'Inactivo'}</Badge>
      {!u.emailConfirmado && <Badge tono="amber">Sin confirmar</Badge>}</div> },
    { key: 'creado', header: 'Registro', render: (u) => fecha(u.creadoEn), hideOnMobile: true },
    { key: 'acc', header: 'Acciones', className: 'text-right', render: (u) => (
      <div className="flex justify-end gap-1">
        <Button size="sm" variant="ghost" aria-label="Editar" onClick={() => setModo({ tipo: 'editar', u })}><Pencil className="h-4 w-4" /></Button>
        {u.id !== yo?.id && <Button size="sm" variant="ghost" aria-label="Cambiar rol" onClick={() => setModo({ tipo: 'rol', u })}><Shield className="h-4 w-4" /></Button>}
        <Button size="sm" variant="ghost" aria-label="Enviar notificación" onClick={() => setModo({ tipo: 'notificar', u })}><Send className="h-4 w-4" /></Button>
        {u.id !== yo?.id && <Button size="sm" variant="ghost" aria-label={u.activo ? 'Desactivar' : 'Activar'} className={u.activo ? 'text-red-600' : 'text-emerald-600'} onClick={() => setToggle(u)}><Power className="h-4 w-4" /></Button>}
      </div>) },
  ]

  return (
    <>
      <PageHeader eyebrow="Administración" title="Usuarios y roles" subtitle="Crea, edita, activa o desactiva cuentas y gestiona sus roles."
        actions={<Button icon={<Plus className="h-4 w-4" />} onClick={() => setModo({ tipo: 'crear' })}>Crear usuario</Button>} />
      {roles.data && <div className="mb-5 grid grid-cols-1 gap-3 sm:grid-cols-3">
        {roles.data.map((r) => <Card key={r.id} className="p-4"><p className="truncate text-xs font-semibold uppercase tracking-wide text-ink-500">{r.nombre}</p><p className="mt-1 text-2xl font-bold">{r.cantidadUsuarios}</p></Card>)}
      </div>}
      <Card>
        <div className="flex flex-col gap-3 border-b border-ink-100 p-4 lg:flex-row">
          <SearchBar value={filtros.q} placeholder="Buscar por email, nombre, razón social o RUT" onSearch={(q) => setFiltros({ ...filtros, q, page: 0 })} />
          <Select aria-label="Rol" className="lg:w-48" value={filtros.rol} placeholder="Todos los roles" onChange={(e) => setFiltros({ ...filtros, rol: e.target.value, page: 0 })}
            options={['PYME', 'LICITADOR', 'ADMINISTRADOR'].map((r) => ({ value: r, label: r }))} />
          <Select aria-label="Estado" className="lg:w-44" value={filtros.activo} placeholder="Todos" onChange={(e) => setFiltros({ ...filtros, activo: e.target.value, page: 0 })}
            options={[{ value: 'true', label: 'Activos' }, { value: 'false', label: 'Inactivos' }]} />
        </div>
        {loading && <Spinner />}
        {error && <ErrorState message={error.message} onRetry={reload} />}
        {data && <>
          <Table columns={columnas} rows={data.content} rowKey={(u) => u.id} empty={<EmptyState title="Sin usuarios" message="No hay usuarios con esos filtros." />} />
          <Pagination page={data.page} totalPages={data.totalPages} totalElements={data.totalElements} onChange={(page) => setFiltros({ ...filtros, page })} />
        </>}
      </Card>
      {modo && <UsuarioModal modo={modo} onClose={() => setModo(null)} onDone={() => { setModo(null); reload(); roles.reload() }} />}
      <ConfirmDialog open={!!toggle} onClose={() => setToggle(null)} onConfirm={cambiarEstado} loading={procesando} danger={toggle?.activo}
        title={toggle?.activo ? 'Desactivar usuario' : 'Activar usuario'} confirmText={toggle?.activo ? 'Desactivar' : 'Activar'}
        message={toggle?.activo ? `${toggle?.email} no podrá iniciar sesión hasta que lo reactives.` : `${toggle?.email} podrá volver a iniciar sesión.`} />
    </>
  )
}

function UsuarioModal({ modo, onClose, onDone }: { modo: Modo; onClose: () => void; onDone: () => void }) {
  const u = modo.tipo === 'crear' ? null : modo.u
  const [f, setF] = useState<AdminUsuarioInput & { asunto?: string; mensaje?: string }>({
    email: u?.email ?? '', password: '', rol: (modo.tipo === 'rol' ? (u?.rol === 'PYME' ? 'LICITADOR' : 'PYME') : u?.rol ?? 'PYME') as Rol,
    rut: u?.rut ?? '', razonSocial: u?.razonSocial ?? '', nombreContacto: u?.nombreContacto ?? '', area: u?.area ?? '', nombre: '',
  })
  const [err, setErr] = useState<Record<string, string>>({})
  const [enviando, setEnviando] = useState(false)
  const set = (k: string) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) =>
    setF((p) => ({ ...p, [k]: k === 'rut' ? formatearRut(e.target.value) : e.target.value }))

  const guardar = async () => {
    setEnviando(true)
    setErr({})
    try {
      const perfil = { rut: f.rut || undefined, razonSocial: f.razonSocial || undefined, nombreContacto: f.nombreContacto || undefined, area: f.area || undefined }
      if (modo.tipo === 'crear') await adminApi.crearUsuario({ email: f.email, password: f.password, rol: f.rol, ...perfil, nombre: f.nombre || undefined })
      else if (modo.tipo === 'editar') await adminApi.editarUsuario(modo.u.id, { email: f.email, ...perfil })
      else if (modo.tipo === 'rol') await adminApi.rolUsuario(modo.u.id, { rol: f.rol, ...perfil })
      else await adminApi.enviarNotificacion({ usuarioId: modo.u.id, asunto: f.asunto ?? '', mensaje: f.mensaje ?? '' })
      toast.success(modo.tipo === 'notificar' ? 'Notificación enviada por correo' : 'Cambios guardados')
      onDone()
    } catch (e) {
      const a = apiError(e)
      setErr(a.errores ?? { general: a.message })
    } finally { setEnviando(false) }
  }

  const titulo = { crear: 'Crear usuario', editar: 'Editar usuario', rol: 'Cambiar rol', notificar: 'Enviar notificación' }[modo.tipo]
  const rol = f.rol
  return (
    <Modal open onClose={onClose} title={titulo} description={u?.email} size="md"
      footer={<><Button variant="secondary" onClick={onClose}>Cancelar</Button><Button onClick={guardar} loading={enviando}>{modo.tipo === 'notificar' ? 'Enviar' : 'Guardar'}</Button></>}>
      <div className="space-y-4">
        {modo.tipo === 'notificar' ? <>
          <Input label="Asunto" value={f.asunto ?? ''} onChange={set('asunto')} error={err.asunto} maxLength={150} required />
          <div><label className="label" htmlFor="msg">Mensaje</label><textarea id="msg" className="field min-h-[120px]" value={f.mensaje ?? ''} onChange={set('mensaje')} maxLength={3000} /></div>
        </> : <>
          {modo.tipo !== 'rol' && <Input label="Email" type="email" value={f.email} onChange={set('email')} error={err.email} required />}
          {modo.tipo === 'crear' && <Input label="Contraseña inicial" type="password" value={f.password} onChange={set('password')} error={err.password} hint="Mín. 8 caracteres con mayúscula, minúscula y número" required />}
          {modo.tipo !== 'editar' && <Select label={modo.tipo === 'rol' ? 'Nuevo rol' : 'Rol'} value={rol} onChange={set('rol')} error={err.rol}
            options={(['PYME', 'LICITADOR', 'ADMINISTRADOR'] as Rol[]).filter((r) => modo.tipo !== 'rol' || r !== u?.rol).map((r) => ({ value: r, label: r }))} />}
          {modo.tipo === 'rol' && <p className="rounded-xl bg-amber-50 px-4 py-3 text-xs text-amber-800">El perfil actual se reemplaza por el del nuevo rol. Completa los datos del nuevo perfil.</p>}
          {(rol === 'PYME' || rol === 'LICITADOR') && (modo.tipo !== 'editar' || u?.rut) && <Input label="RUT" value={f.rut ?? ''} onChange={set('rut')} error={err.rut} />}
          {rol === 'PYME' && <Input label="Nombre de contacto" value={f.nombreContacto ?? ''} onChange={set('nombreContacto')} error={err.nombreContacto} />}
          {rol === 'LICITADOR' && <Input label="Razón social" value={f.razonSocial ?? ''} onChange={set('razonSocial')} error={err.razonSocial} />}
          {rol === 'ADMINISTRADOR' && <Input label="Área" value={f.area ?? ''} onChange={set('area')} error={err.area} />}
          {rol === 'ADMINISTRADOR' && modo.tipo === 'crear' && <Input label="Nombre" value={f.nombre ?? ''} onChange={set('nombre')} />}
        </>}
        {err.general && <p role="alert" className="rounded-xl bg-red-50 px-4 py-3 text-sm text-red-700">{err.general}</p>}
      </div>
    </Modal>
  )
}
