/** Cor de área com fallback neutro: toda marca de dado de uma área usa a cor escolhida pelo usuário. */
export function corDaArea(cor: string | null | undefined): string {
  return cor ?? 'var(--color-ink-faint)'
}

/** Degrau da rampa sequencial pra uma taxa de conclusão (null = nada planejado). */
export function degrauDaTaxa(taxa: number | null): string {
  if (taxa === null) return 'transparent'
  if (taxa === 0) return 'var(--color-seq-0)'
  if (taxa < 34) return 'var(--color-seq-1)'
  if (taxa < 67) return 'var(--color-seq-2)'
  if (taxa < 100) return 'var(--color-seq-3)'
  return 'var(--color-seq-4)'
}

export function taxaDe(planejado: number, concluido: number): number | null {
  if (planejado === 0) return null
  return Math.min(100, Math.round((concluido * 100) / planejado))
}
