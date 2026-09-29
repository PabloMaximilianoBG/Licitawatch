import { Cell, Legend, Pie, PieChart, ResponsiveContainer, Tooltip } from 'recharts'
import { etiquetaEstado } from '@/utils/format'

const COLORES: Record<string, string> = {
  BORRADOR: '#94a3b8', PUBLICADA: '#10b981', CERRADA: '#f59e0b', ADJUDICADA: '#6366f1', CANCELADA: '#ef4444', SUSPENDIDA: '#b91c1c',
  ENVIADA: '#3b82f6', EN_REVISION: '#f59e0b', SELECCIONADA: '#10b981', RECHAZADA: '#ef4444', RETIRADA: '#94a3b8',
  PENDIENTE: '#f59e0b', APROBADO: '#10b981', RECHAZADO: '#ef4444', ANULADO: '#94a3b8', ACTIVA: '#10b981', VENCIDA: '#94a3b8',
  PYME: '#3b82f6', LICITADOR: '#6366f1', ADMINISTRADOR: '#8b5cf6',
}
const PALETA = ['#3b82f6', '#6366f1', '#10b981', '#f59e0b', '#ec4899', '#14b8a6', '#8b5cf6', '#ef4444']

export function EstadoDonut({ data }: { data: Record<string, number> }) {
  const filas = Object.entries(data).filter(([, v]) => v > 0).map(([k, v]) => ({ name: etiquetaEstado[k] ?? k, key: k, value: v }))
  if (!filas.length) return <div className="grid h-full place-items-center text-sm text-ink-400">Sin datos aún</div>
  return (
    <ResponsiveContainer width="100%" height="100%">
      <PieChart>
        <Pie data={filas} dataKey="value" nameKey="name" innerRadius="55%" outerRadius="80%" paddingAngle={2} stroke="none">
          {filas.map((f, i) => <Cell key={f.key} fill={COLORES[f.key] ?? PALETA[i % PALETA.length]} />)}
        </Pie>
        <Tooltip contentStyle={{ borderRadius: 12, border: '1px solid #e2e8f0', fontSize: 12 }} />
        <Legend iconType="circle" wrapperStyle={{ fontSize: 12 }} />
      </PieChart>
    </ResponsiveContainer>
  )
}
