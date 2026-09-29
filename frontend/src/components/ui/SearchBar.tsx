import { Search, X } from 'lucide-react'
import { useEffect, useState } from 'react'

export function SearchBar({ value, onSearch, placeholder = 'Buscar…' }: { value: string; onSearch: (q: string) => void; placeholder?: string }) {
  const [q, setQ] = useState(value)
  useEffect(() => setQ(value), [value])
  return (
    <form role="search" onSubmit={(e) => { e.preventDefault(); onSearch(q.trim()) }} className="relative flex-1">
      <Search className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-ink-400" />
      <input value={q} onChange={(e) => setQ(e.target.value)} placeholder={placeholder} aria-label={placeholder}
        className="field h-11 pl-10 pr-24" maxLength={120} />
      {q && <button type="button" onClick={() => { setQ(''); onSearch('') }} aria-label="Limpiar búsqueda"
        className="absolute right-20 top-1/2 -translate-y-1/2 rounded p-1 text-ink-400 hover:text-ink-700"><X className="h-4 w-4" /></button>}
      <button type="submit" className="absolute right-1.5 top-1/2 h-8 -translate-y-1/2 rounded-lg bg-brand-gradient px-3 text-xs font-semibold text-white">Buscar</button>
    </form>
  )
}
