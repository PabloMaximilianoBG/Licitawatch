import { Link, useParams } from 'react-router-dom'
import { ArrowLeft } from 'lucide-react'
import { ErrorState, Spinner } from '@/components/ui'
import { Voucher } from '@/components/Voucher'
import { ventasApi } from '@/api/services'
import { useAsync } from '@/hooks/useAsync'

export default function ComprobantePage() {
  const { id } = useParams()
  const { data, loading, error, reload } = useAsync(() => ventasApi.comprobante(id!), [id])
  return (
    <>
      <Link to="/dashboard/pagos" className="mb-6 inline-flex items-center gap-1.5 text-sm text-ink-500 hover:text-ink-900 print:hidden"><ArrowLeft className="h-4 w-4" />Historial de pagos</Link>
      {loading && <Spinner />}
      {error && <ErrorState message={error.message} onRetry={reload} />}
      {data && <Voucher c={data} />}
    </>
  )
}
