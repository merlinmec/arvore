import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../lib/useAuth'
import { IconLogOut, IconTree } from './icons'

function classeLink({ isActive }: { isActive: boolean }) {
  return `rounded-full px-3.5 py-1.5 text-sm font-medium transition-colors ${
    isActive
      ? 'bg-accent-soft text-accent-strong'
      : 'text-ink-muted hover:bg-surface-2 hover:text-ink'
  }`
}

export function Layout() {
  const { sair } = useAuth()

  return (
    <div className="min-h-screen bg-canvas text-ink">
      <header className="sticky top-0 z-10 border-b border-line bg-canvas/85 backdrop-blur-sm">
        <div className="mx-auto flex max-w-4xl items-center justify-between px-6 py-3.5">
          <div className="flex items-center gap-6">
            <div className="flex items-center gap-2 text-accent-strong">
              <IconTree className="h-5 w-5" />
              <span className="text-sm font-semibold tracking-wide text-ink">árvore</span>
            </div>
            <nav className="flex gap-1">
              <NavLink to="/" end className={classeLink}>
                Hoje
              </NavLink>
              <NavLink to="/areas" className={classeLink}>
                Áreas
              </NavLink>
            </nav>
          </div>
          <button
            onClick={sair}
            className="flex items-center gap-1.5 rounded-full px-3 py-1.5 text-sm text-ink-faint transition-colors hover:bg-surface-2 hover:text-ink-muted"
          >
            <IconLogOut className="h-4 w-4" />
            Sair
          </button>
        </div>
      </header>
      <main className="mx-auto max-w-4xl px-6 py-10">
        <Outlet />
      </main>
    </div>
  )
}
