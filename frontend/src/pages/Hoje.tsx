import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useEffect, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { DiarioDoDia } from '../components/DiarioDoDia'
import {
  IconCheck,
  IconChevronLeft,
  IconChevronRight,
  IconFlame,
  IconInbox,
  IconPlay,
  IconStop,
  IconX,
} from '../components/icons'
import { api } from '../lib/api'
import { corDaArea } from '../lib/cores'
import { invalidarDerivados, useAlternarConclusao } from '../lib/conclusao'
import { formatarCronometro, useControleFoco, useCronometro } from '../lib/foco'
import { lerNumero } from '../lib/numeros'
import {
  formatarDiaSemana,
  formatarDiaSemanaLongo,
  formatarLongo,
  hojeIso,
  inicioDaSemana,
  paraData,
  rotuloRelativo,
  somarDias,
} from '../lib/datas'
import { useDia, useFocoAtual, useHistorico, useRevisao } from '../lib/queries'
import type { DiaHoje, ItemHoje } from '../lib/types'

/** Se o item está em dia: feito, ou, num hábito negativo, sem recaída. */
function cumprido(item: ItemHoje) {
  return item.negativa ? !item.concluido : item.concluido
}

interface GrupoArea {
  id: string
  nome: string
  cor: string | null
  itens: ItemHoje[]
}

/**
 * A semana em volta do dia escolhido: cada coluna enche de baixo pra cima conforme o dia foi
 * cumprido. Serve de navegação e de resumo ao mesmo tempo.
 */
function TiraDaSemana({ data, hoje, aoEscolher }: { data: string; hoje: string; aoEscolher: (iso: string) => void }) {
  const segunda = inicioDaSemana(data)
  const { data: historico } = useHistorico({ inicio: segunda, fim: somarDias(segunda, 6) })
  const dias = Array.from({ length: 7 }, (_, indice) => somarDias(segunda, indice))

  return (
    <div className="grid grid-cols-7 gap-1.5 sm:gap-2" role="tablist" aria-label="Dias da semana">
      {dias.map((iso) => {
        const resumo = historico?.find((dia) => dia.data === iso)
        const planejado = resumo?.planejado ?? 0
        const preenchimento = planejado > 0 ? (resumo!.concluido / planejado) * 100 : 0
        const selecionado = iso === data
        const futuro = iso > hoje
        const completo = planejado > 0 && resumo!.concluido === planejado && !futuro
        return (
          <button
            key={iso}
            type="button"
            role="tab"
            aria-selected={selecionado}
            aria-label={`${formatarLongo(iso)}${planejado ? `, ${resumo!.concluido} de ${planejado} concluídas` : ', nada planejado'}`}
            onClick={() => aoEscolher(iso)}
            className={`group flex flex-col items-center gap-2 rounded-lg border px-1 pb-2 pt-2.5 transition-colors duration-150 ${
              selecionado ? 'border-line-strong bg-surface-2' : 'border-transparent hover:bg-surface'
            }`}
          >
            <span className={`text-[11px] capitalize ${selecionado ? 'text-ink-muted' : 'text-ink-faint'}`}>
              {formatarDiaSemana(iso)}
            </span>
            <span
              className={`text-base font-semibold tabular-nums ${
                iso === hoje ? 'text-accent-strong' : selecionado ? 'text-ink' : 'text-ink-muted'
              }`}
            >
              {paraData(iso).getDate()}
            </span>
            <span
              className={`relative h-10 w-2.5 overflow-hidden rounded-full ${
                planejado === 0 ? 'bg-transparent outline outline-1 -outline-offset-1 outline-line' : 'bg-surface-3'
              } ${futuro && planejado > 0 ? 'opacity-60' : ''}`}
            >
              <span
                className={`absolute inset-x-0 bottom-0 rounded-full transition-[height] duration-500 ease-out ${
                  completo ? 'bg-accent-strong' : 'bg-accent'
                }`}
                style={{ height: `${preenchimento}%` }}
              />
            </span>
            <span className="h-3 text-[10px] tabular-nums text-ink-faint">
              {planejado > 0 ? `${resumo!.concluido}/${planejado}` : ''}
            </span>
          </button>
        )
      })}
    </div>
  )
}

