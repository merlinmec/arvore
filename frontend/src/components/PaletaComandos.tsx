import { useEffect, useId, useMemo, useRef, useState, type KeyboardEvent, type ReactNode } from 'react'
import { useNavigate } from 'react-router-dom'
import { caminhoAte, achatarArvore } from '../lib/arvore'
import { useAlternarConclusao } from '../lib/conclusao'
import { corDaArea } from '../lib/cores'
import { hojeIso, somarDias } from '../lib/datas'
import { useAreas, useDia } from '../lib/queries'
import type { ItemHoje } from '../lib/types'
import { IconBranches, IconCalendar, IconChart, IconCheck, IconDatabase, IconPlus, IconSun, IconX } from './icons'

interface Comando {
  id: string
  grupo: 'Ir para' | 'Áreas' | 'Hoje'
  rotulo: string
  detalhe?: string
  /** Palavras extras pra busca, além do rótulo e do detalhe. */
  termos?: string
  icone: ReactNode
  executar: () => void
}

/** Minúsculas e sem acento: "revisao" acha "Revisão". */
function normalizar(texto: string) {
  return texto
    .normalize('NFD')
    .replace(/\p{Diacritic}/gu, '')
    .toLowerCase()
}

/** Todas as palavras da busca precisam aparecer em algum lugar do comando, em qualquer ordem. */
function combina(comando: Comando, busca: string) {
  const alvo = normalizar(`${comando.rotulo} ${comando.detalhe ?? ''} ${comando.termos ?? ''} ${comando.grupo}`)
  return normalizar(busca)
    .split(/\s+/)
    .filter(Boolean)
    .every((palavra) => alvo.includes(palavra))
}

function rotuloDoItem(item: ItemHoje) {
  if (item.negativa)
    return item.concluido ? `Desfazer recaída em ${item.atividadeNome}` : `Registrar recaída em ${item.atividadeNome}`
  return item.concluido ? `Desmarcar ${item.atividadeNome}` : `Concluir ${item.atividadeNome}`
}

/**
 * Paleta de comandos (Ctrl+K): ir pra qualquer tela ou área e marcar as atividades de hoje sem
 * tirar a mão do teclado. ↑ ↓ escolhem, Enter executa, Esc fecha.
 */
