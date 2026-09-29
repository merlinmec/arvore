import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { api } from '../lib/api'
import { corDaArea } from '../lib/cores'
import { diferencaEmDias, formatarCurto, hojeIso } from '../lib/datas'
import {
  DIAS_SEMANA,
  type Atividade,
  type DiaSemana,
  type ObjetivoComAtividades,
  type StatusObjetivo,
} from '../lib/types'
import { IconCalendar, IconPlus, IconTrash } from './icons'

const STATUS: Record<StatusObjetivo, { rotulo: string; estilo: string }> = {
  ATIVO: { rotulo: 'Ativo', estilo: 'bg-accent-soft text-accent-strong' },
  CONCLUIDO: { rotulo: 'Concluído', estilo: 'bg-surface-3 text-ink-muted' },
  PAUSADO: { rotulo: 'Pausado', estilo: 'bg-warning-soft text-warning' },
  ARQUIVADO: { rotulo: 'Arquivado', estilo: 'bg-surface-3 text-ink-faint' },
}

const NOMES_DIA: Record<DiaSemana, string> = {
  SEGUNDA: 'segunda',
  TERCA: 'terça',
  QUARTA: 'quarta',
  QUINTA: 'quinta',
  SEXTA: 'sexta',
  SABADO: 'sábado',
  DOMINGO: 'domingo',
}

function Prazo({ prazo }: { prazo: string }) {
  const restantes = diferencaEmDias(hojeIso(), prazo)
  const texto =
    restantes > 1
      ? `faltam ${restantes} dias`
      : restantes === 1
        ? 'vence amanhã'
        : restantes === 0
          ? 'vence hoje'
          : `venceu há ${Math.abs(restantes)} ${Math.abs(restantes) === 1 ? 'dia' : 'dias'}`
  const urgente = restantes <= 7
  return (
    <p className={`flex items-center gap-1.5 text-xs ${urgente ? 'text-warning' : 'text-ink-faint'}`}>
      <IconCalendar className="h-3.5 w-3.5" />
      {formatarCurto(prazo)} · {texto}
    </p>
  )
}

function AtividadeItem({ atividade, areaId, cor }: { atividade: Atividade; areaId: string; cor: string }) {
  const queryClient = useQueryClient()

  async function invalidar() {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['objetivos', areaId] }),
      queryClient.invalidateQueries({ queryKey: ['hoje'] }),
      queryClient.invalidateQueries({ queryKey: ['insights'] }),
      queryClient.invalidateQueries({ queryKey: ['areas'] }),
    ])
  }

  const alternarDia = useMutation({
    mutationFn: (dia: DiaSemana) =>
      atividade.diasAgendados.includes(dia)
        ? api.delete(`/atividades/${atividade.id}/agenda/${dia}`)
        : api.post(`/atividades/${atividade.id}/agenda/${dia}`),
    onSuccess: invalidar,
  })

  const definirFrequencia = useMutation({
    mutationFn: (vezes: number | null) =>
      vezes === null
        ? api.delete(`/atividades/${atividade.id}/agenda/frequencia`)
        : api.put(`/atividades/${atividade.id}/agenda/frequencia`, { vezes }),
    onSuccess: invalidar,
  })

  const excluirAtividade = useMutation({
    mutationFn: () => api.delete(`/objetivos/${atividade.objetivoId}/atividades/${atividade.id}`),
    onSuccess: invalidar,
  })

  const flexivel = atividade.vezesPorSemana !== null

  return (
    <li className="group flex flex-wrap items-center gap-x-3 gap-y-2 py-2.5">
      <p className="min-w-0 flex-1 truncate text-sm text-ink">{atividade.nome}</p>
      <div className="flex items-center gap-1.5">
        <select
          value={atividade.vezesPorSemana ?? ''}
          onChange={(evento) =>
            definirFrequencia.mutate(evento.target.value === '' ? null : Number(evento.target.value))
          }
          aria-label={`Frequência de ${atividade.nome}`}
          title="A mudança vale a partir de hoje, o histórico não muda"
          className="h-6 cursor-pointer appearance-none rounded-[5px] border-none bg-surface-3 px-2 text-[11px] font-medium text-ink-muted hover:text-ink"
        >
          <option value="" className="bg-surface text-ink">
            Dias fixos
          </option>
          {[1, 2, 3, 4, 5, 6, 7].map((vezes) => (
            <option key={vezes} value={vezes} className="bg-surface text-ink">
              {vezes}× por semana
            </option>
          ))}
        </select>
        {!flexivel && (
          <div className="flex gap-0.5" role="group" aria-label={`Dias de ${atividade.nome}`}>
            {DIAS_SEMANA.map(({ valor, label }) => {
              const ativo = atividade.diasAgendados.includes(valor)
              return (
                <button
                  key={valor}
                  type="button"
                  onClick={() => alternarDia.mutate(valor)}
                  aria-pressed={ativo}
                  aria-label={`${ativo ? 'Tirar' : 'Colocar'} ${atividade.nome} na ${NOMES_DIA[valor]}`}
                  title={`${label}: a mudança vale a partir de hoje, o histórico não muda`}
                  className={`flex h-6 w-6 items-center justify-center rounded-[5px] text-[10px] font-semibold transition-colors duration-150 ${
                    ativo ? 'text-canvas' : 'bg-surface-3 text-ink-faint hover:text-ink-muted'
                  }`}
                  style={ativo ? { backgroundColor: cor } : undefined}
                >
                  {label.slice(0, 1)}
                </button>
              )
            })}
          </div>
        )}
        <button
          type="button"
          onClick={() => {
            if (window.confirm(`Excluir a atividade "${atividade.nome}"? O histórico dela some junto.`)) {
              excluirAtividade.mutate()
            }
          }}
          aria-label={`Excluir atividade ${atividade.nome}`}
          className="ml-1 rounded p-1 text-ink-faint opacity-0 transition-[opacity,color] group-hover:opacity-100 hover:text-danger focus-visible:opacity-100"
        >
          <IconTrash className="h-3.5 w-3.5" />
        </button>
      </div>
    </li>
  )
}

