import { Link } from 'react-router-dom'
import { corDaArea, degrauDaTaxa } from '../../lib/cores'
import { formatarMinutos } from '../../lib/foco'
import {
  DIAS_SEMANA,
  type AreaConsistencia,
  type AtividadeConsistencia,
  type DiarioConsistencia,
  type DiaSemanaConsistencia,
  type NivelConsistencia,
  type TempoFoco,
} from '../../lib/types'
import { IconFlame } from '../icons'

/** Uma linha por galho da árvore: trilho = planejado, preenchimento = concluído, na cor da área. */
export function ConsistenciaPorArea({ areas }: { areas: AreaConsistencia[] }) {
  if (areas.length === 0) {
    return <p className="text-sm text-ink-faint">Nenhuma área com atividades planejadas no período.</p>
  }
  return (
    <ul className="space-y-4">
      {areas.map((area) => (
        <li key={area.areaId}>
          <div className="flex items-baseline justify-between gap-3 text-sm">
            <Link
              to={`/areas/${area.areaId}`}
              className="flex min-w-0 items-center gap-2 text-ink transition-colors hover:text-accent-strong"
            >
              <span className="h-2 w-2 shrink-0 rounded-full" style={{ backgroundColor: corDaArea(area.corTema) }} />
              <span className="truncate">{area.nome}</span>
            </Link>
            <span className="shrink-0 tabular-nums text-ink-muted">
              {area.taxa === null ? '—' : `${area.taxa}%`}
            </span>
          </div>
          <div className="mt-1.5 h-2 overflow-hidden rounded-full bg-surface-3">
            <div
              className="h-full rounded-full transition-[width] duration-500 ease-out"
              style={{ width: `${area.taxa ?? 0}%`, backgroundColor: corDaArea(area.corTema) }}
            />
          </div>
          <p className="mt-1 text-xs tabular-nums text-ink-faint">
            {area.concluido} de {area.planejado}
          </p>
        </li>
      ))}
    </ul>
  )
}

/** Taxa por dia da semana — revela o dia em que a rotina costuma quebrar. */
export function PadraoSemanal({ dias }: { dias: DiaSemanaConsistencia[] }) {
  const comPlano = dias.filter((dia) => dia.taxa !== null)
  const rotulo = (valor: string) => DIAS_SEMANA.find((dia) => dia.valor === valor)?.label ?? valor
  const melhor = comPlano.length > 1 ? comPlano.reduce((a, b) => ((b.taxa ?? 0) > (a.taxa ?? 0) ? b : a)) : null
  const piorCandidato = comPlano.length > 1 ? comPlano.reduce((a, b) => ((b.taxa ?? 0) < (a.taxa ?? 0) ? b : a)) : null
  // só aponta um dia fraco quando existe diferença real entre os dias
  const pior = melhor && piorCandidato && (piorCandidato.taxa ?? 0) < (melhor.taxa ?? 0) ? piorCandidato : null

  return (
    <div className="space-y-5">
      <div className="flex h-36 items-end gap-1.5 sm:gap-2" role="img" aria-label="Taxa de conclusão por dia da semana">
        {dias.map((dia) => (
          <div key={dia.dia} className="group flex h-full min-w-0 flex-1 flex-col items-center justify-end gap-2">
            <span className="text-[11px] tabular-nums text-ink-faint transition-colors group-hover:text-ink">
              {dia.taxa === null ? '' : `${dia.taxa}%`}
            </span>
            <div className="relative w-full max-w-9 flex-1 overflow-hidden rounded-[4px] bg-surface-3">
              <div
                className="absolute inset-x-0 bottom-0 rounded-[4px] transition-[height] duration-500 ease-out"
                style={{ height: `${dia.taxa ?? 0}%`, backgroundColor: degrauDaTaxa(dia.taxa === null ? null : Math.max(dia.taxa, 1)) }}
              />
            </div>
            <span className={`text-xs ${dia === pior ? 'text-warning' : 'text-ink-muted'}`}>{rotulo(dia.dia)}</span>
          </div>
        ))}
      </div>
      {melhor && pior && (
        <p className="text-sm text-ink-muted">
          Seu dia mais firme é <span className="text-ink">{rotulo(melhor.dia).toLowerCase()}</span> ({melhor.taxa}%).
          O que mais escapa é <span className="text-warning">{rotulo(pior.dia).toLowerCase()}</span> ({pior.taxa}%).
        </p>
      )}
    </div>
  )
}

