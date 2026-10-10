import { useCallback, useEffect, useRef, useState, type ReactNode } from 'react'
import { DesfazerContext, type Exclusao } from './desfazer-context'

const PRAZO_MS = 6000

/**
 * Exclusões com desfazer, no lugar do window.confirm: o item some na hora, um aviso oferece
 * "Desfazer" por alguns segundos e só então a exclusão vai pra API. Ctrl+Z desfaz a mais recente.
 *
 * Se a aba fechar antes do prazo, as pendentes são disparadas na hora; se a requisição não chegar a
 * sair, o item simplesmente continua existindo, que é o lado seguro de errar.
 */
export function DesfazerProvider({ children }: { children: ReactNode }) {
  // avisos visíveis, do mais antigo pro mais recente
  const [avisos, setAvisos] = useState<Exclusao[]>([])
  // escondidos até a API confirmar e as listas recarregarem, pra o item não piscar de volta
  const [escondidos, setEscondidos] = useState<ReadonlySet<string>>(new Set())
  const [falha, setFalha] = useState<string | null>(null)
  const timers = useRef(new Map<string, { timer: number; exclusao: Exclusao }>())

  const mostrar = useCallback((id: string, visivel: boolean) => {
    setEscondidos((atual) => {
      const proximo = new Set(atual)
      if (visivel) proximo.delete(id)
      else proximo.add(id)
      return proximo
    })
  }, [])

  const executar = useCallback(
    async (exclusao: Exclusao) => {
      timers.current.delete(exclusao.id)
      setAvisos((atual) => atual.filter((aviso) => aviso.id !== exclusao.id))
      try {
        await exclusao.executar()
      } catch {
        setFalha('Não foi possível excluir. O item voltou pra lista.')
      } finally {
        mostrar(exclusao.id, true)
      }
    },
    [mostrar],
  )

  const excluir = useCallback(
    (exclusao: Exclusao) => {
      if (timers.current.has(exclusao.id)) return
      setFalha(null)
      mostrar(exclusao.id, false)
      setAvisos((atual) => [...atual, exclusao])
      const timer = window.setTimeout(() => executar(exclusao), PRAZO_MS)
      timers.current.set(exclusao.id, { timer, exclusao })
    },
    [executar, mostrar],
  )

  const desfazer = useCallback(
    (id: string) => {
      const pendente = timers.current.get(id)
      if (!pendente) return
      window.clearTimeout(pendente.timer)
      timers.current.delete(id)
      setAvisos((atual) => atual.filter((aviso) => aviso.id !== id))
      mostrar(id, true)
    },
    [mostrar],
  )

  const pendente = useCallback((id: string) => escondidos.has(id), [escondidos])

  useEffect(() => {
    function aoTeclar(evento: KeyboardEvent) {
      if (!(evento.ctrlKey || evento.metaKey) || evento.key.toLowerCase() !== 'z' || evento.shiftKey) return
      const alvo = evento.target as HTMLElement | null
      // dentro de um campo, Ctrl+Z é o desfazer do próprio texto
      if (alvo?.closest('input, textarea, select, [contenteditable="true"]')) return
      const ultima = [...timers.current.keys()].pop()
      if (!ultima) return
      evento.preventDefault()
      desfazer(ultima)
    }
    function aoSair() {
      for (const { timer, exclusao } of [...timers.current.values()]) {
        window.clearTimeout(timer)
        void executar(exclusao)
      }
    }
    window.addEventListener('keydown', aoTeclar)
    window.addEventListener('pagehide', aoSair)
    return () => {
      window.removeEventListener('keydown', aoTeclar)
      window.removeEventListener('pagehide', aoSair)
    }
  }, [desfazer, executar])

  useEffect(() => {
    if (!falha) return
    const timer = window.setTimeout(() => setFalha(null), PRAZO_MS)
    return () => window.clearTimeout(timer)
  }, [falha])

  return (
    <DesfazerContext.Provider value={{ excluir, pendente }}>
      {children}
      <div
        className="pointer-events-none fixed inset-x-0 bottom-4 z-50 flex flex-col items-center gap-2 px-4"
        aria-live="polite"
      >
        {avisos.map((aviso) => (
          <div
            key={aviso.id}
            className="animate-rise pointer-events-auto relative flex w-full max-w-sm items-center gap-3 overflow-hidden rounded-lg border border-line-strong bg-surface-2 py-2.5 pl-4 pr-2 text-sm shadow-lg shadow-black/40"
          >
            <span className="min-w-0 flex-1 truncate text-ink">{aviso.mensagem}</span>
            <button
              type="button"
              onClick={() => desfazer(aviso.id)}
              className="shrink-0 rounded-md px-2.5 py-1 font-medium text-accent-strong transition-colors hover:bg-accent-soft"
            >
              Desfazer
            </button>
            <span
              aria-hidden
              className="prazo-desfazer absolute inset-x-0 bottom-0 h-0.5 bg-accent/60"
              style={{ animationDuration: `${PRAZO_MS}ms` }}
            />
          </div>
        ))}
        {falha && (
          <div
            role="alert"
            className="animate-rise pointer-events-auto w-full max-w-sm rounded-lg border border-danger/40 bg-danger-soft px-4 py-2.5 text-sm text-danger"
          >
            {falha}
          </div>
        )}
      </div>
    </DesfazerContext.Provider>
  )
}
