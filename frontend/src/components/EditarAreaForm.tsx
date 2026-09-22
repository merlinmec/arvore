import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { api } from '../lib/api'
import type { AreaArvore } from '../lib/types'

export function EditarAreaForm({ area, aoCancelar }: { area: AreaArvore; aoCancelar: () => void }) {
  const [nome, setNome] = useState(area.nome)
  const [descricaoGeral, setDescricaoGeral] = useState(area.descricaoGeral ?? '')
  const [corTema, setCorTema] = useState(area.corTema ?? '#5fc489')
  const [wallpaperUrl, setWallpaperUrl] = useState(area.wallpaperUrl ?? '')
  const queryClient = useQueryClient()

  const editarArea = useMutation({
    mutationFn: () =>
      api.patch(`/areas/${area.id}`, {
        nome,
        descricaoGeral: descricaoGeral || null,
        corTema: corTema || null,
        wallpaperUrl: wallpaperUrl || null,
      }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['areas'] })
      aoCancelar()
    },
  })

  function aoEnviar(evento: FormEvent) {
    evento.preventDefault()
    editarArea.mutate()
  }

  return (
    <form onSubmit={aoEnviar} className="space-y-3 rounded-lg border border-line bg-surface p-4">
      <div className="space-y-1">
        <label className="text-sm text-ink-muted" htmlFor="edit-nome">
          Nome
        </label>
        <input
          id="edit-nome"
          required
          value={nome}
          onChange={(evento) => setNome(evento.target.value)}
          className="w-full rounded-md border border-line bg-canvas px-3 py-2 text-ink focus:border-accent"
        />
      </div>

      <div className="space-y-1">
        <label className="text-sm text-ink-muted" htmlFor="edit-descricao">
          Descrição geral
        </label>
        <textarea
          id="edit-descricao"
          rows={3}
          value={descricaoGeral}
          onChange={(evento) => setDescricaoGeral(evento.target.value)}
          className="w-full rounded-md border border-line bg-canvas px-3 py-2 text-ink focus:border-accent"
        />
      </div>

      <div className="flex items-center gap-3">
        <label className="text-sm text-ink-muted" htmlFor="edit-cor">
          Cor tema
        </label>
        <input
          id="edit-cor"
          type="color"
          value={corTema}
          onChange={(evento) => setCorTema(evento.target.value)}
          className="h-9 w-16 rounded-md border border-line bg-canvas"
        />
      </div>

      <div className="space-y-1">
        <label className="text-sm text-ink-muted" htmlFor="edit-wallpaper">
          Wallpaper (URL)
        </label>
        <input
          id="edit-wallpaper"
          type="url"
          placeholder="https://..."
          value={wallpaperUrl}
          onChange={(evento) => setWallpaperUrl(evento.target.value)}
          className="w-full rounded-md border border-line bg-canvas px-3 py-2 text-ink placeholder:text-ink-faint focus:border-accent"
        />
      </div>

      <div className="flex gap-2">
        <button
          type="submit"
          disabled={editarArea.isPending}
          className="rounded-md bg-accent px-3 py-1.5 text-sm font-medium text-accent-ink transition-colors hover:bg-accent-strong disabled:opacity-50"
        >
          {editarArea.isPending ? 'Salvando...' : 'Salvar'}
        </button>
        <button
          type="button"
          onClick={aoCancelar}
          className="text-sm text-ink-faint hover:text-ink-muted"
        >
          Cancelar
        </button>
      </div>
    </form>
  )
}
