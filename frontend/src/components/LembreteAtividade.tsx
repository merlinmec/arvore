import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { api } from '../lib/api'
import { horario } from '../lib/lembretes'
import type { Atividade } from '../lib/types'
import { IconBell } from './icons'

function permissao(): NotificationPermission | 'indisponivel' {
  return 'Notification' in window ? Notification.permission : 'indisponivel'
}

/**
 * Sino na linha da atividade: mostra o horário do lembrete e abre um campo pra definir ou remover.
 * Ao salvar o primeiro lembrete, pede a permissão de notificação do navegador.
 */
export function LembreteAtividade({ atividade, areaId }: { atividade: Atividade; areaId: string }) {
  const queryClient = useQueryClient()
  const [aberto, setAberto] = useState(false)
  const [valor, setValor] = useState(atividade.lembrete ? horario(atividade.lembrete) : '08:00')
  const [estadoPermissao, setEstadoPermissao] = useState(permissao)

  async function invalidar() {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['objetivos', areaId] }),
      queryClient.invalidateQueries({ queryKey: ['hoje'] }),
    ])
  }

  const salvar = useMutation({
    mutationFn: (novo: string | null) =>
      novo === null
        ? api.delete(`/atividades/${atividade.id}/lembrete`)
        : api.put(`/atividades/${atividade.id}/lembrete`, { horario: novo }),
    onSuccess: async () => {
      setAberto(false)
      await invalidar()
    },
  })

  async function aoEnviar(evento: FormEvent) {
    evento.preventDefault()
    if (!valor) return
    if (estadoPermissao === 'default') {
      setEstadoPermissao(await Notification.requestPermission())
    }
    salvar.mutate(valor)
  }

  if (!aberto) {
    return (
      <button
        type="button"
        onClick={() => setAberto(true)}
        aria-label={
          atividade.lembrete
            ? `Lembrete de ${atividade.nome} às ${horario(atividade.lembrete)}`
            : `Definir lembrete pra ${atividade.nome}`
        }
        title={atividade.lembrete ? 'Lembrete' : 'Definir lembrete'}
        className={`flex items-center gap-1 rounded px-1 py-0.5 text-xs tabular-nums transition-[opacity,color] ${
          atividade.lembrete
            ? 'text-ink-muted hover:text-ink'
            : 'text-ink-faint opacity-0 hover:text-ink-muted focus-visible:opacity-100 group-hover:opacity-100'
        }`}
      >
        <IconBell className="h-3.5 w-3.5" />
        {atividade.lembrete && horario(atividade.lembrete)}
      </button>
    )
  }

  return (
    <form onSubmit={aoEnviar} className="flex flex-wrap items-center gap-1.5 text-xs">
      <input
        type="time"
        value={valor}
        onChange={(evento) => setValor(evento.target.value)}
        aria-label={`Horário do lembrete de ${atividade.nome}`}
        autoFocus
        className="rounded-md border border-line bg-canvas px-1.5 py-0.5 text-sm tabular-nums text-ink focus:border-accent"
      />
      <button
        type="submit"
        disabled={salvar.isPending}
        className="rounded-md bg-accent px-2 py-1 font-medium text-accent-ink transition-colors hover:bg-accent-strong disabled:opacity-50"
      >
        Salvar
      </button>
      {atividade.lembrete && (
        <button
          type="button"
          onClick={() => salvar.mutate(null)}
          className="rounded-md px-2 py-1 text-ink-faint transition-colors hover:text-danger"
        >
          Remover
        </button>
      )}
      <button
        type="button"
        onClick={() => setAberto(false)}
        className="rounded-md px-2 py-1 text-ink-faint transition-colors hover:text-ink"
      >
        Cancelar
      </button>
      {estadoPermissao === 'denied' && (
        <span className="basis-full text-warning">
          As notificações estão bloqueadas pro site; libere nas configurações do navegador.
        </span>
      )}
      {estadoPermissao === 'indisponivel' && (
        <span className="basis-full text-warning">Este navegador não mostra notificações.</span>
      )}
    </form>
  )
}
