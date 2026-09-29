import axios, { AxiosError } from 'axios'
import type { ApiError } from '@/types'
import { useAuth } from '@/store/auth'
import { toast } from '@/store/toast'

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

/** Todo el frontend habla SOLO con el API Gateway (8080). */
export const api = axios.create({ baseURL: API_BASE_URL, timeout: 90_000 })

// Adjunta el JWT en cada request
api.interceptors.request.use((config) => {
  const token = useAuth.getState().token
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// Manejo centralizado de 401 / 403 / 429 / errores de red
api.interceptors.response.use(
  (r) => r,
  (error: AxiosError<ApiError>) => {
    const status = error.response?.status
    const code = error.response?.data?.code
    const url = error.config?.url ?? ''
    if (status === 401 && !url.includes('/api/auth/')) {
      const expirada = code === 'TOKEN_EXPIRADO'
      useAuth.getState().logout()
      toast.error(expirada ? 'Tu sesión expiró. Inicia sesión nuevamente.' : 'Debes iniciar sesión para continuar.')
      if (!window.location.pathname.startsWith('/login')) {
        window.location.assign(`/login?${expirada ? 'expirada=1&' : ''}redirect=${encodeURIComponent(window.location.pathname + window.location.search)}`)
      }
    } else if (status === 403 && code === 'ACCESO_DENEGADO') {
      toast.error('No tienes permisos para realizar esta acción.')
    } else if (status === 429) {
      toast.error(error.response?.data?.message ?? 'Demasiadas solicitudes. Intenta en unos segundos.')
    } else if (!error.response) {
      toast.error('No se pudo conectar con LicitaWatch. Verifica que el API Gateway esté en ejecución.')
    }
    return Promise.reject(error)
  },
)

/** Normaliza cualquier error de Axios al formato de error de la API. */
export function apiError(e: unknown): ApiError {
  const err = e as AxiosError<ApiError>
  if (err?.response?.data && typeof err.response.data === 'object') {
    return { ...err.response.data, status: err.response.status }
  }
  if (err?.response) return { status: err.response.status, message: 'Ocurrió un error inesperado.' }
  return { status: 0, message: 'No se pudo conectar con el servidor.' }
}
