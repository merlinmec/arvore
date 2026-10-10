import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { ConfirmarExclusao } from '../components/ConfirmarExclusao'
import { EditarAreaForm } from '../components/EditarAreaForm'
import { Heatmap } from '../components/graficos/Heatmap'
import { IconChevronRight, IconPencil, IconTrash } from '../components/icons'
import { NotaCard } from '../components/NotaCard'
import { NovaNotaForm } from '../components/NovaNotaForm'
import { NovoObjetivoForm } from '../components/NovoObjetivoForm'
import { ObjetivoCard } from '../components/ObjetivoCard'
import { api } from '../lib/api'
import { caminhoAte } from '../lib/arvore'
import { corDaArea } from '../lib/cores'
import { hojeIso, inicioDaSemana, somarDias } from '../lib/datas'
import { useAreas, useHistorico } from '../lib/queries'
import { useDesfazer } from '../lib/useDesfazer'
import type { Nota, ObjetivoComAtividades } from '../lib/types'

const SEMANAS_HISTORICO = 26

export function AreaDetalhe() {
  const { id } = useParams<{ id: string }>()
  const navegar = useNavigate()
  const queryClient = useQueryClient()
  const [editando, setEditando] = useState(false)
  const [confirmandoExclusao, setConfirmandoExclusao] = useState(false)

  const { data: areas, isLoading: carregandoAreas } = useAreas()
  const caminho = id ? caminhoAte(areas ?? [], id) : []
  const area = caminho[caminho.length - 1] ?? null

  const hoje = hojeIso()
  const { data: historico } = useHistorico({
    inicio: somarDias(inicioDaSemana(hoje), -7 * (SEMANAS_HISTORICO - 1)),
    fim: hoje,
    areaId: id,
  })

  const excluirArea = useMutation({
    mutationFn: () => api.delete(`/areas/${id}`),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['areas'] })
      navegar('/areas')
    },
  })

  const { data: todosObjetivos, isLoading: carregandoObjetivos } = useQuery({
    queryKey: ['objetivos', id],
    queryFn: async () => (await api.get<ObjetivoComAtividades[]>(`/areas/${id}/objetivos`)).data,
    enabled: Boolean(id),
  })

  const { data: todasNotas, isLoading: carregandoNotas } = useQuery({
    queryKey: ['notas', id],
    queryFn: async () => (await api.get<Nota[]>(`/areas/${id}/notas`)).data,
    enabled: Boolean(id),
  })

  // o que está com exclusão pendente já sai das listas e dos contadores
  const { pendente } = useDesfazer()
  const objetivos = todosObjetivos?.filter((objetivo) => !pendente(objetivo.id))
  const notas = todasNotas?.filter((nota) => !pendente(nota.id))

  if (carregandoAreas) {
    return <div className="h-48 animate-pulse rounded-xl bg-surface" />
  }

  if (!area) {
    return (
      <div className="space-y-3">
        <p className="text-ink-muted">Área não encontrada. Ela pode ter sido excluída.</p>
        <Link to="/areas" className="text-sm text-accent-strong hover:underline">
          Voltar para Áreas
        </Link>
      </div>
    )
  }

  const cor = corDaArea(area.corTema)
  const ativos = (objetivos ?? []).filter((objetivo) => objetivo.status === 'ATIVO')
  const outros = (objetivos ?? []).filter((objetivo) => objetivo.status !== 'ATIVO')

  return (
    <div className="space-y-8">
      <nav aria-label="Caminho" className="flex flex-wrap items-center gap-1 text-sm text-ink-faint">
        <Link to="/areas" className="transition-colors hover:text-ink-muted">
          Áreas
        </Link>
        {caminho.map((parte) => (
          <span key={parte.id} className="flex items-center gap-1">
            <IconChevronRight className="h-3.5 w-3.5" />
            {parte.id === area.id ? (
              <span className="text-ink-muted">{parte.nome}</span>
            ) : (
              <Link to={`/areas/${parte.id}`} className="transition-colors hover:text-ink-muted">
                {parte.nome}
              </Link>
            )}
          </span>
        ))}
      </nav>

      {editando ? (
        <EditarAreaForm area={area} aoCancelar={() => setEditando(false)} />
      ) : (
        <section className="overflow-hidden rounded-xl border border-line bg-surface">
          <div
            className="relative h-28 sm:h-36"
            style={
              area.wallpaperUrl
                ? { backgroundImage: `url(${area.wallpaperUrl})`, backgroundSize: 'cover', backgroundPosition: 'center' }
                : {
                    background: `linear-gradient(120deg, color-mix(in oklab, ${cor} 60%, var(--color-surface)) 0%, color-mix(in oklab, ${cor} 22%, var(--color-surface)) 55%, var(--color-surface) 100%)`,
                  }
            }
          >
            <div className="absolute inset-x-0 bottom-0 h-16 bg-gradient-to-t from-surface to-transparent" />
            {confirmandoExclusao ? (
              <div className="absolute inset-x-3 top-3 flex justify-end">
                <ConfirmarExclusao
                  pergunta={`Excluir "${area.nome}" com subáreas, objetivos e notas? Não dá pra desfazer.`}
                  ocupado={excluirArea.isPending}
                  aoConfirmar={() => excluirArea.mutate()}
                  aoCancelar={() => setConfirmandoExclusao(false)}
                />
              </div>
            ) : (
              <div className="absolute right-3 top-3 flex gap-1">
                <button
                  type="button"
                  onClick={() => setEditando(true)}
                  aria-label="Editar área"
                  title="Editar área"
                  className="rounded-md bg-canvas/60 p-2 text-ink-muted backdrop-blur-sm transition-colors hover:bg-canvas/80 hover:text-ink"
                >
                  <IconPencil className="h-4 w-4" />
                </button>
                <button
                  type="button"
                  onClick={() => setConfirmandoExclusao(true)}
                  aria-label="Excluir área"
                  title="Excluir área"
                  className="rounded-md bg-canvas/60 p-2 text-ink-muted backdrop-blur-sm transition-colors hover:bg-danger-soft hover:text-danger"
                >
                  <IconTrash className="h-4 w-4" />
                </button>
              </div>
            )}
          </div>

          <div className="grid gap-6 px-6 pb-6 lg:grid-cols-[1fr_auto] lg:items-end">
            <div className="min-w-0">
              <h1 className="text-3xl font-semibold tracking-tight text-ink">{area.nome}</h1>
              {area.descricaoGeral && <p className="mt-2 max-w-xl text-sm text-ink-muted">{area.descricaoGeral}</p>}
            </div>
            <dl className="flex gap-8">
              <div>
                <dt className="text-xs text-ink-faint">Semana</dt>
                <dd className="mt-0.5 text-2xl font-semibold tabular-nums text-ink">
                  {area.progresso.percentual === null ? '—' : `${area.progresso.percentual}%`}
                </dd>
              </div>
              <div>
                <dt className="text-xs text-ink-faint">Objetivos ativos</dt>
                <dd className="mt-0.5 text-2xl font-semibold tabular-nums text-ink">{ativos.length}</dd>
              </div>
              <div>
                <dt className="text-xs text-ink-faint">Subáreas</dt>
                <dd className="mt-0.5 text-2xl font-semibold tabular-nums text-ink">{area.filhos.length}</dd>
              </div>
            </dl>
          </div>

          <div className="border-t border-line px-6 py-5">
            <div className="mb-4 flex items-baseline justify-between gap-3">
              <h2 className="text-sm font-medium text-ink">Últimos 6 meses</h2>
              <Link to={`/painel?area=${area.id}`} className="text-xs text-ink-faint transition-colors hover:text-ink-muted">
                Ver no painel →
              </Link>
            </div>
            {historico ? <Heatmap dias={historico} hoje={hoje} /> : <div className="h-28 animate-pulse rounded-md bg-surface-2" />}
          </div>
        </section>
      )}

      {area.filhos.length > 0 && (
        <section className="space-y-3">
          <h2 className="text-lg font-medium text-ink">Subáreas</h2>
          <ul className="grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
            {area.filhos.map((filho) => (
              <li key={filho.id}>
                <Link
                  to={`/areas/${filho.id}`}
                  className="flex items-center gap-2.5 rounded-lg border border-line bg-surface px-3.5 py-2.5 transition-colors hover:border-line-strong"
                >
                  <span className="h-2 w-2 rounded-full" style={{ backgroundColor: corDaArea(filho.corTema) }} />
                  <span className="min-w-0 flex-1 truncate text-sm text-ink">{filho.nome}</span>
                  <span className="text-sm tabular-nums text-ink-muted">
                    {filho.progresso.percentual === null ? '—' : `${filho.progresso.percentual}%`}
                  </span>
                </Link>
              </li>
            ))}
          </ul>
        </section>
      )}

      <div className="grid gap-8 lg:grid-cols-[1fr_22rem]">
        <section className="min-w-0 space-y-4">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <h2 className="flex items-baseline gap-2 text-lg font-medium text-ink">
              Objetivos
              {(objetivos ?? []).length > 0 && (
                <span className="text-sm tabular-nums text-ink-faint">{objetivos?.length}</span>
              )}
            </h2>
            <NovoObjetivoForm areaId={area.id} />
          </div>

          {carregandoObjetivos && <div className="h-40 animate-pulse rounded-xl bg-surface" />}
          {!carregandoObjetivos && (objetivos ?? []).length === 0 && (
            <p className="rounded-xl border border-dashed border-line px-5 py-8 text-center text-sm text-ink-faint">
              Objetivos são o que você quer alcançar nesta área. Cada um ganha atividades com dias fixos na semana.
            </p>
          )}

          <div className="space-y-3">
            {[...ativos, ...outros].map((objetivo) => (
              <ObjetivoCard key={objetivo.id} objetivo={objetivo} corArea={area.corTema} />
            ))}
          </div>
        </section>

        <section className="min-w-0 space-y-4">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <h2 className="flex items-baseline gap-2 text-lg font-medium text-ink">
              Notas
              {(notas ?? []).length > 0 && <span className="text-sm tabular-nums text-ink-faint">{notas?.length}</span>}
            </h2>
            <NovaNotaForm areaId={area.id} />
          </div>

          {carregandoNotas && <div className="h-24 animate-pulse rounded-xl bg-surface" />}
          {!carregandoNotas && (notas ?? []).length === 0 && (
            <p className="rounded-xl border border-dashed border-line px-5 py-8 text-center text-sm text-ink-faint">
              Anotações em Markdown, com tags que cruzam áreas.
            </p>
          )}

          <div className="space-y-3">
            {(notas ?? []).map((nota) => (
              <NotaCard key={nota.id} nota={nota} />
            ))}
          </div>
        </section>
      </div>
    </div>
  )
}
