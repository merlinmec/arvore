import { Link } from 'react-router-dom'
import { AreaTree } from '../components/AreaTree'
import { IconPlus } from '../components/icons'
import { useAreas } from '../lib/queries'

export function Areas() {
  const { data, isLoading, isError } = useAreas()

  return (
    <div className="space-y-6">
      <header className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="text-3xl font-semibold tracking-tight text-ink">Áreas</h1>
          <p className="mt-1 text-sm text-ink-faint">Os ramos da sua árvore e como cada um está nesta semana.</p>
        </div>
        <Link
          to="/areas/nova"
          className="flex items-center gap-1.5 rounded-md bg-accent px-3 py-1.5 text-sm font-medium text-accent-ink transition-colors hover:bg-accent-strong"
        >
          <IconPlus className="h-4 w-4" />
          Nova área
        </Link>
      </header>

      {isLoading && (
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {[0, 1, 2].map((n) => (
            <div key={n} className="h-44 animate-pulse rounded-xl bg-surface" />
          ))}
        </div>
      )}

      {isError && <p className="text-sm text-danger">Não foi possível carregar as áreas.</p>}

      {!isLoading && !isError && <AreaTree areas={data ?? []} />}
    </div>
  )
}
