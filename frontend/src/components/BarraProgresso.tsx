import type { Progresso } from '../lib/types'

export function BarraProgresso({
  progresso,
  corTema,
  compact = false,
}: {
  progresso: Progresso
  corTema?: string | null
  compact?: boolean
}) {
  if (progresso.percentual === null) {
    return <p className="text-xs text-ink-faint">Sem atividades agendadas</p>
  }

  return (
    <div className="space-y-1.5">
      <div className="h-1.5 w-full overflow-hidden rounded-full bg-surface-2">
        <div
          className="h-full rounded-full transition-[width] duration-500 ease-out"
          style={{ width: `${progresso.percentual}%`, backgroundColor: corTema ?? 'var(--color-accent)' }}
        />
      </div>
      {!compact && (
        <p className="text-xs text-ink-faint">
          <span className="tabular-nums text-ink-muted">{progresso.percentual}%</span> da semana ·{' '}
          <span className="tabular-nums">
            {progresso.concluido}/{progresso.planejado}
          </span>
        </p>
      )}
    </div>
  )
}
