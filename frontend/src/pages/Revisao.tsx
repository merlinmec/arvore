import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent, type ReactNode } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { GradeAtividade } from '../components/GradeAtividade'
import { IconCheck, IconChevronLeft, IconChevronRight, IconTrendDown, IconTrendUp } from '../components/icons'
import { api } from '../lib/api'
import { corDaArea } from '../lib/cores'
import { formatarCurto, formatarDiaSemana, hojeIso, inicioDaSemana, paraData, somarDias } from '../lib/datas'
import { useRevisao, useRevisoes } from '../lib/queries'
import type { AtividadeRevisao, RevisaoSemana } from '../lib/types'

/** No fim de semana revisa a semana em curso; nos outros dias, a que acabou. */
function semanaPadrao(hoje: string): string {
  const diaDaSemana = paraData(hoje).getDay()
  const segunda = inicioDaSemana(hoje)
  return diaDaSemana === 0 || diaDaSemana === 6 ? segunda : somarDias(segunda, -7)
}

function Passo({
  numero,
  titulo,
  detalhe,
  children,
}: {
  numero: number
  titulo: string
  detalhe?: string
  children: ReactNode
}) {
  return (
    <section className="rounded-xl border border-line bg-surface p-5">
      <div className="mb-4 flex items-start gap-3">
        <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-surface-3 text-xs font-semibold tabular-nums text-ink-muted">
          {numero}
        </span>
        <div>
          <h2 className="text-sm font-medium text-ink">{titulo}</h2>
          {detalhe && <p className="mt-0.5 text-xs text-ink-faint">{detalhe}</p>}
        </div>
      </div>
      {children}
    </section>
  )
}

function ComoFoi({ revisao }: { revisao: RevisaoSemana }) {
  const dias = Array.from({ length: 7 }, (_, indice) => somarDias(revisao.semana, indice))
  const diferenca =
    revisao.taxa !== null && revisao.taxaSemanaAnterior !== null ? revisao.taxa - revisao.taxaSemanaAnterior : null

  return (
    <div className="flex flex-wrap items-end gap-x-10 gap-y-6">
      <div>
        <p className="text-4xl font-semibold tabular-nums tracking-tight text-ink">
          {revisao.taxa === null ? '—' : `${revisao.taxa}%`}
        </p>
        <p className="mt-1 text-xs text-ink-faint">
          {revisao.planejado > 0 ? `${revisao.concluido} de ${revisao.planejado} cumpridas` : 'nada planejado'}
        </p>
        {diferenca !== null && (
          <p
            className={`mt-1 flex items-center gap-1 text-xs ${
              diferenca > 0 ? 'text-accent-strong' : diferenca < 0 ? 'text-warning' : 'text-ink-faint'
            }`}
          >
            {diferenca > 0 && <IconTrendUp className="h-3.5 w-3.5" />}
            {diferenca < 0 && <IconTrendDown className="h-3.5 w-3.5" />}
            <span className="tabular-nums">
              {diferenca === 0 ? 'igual à' : `${diferenca > 0 ? '+' : '−'}${Math.abs(diferenca)} pts vs.`}
            </span>
            semana anterior
          </p>
        )}
      </div>

      <div className="grid flex-1 grid-cols-7 gap-1.5" aria-label="Dias da semana">
        {dias.map((iso) => {
          const dia = revisao.dias.find((d) => d.data === iso)
          const futuro = iso > revisao.apuradoAte
          const planejado = dia?.planejado ?? 0
          const preenchimento = planejado > 0 ? (dia!.concluido / planejado) * 100 : 0
          const completo = planejado > 0 && dia!.concluido === planejado
          return (
            <div key={iso} className="flex flex-col items-center gap-1.5">
              <span className="text-[11px] capitalize text-ink-faint">{formatarDiaSemana(iso)}</span>
              <span
                className={`relative h-12 w-2.5 overflow-hidden rounded-full ${
                  futuro || planejado === 0
                    ? 'bg-transparent outline outline-1 -outline-offset-1 outline-line'
                    : 'bg-surface-3'
                } ${futuro ? 'opacity-50' : ''}`}
              >
                <span
                  className={`absolute inset-x-0 bottom-0 rounded-full ${completo ? 'bg-accent-strong' : 'bg-accent'}`}
                  style={{ height: `${preenchimento}%` }}
                />
              </span>
              <span className="h-3 text-[10px] tabular-nums text-ink-faint">
                {futuro ? '' : planejado > 0 ? `${dia!.concluido}/${planejado}` : '—'}
              </span>
            </div>
          )
        })}
      </div>
    </div>
  )
}

