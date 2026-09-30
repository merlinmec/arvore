const FORMATO = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 1 })

/** Número pra leitura: vírgula decimal e no máximo uma casa ("12,5"). */
export function formatarNumero(valor: number): string {
  return FORMATO.format(valor)
}

/** Lê o que a pessoa digitou aceitando vírgula ou ponto; vazio ou inválido vira null. */
export function lerNumero(texto: string): number | null {
  const normalizado = texto.trim().replace(',', '.')
  if (normalizado === '') return null
  const valor = Number(normalizado)
  return Number.isFinite(valor) && valor >= 0 ? valor : null
}
