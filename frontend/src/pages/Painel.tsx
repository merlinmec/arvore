import type { ReactNode } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { Heatmap } from '../components/graficos/Heatmap'
import {
  ConsistenciaPorArea,
  DiarioXConsistencia,
  PadraoSemanal,
  TabelaAtividades,
} from '../components/graficos/PainelBlocos'
import { SerieDiaria } from '../components/graficos/SerieDiaria'
import { IconChart, IconFlame, IconTrendDown, IconTrendUp } from '../components/icons'
import { achatarArvore } from '../lib/arvore'
import { formatarCurto } from '../lib/datas'
import { useAreas, useDiarioConsistencia, useHistorico, useResumo } from '../lib/queries'

const PERIODOS = [
  { dias: 7, rotulo: '7 dias' },
  { dias: 30, rotulo: '30 dias' },
  { dias: 90, rotulo: '90 dias' },
  { dias: 365, rotulo: '1 ano' },
]

function Bloco({ titulo, detalhe, children, className = '' }: { titulo: string; detalhe?: string; children: ReactNode; className?: string }) {
  return (
    <section className={`rounded-xl border border-line bg-surface p-5 ${className}`}>
      <div className="mb-5 flex items-baseline justify-between gap-3">
        <h2 className="text-sm font-medium text-ink">{titulo}</h2>
        {detalhe && <p className="text-xs text-ink-faint">{detalhe}</p>}
      </div>
      {children}
    </section>
  )
}

function Variacao({ atual, anterior, dias }: { atual: number | null; anterior: number | null; dias: number }) {
  if (atual === null || anterior === null) {
    return <p className="mt-1 text-xs text-ink-faint">sem período anterior pra comparar</p>
  }
  const diferenca = atual - anterior
  if (diferenca === 0) {
    return <p className="mt-1 text-xs text-ink-faint">igual aos {dias} dias anteriores</p>
  }
  const subiu = diferenca > 0
  const Icone = subiu ? IconTrendUp : IconTrendDown
  return (
    <p className={`mt-1 flex items-center gap-1 text-xs ${subiu ? 'text-accent-strong' : 'text-warning'}`}>
      <Icone className="h-3.5 w-3.5" />
      <span className="tabular-nums">
        {subiu ? '+' : '−'}
        {Math.abs(diferenca)} pts
      </span>
      <span className="text-ink-faint">vs. {dias} dias anteriores</span>
    </p>
  )
}

