import { api } from './client'
import type {
  Comprobante, EstadisticasLicitaciones, EstadisticasUsuarios, EstadisticasVentas, EstadoAsistente, Licitacion,
  LicitacionInput, LoginResponse, MensajeChat, Notificacion, Page, Pago, Plan, Postulacion, RegistroResponse, Resumen,
  Rol, RolInfo, Suscripcion, Transaccion, UsoPlan, Usuario, Venta,
} from '@/types'

const d = <T,>(p: Promise<{ data: T }>) => p.then((r) => r.data)

export const authApi = {
  login: (email: string, password: string) => d<LoginResponse>(api.post('/api/auth/login', { email, password })),
  registrarPyme: (b: { email: string; password: string; confirmPassword: string; rut: string; nombreContacto: string }) =>
    d<RegistroResponse>(api.post('/api/auth/registro/pyme', b)),
  registrarLicitador: (b: { email: string; password: string; confirmPassword: string; rut: string; razonSocial: string }) =>
    d<RegistroResponse>(api.post('/api/auth/registro/licitador', b)),
  confirmarEmail: (token: string) => d<{ mensaje: string }>(api.post('/api/auth/confirmar-email', { token })),
  reenviar: (email: string) => d<{ mensaje: string }>(api.post('/api/auth/reenviar-confirmacion', { email })),
}

export const usuariosApi = {
  me: () => d<Usuario>(api.get('/api/usuarios/me')),
  actualizar: (b: Partial<Pick<Usuario, 'nombreContacto' | 'razonSocial' | 'area' | 'nombre'>>) => d<Usuario>(api.put('/api/usuarios/me', b)),
  cambiarPassword: (b: { passwordActual: string; passwordNueva: string; confirmPassword: string }) =>
    d<{ mensaje: string }>(api.put('/api/usuarios/me/password', b)),
}

export interface FiltrosLicitacion {
  q?: string; categoria?: string; region?: string; montoMin?: string; montoMax?: string
  cierreDesde?: string; cierreHasta?: string; orden?: string; page?: number; size?: number
}
const limpiar = (o: object) => Object.fromEntries(Object.entries(o).filter(([, v]) => v !== '' && v !== undefined && v !== null))

export const licitacionesApi = {
  buscar: (f: FiltrosLicitacion) => d<Page<Licitacion>>(api.get('/api/licitaciones', { params: limpiar(f) })),
  obtener: (id: number | string) => d<Licitacion>(api.get(`/api/licitaciones/${id}`)),
  mias: () => d<Licitacion[]>(api.get('/api/licitaciones/mias')),
  resumen: () => d<Resumen>(api.get('/api/licitaciones/resumen')),
  uso: () => d<UsoPlan>(api.get('/api/licitaciones/uso-plan')),
  crear: (b: LicitacionInput) => d<Licitacion>(api.post('/api/licitaciones', b)),
  actualizar: (id: number, b: LicitacionInput) => d<Licitacion>(api.put(`/api/licitaciones/${id}`, b)),
  cambiarEstado: (id: number, estado: string) => d<Licitacion>(api.patch(`/api/licitaciones/${id}/estado`, { estado })),
  eliminar: (id: number) => api.delete(`/api/licitaciones/${id}`),
  postulaciones: (id: number | string) => d<Postulacion[]>(api.get(`/api/licitaciones/${id}/postulaciones`)),
  postular: (id: number, b: { propuesta: string; montoOfertado: number; plazoEntregaDias: number }) =>
    d<Postulacion>(api.post(`/api/licitaciones/${id}/postulaciones`, b)),
}

export const postulacionesApi = {
  mias: () => d<Postulacion[]>(api.get('/api/postulaciones/mias')),
  obtener: (id: number) => d<Postulacion>(api.get(`/api/postulaciones/${id}`)),
  actualizar: (id: number, b: { propuesta: string; montoOfertado: number; plazoEntregaDias: number }) =>
    d<Postulacion>(api.put(`/api/postulaciones/${id}`, b)),
  retirar: (id: number) => d<Postulacion>(api.patch(`/api/postulaciones/${id}/retirar`)),
  evaluar: (id: number, estado: 'EN_REVISION' | 'RECHAZADA', comentario?: string) =>
    d<Postulacion>(api.patch(`/api/postulaciones/${id}/estado`, { estado, comentario })),
  seleccionar: (id: number, comentario?: string) => d<Postulacion>(api.post(`/api/postulaciones/${id}/seleccionar`, { comentario })),
}

