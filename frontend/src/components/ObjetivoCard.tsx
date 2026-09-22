import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { api } from '../lib/api'
import {
  DIAS_SEMANA,
  type Atividade,
  type DiaSemana,
  type ObjetivoComAtividades,
  type StatusObjetivo,
} from '../lib/types'
import { BarraProgresso } from './BarraProgresso'
import { IconPlus, IconTrash } from './icons'

const OPCOES_STATUS: StatusObjetivo[] = ['ATIVO', 'CONCLUIDO', 'PAUSADO', 'ARQUIVADO']

const ESTILO_STATUS: Record<StatusObjetivo, string> = {
  ATIVO: 'bg-accent-soft text-accent-strong',
  CONCLUIDO: 'bg-surface-2 text-ink-muted',
  PAUSADO: 'bg-warning-soft text-warning',
  ARQUIVADO: 'bg-surface-2 text-ink-faint',
}

function AtividadeItem({ atividade, areaId }: { atividade: Atividade; areaId: string }) {
  const queryClient = useQueryClient()

  const alternarDia = useMutation({
    mutationFn: (dia: DiaSemana) =>
      atividade.diasAgendados.includes(dia)
        ? api.delete(`/atividades/${atividade.id}/agenda/${dia}`)
        : api.post(`/atividades/${atividade.id}/agenda/${dia}`),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['objetivos', areaId] })
      await queryClient.invalidateQueries({ queryKey: ['hoje'] })
    },
  })

  const excluirAtividade = useMutation({
    mutationFn: () => api.delete(`/objetivos/${atividade.objetivoId}/atividades/${atividade.id}`),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['objetivos', areaId] })
      await queryClient.invalidateQueries({ queryKey: ['hoje'] })
    },
  })

  return (
    <li className="group space-y-1.5 rounded-md px-2 py-1.5 -mx-2 transition-colors hover:bg-surface-2">
      <div className="flex items-center justify-between gap-2">
        <p className="text-sm text-ink-muted">{atividade.nome}</p>
        <button
          type="button"
          onClick={() => {
            if (window.confirm(`Excluir a atividade "${atividade.nome}"?`)) {
              excluirAtividade.mutate()
            }
          }}
          aria-label="Excluir atividade"
          className="rounded p-1 text-ink-faint opacity-0 transition-colors group-hover:opacity-100 hover:text-danger"
        >
          <IconTrash className="h-3.5 w-3.5" />
        </button>
      </div>
      <div className="flex gap-1">
        {DIAS_SEMANA.map(({ valor, label }) => {
          const ativo = atividade.diasAgendados.includes(valor)
          return (
            <button
              key={valor}
              type="button"
              onClick={() => alternarDia.mutate(valor)}
              className={`flex h-6 w-6 items-center justify-center rounded-full text-[10px] font-medium transition-colors ${
                ativo ? 'bg-accent text-accent-ink' : 'bg-surface-2 text-ink-faint hover:text-ink-muted'
              }`}
            >
              {label.slice(0, 1)}
            </button>
          )
        })}
      </div>
    </li>
  )
}

export function ObjetivoCard({ objetivo }: { objetivo: ObjetivoComAtividades }) {
  const [nomeAtividade, setNomeAtividade] = useState('')
  const queryClient = useQueryClient()

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
      await queryClient.invalidateQueries({ queryKey: ['objetivos', objetivo.areaId] })
      await queryClient.invalidateQueries({ queryKey: ['hoje'] })
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
      await queryClient.invalidateQueries({ queryKey: ['objetivos', objetivo.areaId] })
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
    <div className="rounded-lg border border-line bg-surface p-4">
      <div className="flex items-start justify-between gap-4">
        <p className="text-ink">{objetivo.descricao}</p>
        <div className="flex shrink-0 items-center gap-1">
          <select
            value={objetivo.status}
            onChange={(evento) => alterarStatus.mutate(evento.target.value as StatusObjetivo)}
            className={`appearance-none rounded-full border-none px-2.5 py-1 text-xs font-medium ${ESTILO_STATUS[objetivo.status]}`}
          >
            {OPCOES_STATUS.map((status) => (
              <option key={status} value={status} className="bg-surface text-ink">
                {status}
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

      {objetivo.prazo && <p className="mt-1 text-xs text-ink-faint">Prazo: {objetivo.prazo}</p>}

      <div className="mt-3">
        <BarraProgresso progresso={objetivo.progresso} compact />
      </div>

      <ul className="mt-3 space-y-1.5 border-t border-line pt-3">
        {objetivo.atividades.map((atividade) => (
          <AtividadeItem key={atividade.id} atividade={atividade} areaId={objetivo.areaId} />
        ))}
      </ul>

      <form onSubmit={aoEnviar} className="mt-3 flex gap-2">
        <input
          value={nomeAtividade}
          onChange={(evento) => setNomeAtividade(evento.target.value)}
          placeholder="Nova atividade"
          className="flex-1 rounded-md border border-line bg-canvas px-2.5 py-1.5 text-sm text-ink placeholder:text-ink-faint focus:border-accent"
        />
        <button
          type="submit"
          disabled={criarAtividade.isPending}
          aria-label="Adicionar atividade"
          className="rounded-md border border-line px-2.5 text-ink-muted transition-colors hover:border-line-strong hover:text-ink disabled:opacity-50"
        >
          <IconPlus className="h-4 w-4" />
        </button>
      </form>
    </div>
  )
}
