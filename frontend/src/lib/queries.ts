import { useQuery } from '@tanstack/react-query'
import { api } from './api'
import type { AreaArvore, DiaConsistencia, DiaHoje, ResumoInsights } from './types'

/**
 * Hooks de leitura compartilhados. As chaves começam por um prefixo estável ('hoje', 'insights',
 * 'areas') pra que uma mutação invalide todas as variações de uma vez.
 */

export function useAreas() {
  return useQuery({
    queryKey: ['areas'],
    queryFn: async () => (await api.get<AreaArvore[]>('/areas')).data,
  })
}

export function useDia(data: string) {
  return useQuery({
    queryKey: ['hoje', data],
    queryFn: async () => (await api.get<DiaHoje>('/hoje', { params: { data } })).data,
    placeholderData: (anterior) => anterior,
  })
}

export function useHistorico(params: { inicio?: string; fim?: string; areaId?: string | null }) {
  return useQuery({
    queryKey: ['insights', 'historico', params],
    queryFn: async () =>
      (
        await api.get<DiaConsistencia[]>('/insights/historico', {
          params: { ...params, areaId: params.areaId ?? undefined },
        })
      ).data,
    placeholderData: (anterior) => anterior,
  })
}

export function useResumo(dias: number, areaId: string | null) {
  return useQuery({
    queryKey: ['insights', 'resumo', dias, areaId],
    queryFn: async () =>
      (
        await api.get<ResumoInsights>('/insights/resumo', {
          params: { dias, areaId: areaId ?? undefined },
        })
      ).data,
    placeholderData: (anterior) => anterior,
  })
}
