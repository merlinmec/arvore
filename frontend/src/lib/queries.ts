import { useQuery } from '@tanstack/react-query'
import { api } from './api'
import type {
  AreaArvore,
  DiaConsistencia,
  DiaHoje,
  Diario,
  DiarioConsistencia,
  ResumoInsights,
  Revisao,
  RevisaoSemana,
  SessaoFoco,
  TempoFoco,
} from './types'

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

/** Revisão da semana que contém a data (o backend leva qualquer dia pra segunda-feira). */
export function useRevisao(data: string, habilitada = true) {
  return useQuery({
    queryKey: ['revisoes', 'semana', data],
    queryFn: async () => (await api.get<RevisaoSemana>(`/revisoes/${data}`)).data,
    placeholderData: (anterior) => anterior,
    enabled: habilitada,
  })
}

export function useRevisoes() {
  return useQuery({
    queryKey: ['revisoes', 'lista'],
    queryFn: async () => (await api.get<Revisao[]>('/revisoes')).data,
  })
}

export function useDiario(data: string, habilitada = true) {
  return useQuery({
    queryKey: ['diario', data],
    queryFn: async () => (await api.get<Diario>(`/diario/${data}`)).data,
    enabled: habilitada,
  })
}

export function useDiarioConsistencia(dias: number, areaId: string | null) {
  return useQuery({
    queryKey: ['insights', 'diario', dias, areaId],
    queryFn: async () =>
      (
        await api.get<DiarioConsistencia>('/insights/diario', {
          params: { dias, areaId: areaId ?? undefined },
        })
      ).data,
    placeholderData: (anterior) => anterior,
  })
}

/** A sessão de foco contando agora, ou null (a API responde 204 sem corpo). */
export function useFocoAtual() {
  return useQuery({
    queryKey: ['foco', 'atual'],
    queryFn: async () => {
      const resposta = await api.get<SessaoFoco | ''>('/foco/atual')
      return resposta.status === 204 || !resposta.data ? null : resposta.data
    },
  })
}

export function useTempoFoco(dias: number, areaId: string | null) {
  return useQuery({
    queryKey: ['insights', 'tempo', dias, areaId],
    queryFn: async () =>
      (
        await api.get<TempoFoco>('/insights/tempo', {
          params: { dias, areaId: areaId ?? undefined },
        })
      ).data,
    placeholderData: (anterior) => anterior,
  })
}