export function Painel() {
  const [parametros, setParametros] = useSearchParams()
  const dias = Number(parametros.get('dias')) || 30
  const areaId = parametros.get('area')

  const { data: areas } = useAreas()
  const { data: resumo, isLoading, isError } = useResumo(dias, areaId)
  const { data: historico } = useHistorico({ areaId })
  const { data: diario } = useDiarioConsistencia(dias, areaId)

  function atualizar(chave: string, valor: string | null) {
    const proximos = new URLSearchParams(parametros)
    if (valor === null) proximos.delete(chave)
    else proximos.set(chave, valor)
    setParametros(proximos, { replace: true })
  }

  const liderSequencia = resumo?.atividades.reduce<(typeof resumo.atividades)[number] | null>(
    (lider, atividade) => (atividade.streakAtual > (lider?.streakAtual ?? 0) ? atividade : lider),
    null,
  )
  const vazio = resumo && resumo.planejado === 0 && resumo.atividades.length === 0

  return (
    <div className="space-y-6">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-3xl font-semibold tracking-tight text-ink">Painel</h1>
          <p className="mt-1 text-sm text-ink-faint">
            {resumo ? `${formatarCurto(resumo.inicio)} – ${formatarCurto(resumo.fim)}` : 'Consistência do que você planejou'}
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <select
            value={areaId ?? ''}
            onChange={(evento) => atualizar('area', evento.target.value || null)}
            aria-label="Filtrar por área"
            className="h-8 rounded-md border border-line bg-surface px-2.5 text-sm text-ink focus:border-accent"
          >
            <option value="">Todas as áreas</option>
            {achatarArvore(areas ?? []).map((area) => (
              <option key={area.id} value={area.id}>
                {'  '.repeat(area.profundidade)}
                {area.profundidade > 0 ? '└ ' : ''}
                {area.nome}
              </option>
            ))}
          </select>
          <div className="flex rounded-md border border-line bg-surface p-0.5" role="group" aria-label="Período">
            {PERIODOS.map((periodo) => (
              <button
                key={periodo.dias}
                type="button"
                aria-pressed={dias === periodo.dias}
                onClick={() => atualizar('dias', String(periodo.dias))}
                className={`rounded-[5px] px-2.5 py-1 text-sm transition-colors duration-150 ${
                  dias === periodo.dias ? 'bg-surface-3 text-ink' : 'text-ink-faint hover:text-ink-muted'
                }`}
              >
                {periodo.rotulo}
              </button>
            ))}
          </div>
        </div>
      </header>

      {isError && <p className="text-sm text-danger">Não foi possível carregar o painel. Confira se o backend está rodando.</p>}

      {isLoading && (
        <div className="space-y-4">
          <div className="h-24 animate-pulse rounded-xl bg-surface" />
          <div className="grid gap-4 lg:grid-cols-3">
            <div className="h-72 animate-pulse rounded-xl bg-surface lg:col-span-2" />
            <div className="h-72 animate-pulse rounded-xl bg-surface" />
          </div>
        </div>
      )}

      {vazio && (
        <div className="flex flex-col items-center gap-3 rounded-xl border border-dashed border-line px-6 py-16 text-center">
          <IconChart className="h-8 w-8 text-ink-faint" />
          <p className="max-w-sm text-sm text-ink-muted">
            O painel mede o que você cumpriu contra o que planejou. Crie atividades numa área, encaixe na grade
            semanal e marque em <Link to="/" className="text-accent-strong hover:underline">Hoje</Link>.
          </p>
          <Link to="/areas" className="text-sm text-ink hover:text-accent-strong">
            Ir para Áreas →
          </Link>
        </div>
      )}

      {resumo && !vazio && (
        <>
          <dl className="grid grid-cols-2 overflow-hidden rounded-xl border border-line bg-surface lg:grid-cols-4">
            <div className="border-b border-r border-line p-5 lg:border-b-0">
              <dt className="text-xs text-ink-faint">Taxa de conclusão</dt>
              <dd className="mt-1.5 text-3xl font-semibold tabular-nums tracking-tight text-ink">
                {resumo.taxa === null ? '—' : `${resumo.taxa}%`}
              </dd>
              <Variacao atual={resumo.taxa} anterior={resumo.taxaPeriodoAnterior} dias={dias} />
            </div>
            <div className="border-b border-line p-5 lg:border-b-0 lg:border-r">
              <dt className="text-xs text-ink-faint">Concluídas</dt>
              <dd className="mt-1.5 text-3xl font-semibold tabular-nums tracking-tight text-ink">
                {resumo.concluido}
                <span className="text-lg font-normal text-ink-faint"> / {resumo.planejado}</span>
              </dd>
              <p className="mt-1 text-xs text-ink-faint">do que estava na grade</p>
            </div>
            <div className="border-r border-line p-5">
              <dt className="text-xs text-ink-faint">Dias completos</dt>
              <dd className="mt-1.5 text-3xl font-semibold tabular-nums tracking-tight text-ink">
                {resumo.diasPerfeitos}
                <span className="text-lg font-normal text-ink-faint"> / {resumo.diasComPlano}</span>
              </dd>
              <p className="mt-1 text-xs text-ink-faint">dias com tudo feito</p>
            </div>
            <div className="p-5">
              <dt className="text-xs text-ink-faint">Maior sequência ativa</dt>
              <dd className="mt-1.5 flex items-center gap-1.5 text-3xl font-semibold tabular-nums tracking-tight text-ink">
                {liderSequencia ? (
                  <>
                    <IconFlame className="h-6 w-6 text-warning" />
                    {liderSequencia.streakAtual}
                  </>
                ) : (
                  '—'
                )}
              </dd>
              <p className="mt-1 truncate text-xs text-ink-faint">
                {liderSequencia ? liderSequencia.nome : 'nenhuma sequência em andamento'}
              </p>
            </div>
          </dl>

          <div className="grid gap-4 lg:grid-cols-3">
            <Bloco titulo="Dia a dia" detalhe="concluído por área sobre o planejado" className="lg:col-span-2">
              <SerieDiaria serie={resumo.serie} areas={resumo.porArea} hoje={resumo.fim} />
            </Bloco>
            <Bloco titulo={areaId ? 'Por subárea' : 'Por área'}>
              <ConsistenciaPorArea areas={resumo.porArea} />
            </Bloco>
          </div>

          <Bloco titulo="Último ano" detalhe="cada quadrado é um dia">
            {historico ? <Heatmap dias={historico} hoje={resumo.fim} /> : <div className="h-32 animate-pulse rounded-md bg-surface-2" />}
          </Bloco>

          <div className="grid gap-4 lg:grid-cols-3">
            <Bloco titulo="Padrão da semana">
              <PadraoSemanal dias={resumo.porDiaSemana} />
            </Bloco>
            <Bloco titulo="Atividades" detalhe="da mais para a menos consistente" className="lg:col-span-2">
              <TabelaAtividades atividades={resumo.atividades} />
            </Bloco>
          </div>

          {diario && (
            <Bloco titulo="Diário e consistência" detalhe={`${diario.diasRegistrados} dias registrados no período`}>
              <DiarioXConsistencia dados={diario} />
            </Bloco>
          )}
        </>
      )}
    </div>
  )
}
