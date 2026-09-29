export type Rol = 'PYME' | 'LICITADOR' | 'ADMINISTRADOR'

export interface Usuario {
  id: number
  email: string
  rol: Rol
  activo: boolean
  emailConfirmado: boolean
  perfilId: number | null
  nombre: string
  rut: string | null
  razonSocial: string | null
  nombreContacto: string | null
  area: string | null
  creadoEn: string | null
  ultimoLogin: string | null
}

export interface LoginResponse {
  token: string
  tipo: string
  expiraEn: string
  usuario: Usuario
}

export interface RegistroResponse {
  usuarioId: number
  email: string
  rol: Rol
  correoEnviado: boolean
  mensaje: string
}

export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface ApiError {
  status: number
  code?: string
  message: string
  errores?: Record<string, string>
}

export type EstadoLicitacion = 'BORRADOR' | 'PUBLICADA' | 'CERRADA' | 'ADJUDICADA' | 'CANCELADA' | 'SUSPENDIDA'
export type EstadoPostulacion = 'ENVIADA' | 'EN_REVISION' | 'SELECCIONADA' | 'RECHAZADA' | 'RETIRADA'

export interface Licitacion {
  id: number
  licitadorId: number
  licitadorNombre?: string
  titulo: string
  descripcion: string
  requisitos?: string
  condiciones?: string
  categoria: string
  region?: string
  montoEstimado: number
  fechaCierre: string
  estado: EstadoLicitacion
  destacada: boolean
  fechaPublicacion?: string
  creadoEn?: string
  actualizadoEn?: string
  diasParaCierre?: number
  cantidadPostulaciones?: number
  miPostulacionId?: number
  miPostulacionEstado?: EstadoPostulacion
}

export interface LicitacionInput {
  titulo: string
  descripcion: string
  requisitos: string
  condiciones: string
  categoria: string
  region: string
  montoEstimado: number | ''
  fechaCierre: string
  publicar: boolean
}

export interface Postulacion {
  id: number
  licitacionId: number
  licitacionTitulo: string
  licitacionEstado: EstadoLicitacion
  fechaCierre: string
  pymeId: number
  pymeNombre?: string
  pymeRut?: string
  pymeEmail?: string
  licitadorNombre?: string
  licitadorEmail?: string
  fechaPostulacion: string
  estado: EstadoPostulacion
  propuesta: string
  montoOfertado: number
  plazoEntregaDias: number
  comentarioLicitador?: string
  actualizadoEn?: string
}

export interface UsoPlan {
  plan: string
  publicacionesMes: number
  limitePublicacionesMes: number | null
  postulacionesMes: number
  limitePostulacionesMes: number | null
  busquedasMes: number
  limiteBusquedasMes: number | null
}

export interface Resumen {
  rol: Rol
  kpis: Record<string, number>
  postulacionesPorEstado?: Record<string, number>
  licitacionesPorEstado?: Record<string, number>
  proximasACerrar: Licitacion[]
  uso?: UsoPlan
}

export interface Plan {
  id: number
  nombre: 'ESTANDAR' | 'PREMIUM'
  etiqueta: string
  precio: number
  precioFormateado: string
  descripcion: string
  limitePublicacionesMes: number | null
  limitePostulacionesMes: number | null
  limiteBusquedasMes: number | null
  accesoLicitasist: boolean
  prioridadVisibilidad: boolean
  soporte: string
  duracionDias: number | null
  beneficios: string[]
}

export interface Suscripcion {
  id: number
  usuarioId: number
  clienteNombre?: string
  clienteEmail?: string
  plan: Plan
  estado: 'PENDIENTE' | 'ACTIVA' | 'VENCIDA' | 'CANCELADA'
  fechaInicio?: string
  fechaFin?: string
  diasRestantes?: number
  esPremium: boolean
  accesoLicitasist: boolean
}

export interface Venta {
  id: number
  suscripcionId: number
  usuarioId: number
  plan: string
  monto: number
  montoFormateado: string
  fecha: string
  pagoId: number
  estadoPago: EstadoPago
  numeroOrden: string
  estadoSuscripcion: string
}

export type EstadoPago = 'PENDIENTE' | 'APROBADO' | 'RECHAZADO' | 'ANULADO'

export interface Pago {
  id: number
  ventaId: number
  suscripcionId: number
  usuarioId: number
  clienteNombre?: string
  clienteEmail?: string
  plan: string
  monto: number
  montoFormateado: string
  fecha: string
  estado: EstadoPago
  pasarela: string
  idTransaccion?: string
  numeroOrden: string
  codigoAutorizacion?: string
  tarjeta?: string
  tipoPago?: string
  cuotas?: number
  fechaTransaccion?: string
  codigoRespuesta?: number
  estadoSuscripcion?: string
  mensaje: string
  comprobanteDisponible: boolean
}

export interface Transaccion {
  ventaId: number
  pagoId: number
  numeroOrden: string
  monto: number
  token: string
  url: string
}

export interface Comprobante {
  pagoId: number
  estado: string
  clienteNombre: string
  clienteRut: string
  clienteEmail: string
  perfil: string
  plan: string
  monto: number
  montoFormateado: string
  fecha: string
  hora: string
  numeroOrden: string
  idTransaccion: string
  codigoAutorizacion?: string
  pasarela: string
  tarjeta?: string
  tipoPago?: string
  cuotas?: number
  vigenteHasta?: string
}

export interface Notificacion {
  id: number
  usuarioId: number
  tipo: string
  canal: string
  destinatario: string
  asunto: string
  resumen?: string
  estado: 'ENVIADO' | 'FALLIDO'
  detalleError?: string
  leida: boolean
  creadoEn: string
  enviadoEn?: string
}

export interface EstadoAsistente {
  acceso: boolean
  rol: Rol
  plan: string
  motivo: string
  modelo: string
  sugerencias: string[]
}

export interface MensajeChat {
  rol: 'user' | 'assistant'
  contenido: string
  fuentes?: string[]
  error?: boolean
  fecha: string
}

export interface MesCantidad { mes: string; cantidad: number }

export interface EstadisticasUsuarios {
  totalUsuarios: number
  activos: number
  inactivos: number
  emailNoConfirmado: number
  porRol: Record<string, number>
  nuevosUltimos30Dias: number
  registrosPorMes: MesCantidad[]
}

export interface EstadisticasLicitaciones {
  totalLicitaciones: number
  licitacionesPorEstado: Record<string, number>
  licitacionesPorCategoria: Record<string, number>
  totalPostulaciones: number
  postulacionesPorEstado: Record<string, number>
  montoTotalLicitado: number
  publicacionesPorMes: MesCantidad[]
}

export interface EstadisticasVentas {
  totalVentas: number
  ingresosAprobados: number
  premiumActivas: number
  pagosPorEstado: Record<string, number>
  suscripcionesPorEstado: Record<string, number>
  ingresosPorMes: { mes: string; monto: number; cantidad: number }[]
}

export interface RolInfo { id: number; nombre: Rol; cantidadUsuarios: number }
