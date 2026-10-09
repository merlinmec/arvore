import { NavLink, Outlet } from 'react-router-dom'
import { useAreas } from '../lib/queries'
import { useAuth } from '../lib/useAuth'
import { IconBranches, IconCalendar, IconChart, IconLogOut, IconSun, IconTree } from './icons'

const NAVEGACAO = [
  { para: '/', rotulo: 'Hoje', Icone: IconSun, fim: true },
  { para: '/painel', rotulo: 'Painel', Icone: IconChart, fim: false },
  { para: '/revisao', rotulo: 'Revisão', Icone: IconCalendar, fim: false },
  { para: '/areas', rotulo: 'Áreas', Icone: IconBranches, fim: true },
]

function classeItem({ isActive }: { isActive: boolean }) {
  return `group flex items-center gap-2.5 rounded-md px-2.5 py-1.5 text-sm transition-colors duration-150 ${
    isActive
      ? 'bg-surface-2 font-medium text-ink'
      : 'text-ink-muted hover:bg-surface hover:text-ink'
  }`
}

function Marca() {
  return (
    <div className="flex items-center gap-2 text-accent-strong">
      <IconTree className="h-5 w-5" />
      <span className="text-[15px] font-semibold tracking-tight text-ink">árvore</span>
    </div>
  )
}

/** Lista das áreas raiz na lateral: atalho e, ao mesmo tempo, um termômetro da semana. */
function GalhosDaSemana() {
  const { data: areas } = useAreas()

  if (!areas || areas.length === 0) {
    return null
  }

  return (
    <div className="space-y-1">
      <p className="px-2.5 pb-1 text-xs font-medium text-ink-faint">Esta semana</p>
      {areas.map((area) => {
        const percentual = area.progresso.percentual
        return (
          <NavLink key={area.id} to={`/areas/${area.id}`} className={classeItem}>
            <span
              className="h-2 w-2 shrink-0 rounded-full"
              style={{ backgroundColor: area.corTema ?? 'var(--color-ink-faint)' }}
            />
            <span className="min-w-0 flex-1 truncate">{area.nome}</span>
            <span className="tabular-nums text-xs text-ink-faint">
              {percentual === null ? '—' : `${percentual}%`}
            </span>
          </NavLink>
        )
      })}
    </div>
  )
}

export function Layout() {
  const { sair } = useAuth()

  return (
    <div className="min-h-screen bg-canvas text-ink lg:grid lg:grid-cols-[15rem_1fr]">
      <aside className="sticky top-0 hidden h-screen flex-col border-r border-line bg-rail px-3 py-5 lg:flex">
        <div className="px-2.5">
          <Marca />
        </div>

        <nav className="mt-7 space-y-0.5" aria-label="Principal">
          {NAVEGACAO.map(({ para, rotulo, Icone, fim }) => (
            <NavLink key={para} to={para} end={fim} className={classeItem}>
              <Icone className="h-4 w-4 shrink-0 text-ink-faint transition-colors group-hover:text-ink-muted group-aria-[current=page]:text-accent-strong" />
              {rotulo}
            </NavLink>
          ))}
        </nav>

        <div className="mt-8 min-h-0 flex-1 overflow-y-auto">
          <GalhosDaSemana />
        </div>

        <button
          type="button"
          onClick={sair}
          className="mt-4 flex items-center gap-2.5 rounded-md px-2.5 py-1.5 text-sm text-ink-faint transition-colors hover:bg-surface hover:text-ink-muted"
        >
          <IconLogOut className="h-4 w-4" />
          Sair
        </button>
      </aside>

      <header className="sticky top-0 z-20 border-b border-line bg-canvas/90 backdrop-blur-sm lg:hidden">
        <div className="flex items-center justify-between gap-3 px-4 py-3">
          <Marca />
          <button
            type="button"
            onClick={sair}
            aria-label="Sair"
            className="rounded-md p-1.5 text-ink-faint transition-colors hover:bg-surface hover:text-ink-muted"
          >
            <IconLogOut className="h-4 w-4" />
          </button>
        </div>
        <nav className="flex gap-1 px-3 pb-2" aria-label="Principal">
          {NAVEGACAO.map(({ para, rotulo, Icone, fim }) => (
            <NavLink key={para} to={para} end={fim} className={classeItem}>
              <Icone className="h-4 w-4" />
              {rotulo}
            </NavLink>
          ))}
        </nav>
      </header>

      <main className="mx-auto w-full max-w-6xl px-4 py-8 sm:px-6 lg:px-10 lg:py-10">
        <Outlet />
      </main>
    </div>
  )
}