/**
 * Liga o temporizador de foco na atividade. Na que está contando, vira o cronômetro com o botão de
 * parar; nas outras, aparece no hover (sempre no celular, que não tem hover).
 */
function BotaoFoco({ item }: { item: ItemHoje }) {
  const { data: sessao } = useFocoAtual()
  const { iniciar, parar } = useControleFoco()
  const emFoco = sessao?.atividadeId === item.atividadeId
  const segundos = useCronometro(emFoco ? sessao.inicio : null)

  if (emFoco) {
    return (
      <button
        type="button"
        onClick={() => parar.mutate()}
        aria-label={`Parar o foco em ${item.atividadeNome}`}
        className="flex shrink-0 items-center gap-1.5 rounded-full bg-accent-soft px-2 py-0.5 text-xs font-medium tabular-nums text-accent-strong transition-colors hover:bg-accent/20"
      >
        <IconStop className="h-3.5 w-3.5" />
        {formatarCronometro(segundos)}
      </button>
    )
  }
  return (
    <button
      type="button"
      onClick={() => iniciar.mutate(item.atividadeId)}
      disabled={iniciar.isPending}
      aria-label={`Iniciar foco em ${item.atividadeNome}`}
      title={sessao ? `Trocar o foco pra ${item.atividadeNome}` : 'Iniciar foco'}
      className="shrink-0 rounded-full p-1 text-ink-faint transition-[opacity,color] hover:text-accent-strong focus-visible:opacity-100 group-hover:opacity-100 sm:opacity-0"
    >
      <IconPlay className="h-3.5 w-3.5" />
    </button>
  )
}

function ItemAtividade({
  item,
  editavel,
  podeFocar,
  aoAlternar,
  aoRegistrarValor,
}: {
  item: ItemHoje
  editavel: boolean
  podeFocar: boolean
  aoAlternar: () => void
  aoRegistrarValor: (valor: number) => void
}) {
  if (item.negativa) return <ItemHabitoNegativo item={item} editavel={editavel} aoAlternar={aoAlternar} />
  return (
    <li
      className={`animate-rise group flex items-center gap-3.5 px-4 py-3 transition-colors duration-150 ${
        editavel ? 'hover:bg-surface-2/70' : ''
      }`}
    >
      <button
        type="button"
        onClick={aoAlternar}
        disabled={!editavel}
        aria-pressed={item.concluido}
        aria-label={`${item.concluido ? 'Desmarcar' : 'Concluir'} ${item.atividadeNome}`}
        className={`flex h-6 w-6 shrink-0 items-center justify-center rounded-full border-[1.5px] transition-[background-color,border-color,transform] duration-150 active:scale-90 disabled:cursor-not-allowed ${
          item.concluido
            ? 'border-accent bg-accent text-accent-ink'
            : editavel
              ? 'border-line-strong text-transparent hover:border-accent'
              : 'border-dashed border-line-strong text-transparent'
        }`}
      >
        {item.concluido && <IconCheck className="check-draw h-3.5 w-3.5" strokeWidth={2.5} />}
      </button>
      <div className="min-w-0 flex-1">
        <p
          className={`truncate transition-colors duration-200 ${
            item.concluido ? 'text-ink-faint line-through decoration-line-strong' : 'text-ink'
          }`}
        >
          {item.atividadeNome}
        </p>
        <p className="mt-0.5 truncate text-xs text-ink-faint">
          {item.objetivoDescricao}
          {item.metaSemanal !== null && (
            <span className="tabular-nums">
              {' · '}
              <span className={item.feitasNaSemana! >= item.metaSemanal ? 'text-accent-strong' : 'text-ink-muted'}>
                {item.feitasNaSemana} de {item.metaSemanal}
              </span>{' '}
              na semana
            </span>
          )}
        </p>
      </div>
      {item.metaUnidade !== null && (
        <CampoValor
          // remonta quando o valor salvo muda, pra o campo refletir o que está no servidor
          key={item.valor ?? 'vazio'}
          item={item}
          editavel={editavel}
          aoRegistrar={aoRegistrarValor}
        />
      )}
      {podeFocar && <BotaoFoco item={item} />}
      {item.streakAtual > 0 && (
        <span
          className="flex shrink-0 items-center gap-1 rounded-full bg-warning-soft px-2 py-0.5 text-xs font-medium tabular-nums text-warning"
          title={`${item.streakAtual} ${item.streakAtual === 1 ? 'vez seguida' : 'vezes seguidas'} ${
            item.metaSemanal === null ? 'no dia agendado' : 'sem perder a meta da semana'
          }`}
        >
          <IconFlame className="h-3.5 w-3.5" />
          {item.streakAtual}
        </span>
      )}
    </li>
  )
}