/** Tabela de atividades por consistência, com sequência atual e recorde do período. */
export function TabelaAtividades({ atividades }: { atividades: AtividadeConsistencia[] }) {
  const comPlano = atividades.filter((atividade) => atividade.planejado > 0)
  if (comPlano.length === 0) {
    return <p className="text-sm text-ink-faint">Nenhuma atividade teve dias planejados no período.</p>
  }

  return (
    <div className="-mx-5 overflow-x-auto">
      <table className="w-full min-w-[38rem] text-sm">
        <thead>
          <tr className="border-b border-line text-left text-xs text-ink-faint">
            <th className="px-5 pb-2 font-normal">Atividade</th>
            <th className="w-40 pb-2 font-normal">Consistência</th>
            <th
              className="w-16 pb-2 text-right font-normal"
              title="Força do hábito hoje, de 0 a 100: cada vez cumprida puxa pra cima e cada falha pra baixo, com o passado pesando cada vez menos. Uma falha isolada custa pouco; várias seguidas derrubam."
            >
              Força
            </th>
            <th className="w-16 pb-2 text-right font-normal">Feitas</th>
            <th className="w-20 pb-2 text-right font-normal">Sequência</th>
            <th className="w-20 px-5 pb-2 text-right font-normal">Recorde</th>
          </tr>
        </thead>
        <tbody>
          {comPlano.map((atividade) => {
            const fraca = (atividade.taxa ?? 0) < 50
            return (
              <tr key={atividade.atividadeId} className="border-b border-line/60 last:border-0 hover:bg-surface-2/60">
                <td className="max-w-0 px-5 py-2.5">
                  <p className="truncate text-ink">{atividade.nome}</p>
                  <p className="mt-0.5 flex items-center gap-1.5 truncate text-xs text-ink-faint">
                    <span
                      className="h-1.5 w-1.5 shrink-0 rounded-full"
                      style={{ backgroundColor: corDaArea(atividade.areaCorTema) }}
                    />
                    {atividade.areaNome} · {atividade.objetivoDescricao}
                  </p>
                </td>
                <td className="py-2.5 pr-4">
                  <div className="flex items-center gap-2">
                    <div className="h-1.5 flex-1 overflow-hidden rounded-full bg-surface-3">
                      <div
                        className="h-full rounded-full"
                        style={{
                          width: `${atividade.taxa ?? 0}%`,
                          backgroundColor: fraca ? 'var(--color-warning)' : 'var(--color-accent)',
                        }}
                      />
                    </div>
                    <span className={`w-9 text-right text-xs tabular-nums ${fraca ? 'text-warning' : 'text-ink-muted'}`}>
                      {atividade.taxa}%
                    </span>
                  </div>
                </td>
                <td className="py-2.5 text-right tabular-nums">
                  {atividade.forca === null ? (
                    <span className="text-ink-faint">—</span>
                  ) : (
                    <span className={atividade.forca >= 50 ? 'text-ink' : 'text-ink-muted'}>{atividade.forca}</span>
                  )}
                </td>
                <td className="py-2.5 text-right tabular-nums text-ink-muted">
                  {atividade.concluido}
                  <span className="text-ink-faint">/{atividade.planejado}</span>
                </td>
                <td className="py-2.5 text-right">
                  {atividade.streakAtual > 0 ? (
                    <span className="inline-flex items-center gap-1 tabular-nums text-warning">
                      <IconFlame className="h-3.5 w-3.5" />
                      {atividade.streakAtual}
                    </span>
                  ) : (
                    <span className="text-ink-faint">—</span>
                  )}
                </td>
                <td className="px-5 py-2.5 text-right tabular-nums text-ink-muted">{atividade.maiorSequencia}</td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}

function taxaDosNiveis(niveis: NivelConsistencia[], de: number, ate: number): number | null {
  const faixa = niveis.filter((nivel) => nivel.nivel >= de && nivel.nivel <= ate)
  const planejado = faixa.reduce((soma, nivel) => soma + nivel.planejado, 0)
  const concluido = faixa.reduce((soma, nivel) => soma + nivel.concluido, 0)
  return planejado > 0 ? Math.round((concluido / planejado) * 100) : null
}

function BarrasPorNivel({
  titulo,
  niveis,
  alto,
  baixo,
}: {
  titulo: string
  niveis: NivelConsistencia[]
  /** O adjetivo concorda com o substantivo: humor alto, energia alta. */
  alto: string
  baixo: string
}) {
  const baixa = taxaDosNiveis(niveis, 1, 2)
  const alta = taxaDosNiveis(niveis, 4, 5)
  return (
    <div className="min-w-0 flex-1 space-y-3">
      <p className="text-xs text-ink-faint">{titulo}</p>
      <div
        className="flex h-28 items-end gap-1.5"
        role="img"
        aria-label={`Taxa de conclusão por nível de ${titulo.toLowerCase()}`}
      >
        {niveis.map((nivel) => (
          <div
            key={nivel.nivel}
            className="group flex h-full min-w-0 flex-1 flex-col items-center justify-end gap-1.5"
            title={`${nivel.dias} ${nivel.dias === 1 ? 'dia' : 'dias'} com ${titulo.toLowerCase()} ${nivel.nivel}`}
          >
            <span className="text-[11px] tabular-nums text-ink-faint transition-colors group-hover:text-ink">
              {nivel.dias === 0 ? '—' : nivel.taxa === null ? '' : `${nivel.taxa}%`}
            </span>
            <div
              className={`relative w-full max-w-8 flex-1 overflow-hidden rounded-[4px] ${
                nivel.dias === 0 ? 'bg-surface-2' : 'bg-surface-3'
              }`}
            >
              <div
                className="absolute inset-x-0 bottom-0 rounded-[4px] transition-[height] duration-500 ease-out"
                style={{
                  height: `${nivel.taxa ?? 0}%`,
                  backgroundColor: degrauDaTaxa(nivel.taxa === null ? null : Math.max(nivel.taxa, 1)),
                }}
              />
            </div>
            <span className="text-xs tabular-nums text-ink-muted">{nivel.nivel}</span>
          </div>
        ))}
      </div>
      {baixa !== null && alta !== null && baixa !== alta && (
        <p className="text-xs text-ink-muted">
          Com {titulo.toLowerCase()} {alto} você cumpre <span className="tabular-nums text-ink">{alta}%</span>; {baixo},{' '}
          <span className="tabular-nums text-ink">{baixa}%</span>.
        </p>
      )}
    </div>
  )
}

/** Humor e energia do diário contra a taxa de conclusão dos mesmos dias. */
export function DiarioXConsistencia({ dados }: { dados: DiarioConsistencia }) {
  if (dados.diasRegistrados === 0) {
    return (
      <p className="text-sm text-ink-faint">
        Registre humor e energia no diário da tela{' '}
        <Link to="/" className="text-accent-strong hover:underline">
          Hoje
        </Link>{' '}
        pra ver como eles se cruzam com a sua consistência.
      </p>
    )
  }
  return (
    <div className="flex flex-col gap-6 sm:flex-row">
      <BarrasPorNivel titulo="Humor" niveis={dados.porHumor} alto="alto" baixo="baixo" />
      <BarrasPorNivel titulo="Energia" niveis={dados.porEnergia} alto="alta" baixo="baixa" />
    </div>
  )
}

/** Tempo de foco do período, estilo Clockify: total, divisão por área e as atividades com mais tempo. */
export function TempoDeFoco({ tempo }: { tempo: TempoFoco }) {
  if (tempo.totalMinutos === 0) {
    return (
      <p className="text-sm text-ink-faint">
        Nenhum tempo de foco no período. Na tela{' '}
        <Link to="/" className="text-accent-strong hover:underline">
          Hoje
        </Link>
        , o botão ▶ ao lado de uma atividade começa a contar.
      </p>
    )
  }
  const maiorArea = Math.max(...tempo.porArea.map((area) => area.minutos), 1)
  return (
    <div className="grid gap-6 sm:grid-cols-[12rem_1fr]">
      <div>
        <p className="text-3xl font-semibold tabular-nums tracking-tight text-ink">
          {formatarMinutos(tempo.totalMinutos)}
        </p>
        <p className="mt-1 text-xs text-ink-faint">
          em {tempo.sessoes} {tempo.sessoes === 1 ? 'sessão' : 'sessões'}
        </p>
        <ul className="mt-4 space-y-3">
          {tempo.porArea.map((area) => (
            <li key={area.areaId}>
              <div className="flex items-baseline justify-between gap-2 text-xs">
                <span className="flex min-w-0 items-center gap-1.5 text-ink-muted">
                  <span
                    className="h-1.5 w-1.5 shrink-0 rounded-full"
                    style={{ backgroundColor: corDaArea(area.corTema) }}
                  />
                  <span className="truncate">{area.nome}</span>
                </span>
                <span className="shrink-0 tabular-nums text-ink-faint">{formatarMinutos(area.minutos)}</span>
              </div>
              <div className="mt-1 h-1.5 overflow-hidden rounded-full bg-surface-3">
                <div
                  className="h-full rounded-full"
                  style={{ width: `${(area.minutos / maiorArea) * 100}%`, backgroundColor: corDaArea(area.corTema) }}
                />
              </div>
            </li>
          ))}
        </ul>
      </div>
      <div className="min-w-0">
        <p className="mb-2 text-xs text-ink-faint">Atividades com mais tempo</p>
        <ul className="divide-y divide-line/60">
          {tempo.atividades.slice(0, 6).map((atividade) => (
            <li key={atividade.atividadeId} className="flex items-center gap-3 py-2 text-sm">
              <span
                className="h-1.5 w-1.5 shrink-0 rounded-full"
                style={{ backgroundColor: corDaArea(atividade.areaCorTema) }}
              />
              <span className="min-w-0 flex-1 truncate text-ink">{atividade.nome}</span>
              <span className="hidden shrink-0 text-xs text-ink-faint sm:inline">
                {atividade.sessoes} {atividade.sessoes === 1 ? 'sessão' : 'sessões'}
              </span>
              <span className="w-20 shrink-0 text-right tabular-nums text-ink-muted">
                {formatarMinutos(atividade.minutos)}
              </span>
            </li>
          ))}
        </ul>
      </div>
    </div>
  )
}