export function PaletaComandos({ aoFechar }: { aoFechar: () => void }) {
  const navegar = useNavigate()
  const hoje = hojeIso()
  const { data: areas } = useAreas()
  const { data: dia } = useDia(hoje)
  const { mutate: marcar } = useAlternarConclusao(hoje)
  const [busca, setBusca] = useState('')
  const [ativo, setAtivo] = useState(0)
  const lista = useRef<HTMLUListElement>(null)
  const idLista = useId()

  const comandos = useMemo<Comando[]>(() => {
    const ir = (para: string) => () => navegar(para)
    const telas: Comando[] = [
      { id: 'hoje', grupo: 'Ir para', rotulo: 'Hoje', icone: <IconSun />, executar: ir('/') },
      {
        id: 'ontem',
        grupo: 'Ir para',
        rotulo: 'Ontem',
        termos: 'dia anterior',
        icone: <IconSun />,
        executar: ir(`/?data=${somarDias(hoje, -1)}`),
      },
      {
        id: 'painel',
        grupo: 'Ir para',
        rotulo: 'Painel',
        termos: 'insights consistencia graficos',
        icone: <IconChart />,
        executar: ir('/painel'),
      },
      {
        id: 'revisao',
        grupo: 'Ir para',
        rotulo: 'Revisão da semana',
        icone: <IconCalendar />,
        executar: ir('/revisao'),
      },
      { id: 'areas', grupo: 'Ir para', rotulo: 'Áreas', icone: <IconBranches />, executar: ir('/areas') },
      {
        id: 'nova-area',
        grupo: 'Ir para',
        rotulo: 'Nova área',
        termos: 'criar',
        icone: <IconPlus />,
        executar: ir('/areas/nova'),
      },
      {
        id: 'dados',
        grupo: 'Ir para',
        rotulo: 'Dados',
        termos: 'exportar csv backup restaurar',
        icone: <IconDatabase />,
        executar: ir('/dados'),
      },
    ]
    const doDia: Comando[] = (dia?.itens ?? []).map((item) => ({
      id: `item-${item.atividadeId}`,
      grupo: 'Hoje',
      rotulo: rotuloDoItem(item),
      detalhe: `${item.areaNome} · ${item.objetivoDescricao}`,
      termos: item.negativa ? 'recai habito negativo' : 'marcar feito',
      icone: item.concluido ? <IconX /> : <IconCheck />,
      executar: () => marcar(item),
    }))
    const daArvore: Comando[] = achatarArvore(areas ?? []).map((area) => {
      const caminho = caminhoAte(areas ?? [], area.id)
      const cor = caminho[caminho.length - 1]?.corTema ?? null
      return {
        id: `area-${area.id}`,
        grupo: 'Áreas',
        rotulo: area.nome,
        detalhe:
          caminho.length > 1
            ? caminho
                .slice(0, -1)
                .map((parte) => parte.nome)
                .join(' / ')
            : undefined,
        icone: <span className="block h-2 w-2 rounded-full" style={{ backgroundColor: corDaArea(cor) }} />,
        executar: ir(`/areas/${area.id}`),
      }
    })
    return [...doDia, ...telas, ...daArvore]
  }, [marcar, areas, dia, hoje, navegar])

  const visiveis = busca.trim() ? comandos.filter((comando) => combina(comando, busca)) : comandos
  const indiceAtivo = Math.min(ativo, Math.max(visiveis.length - 1, 0))

  useEffect(() => {
    lista.current?.querySelector('[aria-selected="true"]')?.scrollIntoView({ block: 'nearest' })
  }, [indiceAtivo])

  function executar(comando: Comando | undefined) {
    if (!comando) return
    aoFechar()
    comando.executar()
  }

  function aoTeclar(evento: KeyboardEvent) {
    if (evento.key === 'ArrowDown') {
      evento.preventDefault()
      setAtivo((indiceAtivo + 1) % Math.max(visiveis.length, 1))
    } else if (evento.key === 'ArrowUp') {
      evento.preventDefault()
      setAtivo((indiceAtivo - 1 + visiveis.length) % Math.max(visiveis.length, 1))
    } else if (evento.key === 'Enter') {
      evento.preventDefault()
      executar(visiveis[indiceAtivo])
    } else if (evento.key === 'Escape') {
      evento.preventDefault()
      aoFechar()
    }
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-start justify-center bg-black/60 px-4 pt-[12vh] backdrop-blur-[2px]"
      onMouseDown={(evento) => {
        if (evento.target === evento.currentTarget) aoFechar()
      }}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-label="Paleta de comandos"
        className="animate-rise w-full max-w-lg overflow-hidden rounded-xl border border-line-strong bg-surface shadow-2xl shadow-black/50"
        onKeyDown={aoTeclar}
      >
        <input
          autoFocus
          role="combobox"
          aria-expanded="true"
          aria-controls={idLista}
          aria-activedescendant={visiveis[indiceAtivo] ? `${idLista}-${visiveis[indiceAtivo].id}` : undefined}
          aria-autocomplete="list"
          value={busca}
          onChange={(evento) => {
            setBusca(evento.target.value)
            setAtivo(0)
          }}
          placeholder="Ir pra uma tela ou área, marcar uma atividade…"
          className="w-full border-b border-line bg-transparent px-4 py-3.5 text-[15px] text-ink placeholder:text-ink-faint focus:outline-none"
        />
        <ul ref={lista} id={idLista} role="listbox" className="max-h-[50vh] overflow-y-auto p-1.5">
          {visiveis.length === 0 && <li className="px-3 py-6 text-center text-sm text-ink-faint">Nada encontrado.</li>}
          {visiveis.map((comando, indice) => {
            const novoGrupo = indice === 0 || visiveis[indice - 1].grupo !== comando.grupo
            const selecionado = indice === indiceAtivo
            return (
              <li key={comando.id} role="presentation">
                {novoGrupo && (
                  <p className="px-2.5 pb-1 pt-2 text-[11px] font-medium text-ink-faint">{comando.grupo}</p>
                )}
                <div
                  id={`${idLista}-${comando.id}`}
                  role="option"
                  aria-selected={selecionado}
                  onMouseMove={() => setAtivo(indice)}
                  onClick={() => executar(comando)}
                  className={`flex cursor-pointer items-center gap-3 rounded-md px-2.5 py-2 text-sm ${
                    selecionado ? 'bg-surface-3 text-ink' : 'text-ink-muted'
                  }`}
                >
                  <span className="flex h-4 w-4 shrink-0 items-center justify-center text-ink-faint [&>svg]:h-4 [&>svg]:w-4">
                    {comando.icone}
                  </span>
                  <span className="min-w-0 flex-1 truncate">{comando.rotulo}</span>
                  {comando.detalhe && (
                    <span className="max-w-[45%] truncate text-xs text-ink-faint">{comando.detalhe}</span>
                  )}
                </div>
              </li>
            )
          })}
        </ul>
        <p className="flex flex-wrap gap-x-4 gap-y-1 border-t border-line px-4 py-2 text-[11px] text-ink-faint">
          <span>
            <kbd className="font-sans text-ink-muted">↑ ↓</kbd> escolher
          </span>
          <span>
            <kbd className="font-sans text-ink-muted">Enter</kbd> executar
          </span>
          <span>
            <kbd className="font-sans text-ink-muted">Esc</kbd> fechar
          </span>
          <span className="ml-auto">
            Em Hoje: <kbd className="font-sans text-ink-muted">← →</kbd> trocam o dia,{' '}
            <kbd className="font-sans text-ink-muted">H</kbd> volta pra hoje
          </span>
        </p>
      </div>
    </div>
  )
}
