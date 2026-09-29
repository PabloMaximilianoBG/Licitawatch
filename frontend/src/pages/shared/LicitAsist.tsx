import { useEffect, useRef, useState, type FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import { AlertTriangle, Bot, Crown, Database, Lock, RotateCcw, SendHorizonal, Sparkles, User, X } from 'lucide-react'
import { Badge, Button, ErrorState, Spinner } from '@/components/ui'
import { asistenteApi, licitacionesApi } from '@/api/services'
import { apiError } from '@/api/client'
import { useAsync } from '@/hooks/useAsync'
import { useAuth } from '@/store/auth'
import type { MensajeChat } from '@/types'
import { cn, etiquetaEstado } from '@/utils/format'

const CLAVE = 'licitasist-historial'

/** Interfaz tipo AI Assistant (sec. 34). El acceso lo decide el backend (Premium o Administrador). */
export default function LicitAsist() {
  const usuario = useAuth((s) => s.usuario)
  const [params, setParams] = useSearchParams()
  const licitacionId = params.get('licitacion') ? Number(params.get('licitacion')) : undefined
  const estado = useAsync(asistenteApi.estado)
  const contexto = useAsync(() => (licitacionId ? licitacionesApi.obtener(licitacionId) : Promise.resolve(null)), [licitacionId])
  const [mensajes, setMensajes] = useState<MensajeChat[]>(() => {
    try { return JSON.parse(sessionStorage.getItem(`${CLAVE}-${usuario?.id}`) ?? '[]') } catch { return [] }
  })
  const [texto, setTexto] = useState('')
  const [pensando, setPensando] = useState(false)
  const fin = useRef<HTMLDivElement>(null)
  const inputRef = useRef<HTMLTextAreaElement>(null)

  useEffect(() => { sessionStorage.setItem(`${CLAVE}-${usuario?.id}`, JSON.stringify(mensajes.slice(-30))) }, [mensajes, usuario?.id])
  useEffect(() => { fin.current?.scrollIntoView({ behavior: 'smooth' }) }, [mensajes, pensando])

  const enviar = async (pregunta: string, e?: FormEvent) => {
    e?.preventDefault()
    const q = pregunta.trim()
    if (!q || pensando) return
    const previo = mensajes
    setMensajes([...previo, { rol: 'user', contenido: q, fecha: new Date().toISOString() }])
    setTexto('')
    setPensando(true)
    try {
      const r = await asistenteApi.chat(q, previo, licitacionId)
      setMensajes((m) => [...m, { rol: 'assistant', contenido: r.respuesta, fuentes: r.fuentes, fecha: r.generadoEn }])
    } catch (err) {
      const a = apiError(err)
      setMensajes((m) => [...m, { rol: 'assistant', contenido: a.message, error: true, fecha: new Date().toISOString() }])
      if (a.code === 'PREMIUM_REQUERIDO') estado.reload()
    } finally {
      setPensando(false)
      inputRef.current?.focus()
    }
  }

  if (estado.loading) return <Spinner label="Verificando tu acceso a LicitAsist…" />
  if (estado.error) return <ErrorState message={estado.error.message} onRetry={estado.reload} />
  const est = estado.data!

  if (!est.acceso) {
    return (
      <div className="relative mx-auto max-w-2xl overflow-hidden rounded-3xl bg-ink-950 p-8 text-center text-white sm:p-12">
        <div className="absolute inset-0 bg-grid-dark [background-size:28px_28px]" />
        <div className="absolute left-1/2 top-0 h-60 w-60 -translate-x-1/2 rounded-full bg-brand-600/40 blur-3xl" />
        <div className="relative">
          <div className="mx-auto grid h-16 w-16 place-items-center rounded-2xl bg-brand-gradient shadow-glow"><Lock className="h-7 w-7" /></div>
          <h1 className="mt-6 text-2xl font-bold sm:text-3xl">LicitAsist es parte de Premium</h1>
          <p className="mt-3 text-ink-300">{est.motivo}</p>
          <ul className="mx-auto mt-6 max-w-sm space-y-2 text-left text-sm text-ink-300">
            {est.sugerencias.slice(0, 3).map((s) => <li key={s} className="flex gap-2"><Sparkles className="h-4 w-4 shrink-0 text-brand-300" />“{s}”</li>)}
          </ul>
          <Link to="/planes" className="mt-8 inline-block"><Button size="lg" icon={<Crown className="h-5 w-5" />}>Contratar Premium · $24.990</Button></Link>
        </div>
      </div>
    )
  }

  return (
    <div className="flex h-[calc(100vh-8.5rem)] flex-col overflow-hidden rounded-3xl border border-ink-200 bg-white shadow-card lg:h-[calc(100vh-10rem)]">
      <header className="flex flex-wrap items-center gap-3 border-b border-ink-100 px-4 py-3 sm:px-6">
        <div className="grid h-10 w-10 place-items-center rounded-xl bg-brand-gradient text-white shadow-glow"><Bot className="h-5 w-5" /></div>
        <div className="min-w-0 flex-1">
          <p className="flex items-center gap-2 font-semibold">LicitAsist <Badge tono="violet">{est.plan === 'ADMINISTRADOR' ? 'Admin' : 'Premium'}</Badge></p>
          <p className="truncate text-xs text-ink-500">IA con datos reales de LicitaWatch · {est.modelo} · modo {etiquetaEstado[est.rol]}</p>
        </div>
        {mensajes.length > 0 && <Button size="sm" variant="ghost" icon={<RotateCcw className="h-4 w-4" />} onClick={() => setMensajes([])}>Nueva conversación</Button>}
      </header>

      {contexto.data && (
        <div className="flex items-center gap-2 border-b border-ink-100 bg-brand-50/60 px-4 py-2 text-xs text-brand-800 sm:px-6">
          <Database className="h-3.5 w-3.5" /><span className="truncate">Contexto: licitación #{contexto.data.id} · {contexto.data.titulo}</span>
          <button onClick={() => setParams({})} className="ml-auto rounded p-0.5 hover:bg-brand-100" aria-label="Quitar contexto"><X className="h-3.5 w-3.5" /></button>
        </div>
      )}

      <div className="scrollbar-thin flex-1 space-y-5 overflow-y-auto bg-ink-50/50 px-4 py-6 sm:px-6" aria-live="polite">
        {mensajes.length === 0 && (
          <div className="mx-auto max-w-2xl py-6 text-center">
            <div className="mx-auto grid h-14 w-14 place-items-center rounded-2xl bg-brand-gradient text-white"><Sparkles className="h-6 w-6" /></div>
            <h2 className="mt-4 text-xl font-bold">¿En qué te ayudo hoy, {usuario?.nombre?.split(' ')[0]}?</h2>
            <p className="mt-1 text-sm text-ink-500">Consulto las licitaciones, postulaciones y datos de la plataforma para responderte.</p>
            <div className="mt-6 grid gap-2 text-left sm:grid-cols-2">
              {est.sugerencias.map((s) => (
                <button key={s} onClick={() => enviar(s)} className="rounded-xl border border-ink-200 bg-white px-4 py-3 text-sm text-ink-700 transition hover:border-brand-300 hover:shadow-glow">{s}</button>
              ))}
            </div>
          </div>
        )}
        {mensajes.map((m, i) => (
          <div key={i} className={cn('flex gap-3 animate-fade-in', m.rol === 'user' && 'flex-row-reverse')}>
            <div className={cn('grid h-8 w-8 shrink-0 place-items-center rounded-lg', m.rol === 'user' ? 'bg-ink-900 text-white' : m.error ? 'bg-red-100 text-red-600' : 'bg-brand-gradient text-white')}>
              {m.rol === 'user' ? <User className="h-4 w-4" /> : m.error ? <AlertTriangle className="h-4 w-4" /> : <Bot className="h-4 w-4" />}
            </div>
            <div className={cn('max-w-[85%] rounded-2xl px-4 py-3 text-sm sm:max-w-[75%]',
              m.rol === 'user' ? 'rounded-tr-sm bg-ink-900 text-white' : m.error ? 'rounded-tl-sm border border-red-200 bg-red-50 text-red-700' : 'rounded-tl-sm border border-ink-200 bg-white text-ink-700')}>
              {m.rol === 'assistant' && !m.error
                ? <div className="markdown"><ReactMarkdown remarkPlugins={[remarkGfm]}>{m.contenido}</ReactMarkdown></div>
                : <p className="whitespace-pre-wrap">{m.contenido}</p>}
              {m.fuentes && m.fuentes.length > 0 && (
                <div className="mt-3 flex flex-wrap gap-1.5 border-t border-ink-100 pt-2">
                  {m.fuentes.map((f) => <span key={f} className="inline-flex items-center gap-1 rounded-md bg-ink-50 px-2 py-0.5 text-[11px] text-ink-500"><Database className="h-3 w-3" />{f}</span>)}
                </div>
              )}
            </div>
          </div>
        ))}
        {pensando && (
          <div className="flex gap-3" role="status" aria-label="LicitAsist está pensando">
            <div className="grid h-8 w-8 place-items-center rounded-lg bg-brand-gradient text-white"><Bot className="h-4 w-4" /></div>
            <div className="flex items-center gap-1.5 rounded-2xl rounded-tl-sm border border-ink-200 bg-white px-4 py-3">
              {[0, 1, 2].map((d) => <span key={d} className="h-2 w-2 rounded-full bg-brand-500 animate-pulsedot" style={{ animationDelay: `${d * 0.15}s` }} />)}
              <span className="ml-2 text-xs text-ink-500">Consultando datos y generando respuesta…</span>
            </div>
          </div>
        )}
        <div ref={fin} />
      </div>

      <form onSubmit={(e) => enviar(texto, e)} className="border-t border-ink-100 bg-white p-3 sm:p-4">
        <div className="flex items-end gap-2 rounded-2xl border border-ink-200 bg-white p-2 focus-within:border-brand-500 focus-within:ring-4 focus-within:ring-brand-500/15">
          <label htmlFor="pregunta" className="sr-only">Escribe tu pregunta</label>
          <textarea id="pregunta" ref={inputRef} rows={1} value={texto} maxLength={2000} disabled={pensando}
            onChange={(e) => setTexto(e.target.value)}
            onKeyDown={(e) => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); enviar(texto) } }}
            placeholder="Pregúntale a LicitAsist… (Enter para enviar, Shift+Enter para nueva línea)"
            className="max-h-40 min-h-[40px] flex-1 resize-none bg-transparent px-2 py-2 text-sm outline-none placeholder:text-ink-400" />
          <Button type="submit" disabled={!texto.trim()} loading={pensando} aria-label="Enviar pregunta" icon={!pensando && <SendHorizonal className="h-4 w-4" />}>
            <span className="hidden sm:inline">Enviar</span>
          </Button>
        </div>
        <p className="mt-2 text-center text-[11px] text-ink-400">LicitAsist usa solo datos a los que tu perfil tiene acceso. Revisa la información antes de usarla.</p>
      </form>
    </div>
  )
}
