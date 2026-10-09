import { useMutation, useQueryClient } from '@tanstack/react-query'
import { api } from '../lib/api'
import { DIAS_SEMANA, type Atividade, type DiaSemana } from '../lib/types'

const NOMES_DIA: Record<DiaSemana, string> = {
  SEGUNDA: 'segunda',
  TERCA: 'terça',
  QUARTA: 'quarta',
  QUINTA: 'quinta',
  SEXTA: 'sexta',
  SABADO: 'sábado',
  DOMINGO: 'domingo',
}

/**
 * Editor da grade de uma atividade: dias fixos ou "N× por semana". Usado no card do objetivo e na
 * revisão semanal. Toda mudança vale a partir de hoje e não reescreve o histórico.
 */
export function GradeAtividade({ atividade, cor }: { atividade: Atividade; cor: string }) {
  const queryClient = useQueryClient()

  // a grade muda o plano de todas as telas de dados
  async function invalidar() {
    await Promise.all(
      ['objetivos', 'hoje', 'insights', 'areas', 'revisoes'].map((chave) =>
        queryClient.invalidateQueries({ queryKey: [chave] }),
      ),
    )
  }

  const alternarDia = useMutation({
    mutationFn: (dia: DiaSemana) =>
      atividade.diasAgendados.includes(dia)
        ? api.delete(`/atividades/${atividade.id}/agenda/${dia}`)
        : api.post(`/atividades/${atividade.id}/agenda/${dia}`),
    onSuccess: invalidar,
  })

  const definirFrequencia = useMutation({
    mutationFn: (vezes: number | null) =>
      vezes === null
        ? api.delete(`/atividades/${atividade.id}/agenda/frequencia`)
        : api.put(`/atividades/${atividade.id}/agenda/frequencia`, { vezes }),
    onSuccess: invalidar,
  })

  const flexivel = atividade.vezesPorSemana !== null

  if (atividade.negativa) {
    return (
      <span
        className="rounded-[5px] bg-surface-3 px-2 py-1 text-[11px] font-medium text-ink-muted"
        title="Vale todo dia: o dia conta como cumprido se não houver recaída"
      >
        Hábito negativo · todo dia
      </span>
    )
  }

  return (
    <>
      <select
        value={atividade.vezesPorSemana ?? ''}
        onChange={(evento) => definirFrequencia.mutate(evento.target.value === '' ? null : Number(evento.target.value))}
        aria-label={`Frequência de ${atividade.nome}`}
        title="A mudança vale a partir de hoje, o histórico não muda"
        className="h-6 cursor-pointer appearance-none rounded-[5px] border-none bg-surface-3 px-2 text-[11px] font-medium text-ink-muted hover:text-ink"
      >
        <option value="" className="bg-surface text-ink">
          Dias fixos
        </option>
        {[1, 2, 3, 4, 5, 6, 7].map((vezes) => (
          <option key={vezes} value={vezes} className="bg-surface text-ink">
            {vezes}× por semana
          </option>
        ))}
      </select>
      {!flexivel && (
        <div className="flex gap-0.5" role="group" aria-label={`Dias de ${atividade.nome}`}>
          {DIAS_SEMANA.map(({ valor, label }) => {
            const ativo = atividade.diasAgendados.includes(valor)
            return (
              <button
                key={valor}
                type="button"
                onClick={() => alternarDia.mutate(valor)}
                aria-pressed={ativo}
                aria-label={`${ativo ? 'Tirar' : 'Colocar'} ${atividade.nome} na ${NOMES_DIA[valor]}`}
                title={`${label}: a mudança vale a partir de hoje, o histórico não muda`}
                className={`flex h-6 w-6 items-center justify-center rounded-[5px] text-[10px] font-semibold transition-colors duration-150 ${
                  ativo ? 'text-canvas' : 'bg-surface-3 text-ink-faint hover:text-ink-muted'
                }`}
                style={ativo ? { backgroundColor: cor } : undefined}
              >
                {label.slice(0, 1)}
              </button>
            )
          })}
        </div>
      )}
    </>
  )
}