function OQueEscapou({ atividades }: { atividades: AtividadeRevisao[] }) {
  const escaparam = atividades.filter((item) => item.diasPerdidos.length > 0)
  const cumpridas = atividades.filter((item) => item.planejado > 0 && item.diasPerdidos.length === 0)

  return (
    <div className="space-y-4">
      {escaparam.length === 0 ? (
        <p className="flex items-center gap-2 text-sm text-ink-muted">
          <IconCheck className="h-4 w-4 text-accent-strong" />
          Nada escapou: tudo que estava no plano foi feito.
        </p>
      ) : (
        <ul className="divide-y divide-line overflow-hidden rounded-lg border border-line">
          {escaparam.map((item) => (
            <li key={item.atividade.id} className="flex flex-wrap items-center gap-x-3 gap-y-1.5 px-3.5 py-2.5">
              <span
                className="h-2 w-2 shrink-0 rounded-full"
                style={{ backgroundColor: corDaArea(item.areaCorTema) }}
              />
              <div className="min-w-0 flex-1">
                <p className="truncate text-sm text-ink">{item.atividade.nome}</p>
                <p className="truncate text-xs text-ink-faint">
                  {item.areaNome} · {item.objetivoDescricao}
                </p>
              </div>
              <div className="flex flex-wrap items-center gap-1">
                <span className="mr-1 text-xs text-ink-faint">{item.atividade.negativa ? 'recaída' : 'escapou'}</span>
                {item.diasPerdidos.map((dia) => (
                  <span
                    key={dia}
                    className={`rounded-[5px] px-1.5 py-0.5 text-[11px] font-medium capitalize ${
                      item.atividade.negativa ? 'bg-danger-soft text-danger' : 'bg-warning-soft text-warning'
                    }`}
                  >
                    {formatarDiaSemana(dia)}
                  </span>
                ))}
              </div>
            </li>
          ))}
        </ul>
      )}
      {cumpridas.length > 0 && (
        <p className="text-xs leading-relaxed text-ink-faint">
          <span className="text-ink-muted">Cumpridas: </span>
          {cumpridas.map((item) => item.atividade.nome).join(', ')}
        </p>
      )}
    </div>
  )
}

function Nota({ semana, nota }: { semana: string; nota: string | null }) {
  const queryClient = useQueryClient()
  const [texto, setTexto] = useState(nota ?? '')

  const salvar = useMutation({
    mutationFn: () => api.put(`/revisoes/${semana}`, { nota: texto }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['revisoes'] }),
  })

  function aoEnviar(evento: FormEvent) {
    evento.preventDefault()
    if (texto.trim()) salvar.mutate()
  }

  const salva = nota !== null && texto.trim() === nota

  return (
    <form onSubmit={aoEnviar} className="space-y-3">
      <textarea
        value={texto}
        onChange={(evento) => setTexto(evento.target.value)}
        rows={4}
        maxLength={4000}
        placeholder="O que funcionou, o que atrapalhou, o que muda na próxima semana."
        aria-label="Nota da revisão"
        className="w-full resize-y rounded-lg border border-line bg-canvas px-3 py-2.5 text-sm leading-relaxed text-ink placeholder:text-ink-faint focus:border-accent"
      />
      <div className="flex items-center justify-end gap-3">
        {salvar.isError && <p className="text-xs text-danger">Não foi possível salvar. Tente de novo.</p>}
        {salva && !salvar.isPending && (
          <p className="flex items-center gap-1 text-xs text-accent-strong">
            <IconCheck className="h-3.5 w-3.5" />
            Revisão salva
          </p>
        )}
        <button
          type="submit"
          disabled={!texto.trim() || salva || salvar.isPending}
          className="rounded-md bg-accent px-3 py-1.5 text-sm font-medium text-accent-ink transition-opacity hover:opacity-90 disabled:opacity-40"
        >
          {nota === null ? 'Salvar revisão' : 'Atualizar revisão'}
        </button>
      </div>
    </form>
  )
}

function ProximaSemana({ atividades }: { atividades: AtividadeRevisao[] }) {
  if (atividades.length === 0) {
    return (
      <p className="text-sm text-ink-faint">
        Nenhuma atividade em objetivos ativos. Crie uma em{' '}
        <Link to="/areas" className="text-ink-muted underline">
          Áreas
        </Link>
        .
      </p>
    )
  }
  return (
    <ul className="divide-y divide-line">
      {atividades.map((item) => (
        <li key={item.atividade.id} className="flex flex-wrap items-center gap-x-3 gap-y-2 py-2.5">
          <span className="h-2 w-2 shrink-0 rounded-full" style={{ backgroundColor: corDaArea(item.areaCorTema) }} />
          <p className="min-w-0 flex-1 truncate text-sm text-ink">
            {item.atividade.nome}
            <span className="ml-2 text-xs text-ink-faint">{item.objetivoDescricao}</span>
          </p>
          <div className="flex items-center gap-1.5">
            <GradeAtividade atividade={item.atividade} cor={corDaArea(item.areaCorTema)} />
          </div>
        </li>
      ))}
    </ul>
  )
}