export const ventasApi = {
  planes: () => d<Plan[]>(api.get('/api/planes')),
  plan: (id: number | string) => d<Plan>(api.get(`/api/planes/${id}`)),
  suscripcion: () => d<Suscripcion>(api.get('/api/suscripciones/mi')),
  crearVenta: (planId: number) => d<Venta>(api.post('/api/ventas', { planId })),
  crearTransaccion: (ventaId: number) => d<Transaccion>(api.post('/api/pagos/webpay/transacciones', { ventaId })),
  pagoPorOrden: (orden: string) => d<Pago>(api.get(`/api/pagos/orden/${encodeURIComponent(orden)}`)),
  historial: () => d<Pago[]>(api.get('/api/pagos/historial')),
  comprobante: (pagoId: number | string) => d<Comprobante>(api.get(`/api/pagos/${pagoId}/comprobante`)),
  comprobantePdf: (pagoId: number | string) => api.get(`/api/pagos/${pagoId}/comprobante/pdf`, { responseType: 'blob' }).then((r) => r.data as Blob),
  reenviarComprobante: (pagoId: number) => d<{ mensaje: string }>(api.post(`/api/pagos/${pagoId}/comprobante/reenviar`)),
}

export const notificacionesApi = {
  mias: (page = 0, size = 20) => d<Page<Notificacion>>(api.get('/api/notificaciones/mias', { params: { page, size } })),
  noLeidas: () => d<{ noLeidas: number }>(api.get('/api/notificaciones/mias/no-leidas')),
  marcarLeida: (id: number) => d<Notificacion>(api.patch(`/api/notificaciones/${id}/leida`)),
  marcarTodas: () => d<{ mensaje: string }>(api.patch('/api/notificaciones/mias/leidas')),
}

export const asistenteApi = {
  estado: () => d<EstadoAsistente>(api.get('/api/asistente/estado')),
  chat: (mensaje: string, historial: MensajeChat[], licitacionId?: number) =>
    d<{ respuesta: string; modelo: string; fuentes: string[]; generadoEn: string }>(
      api.post('/api/asistente/chat', {
        mensaje,
        historial: historial.filter((m) => !m.error).slice(-12).map((m) => ({ rol: m.rol, contenido: m.contenido.slice(0, 6000) })),
        licitacionId,
      }),
    ),
}

export interface AdminUsuarioInput {
  email?: string; password?: string; rol?: Rol; rut?: string; razonSocial?: string; nombreContacto?: string
  area?: string; nombre?: string; emailConfirmado?: boolean
}

export const adminApi = {
  usuarios: (p: { rol?: string; activo?: string; q?: string; page?: number; size?: number }) =>
    d<Page<Usuario>>(api.get('/api/admin/usuarios', { params: limpiar(p) })),
  usuario: (id: number) => d<Usuario>(api.get(`/api/admin/usuarios/${id}`)),
  crearUsuario: (b: AdminUsuarioInput) => d<Usuario>(api.post('/api/admin/usuarios', b)),
  editarUsuario: (id: number, b: AdminUsuarioInput) => d<Usuario>(api.put(`/api/admin/usuarios/${id}`, b)),
  estadoUsuario: (id: number, activo: boolean) => d<Usuario>(api.patch(`/api/admin/usuarios/${id}/estado`, { activo })),
  rolUsuario: (id: number, b: AdminUsuarioInput) => d<Usuario>(api.patch(`/api/admin/usuarios/${id}/rol`, b)),
  roles: () => d<RolInfo[]>(api.get('/api/admin/roles')),
  statsUsuarios: () => d<EstadisticasUsuarios>(api.get('/api/admin/usuarios/estadisticas')),
  licitaciones: (p: { estado?: string; q?: string; page?: number; size?: number }) =>
    d<Page<Licitacion>>(api.get('/api/admin/licitaciones', { params: limpiar(p) })),
  moderar: (id: number, accion: 'SUSPENDER' | 'REACTIVAR', motivo?: string) =>
    d<Licitacion>(api.patch(`/api/admin/licitaciones/${id}/moderar`, { accion, motivo })),
  eliminarLicitacion: (id: number) => api.delete(`/api/admin/licitaciones/${id}`),
  statsLicitaciones: () => d<EstadisticasLicitaciones>(api.get('/api/admin/licitaciones/estadisticas')),
  postulaciones: (p: { estado?: string; licitacionId?: string; page?: number; size?: number }) =>
    d<Page<Postulacion>>(api.get('/api/admin/postulaciones', { params: limpiar(p) })),
  ventas: (page = 0, size = 20) => d<Page<Venta>>(api.get('/api/admin/ventas', { params: { page, size } })),
  pagos: (p: { estado?: string; page?: number; size?: number }) => d<Page<Pago>>(api.get('/api/admin/pagos', { params: limpiar(p) })),
  suscripciones: (p: { estado?: string; plan?: string; page?: number; size?: number }) =>
    d<Page<Suscripcion>>(api.get('/api/admin/suscripciones', { params: limpiar(p) })),
  statsVentas: () => d<EstadisticasVentas>(api.get('/api/admin/ventas/estadisticas')),
  notificaciones: (p: { tipo?: string; estado?: string; page?: number; size?: number }) =>
    d<Page<Notificacion>>(api.get('/api/admin/notificaciones', { params: limpiar(p) })),
  enviarNotificacion: (b: { usuarioId: number; asunto: string; mensaje: string }) => d<Notificacion>(api.post('/api/notificaciones', b)),
}
