import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useEffect, useState } from 'react'
import { api } from './api'
import type { SessaoFoco } from './types'

/** Iniciar e parar o foco; as duas atualizam a sessão atual e o tempo do Painel. */
export function useControleFoco() {
  const queryClient = useQueryClient()
  const atualizar = async (sessao: SessaoFoco | null) => {
    queryClient.setQueryData(['foco', 'atual'], sessao)
    await queryClient.invalidateQueries({ queryKey: ['insights', 'tempo'] })
  }
  const iniciar = useMutation({
    mutationFn: async (atividadeId: string) => (await api.post<SessaoFoco>('/foco/iniciar', { atividadeId })).data,
    onSuccess: (sessao) => atualizar(sessao),
  })
  const parar = useMutation({
    mutationFn: async () => (await api.post<SessaoFoco>('/foco/parar')).data,
    onSuccess: () => atualizar(null),
    // 404: a sessão já tinha sido encerrada (em outra aba, ou passou de 12 h); a tela só se acerta
    onError: () => queryClient.invalidateQueries({ queryKey: ['foco', 'atual'] }),
  })
  return { iniciar, parar }
}

/** Segundos desde o início, atualizados a cada segundo enquanto a sessão está aberta. */
export function useCronometro(inicio: string | null) {
  const [agora, setAgora] = useState(() => Date.now())
  useEffect(() => {
    if (!inicio) return
    const intervalo = window.setInterval(() => setAgora(Date.now()), 1000)
    return () => window.clearInterval(intervalo)
  }, [inicio])
  return inicio ? Math.max(0, Math.floor((agora - new Date(inicio).getTime()) / 1000)) : 0
}

/** 75 s → "1:15"; 3725 s → "1:02:05". */
export function formatarCronometro(segundos: number) {
  const horas = Math.floor(segundos / 3600)
  const minutos = Math.floor((segundos % 3600) / 60)
  const resto = String(segundos % 60).padStart(2, '0')
  return horas > 0 ? `${horas}:${String(minutos).padStart(2, '0')}:${resto}` : `${minutos}:${resto}`
}

/** 45 → "45 min"; 85 → "1 h 25 min"; 120 → "2 h". */
export function formatarMinutos(total: number) {
  const horas = Math.floor(total / 60)
  const minutos = total % 60
  if (horas === 0) return `${minutos} min`
  return minutos === 0 ? `${horas} h` : `${horas} h ${minutos} min`
}
