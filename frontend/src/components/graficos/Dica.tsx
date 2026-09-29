import type { ReactNode } from 'react'

/**
 * Tooltip dos gráficos. Posicionado em coordenadas do container (que deve ser `relative`),
 * ancorado acima do ponto e sem capturar o mouse, pra não roubar o hover da marca.
 */
export function Dica({ x, y, children }: { x: number; y: number; children: ReactNode }) {
  return (
    <div
      role="tooltip"
      className="pointer-events-none absolute z-30 w-max max-w-64 -translate-x-1/2 -translate-y-full rounded-md border border-line-strong bg-surface-3 px-3 py-2 text-xs text-ink shadow-[0_8px_24px_-6px_rgb(0_0_0/0.6)]"
      style={{ left: x, top: y - 10 }}
    >
      {children}
    </div>
  )
}