/**
 * Hábito negativo: o dia já começa cumprido, e a ação é registrar a recaída. Por isso o círculo é só
 * estado, e o botão fica à direita, com o verbo explícito.
 */
function ItemHabitoNegativo({
  item,
  editavel,
  aoAlternar,
}: {
  item: ItemHoje
  editavel: boolean
  aoAlternar: () => void
}) {
  const recaiu = item.concluido
  return (
    <li className="animate-rise flex items-center gap-3.5 px-4 py-3">
      <span
        aria-hidden
        className={`flex h-6 w-6 shrink-0 items-center justify-center rounded-full border-[1.5px] ${
          recaiu
            ? 'border-danger bg-danger-soft text-danger'
            : editavel
              ? 'border-accent bg-accent-soft text-accent-strong'
              : 'border-dashed border-line-strong text-transparent'
        }`}
      >
        {recaiu ? (
          <IconX className="h-3.5 w-3.5" strokeWidth={2.5} />
        ) : (
          editavel && <IconCheck className="h-3.5 w-3.5" strokeWidth={2.5} />
        )}
      </span>
      <div className="min-w-0 flex-1">
        <p className="truncate text-ink">{item.atividadeNome}</p>
        <p className="mt-0.5 truncate text-xs text-ink-faint">
          {item.objetivoDescricao}
          {' · '}
          {recaiu ? <span className="text-danger">recaída registrada</span> : 'hábito negativo'}
        </p>
      </div>
      {editavel && (
        <button
          type="button"
          onClick={aoAlternar}
          aria-pressed={recaiu}
          aria-label={`${recaiu ? 'Desfazer recaída em' : 'Registrar recaída em'} ${item.atividadeNome}`}
          className={`shrink-0 rounded-md border px-2 py-1 text-xs transition-colors duration-150 active:scale-95 ${
            recaiu
              ? 'border-line text-ink-muted hover:border-line-strong hover:text-ink'
              : 'border-line text-ink-faint hover:border-danger hover:text-danger'
          }`}
        >
          {recaiu ? 'Desfazer' : 'Recaí'}
        </button>
      )}
      {item.streakAtual > 0 && (
        <span
          className="flex shrink-0 items-center gap-1 rounded-full bg-warning-soft px-2 py-0.5 text-xs font-medium tabular-nums text-warning"
          title={`${item.streakAtual} ${item.streakAtual === 1 ? 'dia' : 'dias seguidos'} sem recaída`}
        >
          <IconFlame className="h-3.5 w-3.5" />
          {item.streakAtual}
        </span>
      )}
    </li>
  )
}

