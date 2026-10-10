import { useMutation, useQueryClient, type QueryClient } from '@tanstack/react-query'
import { api } from './api'
import type { DiaHoje, ItemHoje } from './types'

/** Tudo que é derivado das conclusões: marcar ou desmarcar algo muda essas telas. */
export async function invalidarDerivados(queryClient: QueryClient) {
  await Promise.all([
    queryClient.invalidateQueries({ queryKey: ['hoje'] }),
    queryClient.invalidateQueries({ queryKey: ['insights'] }),
    queryClient.invalidateQueries({ queryKey: ['areas'] }),
    queryClient.invalidateQueries({ queryKey: ['objetivos'] }),
    queryClient.invalidateQueries({ queryKey: ['revisoes'] }),
  ])
}

/**
 * Marca ou desmarca a atividade no dia. A tela muda na hora e volta atrás se a API recusar: marcar
 * precisa parecer instantâneo. Usado pela tela Hoje e pela paleta de comandos.
 */
export function useAlternarConclusao(data: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (item: ItemHoje) =>
      item.concluido
        ? api.delete(`/atividades/${item.atividadeId}/conclusao`, { params: { data } })
        : api.post(`/atividades/${item.atividadeId}/conclusao`, null, { params: { data } }),
    onMutate: async (item) => {
      await queryClient.cancelQueries({ queryKey: ['hoje', data] })
      const anterior = queryClient.getQueryData<DiaHoje>(['hoje', data])
      queryClient.setQueryData<DiaHoje>(
        ['hoje', data],
        (atual) =>
          atual && {
            ...atual,
            itens: atual.itens.map((i) =>
              i.atividadeId === item.atividadeId
                ? {
                    ...i,
                    concluido: !i.concluido,
                    // recaída zera a sequência na hora; desfazer espera o servidor recalcular
                    streakAtual: i.negativa && !i.concluido ? 0 : i.streakAtual,
                    feitasNaSemana: i.feitasNaSemana === null ? null : i.feitasNaSemana + (i.concluido ? -1 : 1),
                  }
                : i,
            ),
          },
      )
      return { anterior }
    },
    onError: (_erro, _item, contexto) => {
      if (contexto?.anterior) queryClient.setQueryData(['hoje', data], contexto.anterior)
    },
    onSettled: () => invalidarDerivados(queryClient),
  })
}
