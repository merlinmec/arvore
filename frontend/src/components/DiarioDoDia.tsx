import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { api } from '../lib/api'
import { useDiario } from '../lib/queries'
import type { Diario } from '../lib/types'

const ROTULOS_HUMOR = ['Péssimo', 'Ruim', 'Ok', 'Bem', 'Ótimo']
const ROTULOS_ENERGIA = ['Esgotado', 'Baixa', 'Média', 'Boa', 'Alta']

/** Cinco botões de 1 a 5; clicar no nível marcado desmarca. */
function Escala({
  titulo,
  rotulos,
  valor,
  aoEscolher,
}: {
  titulo: string
  rotulos: string[]
  valor: number | null
  aoEscolher: (nivel: number | null) => void
}) {
  return (
    <div>
      <p className="mb-1.5 flex items-baseline justify-between text-xs text-ink-faint">
        {titulo}
        <span className="text-ink-muted">{valor === null ? '' : rotulos[valor - 1]}</span>
      </p>
      <div className="grid grid-cols-5 gap-1" role="radiogroup" aria-label={titulo}>
        {rotulos.map((rotulo, indice) => {
          const nivel = indice + 1
          const marcado = valor === nivel
          return (
            <button
              key={nivel}
              type="button"
              role="radio"
              aria-checked={marcado}
              aria-label={`${titulo}: ${nivel}, ${rotulo.toLowerCase()}`}
              title={rotulo}
              onClick={() => aoEscolher(marcado ? null : nivel)}
              className={`h-8 rounded-md border text-sm tabular-nums transition-colors duration-150 active:scale-95 ${
                marcado
                  ? 'border-accent bg-accent-soft text-accent-strong'
                  : valor !== null && nivel < valor
                    ? 'border-line bg-surface-2 text-ink-muted'
                    : 'border-line text-ink-faint hover:border-line-strong hover:text-ink'
              }`}
            >
              {nivel}
            </button>
          )
        })}
      </div>
    </div>
  )
}

/**
 * Diário da data: humor, energia e uma nota curta. Os níveis salvam no clique e a nota ao sair do
 * campo; tudo vazio apaga o registro do dia.
 */
export function DiarioDoDia({ data, editavel }: { data: string; editavel: boolean }) {
  const { data: diario, isLoading } = useDiario(data, editavel)
  if (!editavel) return null
  if (isLoading || !diario) return <div className="h-40 animate-pulse rounded-xl bg-surface" />
  // remonta ao trocar de dia, pra o campo da nota partir do que está salvo
  return <FormularioDiario key={data} diario={diario} />
}

function FormularioDiario({ diario }: { diario: Diario }) {
  const queryClient = useQueryClient()
  const [texto, setTexto] = useState(diario.nota ?? '')
  const chave = ['diario', diario.data]

  const salvar = useMutation({
    mutationFn: (proximo: Diario) =>
      api.put<Diario>(`/diario/${proximo.data}`, {
        humor: proximo.humor,
        energia: proximo.energia,
        nota: proximo.nota,
      }),
    onMutate: async (proximo) => {
      await queryClient.cancelQueries({ queryKey: chave })
      const anterior = queryClient.getQueryData<Diario>(chave)
      queryClient.setQueryData(chave, proximo)
      return { anterior }
    },
    onError: (_erro, _proximo, contexto) => {
      if (contexto?.anterior) queryClient.setQueryData(chave, contexto.anterior)
    },
    onSettled: () => queryClient.invalidateQueries({ queryKey: ['insights', 'diario'] }),
  })

  // parte do cache, não da prop: a nota salva no blur e o clique num nível logo depois não se atropelam
  function atualizar(mudanca: Partial<Diario>) {
    salvar.mutate({ ...(queryClient.getQueryData<Diario>(chave) ?? diario), ...mudanca })
  }

  function salvarNota() {
    const nota = texto.trim() || null
    if (nota !== (queryClient.getQueryData<Diario>(chave) ?? diario).nota) atualizar({ nota })
  }

  const vazio = diario.humor === null && diario.energia === null && diario.nota === null

  return (
    <section className="rounded-xl border border-line bg-surface p-5" aria-labelledby="titulo-diario">
      <div className="mb-4 flex items-baseline justify-between gap-3">
        <h2 id="titulo-diario" className="text-sm font-medium text-ink">
          Diário
        </h2>
        <p className="text-xs text-ink-faint" aria-live="polite">
          {salvar.isError
            ? 'Não foi possível salvar'
            : salvar.isPending
              ? 'Salvando…'
              : vazio
                ? 'Como foi o dia?'
                : 'Salvo'}
        </p>
      </div>
      <div className="grid gap-4 sm:grid-cols-2">
        <Escala
          titulo="Humor"
          rotulos={ROTULOS_HUMOR}
          valor={diario.humor}
          aoEscolher={(humor) => atualizar({ humor })}
        />
        <Escala
          titulo="Energia"
          rotulos={ROTULOS_ENERGIA}
          valor={diario.energia}
          aoEscolher={(energia) => atualizar({ energia })}
        />
      </div>
      <textarea
        value={texto}
        onChange={(evento) => setTexto(evento.target.value)}
        onBlur={salvarNota}
        rows={2}
        maxLength={4000}
        placeholder="Uma linha sobre o dia (opcional)"
        aria-label="Nota do dia"
        className="mt-4 w-full resize-y rounded-lg border border-line bg-canvas px-3 py-2 text-sm leading-relaxed text-ink placeholder:text-ink-faint focus:border-accent"
      />
    </section>
  )
}
