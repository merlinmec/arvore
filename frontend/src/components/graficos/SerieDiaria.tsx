import { useState } from 'react'
import { corDaArea } from '../../lib/cores'
import { formatarCurto, formatarLongo } from '../../lib/datas'
import type { AreaConsistencia, SerieDia } from '../../lib/types'
import { useLargura } from '../../lib/useLargura'
import { Dica } from './Dica'

const ALTURA_PLOTAGEM = 180
const MARGEM_ESQUERDA = 26
const MARGEM_BASE = 22
const VAO_SEGMENTO = 2

function tetoRedondo(valor: number): number {
  if (valor <= 4) return Math.max(valor, 1)
  const passo = valor <= 10 ? 2 : 5
  return Math.ceil(valor / passo) * passo
}

/**
 * Barras diárias: a barra "fantasma" é o planejado do dia; por dentro dela, o concluído empilhado
 * por área, na cor de cada área. Uma barra cheia até o topo é um dia cumprido por completo.
 */
export function SerieDiaria({
  serie,
  areas,
  hoje,
}: {
  serie: SerieDia[]
  areas: AreaConsistencia[]
  hoje: string
}) {
  const [ref, largura] = useLargura<HTMLDivElement>()
  const [indiceFoco, setIndiceFoco] = useState<number | null>(null)

  const areaPorId = new Map(areas.map((area) => [area.areaId, area]))
  const ordemArea = new Map(areas.map((area, indice) => [area.areaId, indice]))
  const teto = tetoRedondo(Math.max(1, ...serie.map((dia) => dia.planejado)))
  const larguraPlotagem = Math.max(0, largura - MARGEM_ESQUERDA)
  const passo = serie.length > 0 ? larguraPlotagem / serie.length : 0
  const larguraBarra = Math.max(3, Math.min(26, passo * 0.64))
  const escala = (valor: number) => (valor / teto) * ALTURA_PLOTAGEM
  const intervaloRotulo = Math.max(1, Math.ceil(serie.length / 7))

  const foco = indiceFoco !== null ? serie[indiceFoco] : null

  return (
    <div className="space-y-4">
      {areas.length > 1 && (
        <ul className="flex flex-wrap gap-x-4 gap-y-1.5 text-xs text-ink-muted">
          {areas.map((area) => (
            <li key={area.areaId} className="flex items-center gap-1.5">
              <span className="h-2 w-2 rounded-[2px]" style={{ backgroundColor: corDaArea(area.corTema) }} />
              {area.nome}
            </li>
          ))}
          <li className="flex items-center gap-1.5 text-ink-faint">
            <span className="h-2 w-2 rounded-[2px] bg-surface-3" />
            planejado
          </li>
        </ul>
      )}

      <div ref={ref} className="relative" onMouseLeave={() => setIndiceFoco(null)}>
        {largura > 0 && (
          <svg
            width={largura}
            height={ALTURA_PLOTAGEM + MARGEM_BASE}
            role="img"
            aria-label="Atividades concluídas por dia, empilhadas por área, sobre o total planejado do dia."
            className="block overflow-visible"
          >
            {[0, teto / 2, teto].map((valor) => {
              const y = ALTURA_PLOTAGEM - escala(valor)
              return (
                <g key={valor}>
                  <line x1={MARGEM_ESQUERDA} x2={largura} y1={y} y2={y} className="stroke-line" strokeWidth={1} />
                  <text x={0} y={y + 3.5} className="fill-ink-faint text-[10px] tabular-nums">
                    {Number.isInteger(valor) ? valor : ''}
                  </text>
                </g>
              )
            })}

            {serie.map((dia, indice) => {
              const centro = MARGEM_ESQUERDA + passo * indice + passo / 2
              const x = centro - larguraBarra / 2
              const alturaPlanejado = escala(dia.planejado)
              const emFoco = indiceFoco === indice
              const idClip = `barra-${dia.data}`
              const segmentos = [...dia.concluidoPorArea].sort(
                (a, b) => (ordemArea.get(a.areaId) ?? 99) - (ordemArea.get(b.areaId) ?? 99),
              )
              let base = ALTURA_PLOTAGEM
              return (
                <g key={dia.data}>
                  <clipPath id={idClip}>
                    <rect x={x} y={ALTURA_PLOTAGEM - alturaPlanejado} width={larguraBarra} height={alturaPlanejado} rx={3} />
                  </clipPath>
                  <rect
                    x={x}
                    y={ALTURA_PLOTAGEM - alturaPlanejado}
                    width={larguraBarra}
                    height={alturaPlanejado}
                    rx={3}
                    className={emFoco ? 'fill-line-strong' : 'fill-surface-3'}
                  />
                  <g clipPath={`url(#${idClip})`}>
                    {segmentos.map((segmento) => {
                      const altura = escala(segmento.concluido)
                      base -= altura
                      return (
                        <rect
                          key={segmento.areaId}
                          x={x}
                          y={base}
                          width={larguraBarra}
                          height={Math.max(0, altura - VAO_SEGMENTO)}
                          fill={corDaArea(areaPorId.get(segmento.areaId)?.corTema)}
                          opacity={indiceFoco === null || emFoco ? 1 : 0.55}
                        />
                      )
                    })}
                  </g>
                  {(indice % intervaloRotulo === 0 || indice === serie.length - 1) && (
                    <text
                      x={centro}
                      y={ALTURA_PLOTAGEM + 15}
                      textAnchor="middle"
                      className={`text-[10px] ${dia.data === hoje ? 'fill-ink-muted' : 'fill-ink-faint'}`}
                    >
                      {dia.data === hoje ? 'hoje' : formatarCurto(dia.data)}
                    </text>
                  )}
                  <rect
                    x={MARGEM_ESQUERDA + passo * indice}
                    y={0}
                    width={passo}
                    height={ALTURA_PLOTAGEM}
                    fill="transparent"
                    onMouseEnter={() => setIndiceFoco(indice)}
                  />
                </g>
              )
            })}
          </svg>
        )}

        {foco && indiceFoco !== null && (
          <Dica
            x={Math.min(
              Math.max(MARGEM_ESQUERDA + passo * indiceFoco + passo / 2, 90),
              Math.max(90, largura - 90),
            )}
            y={ALTURA_PLOTAGEM - escala(foco.planejado)}
          >
            <p className="font-medium first-letter:uppercase">{formatarLongo(foco.data)}</p>
            <p className="mt-0.5 text-ink-muted">
              {foco.planejado === 0
                ? 'Nada planejado'
                : `${foco.concluido} de ${foco.planejado} concluídas`}
            </p>
            {foco.concluidoPorArea.length > 0 && (
              <ul className="mt-1.5 space-y-0.5 border-t border-line pt-1.5">
                {foco.concluidoPorArea.map((segmento) => {
                  const area = areaPorId.get(segmento.areaId)
                  return (
                    <li key={segmento.areaId} className="flex items-center gap-1.5 text-ink-muted">
                      <span
                        className="h-2 w-2 shrink-0 rounded-[2px]"
                        style={{ backgroundColor: corDaArea(area?.corTema) }}
                      />
                      <span className="flex-1">{area?.nome ?? 'Área'}</span>
                      <span className="tabular-nums text-ink">{segmento.concluido}</span>
                    </li>
                  )
                })}
              </ul>
            )}
          </Dica>
        )}
      </div>
    </div>
  )
}
