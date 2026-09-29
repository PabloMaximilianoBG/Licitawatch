const clpFmt = new Intl.NumberFormat('es-CL', { style: 'currency', currency: 'CLP', maximumFractionDigits: 0 })
const numFmt = new Intl.NumberFormat('es-CL')

export const clp = (n?: number | null) => (n === undefined || n === null ? '—' : clpFmt.format(n))
export const num = (n?: number | null) => (n === undefined || n === null ? '—' : numFmt.format(n))

/** "2026-10-20" → "20 oct 2026" (fechas sin zona horaria como fecha local). */
export function fecha(iso?: string | null, conHora = false) {
  if (!iso) return '—'
  const d = iso.length === 10 ? new Date(`${iso}T00:00:00`) : new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  return d.toLocaleString('es-CL', {
    day: '2-digit', month: 'short', year: 'numeric', ...(conHora ? { hour: '2-digit', minute: '2-digit' } : {}),
  })
}

export function haceCuanto(iso?: string | null) {
  if (!iso) return ''
  const seg = Math.round((Date.now() - new Date(iso).getTime()) / 1000)
  if (seg < 60) return 'hace un momento'
  if (seg < 3600) return `hace ${Math.floor(seg / 60)} min`
  if (seg < 86400) return `hace ${Math.floor(seg / 3600)} h`
  return `hace ${Math.floor(seg / 86400)} d`
}

export function mesCorto(ym: string) {
  const [y, m] = ym.split('-').map(Number)
  return new Date(y, m - 1, 1).toLocaleDateString('es-CL', { month: 'short' }).replace('.', '')
}

/** Validación de RUT chileno (módulo 11), igual que el backend. */
export function rutValido(rut: string) {
  const limpio = rut.replace(/[.\-\s]/g, '').toUpperCase()
  if (!/^\d{7,8}[\dK]$/.test(limpio)) return false
  const cuerpo = limpio.slice(0, -1)
  const dv = limpio.slice(-1)
  let s = 0, m = 2
  for (let i = cuerpo.length - 1; i >= 0; i--) { s += Number(cuerpo[i]) * m; m = m === 7 ? 2 : m + 1 }
  const r = 11 - (s % 11)
  return (r === 11 ? '0' : r === 10 ? 'K' : String(r)) === dv
}

/** Formatea mientras se escribe: 765432103 → 76.543.210-3 */
export function formatearRut(v: string) {
  const limpio = v.replace(/[^\dkK]/g, '').toUpperCase().slice(0, 9)
  if (limpio.length < 2) return limpio
  const cuerpo = limpio.slice(0, -1).replace(/\B(?=(\d{3})+(?!\d))/g, '.')
  return `${cuerpo}-${limpio.slice(-1)}`
}

export const PASSWORD_REGEX = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,72}$/
export const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

export const cn = (...c: unknown[]) => c.filter((x): x is string => typeof x === 'string' && x.length > 0).join(' ')

export const etiquetaEstado: Record<string, string> = {
  BORRADOR: 'Borrador', PUBLICADA: 'Publicada', CERRADA: 'Cerrada', ADJUDICADA: 'Adjudicada', CANCELADA: 'Cancelada',
  SUSPENDIDA: 'Suspendida', ENVIADA: 'Enviada', EN_REVISION: 'En revisión', SELECCIONADA: 'Seleccionada',
  RECHAZADA: 'Rechazada', RETIRADA: 'Retirada', PENDIENTE: 'Pendiente', APROBADO: 'Aprobado', RECHAZADO: 'Rechazado',
  ANULADO: 'Anulado', ACTIVA: 'Activa', VENCIDA: 'Vencida', ENVIADO: 'Enviado', FALLIDO: 'Fallido',
  PYME: 'Pyme', LICITADOR: 'Licitador', ADMINISTRADOR: 'Administrador', ESTANDAR: 'Estándar', PREMIUM: 'Premium',
}

export const CATEGORIAS = ['Tecnología', 'Construcción', 'Servicios generales', 'Aseo y mantención', 'Consultoría',
  'Transporte y logística', 'Salud', 'Educación y capacitación', 'Alimentación', 'Seguridad', 'Marketing', 'Otros']

export const REGIONES = ['Arica y Parinacota', 'Tarapacá', 'Antofagasta', 'Atacama', 'Coquimbo', 'Valparaíso',
  'Metropolitana', "O'Higgins", 'Maule', 'Ñuble', 'Biobío', 'Araucanía', 'Los Ríos', 'Los Lagos', 'Aysén', 'Magallanes']
