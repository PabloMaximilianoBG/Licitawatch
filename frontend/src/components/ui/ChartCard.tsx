import type { ReactNode } from 'react'
import { Card, CardHeader } from './Card'

export function ChartCard({ title, subtitle, children, action }: { title: string; subtitle?: string; children: ReactNode; action?: ReactNode }) {
  return (
    <Card>
      <CardHeader title={title} subtitle={subtitle} action={action} />
      <div className="h-64 px-3 py-4">{children}</div>
    </Card>
  )
}
