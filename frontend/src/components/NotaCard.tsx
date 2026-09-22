import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../lib/api'
import type { Nota } from '../lib/types'
import { IconPencil, IconTrash } from './icons'
import { MarkdownContent } from './MarkdownContent'

export function NotaCard({ nota }: { nota: Nota }) {
  const [editando, setEditando] = useState(false)
  const [titulo, setTitulo] = useState(nota.titulo)
  const [corpo, setCorpo] = useState(nota.corpo)
  const [tags, setTags] = useState(nota.tags.join(', '))
  const queryClient = useQueryClient()

  const excluirNota = useMutation({
    mutationFn: () => api.delete(`/areas/${nota.areaId}/notas/${nota.id}`),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['notas', nota.areaId] })
    },
  })

  const editarNota = useMutation({
    mutationFn: () =>
      api.patch(`/areas/${nota.areaId}/notas/${nota.id}`, {
        titulo,
        corpo,
        tags: tags
          .split(',')
          .map((tag) => tag.trim())
          .filter(Boolean),
      }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['notas', nota.areaId] })
      setEditando(false)
    },
  })

  function aoEnviar(evento: FormEvent) {
    evento.preventDefault()
    editarNota.mutate()
  }

  if (editando) {
    return (
      <form onSubmit={aoEnviar} className="space-y-3 rounded-lg border border-line bg-surface p-4">
        <input
          required
          value={titulo}
          onChange={(evento) => setTitulo(evento.target.value)}
          className="w-full rounded-md border border-line bg-canvas px-3 py-2 text-ink focus:border-accent"
        />
        <textarea
          rows={4}
          value={corpo}
          onChange={(evento) => setCorpo(evento.target.value)}
          className="w-full rounded-md border border-line bg-canvas px-3 py-2 text-ink focus:border-accent"
        />
        <input
          value={tags}
          onChange={(evento) => setTags(evento.target.value)}
          placeholder="tags separadas por vírgula"
          className="w-full rounded-md border border-line bg-canvas px-3 py-2 text-ink placeholder:text-ink-faint focus:border-accent"
        />
        <div className="flex gap-2">
          <button
            type="submit"
            disabled={editarNota.isPending}
            className="rounded-md bg-accent px-3 py-1.5 text-sm font-medium text-accent-ink transition-colors hover:bg-accent-strong disabled:opacity-50"
          >
            Salvar
          </button>
          <button
            type="button"
            onClick={() => setEditando(false)}
            className="text-sm text-ink-faint hover:text-ink-muted"
          >
            Cancelar
          </button>
        </div>
      </form>
    )
  }

  return (
    <div className="group rounded-lg border border-line bg-surface p-4">
      <div className="flex items-start justify-between gap-4">
        <p className="font-medium text-ink">{nota.titulo}</p>
        <div className="flex shrink-0 gap-0.5 opacity-0 transition-opacity group-hover:opacity-100">
          <button
            type="button"
            onClick={() => setEditando(true)}
            aria-label="Editar nota"
            className="rounded p-1.5 text-ink-faint hover:text-ink-muted"
          >
            <IconPencil className="h-3.5 w-3.5" />
          </button>
          <button
            type="button"
            onClick={() => {
              if (window.confirm(`Excluir a nota "${nota.titulo}"?`)) {
                excluirNota.mutate()
              }
            }}
            aria-label="Excluir nota"
            className="rounded p-1.5 text-ink-faint hover:text-danger"
          >
            <IconTrash className="h-3.5 w-3.5" />
          </button>
        </div>
      </div>
      {nota.corpo && <MarkdownContent>{nota.corpo}</MarkdownContent>}
      {nota.tags.length > 0 && (
        <div className="mt-2 flex flex-wrap gap-1">
          {nota.tags.map((tag) => (
            <Link
              key={tag}
              to={`/tags/${encodeURIComponent(tag)}`}
              className="rounded-full bg-surface-2 px-2 py-0.5 text-xs text-ink-faint transition-colors hover:bg-accent-soft hover:text-accent-strong"
            >
              #{tag}
            </Link>
          ))}
        </div>
      )}
    </div>
  )
}