function RevisoesAnteriores({ atual, aoEscolher }: { atual: string; aoEscolher: (semana: string) => void }) {
  const { data: revisoes } = useRevisoes()
  const anteriores = revisoes?.filter((revisao) => revisao.semana !== atual) ?? []
  if (anteriores.length === 0) return null

  return (
    <section>
      <h2 className="mb-2 px-1 text-xs font-medium text-ink-faint">Revisões anteriores</h2>
      <ul className="space-y-1">
        {anteriores.slice(0, 8).map((revisao) => (
          <li key={revisao.semana}>
            <button
              type="button"
              onClick={() => aoEscolher(revisao.semana)}
              className="w-full rounded-md px-2.5 py-2 text-left transition-colors hover:bg-surface"
            >
              <p className="text-xs tabular-nums text-ink-muted">
                {formatarCurto(revisao.semana)} – {formatarCurto(somarDias(revisao.semana, 6))}
              </p>
              <p className="mt-0.5 line-clamp-2 text-sm text-ink-faint">{revisao.nota}</p>
            </button>
          </li>
        ))}
      </ul>
    </section>
  )
}

export function Revisao() {
  const [parametros, setParametros] = useSearchParams()
  const hoje = hojeIso()
  const semana = inicioDaSemana(parametros.get('semana') ?? semanaPadrao(hoje))
  const semanaAtual = inicioDaSemana(hoje)
  const { data: revisao, isLoading, isError } = useRevisao(semana)

  function irPara(nova: string) {
    setParametros({ semana: nova }, { replace: true })
  }

  return (
    <div className="space-y-6">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-3xl font-semibold tracking-tight text-ink">Revisão da semana</h1>
          <p className="mt-1 text-sm text-ink-faint">
            {formatarCurto(semana)} – {formatarCurto(somarDias(semana, 6))}
            {revisao && (
              <span className={revisao.nota === null ? 'text-warning' : 'text-accent-strong'}>
                {' · '}
                {revisao.nota === null ? 'ainda não revisada' : 'revisada'}
              </span>
            )}
          </p>
        </div>
        <div className="flex items-center gap-1">
          <button
            type="button"
            onClick={() => irPara(somarDias(semana, -7))}
            aria-label="Semana anterior"
            className="rounded-md border border-line p-1.5 text-ink-muted transition-colors hover:border-line-strong hover:text-ink"
          >
            <IconChevronLeft className="h-4 w-4" />
          </button>
          <button
            type="button"
            onClick={() => irPara(somarDias(semana, 7))}
            disabled={semana >= semanaAtual}
            aria-label="Próxima semana"
            className="rounded-md border border-line p-1.5 text-ink-muted transition-colors hover:border-line-strong hover:text-ink disabled:opacity-40"
          >
            <IconChevronRight className="h-4 w-4" />
          </button>
        </div>
      </header>

      {isError && (
        <p className="text-sm text-danger">Não foi possível carregar a semana. Confira se o backend está rodando.</p>
      )}
      {isLoading && <div className="h-40 animate-pulse rounded-xl bg-surface" />}

      {revisao && (
        <div className="grid gap-6 lg:grid-cols-[1fr_16rem]">
          <div className="space-y-4">
            <Passo
              numero={1}
              titulo="Como foi"
              detalhe={revisao.apuradoAte < revisao.fim ? 'Semana em curso: conta só até hoje.' : undefined}
            >
              <ComoFoi revisao={revisao} />
            </Passo>
            <Passo numero={2} titulo="O que escapou" detalhe="Dias em que algo planejado não foi feito.">
              <OQueEscapou atividades={revisao.atividades} />
            </Passo>
            <Passo numero={3} titulo="O que você tira dessa semana?">
              <Nota key={`${semana}-${revisao.nota ?? ''}`} semana={semana} nota={revisao.nota} />
            </Passo>
            <Passo
              numero={4}
              titulo="Ajuste a próxima semana"
              detalhe="Tire o que pesou, mude de dia o que escapou. Vale a partir de hoje e não mexe no histórico."
            >
              <ProximaSemana atividades={revisao.atividades.filter((item) => !item.atividade.negativa)} />
            </Passo>
          </div>
          <aside>
            <RevisoesAnteriores atual={semana} aoEscolher={irPara} />
          </aside>
        </div>
      )}
    </div>
  )
}
