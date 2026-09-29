import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { api } from '../lib/api'
import { IconPlus } from './icons'

export function NovoObjetivoForm({ areaId }: { areaId: string }) {
  const [descricao, setDescricao] = useState('')
  const [prazo, setPrazo] = useState('')
  const [aberto, setAberto] = useState(false)
  const queryClient = useQueryClient()

  const criarObjetivo = useMutation({
    mutationFn: () =>
      api.post(`/areas/${areaId}/objetivos`, {
        descricao,
        prazo: prazo || null,
      }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['objetivos', areaId] })
      setDescricao('')
      setPrazo('')
      setAberto(false)
    },
  })

  function aoEnviar(evento: FormEvent) {
    evento.preventDefault()
    criarObjetivo.mutate()
  }

  if (!aberto) {
    return (
      <button
        onClick={() => setAberto(true)}
        className="flex items-center gap-1.5 rounded-md border border-line px-2.5 py-1 text-sm text-ink-muted transition-colors hover:border-line-strong hover:text-ink"
      >
        <IconPlus className="h-4 w-4" />
        Novo objetivo
      </button>
    )
  }

  return (
    <form onSubmit={aoEnviar} className="w-full space-y-3 rounded-xl border border-line-strong bg-surface p-4">
      <div className="space-y-1">
        <label className="text-sm text-ink-muted" htmlFor="descricao">
          Descrição
        </label>
        <input
          id="descricao"
          required
          value={descricao}
          onChange={(evento) => setDescricao(evento.target.value)}
          className="w-full rounded-md border border-line bg-canvas px-3 py-2 text-ink focus:border-accent"
        />
      </div>

      <div className="space-y-1">
        <label className="text-sm text-ink-muted" htmlFor="prazo">
          Prazo (opcional)
        </label>
        <input
          id="prazo"
          type="date"
          value={prazo}
          onChange={(evento) => setPrazo(evento.target.value)}
          className="w-full rounded-md border border-line bg-canvas px-3 py-2 text-ink focus:border-accent"
        />
      </div>

      <div className="flex gap-2">
        <button
          type="submit"
          disabled={criarObjetivo.isPending}
          className="rounded-md bg-accent px-3 py-1.5 text-sm font-medium text-accent-ink transition-colors hover:bg-accent-strong disabled:opacity-50"
        >
          {criarObjetivo.isPending ? 'Salvando...' : 'Salvar'}
        </button>
        <button
          type="button"
          onClick={() => setAberto(false)}
          className="text-sm text-ink-faint hover:text-ink-muted"
        >
          Cancelar
        </button>
      </div>
    </form>
  )
}
