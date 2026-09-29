import { useLayoutEffect, useMemo, useRef, useState, type MouseEvent } from 'react'
import { degrauDaTaxa, taxaDe } from '../../lib/cores'
import { diferencaEmDias, formatarLongo, formatarMes, inicioDaSemana, paraData } from '../../lib/datas'
import type { DiaConsistencia } from '../../lib/types'
import { useLargura } from '../../lib/useLargura'
import { Dica } from './Dica'

const VAO = 3
const PASSO_MINIMO = 13
const PASSO_MAXIMO = 20

const MARGEM_ESQUERDA = 28
const MARGEM_TOPO = 18
const ROTULOS_LINHA = ['Seg', '', 'Qua', '', 'Sex', '', '']

interface Celula {
  dia: DiaConsistencia
  coluna: number
  linha: number
  futuro: boolean
}

interface Foco {
  x: number
  y: number
  celula: Celula
}

/**
 * Grade de consistência estilo contribuições do GitHub: uma coluna por semana (segunda no topo),
 * cor pela taxa do dia na rampa sequencial. Dia sem plano fica só com contorno, pra "não tinha
 * nada" nunca parecer "falhei".
 */
export function Heatmap({ dias, hoje }: { dias: DiaConsistencia[]; hoje: string }) {
  const externoRef = useRef<HTMLDivElement>(null)
  const [medidaRef, larguraDisponivel] = useLargura<HTMLDivElement>()
  const rolagemRef = useRef<HTMLDivElement>(null)
  const [foco, setFoco] = useState<Foco | null>(null)

  const { celulas, colunas, meses } = useMemo(() => {
    if (dias.length === 0) {
      return { celulas: [] as Celula[], colunas: 0, meses: [] as { coluna: number; rotulo: string }[] }
    }
    const segundaInicial = inicioDaSemana(dias[0].data)
    const lista = dias.map((dia) => {
      const deslocamento = diferencaEmDias(segundaInicial, dia.data)
      return {
        dia,
        coluna: Math.floor(deslocamento / 7),
        linha: deslocamento % 7,
        futuro: dia.data > hoje,
      }
    })
    const totalColunas = lista[lista.length - 1].coluna + 1

    const rotulosMes: { coluna: number; rotulo: string }[] = []
    let mesAnterior = -1
    for (const celula of lista) {
      if (celula.linha !== 0) continue
      const mes = paraData(celula.dia.data).getMonth()
      if (mes !== mesAnterior) {
        // evita rótulos colados quando o primeiro mês aparece só por uma semana
        if (rotulosMes.length === 0 || celula.coluna - rotulosMes[rotulosMes.length - 1].coluna >= 3) {
          rotulosMes.push({ coluna: celula.coluna, rotulo: formatarMes(celula.dia.data) })
        }
        mesAnterior = mes
      }
    }
    return { celulas: lista, colunas: totalColunas, meses: rotulosMes }
  }, [dias, hoje])

  // abre já rolado pro presente, que é o que interessa primeiro
  useLayoutEffect(() => {
    const rolagem = rolagemRef.current
    if (rolagem) {
      rolagem.scrollLeft = rolagem.scrollWidth
    }
  }, [colunas, larguraDisponivel])

  function aoPassar(evento: MouseEvent<SVGRectElement>, celula: Celula) {
    const externo = externoRef.current?.getBoundingClientRect()
    const alvo = evento.currentTarget.getBoundingClientRect()
    if (!externo) return
    setFoco({ x: alvo.left - externo.left + alvo.width / 2, y: alvo.top - externo.top, celula })
  }

  // a grade estica pra ocupar o bloco; abaixo do mínimo, rola na horizontal
  const passo = Math.min(
    PASSO_MAXIMO,
    Math.max(PASSO_MINIMO, Math.floor((larguraDisponivel - MARGEM_ESQUERDA) / Math.max(colunas, 1))),
  )
  const tamanhoCelula = passo - VAO
  const largura = MARGEM_ESQUERDA + colunas * passo
  const altura = MARGEM_TOPO + 7 * passo
  const diasComPlano = celulas.filter((c) => !c.futuro && c.dia.planejado > 0)
  const perfeitos = diasComPlano.filter((c) => c.dia.concluido === c.dia.planejado).length

  return (
    <div ref={externoRef} className="relative">
      <div ref={medidaRef} className="w-full" />
      <div ref={rolagemRef} className="overflow-x-auto pb-1" onMouseLeave={() => setFoco(null)}>
        <svg
          width={largura}
          height={altura}
          role="img"
          aria-label={`Consistência diária: ${perfeitos} de ${diasComPlano.length} dias com plano foram cumpridos por completo.`}
          className="block"
        >
          {meses.map(({ coluna, rotulo }) => (
            <text
              key={`${coluna}-${rotulo}`}
              x={MARGEM_ESQUERDA + coluna * passo}
              y={10}
              className="fill-ink-faint text-[10px] capitalize"
            >
              {rotulo}
            </text>
          ))}
          {ROTULOS_LINHA.map((rotulo, linha) =>
            rotulo ? (
              <text
                key={rotulo}
                x={0}
                y={MARGEM_TOPO + linha * passo + tamanhoCelula - 2}
                className="fill-ink-faint text-[10px]"
              >
                {rotulo}
              </text>
            ) : null,
          )}
          {celulas.map((celula) => {
            const { dia, coluna, linha, futuro } = celula
            const semPlano = dia.planejado === 0
            const x = MARGEM_ESQUERDA + coluna * passo
            const y = MARGEM_TOPO + linha * passo
            const ehHoje = dia.data === hoje
            return (
              <rect
                key={dia.data}
                x={x + 0.5}
                y={y + 0.5}
                width={tamanhoCelula - 1}
                height={tamanhoCelula - 1}
                rx={2.5}
                fill={futuro || semPlano ? 'transparent' : degrauDaTaxa(taxaDe(dia.planejado, dia.concluido))}
                stroke={
                  ehHoje
                    ? 'var(--color-ink-muted)'
                    : futuro || semPlano
                      ? 'var(--color-line)'
                      : 'transparent'
                }
                strokeDasharray={futuro ? '2 2' : undefined}
                onMouseEnter={(evento) => aoPassar(evento, celula)}
              />
            )
          })}
        </svg>
      </div>

      {foco && (
        <Dica x={foco.x} y={foco.y}>
          <p className="font-medium first-letter:uppercase">{formatarLongo(foco.celula.dia.data)}</p>
          <p className="mt-0.5 text-ink-muted">
            {foco.celula.dia.planejado === 0
              ? 'Nada planejado'
              : foco.celula.futuro
                ? `${foco.celula.dia.planejado} planejada${foco.celula.dia.planejado > 1 ? 's' : ''}`
                : `${foco.celula.dia.concluido} de ${foco.celula.dia.planejado} concluídas`}
          </p>
        </Dica>
      )}

      <div className="mt-3 flex flex-wrap items-center justify-between gap-3 text-xs text-ink-faint">
        <p>
          <span className="tabular-nums text-ink-muted">{perfeitos}</span> de{' '}
          <span className="tabular-nums text-ink-muted">{diasComPlano.length}</span> dias cumpridos por completo
        </p>
        <div className="flex items-center gap-1.5" aria-hidden>
          <span>sem plano</span>
          <span className="h-2.5 w-2.5 rounded-[3px] border border-line" />
          <span className="ml-2">0%</span>
          {['--color-seq-0', '--color-seq-1', '--color-seq-2', '--color-seq-3', '--color-seq-4'].map((token) => (
            <span key={token} className="h-2.5 w-2.5 rounded-[3px]" style={{ backgroundColor: `var(${token})` }} />
          ))}
          <span>100%</span>
        </div>
      </div>
    </div>
  )
}