export function ObjetivoCard({ objetivo, corArea }: { objetivo: ObjetivoComAtividades; corArea?: string | null }) {
  const [nomeAtividade, setNomeAtividade] = useState('')
  const queryClient = useQueryClient()
  const cor = corDaArea(corArea)
  const ativo = objetivo.status === 'ATIVO'
  const { progresso } = objetivo

  const criarAtividade = useMutation({
    mutationFn: () => api.post(`/objetivos/${objetivo.id}/atividades`, { nome: nomeAtividade }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['objetivos', objetivo.areaId] })
      setNomeAtividade('')
    },
  })

  const excluirObjetivo = useMutation({
    mutationFn: () => api.delete(`/areas/${objetivo.areaId}/objetivos/${objetivo.id}`),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['objetivos', objetivo.areaId] }),
        queryClient.invalidateQueries({ queryKey: ['hoje'] }),
        queryClient.invalidateQueries({ queryKey: ['insights'] }),
      ])
    },
  })

  const alterarStatus = useMutation({
    mutationFn: (status: StatusObjetivo) =>
      api.patch(`/areas/${objetivo.areaId}/objetivos/${objetivo.id}`, {
        descricao: objetivo.descricao,
        prazo: objetivo.prazo,
        status,
      }),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['objetivos', objetivo.areaId] }),
        queryClient.invalidateQueries({ queryKey: ['hoje'] }),
        queryClient.invalidateQueries({ queryKey: ['insights'] }),
      ])
    },
  })

  function aoEnviar(evento: FormEvent) {
    evento.preventDefault()
    if (!nomeAtividade.trim()) {
      return
    }
    criarAtividade.mutate()
  }

  return (
    <article
      className={`rounded-xl border border-line bg-surface transition-opacity ${ativo ? '' : 'opacity-70 hover:opacity-100'}`}
    >
      <div className="space-y-3 p-4 pb-3">
        <div className="flex items-start justify-between gap-4">
          <h3 className="font-medium leading-snug text-ink">{objetivo.descricao}</h3>
          <div className="flex shrink-0 items-center gap-1">
            <select
              value={objetivo.status}
              onChange={(evento) => alterarStatus.mutate(evento.target.value as StatusObjetivo)}
              aria-label="Status do objetivo"
              className={`cursor-pointer appearance-none rounded-full border-none px-2.5 py-0.5 text-xs font-medium ${STATUS[objetivo.status].estilo}`}
            >
              {(Object.keys(STATUS) as StatusObjetivo[]).map((status) => (
                <option key={status} value={status} className="bg-surface text-ink">
                  {STATUS[status].rotulo}
                </option>
              ))}
            </select>
            <button
              type="button"
              onClick={() => {
                if (window.confirm(`Excluir o objetivo "${objetivo.descricao}" e suas atividades?`)) {
                  excluirObjetivo.mutate()
                }
              }}
              aria-label="Excluir objetivo"
              className="rounded p-1.5 text-ink-faint transition-colors hover:text-danger"
            >
              <IconTrash className="h-3.5 w-3.5" />
            </button>
          </div>
        </div>

        {objetivo.prazo && <Prazo prazo={objetivo.prazo} />}

        {progresso.percentual !== null ? (
          <div className="flex items-center gap-3">
            <div className="h-1.5 flex-1 overflow-hidden rounded-full bg-surface-3">
              <div
                className="h-full rounded-full transition-[width] duration-500 ease-out"
                style={{ width: `${progresso.percentual}%`, backgroundColor: cor }}
              />
            </div>
            <p className="shrink-0 text-xs tabular-nums text-ink-faint">
              <span className="text-ink-muted">{progresso.concluido}</span>/{progresso.planejado} na semana
            </p>
          </div>
        ) : (
          <p className="text-xs text-ink-faint">Nenhuma atividade na grade ainda.</p>
        )}
      </div>

      {objetivo.atividades.length > 0 && (
        <ul className="divide-y divide-line border-t border-line px-4">
          {objetivo.atividades.map((atividade) => (
            <AtividadeItem key={atividade.id} atividade={atividade} areaId={objetivo.areaId} cor={cor} />
          ))}
        </ul>
      )}

      <form onSubmit={aoEnviar} className="flex gap-2 border-t border-line p-3">
        <input
          value={nomeAtividade}
          onChange={(evento) => setNomeAtividade(evento.target.value)}
          placeholder="Nova atividade, ex: correr 5 km"
          aria-label="Nome da nova atividade"
          className="min-w-0 flex-1 rounded-md border border-transparent bg-transparent px-2 py-1.5 text-sm text-ink placeholder:text-ink-faint hover:border-line focus:border-accent"
        />
        <button
          type="submit"
          disabled={criarAtividade.isPending || !nomeAtividade.trim()}
          aria-label="Adicionar atividade"
          className="rounded-md border border-line px-2.5 text-ink-muted transition-colors hover:border-line-strong hover:text-ink disabled:opacity-40"
        >
          <IconPlus className="h-4 w-4" />
        </button>
      </form>
    </article>
  )
}
