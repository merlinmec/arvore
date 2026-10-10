import { useEffect, useRef } from 'react'

/**
 * Confirmação inline pra exclusões que não dá pra desfazer (ex.: área com tudo dentro). Abre com o
 * foco em "Cancelar", o lado seguro, e Esc também cancela.
 */
export function ConfirmarExclusao({
  pergunta,
  ocupado,
  aoConfirmar,
  aoCancelar,
}: {
  pergunta: string
  ocupado: boolean
  aoConfirmar: () => void
  aoCancelar: () => void
}) {
  const cancelar = useRef<HTMLButtonElement>(null)

  useEffect(() => {
    cancelar.current?.focus()
  }, [])

  return (
    <div
      role="alertdialog"
      aria-label={pergunta}
      onKeyDown={(evento) => {
        if (evento.key === 'Escape') aoCancelar()
      }}
      className="animate-rise flex max-w-sm flex-wrap items-center gap-2 rounded-lg border border-danger/40 bg-canvas/90 px-3 py-2 text-sm backdrop-blur-sm"
    >
      <span className="min-w-0 flex-1 text-ink">{pergunta}</span>
      <div className="flex gap-1.5">
        <button
          ref={cancelar}
          type="button"
          onClick={aoCancelar}
          className="rounded-md px-2.5 py-1 text-ink-muted transition-colors hover:bg-surface-2 hover:text-ink"
        >
          Cancelar
        </button>
        <button
          type="button"
          onClick={aoConfirmar}
          disabled={ocupado}
          className="rounded-md bg-danger px-2.5 py-1 font-medium text-canvas transition-colors hover:bg-danger-strong disabled:opacity-50"
        >
          {ocupado ? 'Excluindo…' : 'Excluir'}
        </button>
      </div>
    </div>
  )
}
