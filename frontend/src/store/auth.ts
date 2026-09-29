import { create } from 'zustand'
import { persist } from 'zustand/middleware'
import type { Rol, Suscripcion, Usuario } from '@/types'

interface AuthState {
  token: string | null
  expiraEn: string | null
  usuario: Usuario | null
  rol: Rol | null
  suscripcion: Suscripcion | null
  isAuthenticated: () => boolean
  login: (token: string, expiraEn: string, usuario: Usuario) => void
  setUsuario: (u: Usuario) => void
  setSuscripcion: (s: Suscripcion | null) => void
  logout: () => void
}

/** Estado de autenticación (sec. 39): usuario, token, rol, autenticación y suscripción. */
export const useAuth = create<AuthState>()(
  persist(
    (set, get) => ({
      token: null,
      expiraEn: null,
      usuario: null,
      rol: null,
      suscripcion: null,
      isAuthenticated: () => {
        const { token, expiraEn } = get()
        return !!token && (!expiraEn || new Date(expiraEn).getTime() > Date.now())
      },
      login: (token, expiraEn, usuario) => set({ token, expiraEn, usuario, rol: usuario.rol, suscripcion: null }),
      setUsuario: (usuario) => set({ usuario, rol: usuario.rol }),
      setSuscripcion: (suscripcion) => set({ suscripcion }),
      logout: () => set({ token: null, expiraEn: null, usuario: null, rol: null, suscripcion: null }),
    }),
    { name: 'licitawatch-auth' },
  ),
)

export const rutaInicio = (rol: Rol | null) =>
  rol === 'ADMINISTRADOR' ? '/dashboard/admin' : rol === 'LICITADOR' ? '/dashboard/licitador' : '/dashboard/pyme'
