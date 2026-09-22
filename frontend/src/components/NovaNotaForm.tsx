import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { api } from '../lib/api'
import { IconPlus } from './icons'

export function NovaNotaForm({ areaId }: { areaId: string }) {
  const [titulo, setTitulo] = useState('')
  const [corpo, setCorpo] = useState('')
  const [tags, setTags] = useState('')
  const [aberto, setAberto] = useState(false)
  const queryClient = useQueryClient()

  const criarNota = useMutation({
    mutationFn: () =>
      api.post(`/areas/${areaId}/notas`, {
        titulo,
        corpo,
        tags: tags
          .split(',')
          .map((tag) => tag.trim())
          .filter(Boolean),
      }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['notas', areaId] })
      setTitulo('')
      setCorpo('')
      setTags('')
      setAberto(false)
    },
  })

  function aoEnviar(evento: FormEvent) {
    evento.preventDefault()
    criarNota.mutate()
  }

  if (!aberto) {
    return (
      <button
        onClick={() => setAberto(true)}
        className="flex items-center gap-1.5 rounded-md border border-line px-3 py-1.5 text-sm text-ink-muted transition-colors hover:border-line-strong hover:text-ink"
      >
        <IconPlus className="h-4 w-4" />
        Nova nota
      </button>
    )
  }

  return (
    <form onSubmit={aoEnviar} className="max-w-md space-y-3 rounded-lg border border-line bg-surface p-4">
      <div className="space-y-1">
        <label className="text-sm text-ink-muted" htmlFor="titulo">
          Título
        </label>
        <input
          id="titulo"
          required
          value={titulo}
          onChange={(evento) => setTitulo(evento.target.value)}
          className="w-full rounded-md border border-line bg-canvas px-3 py-2 text-ink focus:border-accent"
        />
      </div>

      <div className="space-y-1">
        <label className="text-sm text-ink-muted" htmlFor="corpo">
          Anotação
        </label>
        <textarea
          id="corpo"
          rows={4}
          value={corpo}
          onChange={(evento) => setCorpo(evento.target.value)}
          className="w-full rounded-md border border-line bg-canvas px-3 py-2 text-ink focus:border-accent"
        />
      </div>

      <div className="space-y-1">
        <label className="text-sm text-ink-muted" htmlFor="tags">
          Tags (separadas por vírgula — ex: valorant, viper)
        </label>
        <input
          id="tags"
          value={tags}
          onChange={(evento) => setTags(evento.target.value)}
          placeholder="valorant, viper"
          className="w-full rounded-md border border-line bg-canvas px-3 py-2 text-ink placeholder:text-ink-faint focus:border-accent"
        />
      </div>

      <div className="flex gap-2">
        <button
          type="submit"
          disabled={criarNota.isPending}
          className="rounded-md bg-accent px-3 py-1.5 text-sm font-medium text-accent-ink transition-colors hover:bg-accent-strong disabled:opacity-50"
        >
          {criarNota.isPending ? 'Salvando...' : 'Salvar'}
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