function CampoValor({
  item,
  editavel,
  aoRegistrar,
}: {
  item: ItemHoje
  editavel: boolean
  aoRegistrar: (valor: number) => void
}) {
  const [texto, setTexto] = useState(item.valor === null ? '' : String(item.valor).replace('.', ','))

  function confirmar() {
    const valor = lerNumero(texto)
    if (valor !== null && valor !== item.valor) aoRegistrar(valor)
  }

  return (
    <label className="flex shrink-0 items-center gap-1 text-xs text-ink-faint">
      <input
        inputMode="decimal"
        value={texto}
        disabled={!editavel}
        onChange={(evento) => setTexto(evento.target.value)}
        onBlur={confirmar}
        onKeyDown={(evento) => {
          if (evento.key === 'Enter') evento.currentTarget.blur()
        }}
        placeholder="0"
        aria-label={`Quanto de ${item.atividadeNome}, em ${item.metaUnidade}`}
        className="w-14 rounded-md border border-line bg-canvas px-1.5 py-1 text-right text-sm tabular-nums text-ink placeholder:text-ink-faint focus:border-accent disabled:opacity-50"
      />
      {item.metaUnidade}
    </label>
  )
}

export function Hoje() {
  const queryClient = useQueryClient()
  const [parametros, setParametros] = useSearchParams()
  const [areaSelecionada, setAreaSelecionada] = useState<string | null>(null)

  const hojeLocal = hojeIso()
  const data = parametros.get('data') ?? hojeLocal
  const { data: dia, isLoading, isError } = useDia(data)
  const hoje = dia?.hoje ?? hojeLocal
  // domingo é dia de revisão: convida enquanto a semana não foi revisada
  const domingo = data === hoje && paraData(hoje).getDay() === 0
  const { data: revisao } = useRevisao(hoje, domingo)

  function irPara(iso: string) {
    setParametros(iso === hoje ? {} : { data: iso }, { replace: true })
  }

  // ← e → trocam de dia, H volta pra hoje (fora de campos de texto)
  useEffect(() => {
    function aoTeclar(evento: KeyboardEvent) {
      if (evento.ctrlKey || evento.metaKey || evento.altKey || evento.defaultPrevented) return
      if ((evento.target as HTMLElement | null)?.closest('input, textarea, select, [contenteditable="true"], [role="dialog"]')) return
      if (evento.key === 'ArrowLeft') irPara(somarDias(data, -1))
      else if (evento.key === 'ArrowRight') irPara(somarDias(data, 1))
      else if (evento.key.toLowerCase() === 'h') irPara(hoje)
      else return
      evento.preventDefault()
    }
    window.addEventListener('keydown', aoTeclar)
    return () => window.removeEventListener('keydown', aoTeclar)
  })

  const alternar = useAlternarConclusao(data)

  // registrar um valor ("5 km") também conclui a atividade no dia, se ainda não estava
  const registrarValor = useMutation({
    mutationFn: ({ item, valor }: { item: ItemHoje; valor: number }) =>
      api.post(`/atividades/${item.atividadeId}/conclusao`, null, { params: { data, valor } }),
    onMutate: async ({ item, valor }) => {
      await queryClient.cancelQueries({ queryKey: ['hoje', data] })
      const anterior = queryClient.getQueryData<DiaHoje>(['hoje', data])
      queryClient.setQueryData<DiaHoje>(['hoje', data], (atual) =>
        atual && {
          ...atual,
          itens: atual.itens.map((i) =>
            i.atividadeId === item.atividadeId
              ? {
                  ...i,
                  concluido: true,
                  valor,
                  feitasNaSemana:
                    i.feitasNaSemana === null ? null : i.feitasNaSemana + (i.concluido ? 0 : 1),
                }
              : i,
          ),
        },
      )
      return { anterior }
    },
    onError: (_erro, _variaveis, contexto) => {
      if (contexto?.anterior) queryClient.setQueryData(['hoje', data], contexto.anterior)
    },
    onSettled: () => invalidarDerivados(queryClient),
  })

  const itens = useMemo(() => dia?.itens ?? [], [dia])
  const concluidas = itens.filter(cumprido).length
  const percentual = itens.length > 0 ? Math.round((concluidas / itens.length) * 100) : 0

  const grupos = useMemo(() => {
    const mapa = new Map<string, GrupoArea>()
    for (const item of itens) {
      const grupo = mapa.get(item.areaId) ?? { id: item.areaId, nome: item.areaNome, cor: item.areaCorTema, itens: [] }
      grupo.itens.push(item)
      mapa.set(item.areaId, grupo)
    }
    return [...mapa.values()]
  }, [itens])

  const gruposVisiveis = areaSelecionada ? grupos.filter((grupo) => grupo.id === areaSelecionada) : grupos
  const emSequencia = [...itens].filter((item) => item.streakAtual > 0).sort((a, b) => b.streakAtual - a.streakAtual)
  const relativo = rotuloRelativo(data, hoje)
  const editavel = dia?.editavel ?? data <= hoje

  return (
    <div className="space-y-8">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-3xl font-semibold capitalize tracking-tight text-ink">
            {relativo ?? formatarDiaSemanaLongo(data)}
          </h1>
          <p className="mt-1 text-sm text-ink-faint first-letter:uppercase">{formatarLongo(data)}</p>
        </div>
        <div className="flex items-center gap-1.5">
          {data !== hoje && (
            <button
              type="button"
              onClick={() => irPara(hoje)}
              className="mr-1 rounded-md px-2.5 py-1 text-sm text-accent-strong transition-colors hover:bg-accent-soft"
            >
              Voltar pra hoje
            </button>
          )}
          <button
            type="button"
            onClick={() => irPara(somarDias(data, -1))}
            aria-label="Dia anterior"
            className="rounded-md border border-line p-1.5 text-ink-muted transition-colors hover:border-line-strong hover:text-ink"
          >
            <IconChevronLeft className="h-4 w-4" />
          </button>
          <button
            type="button"
            onClick={() => irPara(somarDias(data, 1))}
            aria-label="Próximo dia"
            className="rounded-md border border-line p-1.5 text-ink-muted transition-colors hover:border-line-strong hover:text-ink"
          >
            <IconChevronRight className="h-4 w-4" />
          </button>
        </div>
      </header>

      <TiraDaSemana data={data} hoje={hoje} aoEscolher={irPara} />

      {isError && <p className="text-sm text-danger">Não foi possível carregar o dia. Confira se o backend está rodando.</p>}

      <div className="grid gap-8 lg:grid-cols-[1fr_17rem]">
        <div className="min-w-0 space-y-6">
          {domingo && revisao?.nota === null && (
            <Link
              to="/revisao"
              className="flex items-center justify-between gap-3 rounded-lg border border-line bg-surface px-4 py-3 text-sm transition-colors hover:border-line-strong"
            >
              <span>
                <span className="text-ink">Domingo, hora da revisão.</span>{' '}
                <span className="text-ink-muted">Veja o que escapou na semana e ajuste a próxima.</span>
              </span>
              <IconChevronRight className="h-4 w-4 shrink-0 text-ink-faint" />
            </Link>
          )}

          {!editavel && itens.length > 0 && (
            <p className="rounded-lg border border-dashed border-line px-4 py-2.5 text-sm text-ink-muted">
              Esse dia ainda não chegou. Dá pra ver o que está planejado, mas só marcar quando ele vier.
            </p>
          )}

          {isLoading && (
            <div className="space-y-2">
              {[0, 1, 2].map((n) => (
                <div key={n} className="h-14 animate-pulse rounded-lg bg-surface" />
              ))}
            </div>
          )}

          {!isLoading && itens.length === 0 && (
            <div className="flex flex-col items-center gap-3 rounded-xl border border-dashed border-line px-6 py-16 text-center">
              <IconInbox className="h-8 w-8 text-ink-faint" />
              <p className="max-w-xs text-sm text-ink-muted">
                Nada planejado {relativo === 'hoje' ? 'pra hoje' : 'nesse dia'}. Atividades aparecem aqui quando
                você marca os dias delas na grade semanal de um objetivo.
              </p>
              <Link to="/areas" className="text-sm text-ink hover:text-accent-strong">
                Abrir áreas →
              </Link>
            </div>
          )}

          {gruposVisiveis.map((grupo) => {
            const feitas = grupo.itens.filter(cumprido).length
            return (
              <section key={grupo.id}>
                <div className="mb-2 flex items-center gap-2 px-1">
                  <span className="h-2 w-2 rounded-full" style={{ backgroundColor: corDaArea(grupo.cor) }} />
                  <Link to={`/areas/${grupo.id}`} className="text-sm font-medium text-ink-muted hover:text-ink">
                    {grupo.nome}
                  </Link>
                  <span className="ml-auto text-xs tabular-nums text-ink-faint">
                    {feitas}/{grupo.itens.length}
                  </span>
                </div>
                <ul className="divide-y divide-line overflow-hidden rounded-xl border border-line bg-surface">
                  {grupo.itens.map((item) => (
                    <ItemAtividade
                      key={item.atividadeId}
                      item={item}
                      editavel={editavel}
                      podeFocar={data === hoje}
                      aoAlternar={() => alternar.mutate(item)}
                      aoRegistrarValor={(valor) => registrarValor.mutate({ item, valor })}
                    />
                  ))}
                </ul>
              </section>
            )
          })}

          <DiarioDoDia data={data} editavel={editavel} />
        </div>

        {itens.length > 0 && (
          <aside className="space-y-6 lg:sticky lg:top-10 lg:self-start">
            <div className="rounded-xl border border-line bg-surface p-5">
              <p className="text-xs text-ink-faint">{editavel ? 'Feito no dia' : 'Planejado'}</p>
              <p className="mt-1 text-4xl font-semibold tabular-nums tracking-tight text-ink">
                {editavel ? concluidas : itens.length}
                {editavel && <span className="text-xl font-normal text-ink-faint">/{itens.length}</span>}
              </p>
              {editavel && (
                <>
                  <div className="mt-4 h-1.5 overflow-hidden rounded-full bg-surface-3">
                    <div
                      className="h-full rounded-full bg-accent transition-[width] duration-500 ease-out"
                      style={{ width: `${percentual}%` }}
                    />
                  </div>
                  <p className="mt-2 text-xs text-ink-faint">
                    {concluidas === itens.length ? 'Dia completo.' : `${itens.length - concluidas} pra fechar o dia`}
                  </p>
                </>
              )}
            </div>

            {grupos.length > 1 && (
              <div>
                <p className="mb-2 px-1 text-xs font-medium text-ink-faint">Filtrar por área</p>
                <ul className="space-y-0.5">
                  {[{ id: null, nome: 'Todas', cor: null, itens }, ...grupos].map((grupo) => {
                    const ativo = areaSelecionada === grupo.id
                    return (
                      <li key={grupo.id ?? 'todas'}>
                        <button
                          type="button"
                          aria-pressed={ativo}
                          onClick={() => setAreaSelecionada(ativo ? null : grupo.id)}
                          className={`flex w-full items-center gap-2.5 rounded-md px-2.5 py-1.5 text-left text-sm transition-colors duration-150 ${
                            ativo ? 'bg-surface-2 text-ink' : 'text-ink-muted hover:bg-surface hover:text-ink'
                          }`}
                        >
                          <span
                            className="h-2 w-2 shrink-0 rounded-full"
                            style={{ backgroundColor: grupo.id ? corDaArea(grupo.cor) : 'var(--color-line-strong)' }}
                          />
                          <span className="flex-1 truncate">{grupo.nome}</span>
                          <span className="text-xs tabular-nums text-ink-faint">{grupo.itens.length}</span>
                        </button>
                      </li>
                    )
                  })}
                </ul>
              </div>
            )}

            {emSequencia.length > 0 && (
              <div>
                <p className="mb-2 px-1 text-xs font-medium text-ink-faint">Em sequência</p>
                <ul className="space-y-1.5 px-1">
                  {emSequencia.slice(0, 5).map((item) => (
                    <li key={item.atividadeId} className="flex items-center gap-2 text-sm">
                      <span className="min-w-0 flex-1 truncate text-ink-muted">{item.atividadeNome}</span>
                      <span className="flex items-center gap-1 tabular-nums text-warning">
                        <IconFlame className="h-3.5 w-3.5" />
                        {item.streakAtual}
                      </span>
                    </li>
                  ))}
                </ul>
              </div>
            )}
          </aside>
        )}
      </div>
    </div>
  )
}
