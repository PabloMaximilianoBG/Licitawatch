import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { Crown } from 'lucide-react'
import { Button, EmptyState, ErrorState, FilterPanel, Input, LoadingCards, PageHeader, Pagination, SearchBar, Select } from '@/components/ui'
import { LicitacionCard } from '@/components/LicitacionCard'
import { licitacionesApi, type FiltrosLicitacion } from '@/api/services'
import { useAsync } from '@/hooks/useAsync'
import { CATEGORIAS, REGIONES } from '@/utils/format'
import { Link } from 'react-router-dom'

const CAMPOS = ['q', 'categoria', 'region', 'montoMin', 'montoMax', 'cierreDesde', 'cierreHasta', 'orden'] as const

export default function BuscarLicitaciones() {
  const [params, setParams] = useSearchParams()
  const filtros: FiltrosLicitacion = Object.fromEntries(CAMPOS.map((k) => [k, params.get(k) ?? ''])) as FiltrosLicitacion
  const page = Number(params.get('page') ?? 0)
  const [borrador, setBorrador] = useState(filtros)
  const { data, loading, error, reload } = useAsync(() => licitacionesApi.buscar({ ...filtros, page, size: 12 }), [params.toString()])

  const aplicar = (f: FiltrosLicitacion, p = 0) => {
    const n = new URLSearchParams()
    Object.entries(f).forEach(([k, v]) => v && n.set(k, String(v)))
    if (p) n.set('page', String(p))
    setParams(n)
  }
  const activos = CAMPOS.filter((k) => k !== 'q' && k !== 'orden' && filtros[k]).length
  const set = (k: keyof FiltrosLicitacion) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => setBorrador((b) => ({ ...b, [k]: e.target.value }))

  return (
    <>
      <PageHeader eyebrow="Oportunidades" title="Buscar licitaciones" subtitle="Filtra por categoría, región, monto y fecha de cierre." />
      <div className="mb-5 flex flex-col gap-3 sm:flex-row">
        <SearchBar value={filtros.q ?? ''} placeholder="Buscar por título, descripción o rubro…" onSearch={(q) => aplicar({ ...filtros, q })} />
        <Select aria-label="Ordenar" className="sm:w-56" value={filtros.orden ?? ''} onChange={(e) => aplicar({ ...filtros, orden: e.target.value })}
          options={[{ value: '', label: 'Cierre más próximo' }, { value: 'recientes', label: 'Más recientes' }, { value: 'monto_desc', label: 'Mayor monto' }, { value: 'monto_asc', label: 'Menor monto' }]} />
      </div>
      <div className="grid gap-6 lg:grid-cols-[280px_1fr]">
        <aside className="h-fit lg:sticky lg:top-24">
          <FilterPanel activos={activos} onApply={() => aplicar({ ...borrador, q: filtros.q, orden: filtros.orden })}
            onReset={() => { const v = { q: filtros.q, orden: filtros.orden }; setBorrador(v); aplicar(v) }}>
            <Select label="Categoría" value={borrador.categoria ?? ''} onChange={set('categoria')} placeholder="Todas" options={CATEGORIAS.map((c) => ({ value: c, label: c }))} />
            <Select label="Región" value={borrador.region ?? ''} onChange={set('region')} placeholder="Todas" options={REGIONES.map((c) => ({ value: c, label: c }))} />
            <div className="grid grid-cols-2 gap-3">
              <Input label="Monto mín." type="number" min={0} value={borrador.montoMin ?? ''} onChange={set('montoMin')} />
              <Input label="Monto máx." type="number" min={0} value={borrador.montoMax ?? ''} onChange={set('montoMax')} />
            </div>
            <Input label="Cierra desde" type="date" value={borrador.cierreDesde ?? ''} onChange={set('cierreDesde')} />
            <Input label="Cierra hasta" type="date" value={borrador.cierreHasta ?? ''} onChange={set('cierreHasta')} />
          </FilterPanel>
        </aside>
        <section>
          {loading && <LoadingCards n={6} />}
          {error && (error.code === 'LIMITE_PLAN'
            ? <div className="card p-2"><EmptyState icon={<Crown className="h-6 w-6" />} title="Límite de búsquedas alcanzado" message={error.message}
              action={<Link to="/planes"><Button>Ver plan Premium</Button></Link>} /></div>
            : <ErrorState message={error.message} onRetry={reload} />)}
          {data && !loading && (
            data.content.length === 0
              ? <div className="card"><EmptyState title="No encontramos licitaciones" message="Prueba con otros términos o limpia los filtros." /></div>
              : <>
                <p className="mb-3 text-sm text-ink-500">{data.totalElements} licitaciones publicadas</p>
                <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
                  {data.content.map((l) => <LicitacionCard key={l.id} l={l} to={`/dashboard/pyme/licitaciones/${l.id}`} />)}
                </div>
                <div className="card mt-4"><Pagination page={data.page} totalPages={data.totalPages} totalElements={data.totalElements} onChange={(p) => aplicar(filtros, p)} /></div>
              </>
          )}
        </section>
      </div>
    </>
  )
}
