import { Link } from 'react-router-dom'
import type { AreaArvore } from '../lib/types'
import { BarraProgresso } from './BarraProgresso'
import { IconTree } from './icons'

export function AreaTree({ areas, raiz = true }: { areas: AreaArvore[]; raiz?: boolean }) {
  if (raiz && areas.length === 0) {
    return (
      <div className="flex flex-col items-center gap-3 rounded-xl border border-dashed border-line py-16 text-center">
        <IconTree className="h-8 w-8 text-ink-faint" />
        <p className="max-w-xs text-sm text-ink-muted">
          Nenhuma área criada ainda. Áreas são os grandes ramos da sua árvore — comece por uma.
        </p>
      </div>
    )
  }

  return (
    <ul className="space-y-2">
      {areas.map((area) => (
        <li key={area.id}>
          <Link
            to={`/areas/${area.id}`}
            className="group block space-y-2.5 rounded-lg border border-line bg-surface px-4 py-3 transition-colors hover:border-line-strong"
          >
            <div className="flex items-center gap-2.5">
              <span
                className="h-2.5 w-2.5 shrink-0 rounded-full"
                style={{ backgroundColor: area.corTema ?? 'var(--color-ink-faint)' }}
              />
              <span className="font-medium text-ink group-hover:text-accent-strong">{area.nome}</span>
            </div>
            <BarraProgresso progresso={area.progresso} corTema={area.corTema} compact />
          </Link>
          {area.filhos.length > 0 && (
            <div className="mt-2 ml-3 border-l border-line pl-4">
              <AreaTree areas={area.filhos} raiz={false} />
            </div>
          )}
        </li>
      ))}
    </ul>
  )
}
