import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { AreaTree } from '../components/AreaTree'
import { IconPlus } from '../components/icons'
import { api } from '../lib/api'
import type { AreaArvore } from '../lib/types'

export function Areas() {
  const { data, isLoading, isError } = useQuery({
    queryKey: ['areas'],
    queryFn: async () => (await api.get<AreaArvore[]>('/areas')).data,
  })

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-semibold text-ink">Áreas</h1>
        <Link
          to="/areas/nova"
          className="flex items-center gap-1.5 rounded-full bg-accent px-3.5 py-1.5 text-sm font-medium text-accent-ink transition-colors hover:bg-accent-strong"
        >
          <IconPlus className="h-4 w-4" />
          Nova área
        </Link>
      </div>

      {isLoading && (
        <div className="space-y-2">
          {[0, 1, 2].map((n) => (
            <div key={n} className="h-16 animate-pulse rounded-lg bg-surface" />
          ))}
        </div>
      )}

      {isError && <p className="text-danger">Não foi possível carregar as áreas.</p>}

      {!isLoading && !isError && <AreaTree areas={data ?? []} />}
    </div>
  )
}
