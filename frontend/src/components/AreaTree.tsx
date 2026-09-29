import { Link } from 'react-router-dom'
import { corDaArea } from '../lib/cores'
import type { AreaArvore } from '../lib/types'
import { IconChevronRight, IconTree } from './icons'

function contarDescendentes(area: AreaArvore): number {
  return area.filhos.reduce((total, filho) => total + 1 + contarDescendentes(filho), 0)
}

function Percentual({ valor }: { valor: number | null }) {
  return (
    <span className="tabular-nums text-sm text-ink-muted">{valor === null ? '—' : `${valor}%`}</span>
  )
}

function BarraFina({ area }: { area: AreaArvore }) {
  return (
    <div className="h-1 overflow-hidden rounded-full bg-surface-3">
      <div
        className="h-full rounded-full transition-[width] duration-500 ease-out"
        style={{ width: `${area.progresso.percentual ?? 0}%`, backgroundColor: corDaArea(area.corTema) }}
      />
    </div>
  )
}

/** Galhos abaixo de uma área raiz, com a linha de conexão da árvore desenhada à esquerda. */
function Galhos({ areas, profundidade = 0 }: { areas: AreaArvore[]; profundidade?: number }) {
  return (
    <ul className={profundidade > 0 ? 'ml-3 border-l border-line pl-3' : ''}>
      {areas.map((area) => (
        <li key={area.id}>
          <Link
            to={`/areas/${area.id}`}
            className="group -mx-2 flex items-center gap-2.5 rounded-md px-2 py-1.5 transition-colors duration-150 hover:bg-surface-2"
          >
            <span className="h-1.5 w-1.5 shrink-0 rounded-full" style={{ backgroundColor: corDaArea(area.corTema) }} />
            <span className="min-w-0 flex-1 truncate text-sm text-ink-muted group-hover:text-ink">{area.nome}</span>
            <Percentual valor={area.progresso.percentual} />
          </Link>
          {area.filhos.length > 0 && <Galhos areas={area.filhos} profundidade={profundidade + 1} />}
        </li>
      ))}
    </ul>
  )
}

/**
 * Cada área raiz vira um bloco com a própria cor no topo (ou o wallpaper, se houver) e as subáreas
 * listadas como galhos. A cor da área é a única cor forte da tela.
 */
export function AreaTree({ areas }: { areas: AreaArvore[] }) {
  if (areas.length === 0) {
    return (
      <div className="flex flex-col items-center gap-3 rounded-xl border border-dashed border-line py-16 text-center">
        <IconTree className="h-8 w-8 text-ink-faint" />
        <p className="max-w-xs text-sm text-ink-muted">
          Nenhuma área criada ainda. Áreas são os grandes ramos da sua árvore, como Saúde, Carreira ou
          Estudos. Comece por uma.
        </p>
      </div>
    )
  }

  return (
    <ul className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
      {areas.map((area) => {
        const cor = corDaArea(area.corTema)
        const subareas = contarDescendentes(area)
        return (
          <li key={area.id} className="animate-rise flex flex-col overflow-hidden rounded-xl border border-line bg-surface">
            <Link to={`/areas/${area.id}`} className="group relative block">
              <div
                className="h-20"
                style={
                  area.wallpaperUrl
                    ? { backgroundImage: `url(${area.wallpaperUrl})`, backgroundSize: 'cover', backgroundPosition: 'center' }
                    : { background: `linear-gradient(135deg, color-mix(in oklab, ${cor} 55%, var(--color-surface)) 0%, color-mix(in oklab, ${cor} 18%, var(--color-surface)) 100%)` }
                }
              />
              <div className="space-y-3 p-4">
                <div className="flex items-center gap-2">
                  <h2 className="min-w-0 flex-1 truncate font-semibold text-ink transition-colors group-hover:text-accent-strong">
                    {area.nome}
                  </h2>
                  <IconChevronRight className="h-4 w-4 shrink-0 text-ink-faint transition-transform duration-150 group-hover:translate-x-0.5" />
                </div>
                {area.descricaoGeral && <p className="line-clamp-2 text-sm text-ink-faint">{area.descricaoGeral}</p>}
                <div className="space-y-1.5">
                  <div className="flex items-baseline justify-between text-xs text-ink-faint">
                    <span>
                      {area.progresso.planejado > 0
                        ? `${area.progresso.concluido} de ${area.progresso.planejado} na semana`
                        : 'sem atividades na grade'}
                    </span>
                    <Percentual valor={area.progresso.percentual} />
                  </div>
                  <BarraFina area={area} />
                </div>
              </div>
            </Link>
            {area.filhos.length > 0 && (
              <div className="mt-auto border-t border-line px-4 pb-3 pt-2.5">
                <p className="mb-1 text-xs text-ink-faint">
                  {subareas} {subareas === 1 ? 'subárea' : 'subáreas'}
                </p>
                <Galhos areas={area.filhos} />
              </div>
            )}
          </li>
        )
      })}
    </ul>
  )
}
