import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { api } from '../lib/api'
import { lerNumero } from '../lib/numeros'
import { IconPlus } from './icons'

export function NovoObjetivoForm({ areaId }: { areaId: string }) {
  const [descricao, setDescricao] = useState('')
  const [prazo, setPrazo] = useState('')
  const [metaValor, setMetaValor] = useState('')
  const [metaUnidade, setMetaUnidade] = useState('')
  const [aberto, setAberto] = useState(false)
  const queryClient = useQueryClient()

  const criarObjetivo = useMutation({
    mutationFn: () =>
      api.post(`/areas/${areaId}/objetivos`, {
        descricao,
        prazo: prazo || null,
        metaValor: lerNumero(metaValor),
        metaUnidade: metaUnidade.trim() || null,
      }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['objetivos', areaId] })
      setDescricao('')
      setPrazo('')
      setMetaValor('')
      setMetaUnidade('')
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

      <fieldset className="space-y-1">
        <legend className="text-sm text-ink-muted">Meta quantitativa (opcional)</legend>
        <div className="flex gap-2">
          <input
            inputMode="decimal"
            value={metaValor}
            onChange={(evento) => setMetaValor(evento.target.value)}
            placeholder="100"
            aria-label="Valor da meta"
            required={metaUnidade.trim() !== ''}
            className="w-28 rounded-md border border-line bg-canvas px-3 py-2 tabular-nums text-ink focus:border-accent"
          />
          <input
            value={metaUnidade}
            onChange={(evento) => setMetaUnidade(evento.target.value)}
            placeholder="km, páginas, horas..."
            maxLength={20}
            aria-label="Unidade da meta"
            required={metaValor.trim() !== ''}
            className="min-w-0 flex-1 rounded-md border border-line bg-canvas px-3 py-2 text-ink focus:border-accent"
          />
        </div>
      </fieldset>

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
