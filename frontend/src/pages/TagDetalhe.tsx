import { useQuery } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'
import { IconChevronLeft } from '../components/icons'
import { MarkdownContent } from '../components/MarkdownContent'
import { api } from '../lib/api'
import type { NotaComArea } from '../lib/types'

export function TagDetalhe() {
  const { nome } = useParams<{ nome: string }>()

  const { data, isLoading } = useQuery({
    queryKey: ['tag', nome],
    queryFn: async () => (await api.get<NotaComArea[]>(`/tags/${encodeURIComponent(nome ?? '')}/notas`)).data,
    enabled: Boolean(nome),
  })

  return (
    <div className="space-y-5">
      <div>
        <Link
          to="/areas"
          className="inline-flex items-center gap-1 text-sm text-ink-faint transition-colors hover:text-ink-muted"
        >
          <IconChevronLeft className="h-4 w-4" />
          Áreas
        </Link>
        <h1 className="mt-2 text-3xl font-semibold text-ink">#{nome}</h1>
        <p className="mt-1 text-sm text-ink-faint">Notas com essa tag, de qualquer área.</p>
      </div>

      {isLoading && <p className="text-sm text-ink-faint">Carregando...</p>}
      {!isLoading && (data ?? []).length === 0 && (
        <p className="text-sm text-ink-faint">Nenhuma nota com essa tag ainda.</p>
      )}

      <div className="space-y-3">
        {(data ?? []).map((nota) => (
          <div key={nota.id} className="rounded-lg border border-line bg-surface p-4">
            <div className="flex items-center justify-between gap-2">
              <p className="font-medium text-ink">{nota.titulo}</p>
              <Link
                to={`/areas/${nota.areaId}`}
                className="shrink-0 text-xs text-ink-faint hover:text-ink-muted"
              >
                {nota.areaNome} →
              </Link>
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
        ))}
      </div>
    </div>
  )
}
