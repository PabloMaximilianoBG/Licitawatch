import { useEffect } from 'react'
import { ventasApi } from '@/api/services'
import { useAuth } from '@/store/auth'

/** Mantiene la suscripción del usuario en el store (fuente de verdad: API Ventas). */
export function useSuscripcion() {
  const { suscripcion, setSuscripcion, token } = useAuth()
  useEffect(() => {
    if (token) ventasApi.suscripcion().then(setSuscripcion).catch(() => undefined)
  }, [token, setSuscripcion])
  return {
    suscripcion,
    refrescar: () => ventasApi.suscripcion().then((s) => { setSuscripcion(s); return s }),
  }
}
