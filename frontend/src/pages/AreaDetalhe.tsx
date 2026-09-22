import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { BarraProgresso } from '../components/BarraProgresso'
import { EditarAreaForm } from '../components/EditarAreaForm'
import { IconChevronLeft, IconPencil, IconTrash } from '../components/icons'
import { NotaCard } from '../components/NotaCard'
import { NovaNotaForm } from '../components/NovaNotaForm'
import { NovoObjetivoForm } from '../components/NovoObjetivoForm'
import { ObjetivoCard } from '../components/ObjetivoCard'
import { api } from '../lib/api'
import { encontrarArea } from '../lib/arvore'
import type { AreaArvore, Nota, ObjetivoComAtividades } from '../lib/types'

export function AreaDetalhe() {
  const { id } = useParams<{ id: string }>()
  const navegar = useNavigate()
  const queryClient = useQueryClient()
  const [editando, setEditando] = useState(false)

  const { data: areas } = useQuery({
    queryKey: ['areas'],
    queryFn: async () => (await api.get<AreaArvore[]>('/areas')).data,
  })

  const area = id ? encontrarArea(areas ?? [], id) : null

  const excluirArea = useMutation({
    mutationFn: () => api.delete(`/areas/${id}`),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['areas'] })
      navegar('/areas')
    },
  })

  const { data: objetivos, isLoading: carregandoObjetivos } = useQuery({
    queryKey: ['objetivos', id],
    queryFn: async () => (await api.get<ObjetivoComAtividades[]>(`/areas/${id}/objetivos`)).data,
    enabled: Boolean(id),
  })

  const { data: notas, isLoading: carregandoNotas } = useQuery({
    queryKey: ['notas', id],
    queryFn: async () => (await api.get<Nota[]>(`/areas/${id}/notas`)).data,
    enabled: Boolean(id),
  })

  if (!area) {
    return <p className="text-ink-muted">Área não encontrada.</p>
  }

  return (
    <div className="space-y-8">
      <Link
        to="/areas"
        className="inline-flex items-center gap-1 text-sm text-ink-faint transition-colors hover:text-ink-muted"
      >
        <IconChevronLeft className="h-4 w-4" />
        Áreas
      </Link>

      {editando ? (
        <EditarAreaForm area={area} aoCancelar={() => setEditando(false)} />
      ) : (
        <div
          className="relative overflow-hidden rounded-xl border border-line"
          style={
            area.wallpaperUrl
              ? {
                  backgroundImage: `url(${area.wallpaperUrl})`,
                  backgroundSize: 'cover',
                  backgroundPosition: 'center',
                }
              : undefined
          }
        >
          <div
            className={
              area.wallpaperUrl
                ? 'bg-gradient-to-t from-canvas via-canvas/85 to-canvas/40 backdrop-blur-[1px]'
                : 'bg-surface'
            }
          >
            <div className="flex items-start justify-between gap-4 p-6">
              <div className="min-w-0">
                <div className="flex items-center gap-2.5">
                  <span
                    className="h-3 w-3 shrink-0 rounded-full"
                    style={{ backgroundColor: area.corTema ?? 'var(--color-ink-faint)' }}
                  />
                  <h1 className="truncate text-2xl font-semibold text-ink">{area.nome}</h1>
                </div>
                {area.descricaoGeral && (
                  <p className="mt-2 max-w-lg text-sm text-ink-muted">{area.descricaoGeral}</p>
                )}
                <div className="mt-4 max-w-xs">
                  <BarraProgresso progresso={area.progresso} corTema={area.corTema} />
                </div>
              </div>
              <div className="flex shrink-0 gap-1">
                <button
                  type="button"
                  onClick={() => setEditando(true)}
                  aria-label="Editar área"
                  title="Editar área"
                  className="rounded-full p-2 text-ink-faint transition-colors hover:bg-surface-2 hover:text-ink-muted"
                >
                  <IconPencil className="h-4 w-4" />
                </button>
                <button
                  type="button"
                  onClick={() => {
                    if (
                      window.confirm(
                        `Excluir a área "${area.nome}" e tudo dentro dela (subáreas, objetivos, notas)?`,
                      )
                    ) {
                      excluirArea.mutate()
                    }
                  }}
                  aria-label="Excluir área"
                  title="Excluir área"
                  className="rounded-full p-2 text-ink-faint transition-colors hover:bg-danger-soft hover:text-danger"
                >
                  <IconTrash className="h-4 w-4" />
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      <section className="space-y-4">
        <div className="flex items-center gap-2">
          <h2 className="text-lg font-medium text-ink">Objetivos</h2>
          {(objetivos ?? []).length > 0 && (
            <span className="tabular-nums text-sm text-ink-faint">{objetivos?.length}</span>
          )}
        </div>

        <NovoObjetivoForm areaId={area.id} />

        {carregandoObjetivos && <p className="text-sm text-ink-faint">Carregando objetivos...</p>}
        {!carregandoObjetivos && (objetivos ?? []).length === 0 && (
          <p className="text-sm text-ink-faint">Nenhum objetivo criado ainda.</p>
        )}

        <div className="space-y-3">
          {(objetivos ?? []).map((objetivo) => (
            <ObjetivoCard key={objetivo.id} objetivo={objetivo} />
          ))}
        </div>
      </section>

      <section className="space-y-4">
        <div className="flex items-center gap-2">
          <h2 className="text-lg font-medium text-ink">Notas</h2>
          {(notas ?? []).length > 0 && (
            <span className="tabular-nums text-sm text-ink-faint">{notas?.length}</span>
          )}
        </div>

        <NovaNotaForm areaId={area.id} />

        {carregandoNotas && <p className="text-sm text-ink-faint">Carregando notas...</p>}
        {!carregandoNotas && (notas ?? []).length === 0 && (
          <p className="text-sm text-ink-faint">Nenhuma nota criada ainda.</p>
        )}

        <div className="space-y-3">
          {(notas ?? []).map((nota) => (
            <NotaCard key={nota.id} nota={nota} />
          ))}
        </div>
      </section>
    </div>
  )
}
