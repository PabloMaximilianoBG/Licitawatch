import { create } from 'zustand'

export type ToastTipo = 'success' | 'error' | 'info'
export interface ToastItem { id: number; tipo: ToastTipo; mensaje: string }

interface ToastState {
  items: ToastItem[]
  push: (tipo: ToastTipo, mensaje: string) => void
  remove: (id: number) => void
}

let seq = 0
export const useToasts = create<ToastState>((set) => ({
  items: [],
  push: (tipo, mensaje) => {
    const id = ++seq
    set((s) => ({ items: [...s.items.filter((t) => t.mensaje !== mensaje), { id, tipo, mensaje }].slice(-4) }))
    setTimeout(() => set((s) => ({ items: s.items.filter((t) => t.id !== id) })), 4500)
  },
  remove: (id) => set((s) => ({ items: s.items.filter((t) => t.id !== id) })),
}))

export const toast = {
  success: (m: string) => useToasts.getState().push('success', m),
  error: (m: string) => useToasts.getState().push('error', m),
  info: (m: string) => useToasts.getState().push('info', m),
}
