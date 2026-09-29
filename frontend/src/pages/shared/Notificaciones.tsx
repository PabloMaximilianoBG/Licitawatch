import { useState } from 'react'
import { CheckCheck, Mail, MailWarning } from 'lucide-react'
import { Button, Card, EmptyState, ErrorState, PageHeader, Pagination, Spinner } from '@/components/ui'
import { notificacionesApi } from '@/api/services'
import { useAsync } from '@/hooks/useAsync'
import { toast } from '@/store/toast'
import { cn, fecha } from '@/utils/format'

export default function Notificaciones() {
  const [page, setPage] = useState(0)
  const { data, loading, error, reload } = useAsync(() => notificacionesApi.mias(page, 15), [page])
  const marcarTodas = async () => { await notificacionesApi.marcarTodas(); toast.success('Notificaciones marcadas como leídas'); reload() }
  const marcar = async (id: number) => { await notificacionesApi.marcarLeida(id); reload() }
  return (
    <>
      <PageHeader eyebrow="Cuenta" title="Notificaciones" subtitle="Avisos enviados a tu correo por la plataforma."
        actions={<Button variant="secondary" icon={<CheckCheck className="h-4 w-4" />} onClick={marcarTodas}>Marcar todas como leídas</Button>} />
      <Card>
        {loading && <Spinner />}
        {error && <ErrorState message={error.message} onRetry={reload} />}
        {data && data.content.length === 0 && <EmptyState title="Sin notificaciones" message="Aquí verás los avisos de licitaciones, postulaciones y pagos." />}
        <ul className="divide-y divide-ink-100">
          {data?.content.map((n) => (
            <li key={n.id} className={cn('flex gap-4 px-5 py-4', !n.leida && 'bg-brand-50/40')}>
              <div className={cn('grid h-10 w-10 shrink-0 place-items-center rounded-xl', n.estado === 'ENVIADO' ? 'bg-brand-50 text-brand-600' : 'bg-red-50 text-red-500')}>
                {n.estado === 'ENVIADO' ? <Mail className="h-5 w-5" /> : <MailWarning className="h-5 w-5" />}
              </div>
              <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-center gap-2">
                  <p className="font-semibold text-ink-900">{n.asunto}</p>
                  {!n.leida && <span className="h-2 w-2 rounded-full bg-brand-500" aria-label="No leída" />}
                </div>
                {n.resumen && <p className="mt-0.5 text-sm text-ink-600">{n.resumen}</p>}
                <p className="mt-1 text-xs text-ink-400">{fecha(n.creadoEn, true)} · {n.estado === 'ENVIADO' ? `enviado a ${n.destinatario}` : 'no se pudo enviar el correo'}</p>
              </div>
              {!n.leida && <Button size="sm" variant="ghost" onClick={() => marcar(n.id)}>Marcar leída</Button>}
            </li>
          ))}
        </ul>
        {data && <Pagination page={data.page} totalPages={data.totalPages} totalElements={data.totalElements} onChange={setPage} />}
      </Card>
    </>
  )
}
