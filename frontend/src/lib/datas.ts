/**
 * Datas trafegam como string ISO (AAAA-MM-DD), igual ao LocalDate do backend. Converter sempre
 * pelo meio-dia local evita o clássico bug de fuso em que "2026-09-28" vira dia 27.
 */

export function paraData(iso: string): Date {
  return new Date(`${iso}T12:00:00`)
}

export function paraIso(data: Date): string {
  const ano = data.getFullYear()
  const mes = String(data.getMonth() + 1).padStart(2, '0')
  const dia = String(data.getDate()).padStart(2, '0')
  return `${ano}-${mes}-${dia}`
}

export function hojeIso(): string {
  return paraIso(new Date())
}

export function somarDias(iso: string, dias: number): string {
  const data = paraData(iso)
  data.setDate(data.getDate() + dias)
  return paraIso(data)
}

/** Segunda-feira da semana da data (a semana do app começa na segunda, como no backend). */
export function inicioDaSemana(iso: string): string {
  const diaDaSemana = (paraData(iso).getDay() + 6) % 7
  return somarDias(iso, -diaDaSemana)
}

export function diferencaEmDias(de: string, ate: string): number {
  return Math.round((paraData(ate).getTime() - paraData(de).getTime()) / 86_400_000)
}

const formatoLongo = new Intl.DateTimeFormat('pt-BR', { weekday: 'long', day: 'numeric', month: 'long' })
const formatoCurto = new Intl.DateTimeFormat('pt-BR', { day: 'numeric', month: 'short' })
const formatoDiaSemana = new Intl.DateTimeFormat('pt-BR', { weekday: 'short' })
const formatoDiaSemanaLongo = new Intl.DateTimeFormat('pt-BR', { weekday: 'long' })
const formatoMes = new Intl.DateTimeFormat('pt-BR', { month: 'short' })

export function formatarLongo(iso: string): string {
  return formatoLongo.format(paraData(iso))
}

export function formatarCurto(iso: string): string {
  return formatoCurto.format(paraData(iso)).replace('.', '')
}

export function formatarDiaSemana(iso: string): string {
  return formatoDiaSemana.format(paraData(iso)).replace('.', '')
}

export function formatarDiaSemanaLongo(iso: string): string {
  return formatoDiaSemanaLongo.format(paraData(iso))
}

export function formatarMes(iso: string): string {
  return formatoMes.format(paraData(iso)).replace('.', '')
}

/** "hoje", "ontem", "amanhã" ou null pra qualquer outra data. */
export function rotuloRelativo(iso: string, hoje: string): string | null {
  const diferenca = diferencaEmDias(hoje, iso)
  if (diferenca === 0) return 'hoje'
  if (diferenca === -1) return 'ontem'
  if (diferenca === 1) return 'amanhã'
  return null
}
