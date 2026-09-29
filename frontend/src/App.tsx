import { Route, Routes } from 'react-router-dom'
import { Toaster } from '@/components/ui'
import { ProtectedRoute, RedirectDashboard } from '@/components/ProtectedRoute'
import { DashboardLayout } from '@/components/layout/DashboardLayout'
import Landing from '@/pages/public/Landing'
import Login from '@/pages/public/Login'
import RegisterSelect from '@/pages/public/RegisterSelect'
import RegisterPyme from '@/pages/public/RegisterPyme'
import RegisterLicitador from '@/pages/public/RegisterLicitador'
import ConfirmEmail from '@/pages/public/ConfirmEmail'
import Planes from '@/pages/public/Planes'
import Pago from '@/pages/pago/Pago'
import PagoResultado from '@/pages/pago/PagoResultado'
import NotFound from '@/pages/public/NotFound'
import PymeResumen from '@/pages/pyme/PymeResumen'
import BuscarLicitaciones from '@/pages/pyme/BuscarLicitaciones'
import LicitacionDetallePyme from '@/pages/pyme/LicitacionDetalle'
import MisPostulaciones from '@/pages/pyme/MisPostulaciones'
import LicitadorResumen from '@/pages/licitador/LicitadorResumen'
import MisLicitaciones from '@/pages/licitador/MisLicitaciones'
import LicitacionForm from '@/pages/licitador/LicitacionForm'
import LicitacionGestion from '@/pages/licitador/LicitacionGestion'
import AdminResumen from '@/pages/admin/AdminResumen'
import AdminUsuarios from '@/pages/admin/AdminUsuarios'
import AdminLicitaciones from '@/pages/admin/AdminLicitaciones'
import AdminPostulaciones from '@/pages/admin/AdminPostulaciones'
import AdminVentas from '@/pages/admin/AdminVentas'
import AdminNotificaciones from '@/pages/admin/AdminNotificaciones'
import Suscripcion from '@/pages/shared/Suscripcion'
import HistorialPagos from '@/pages/shared/HistorialPagos'
import ComprobantePage from '@/pages/shared/ComprobantePage'
import LicitAsist from '@/pages/shared/LicitAsist'
import Perfil from '@/pages/shared/Perfil'
import Notificaciones from '@/pages/shared/Notificaciones'

export default function App() {
  return (
    <>
      <Routes>
        <Route path="/" element={<Landing />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<RegisterSelect />} />
        <Route path="/register/pyme" element={<RegisterPyme />} />
        <Route path="/register/licitador" element={<RegisterLicitador />} />
        <Route path="/confirm-email" element={<ConfirmEmail />} />
        <Route path="/planes" element={<Planes />} />

        <Route element={<ProtectedRoute roles={['PYME', 'LICITADOR']} />}>
          <Route path="/pago" element={<Pago />} />
        </Route>
        <Route element={<ProtectedRoute />}>
          <Route path="/pago/resultado" element={<PagoResultado />} />
          <Route path="/dashboard" element={<RedirectDashboard />} />
          <Route element={<DashboardLayout />}>
            <Route element={<ProtectedRoute roles={['PYME']} />}>
              <Route path="/dashboard/pyme" element={<PymeResumen />} />
              <Route path="/dashboard/pyme/licitaciones" element={<BuscarLicitaciones />} />
              <Route path="/dashboard/pyme/licitaciones/:id" element={<LicitacionDetallePyme />} />
              <Route path="/dashboard/pyme/postulaciones" element={<MisPostulaciones />} />
            </Route>
            <Route element={<ProtectedRoute roles={['LICITADOR']} />}>
              <Route path="/dashboard/licitador" element={<LicitadorResumen />} />
              <Route path="/dashboard/licitador/licitaciones" element={<MisLicitaciones />} />
              <Route path="/dashboard/licitador/licitaciones/nueva" element={<LicitacionForm />} />
              <Route path="/dashboard/licitador/licitaciones/:id" element={<LicitacionGestion />} />
              <Route path="/dashboard/licitador/licitaciones/:id/editar" element={<LicitacionForm />} />
            </Route>
            <Route element={<ProtectedRoute roles={['ADMINISTRADOR']} />}>
              <Route path="/dashboard/admin" element={<AdminResumen />} />
              <Route path="/dashboard/admin/usuarios" element={<AdminUsuarios />} />
              <Route path="/dashboard/admin/licitaciones" element={<AdminLicitaciones />} />
              <Route path="/dashboard/admin/postulaciones" element={<AdminPostulaciones />} />
              <Route path="/dashboard/admin/ventas" element={<AdminVentas />} />
              <Route path="/dashboard/admin/notificaciones" element={<AdminNotificaciones />} />
            </Route>
            <Route element={<ProtectedRoute roles={['PYME', 'LICITADOR']} />}>
              <Route path="/dashboard/suscripcion" element={<Suscripcion />} />
              <Route path="/dashboard/pagos" element={<HistorialPagos />} />
              <Route path="/dashboard/pagos/:id/comprobante" element={<ComprobantePage />} />
            </Route>
            <Route path="/dashboard/asistente" element={<LicitAsist />} />
            <Route path="/dashboard/perfil" element={<Perfil />} />
            <Route path="/dashboard/notificaciones" element={<Notificaciones />} />
            <Route path="/dashboard/*" element={<NotFound embedded />} />
          </Route>
        </Route>
        <Route path="*" element={<NotFound />} />
      </Routes>
      <Toaster />
    </>
  )
}
