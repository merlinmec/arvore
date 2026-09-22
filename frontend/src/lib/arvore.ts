import type { AreaArvore } from './types'

export interface AreaAchatada {
  id: string
  nome: string
  profundidade: number
}

export function achatarArvore(areas: AreaArvore[], profundidade = 0): AreaAchatada[] {
  return areas.flatMap((area) => [
    { id: area.id, nome: area.nome, profundidade },
    ...achatarArvore(area.filhos, profundidade + 1),
  ])
}

export function encontrarArea(areas: AreaArvore[], id: string): AreaArvore | null {
  for (const area of areas) {
    if (area.id === id) {
      return area
    }
    const encontrada = encontrarArea(area.filhos, id)
    if (encontrada) {
      return encontrada
    }
  }
  return null
}
