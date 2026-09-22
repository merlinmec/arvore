import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../lib/api'
import { achatarArvore } from '../lib/arvore'
import type { AreaArvore } from '../lib/types'

export function NovaArea() {
  const [nome, setNome] = useState('')
  const [areaPaiId, setAreaPaiId] = useState('')
  const [descricaoGeral, setDescricaoGeral] = useState('')
  const [corTema, setCorTema] = useState('#5fc489')
  const [wallpaperUrl, setWallpaperUrl] = useState('')
  const [erro, setErro] = useState<string | null>(null)

  const navegar = useNavigate()
  const queryClient = useQueryClient()

  const { data: areas } = useQuery({
    queryKey: ['areas'],
    queryFn: async () => (await api.get<AreaArvore[]>('/areas')).data,
  })

  const areasAchatadas = achatarArvore(areas ?? [])

  const criarArea = useMutation({
    mutationFn: () =>
      api.post('/areas', {
        nome,
        areaPaiId: areaPaiId || null,
        descricaoGeral: descricaoGeral || null,
        corTema: corTema || null,
        wallpaperUrl: wallpaperUrl || null,
      }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['areas'] })
      navegar('/areas')
    },
    onError: () => setErro('Não foi possível criar a área.'),
  })

  function aoEnviar(evento: FormEvent) {
    evento.preventDefault()
    setErro(null)
    criarArea.mutate()
  }

  return (
    <div className="space-y-6">
      <h1 className="text-3xl font-semibold text-ink">Nova área</h1>

      <form onSubmit={aoEnviar} className="max-w-md space-y-4 rounded-lg border border-line bg-surface p-5">
        <div className="space-y-1">
          <label className="text-sm text-ink-muted" htmlFor="nome">
            Nome
          </label>
          <input
            id="nome"
            required
            value={nome}
            onChange={(evento) => setNome(evento.target.value)}
            className="w-full rounded-md border border-line bg-canvas px-3 py-2 text-ink focus:border-accent"
          />
        </div>

        <div className="space-y-1">
          <label className="text-sm text-ink-muted" htmlFor="areaPai">
            Área pai
          </label>
          <select
            id="areaPai"
            value={areaPaiId}
            onChange={(evento) => setAreaPaiId(evento.target.value)}
            className="w-full rounded-md border border-line bg-canvas px-3 py-2 text-ink focus:border-accent"
          >
            <option value="">Nenhuma (área raiz)</option>
            {areasAchatadas.map((area) => (
              <option key={area.id} value={area.id}>
                {'—'.repeat(area.profundidade)} {area.nome}
              </option>
            ))}
          </select>
        </div>

        <div className="space-y-1">
          <label className="text-sm text-ink-muted" htmlFor="descricao">
            Descrição geral
          </label>
          <textarea
            id="descricao"
            rows={3}
            value={descricaoGeral}
            onChange={(evento) => setDescricaoGeral(evento.target.value)}
            className="w-full rounded-md border border-line bg-canvas px-3 py-2 text-ink focus:border-accent"
          />
        </div>

        <div className="flex items-center gap-3">
          <label className="text-sm text-ink-muted" htmlFor="corTema">
            Cor tema
          </label>
          <input
            id="corTema"
            type="color"
            value={corTema}
            onChange={(evento) => setCorTema(evento.target.value)}
            className="h-9 w-16 rounded-md border border-line bg-canvas"
          />
        </div>

        <div className="space-y-1">
          <label className="text-sm text-ink-muted" htmlFor="wallpaper">
            Wallpaper (URL)
          </label>
          <input
            id="wallpaper"
            type="url"
            placeholder="https://..."
            value={wallpaperUrl}
            onChange={(evento) => setWallpaperUrl(evento.target.value)}
            className="w-full rounded-md border border-line bg-canvas px-3 py-2 text-ink placeholder:text-ink-faint focus:border-accent"
          />
        </div>

        {erro && <p className="text-sm text-danger">{erro}</p>}

        <button
          type="submit"
          disabled={criarArea.isPending}
          className="rounded-md bg-accent px-4 py-2 font-medium text-accent-ink transition-colors hover:bg-accent-strong disabled:opacity-50"
        >
          {criarArea.isPending ? 'Criando...' : 'Criar área'}
        </button>
      </form>
    </div>
  )
}
