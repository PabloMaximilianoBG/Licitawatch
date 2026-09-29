import type { ReactNode } from 'react'
import {
  BarChart3, Bell, Bot, Building2, CreditCard, FilePlus2, FileSearch, FileText, Gavel, LayoutDashboard, Receipt,
  Send, ShieldCheck, Sparkles, User, Users, Wallet,
} from 'lucide-react'
import type { Rol } from '@/types'

export interface NavItem { to: string; label: string; icon: ReactNode; end?: boolean; premium?: boolean }
export interface NavGrupo { titulo: string; items: NavItem[] }

const i = (C: typeof LayoutDashboard) => <C className="h-[18px] w-[18px]" />

const cuenta: NavGrupo = {
  titulo: 'Cuenta',
  items: [
    { to: '/dashboard/suscripcion', label: 'Suscripción', icon: i(Wallet) },
    { to: '/dashboard/pagos', label: 'Historial de pagos', icon: i(Receipt) },
    { to: '/dashboard/notificaciones', label: 'Notificaciones', icon: i(Bell) },
    { to: '/dashboard/perfil', label: 'Perfil', icon: i(User) },
  ],
}

/** El sidebar cambia según el rol (sec. 34). */
export const navegacion: Record<Rol, NavGrupo[]> = {
  PYME: [
    { titulo: 'Oportunidades', items: [
      { to: '/dashboard/pyme', label: 'Resumen', icon: i(LayoutDashboard), end: true },
      { to: '/dashboard/pyme/licitaciones', label: 'Buscar licitaciones', icon: i(FileSearch) },
      { to: '/dashboard/pyme/postulaciones', label: 'Mis postulaciones', icon: i(Send) },
      { to: '/dashboard/asistente', label: 'LicitAsist', icon: i(Sparkles), premium: true },
    ] },
    cuenta,
  ],
  LICITADOR: [
    { titulo: 'Gestión', items: [
      { to: '/dashboard/licitador', label: 'Resumen', icon: i(LayoutDashboard), end: true },
      { to: '/dashboard/licitador/licitaciones', label: 'Mis licitaciones', icon: i(Gavel), end: true },
      { to: '/dashboard/licitador/licitaciones/nueva', label: 'Crear licitación', icon: i(FilePlus2) },
      { to: '/dashboard/asistente', label: 'LicitAsist', icon: i(Sparkles), premium: true },
    ] },
    cuenta,
  ],
  ADMINISTRADOR: [
    { titulo: 'Administración', items: [
      { to: '/dashboard/admin', label: 'Estadísticas', icon: i(BarChart3), end: true },
      { to: '/dashboard/admin/usuarios', label: 'Usuarios y roles', icon: i(Users) },
      { to: '/dashboard/admin/licitaciones', label: 'Licitaciones', icon: i(Building2) },
      { to: '/dashboard/admin/postulaciones', label: 'Postulaciones', icon: i(FileText) },
      { to: '/dashboard/admin/ventas', label: 'Ventas y pagos', icon: i(CreditCard) },
      { to: '/dashboard/admin/notificaciones', label: 'Notificaciones', icon: i(Bell) },
      { to: '/dashboard/asistente', label: 'LicitAsist', icon: i(Bot) },
    ] },
    { titulo: 'Cuenta', items: [{ to: '/dashboard/perfil', label: 'Perfil', icon: i(ShieldCheck) }] },
  ],
}
