import { corDaArea } from '../lib/cores'
import { formatarCronometro, useControleFoco, useCronometro } from '../lib/foco'
import { useFocoAtual } from '../lib/queries'
import { IconStop } from './icons'

/** A sessão de foco em andamento, visível em qualquer tela, com o tempo correndo e o botão de parar. */
export function BarraFoco({ compacta = false }: { compacta?: boolean }) {
  const { data: sessao } = useFocoAtual()
  const segundos = useCronometro(sessao?.inicio ?? null)
  const { parar } = useControleFoco()

  if (!sessao) return null

  return (
    <div
      className={`animate-rise flex items-center gap-2.5 rounded-lg border border-accent/30 bg-accent-soft text-sm ${
        compacta ? 'px-3 py-1.5' : 'px-3 py-2.5'
      }`}
      role="status"
      aria-label={`Em foco em ${sessao.atividadeNome}, ${formatarCronometro(segundos)}`}
    >
      <span className="relative flex h-2 w-2 shrink-0">
        <span
          className="absolute inline-flex h-full w-full animate-ping rounded-full opacity-60 motion-reduce:animate-none"
          style={{ backgroundColor: corDaArea(sessao.areaCorTema) }}
        />
        <span className="relative h-2 w-2 rounded-full" style={{ backgroundColor: corDaArea(sessao.areaCorTema) }} />
      </span>
      <span className="min-w-0 flex-1">
        {!compacta && <span className="block text-[11px] text-ink-faint">Em foco</span>}
        <span className="block truncate text-ink">{sessao.atividadeNome}</span>
      </span>
      <span className="shrink-0 font-medium tabular-nums text-accent-strong">{formatarCronometro(segundos)}</span>
      <button
        type="button"
        onClick={() => parar.mutate()}
        disabled={parar.isPending}
        aria-label={`Parar o foco em ${sessao.atividadeNome}`}
        title="Parar"
        className="shrink-0 rounded-md p-1 text-ink-muted transition-colors hover:bg-canvas/40 hover:text-ink disabled:opacity-50"
      >
        <IconStop className="h-4 w-4" />
      </button>
    </div>
  )
}
