import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../lib/api'
import type { ItemHoje } from '../lib/types'
import { IconCheck, IconInbox } from '../components/icons'

const dataDeHoje = new Intl.DateTimeFormat('pt-BR', {
  weekday: 'long',
  day: 'numeric',
  month: 'long',
}).format(new Date())

export function Hoje() {
  const queryClient = useQueryClient()

  const { data, isLoading } = useQuery({
    queryKey: ['hoje'],
    queryFn: async () => (await api.get<ItemHoje[]>('/hoje')).data,
  })

  const alternarConclusao = useMutation({
    mutationFn: (item: ItemHoje) =>
      item.concluidoHoje
        ? api.delete(`/atividades/${item.atividadeId}/conclusao`)
        : api.post(`/atividades/${item.atividadeId}/conclusao`),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['hoje'] })
    },
  })

  const itens = data ?? []
  const concluidas = itens.filter((item) => item.concluidoHoje).length
  const percentualDia = itens.length > 0 ? Math.round((concluidas / itens.length) * 100) : 0

  return (
    <div className="space-y-8">
      <div>
        <div className="flex items-end justify-between gap-4">
          <div>
            <p className="text-sm capitalize text-ink-faint">{dataDeHoje}</p>
            <h1 className="text-3xl font-semibold text-ink">Hoje</h1>
          </div>
          {itens.length > 0 && (
            <div className="text-right">
              <p className="tabular-nums text-2xl font-semibold text-ink">
                {concluidas}
                <span className="text-ink-faint">/{itens.length}</span>
              </p>
              <p className="text-xs text-ink-faint">concluídas</p>
            </div>
          )}
        </div>
        {itens.length > 0 && (
          <div className="mt-4 h-1.5 w-full overflow-hidden rounded-full bg-surface-2">
            <div
              className="h-full rounded-full bg-accent transition-[width] duration-500 ease-out"
              style={{ width: `${percentualDia}%` }}
            />
          </div>
        )}
      </div>

      {isLoading && (
        <div className="space-y-2">
          {[0, 1, 2].map((n) => (
            <div key={n} className="h-14 animate-pulse rounded-lg bg-surface" />
          ))}
        </div>
      )}

      {!isLoading && itens.length === 0 && (
        <div className="flex flex-col items-center gap-3 rounded-xl border border-dashed border-line py-16 text-center">
          <IconInbox className="h-8 w-8 text-ink-faint" />
          <p className="max-w-xs text-sm text-ink-muted">
            Nada planejado pra hoje. Crie uma atividade numa área e encaixe ela na grade semanal.
          </p>
        </div>
      )}

      <ul className="space-y-2">
        {itens.map((item) => (
          <li
            key={item.atividadeId}
            className="animate-rise group flex items-center gap-3 rounded-lg border border-line bg-surface px-4 py-3 transition-colors hover:border-line-strong"
          >
            <button
              type="button"
              onClick={() => alternarConclusao.mutate(item)}
              aria-pressed={item.concluidoHoje}
              aria-label={item.concluidoHoje ? 'Marcar como não concluída' : 'Marcar como concluída'}
              className={`flex h-6 w-6 shrink-0 items-center justify-center rounded-full border-2 transition-colors ${
                item.concluidoHoje
                  ? 'border-accent bg-accent text-accent-ink'
                  : 'border-line-strong text-transparent hover:border-accent'
              }`}
            >
              <IconCheck className="h-3.5 w-3.5" strokeWidth={2.5} />
            </button>
            <div className="min-w-0 flex-1">
              <p className={item.concluidoHoje ? 'truncate text-ink-faint line-through' : 'truncate text-ink'}>
                {item.atividadeNome}
              </p>
              <p className="mt-0.5 flex items-center gap-1.5 truncate text-xs text-ink-faint">
                <span
                  className="h-1.5 w-1.5 shrink-0 rounded-full"
                  style={{ backgroundColor: item.areaCorTema ?? 'var(--color-ink-faint)' }}
                />
                {item.areaNome} · {item.objetivoDescricao}
              </p>
            </div>
          </li>
        ))}
      </ul>
    </div>
  )
}
