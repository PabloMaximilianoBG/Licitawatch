import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { mesCorto } from '@/utils/format'

export function BarrasMes({ data, dataKey = 'cantidad', color = '#3b82f6', formato }: {
  data: { mes: string }[]; dataKey?: string; color?: string; formato?: (v: number) => string
}) {
  return (
    <ResponsiveContainer width="100%" height="100%">
      <BarChart data={data.map((d) => ({ ...d, etiqueta: mesCorto(d.mes) }))} margin={{ left: -10, right: 8, top: 8 }}>
        <CartesianGrid vertical={false} stroke="#eef2f7" />
        <XAxis dataKey="etiqueta" tickLine={false} axisLine={false} fontSize={12} stroke="#94a3b8" />
        <YAxis allowDecimals={false} tickLine={false} axisLine={false} fontSize={12} stroke="#94a3b8" tickFormatter={formato ? (v) => formato(Number(v)) : undefined} width={formato ? 70 : 40} />
        <Tooltip cursor={{ fill: '#f1f5f9' }} contentStyle={{ borderRadius: 12, border: '1px solid #e2e8f0', fontSize: 12 }}
          formatter={(v) => (formato ? formato(Number(v)) : v)} />
        <Bar dataKey={dataKey} fill={color} radius={[6, 6, 0, 0]} maxBarSize={42} />
      </BarChart>
    </ResponsiveContainer>
  )
}
