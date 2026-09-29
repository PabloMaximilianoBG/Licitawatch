import { useCallback, useEffect, useRef, useState } from 'react'
import { apiError } from '@/api/client'
import type { ApiError } from '@/types'

/** Carga de datos con estados loading / error / data y recarga manual. */
export function useAsync<T>(fn: () => Promise<T>, deps: unknown[] = []) {
  const [data, setData] = useState<T | null>(null)
  const [error, setError] = useState<ApiError | null>(null)
  const [loading, setLoading] = useState(true)
  const vivo = useRef(true)
  // eslint-disable-next-line react-hooks/exhaustive-deps
  const cargar = useCallback(fn, deps)
  const reload = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const r = await cargar()
      if (vivo.current) setData(r)
    } catch (e) {
      if (vivo.current) setError(apiError(e))
    } finally {
      if (vivo.current) setLoading(false)
    }
  }, [cargar])
  useEffect(() => {
    vivo.current = true
    reload()
    return () => { vivo.current = false }
  }, [reload])
  return { data, error, loading, reload, setData }
}
