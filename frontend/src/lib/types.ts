export interface Progresso {
  planejado: number
  concluido: number
  percentual: number | null
}

export interface AreaArvore {
  id: string
  nome: string
  descricaoGeral: string | null
  corTema: string | null
  wallpaperUrl: string | null
  ordem: number
  progresso: Progresso
  filhos: AreaArvore[]
}

export type StatusObjetivo = 'ATIVO' | 'CONCLUIDO' | 'PAUSADO' | 'ARQUIVADO'

export type DiaSemana =
  | 'SEGUNDA'
  | 'TERCA'
  | 'QUARTA'
  | 'QUINTA'
  | 'SEXTA'
  | 'SABADO'
  | 'DOMINGO'

export const DIAS_SEMANA: { valor: DiaSemana; label: string }[] = [
  { valor: 'SEGUNDA', label: 'Seg' },
  { valor: 'TERCA', label: 'Ter' },
  { valor: 'QUARTA', label: 'Qua' },
  { valor: 'QUINTA', label: 'Qui' },
  { valor: 'SEXTA', label: 'Sex' },
  { valor: 'SABADO', label: 'Sáb' },
  { valor: 'DOMINGO', label: 'Dom' },
]

export interface Atividade {
  id: string
  objetivoId: string
  nome: string
  diasAgendados: DiaSemana[]
  /** Preenchido quando a atividade segue uma meta semanal em vez de dias fixos. */
  vezesPorSemana: number | null
}

export type SituacaoMeta = 'CONCLUIDA' | 'ADIANTADA' | 'NO_RITMO' | 'ATRASADA' | 'VENCIDA' | 'SEM_PRAZO'

/** Meta quantitativa do objetivo; esperadoHoje e ritmoSemanal só existem com prazo. */
export interface Meta {
  valor: number
  unidade: string
  acumulado: number
  percentual: number
  restante: number
  esperadoHoje: number | null
  ritmoSemanal: number | null
  situacao: SituacaoMeta
}

export interface ObjetivoComAtividades {
  id: string
  areaId: string
  descricao: string
  prazo: string | null
  status: StatusObjetivo
  progresso: Progresso
  meta: Meta | null
  atividades: Atividade[]
}

export interface Nota {
  id: string
  areaId: string
  titulo: string
  corpo: string
  tags: string[]
  criadoEm: string
  atualizadoEm: string
}

export interface NotaComArea {
  id: string
  areaId: string
  areaNome: string
  titulo: string
  corpo: string
  tags: string[]
  atualizadoEm: string
}

export interface ItemHoje {
  atividadeId: string
  atividadeNome: string
  objetivoId: string
  objetivoDescricao: string
  areaId: string
  areaNome: string
  areaCorTema: string | null
  concluido: boolean
  streakAtual: number
  /** Só nas atividades com meta semanal: a meta e quantas vezes já foram feitas na semana até o dia. */
  metaSemanal: number | null
  feitasNaSemana: number | null
  /** Só quando o objetivo tem meta quantitativa: a unidade e o valor registrado nesse dia. */
  metaUnidade: string | null
  valor: number | null
}

export interface DiaHoje {
  data: string
  hoje: string
  editavel: boolean
  itens: ItemHoje[]
}

export interface DiaConsistencia {
  data: string
  planejado: number
  concluido: number
}

export interface SerieDia extends DiaConsistencia {
  concluidoPorArea: { areaId: string; concluido: number }[]
}

export interface AreaConsistencia {
  areaId: string
  nome: string
  corTema: string | null
  planejado: number
  concluido: number
  taxa: number | null
}

export interface DiaSemanaConsistencia {
  dia: DiaSemana
  planejado: number
  concluido: number
  taxa: number | null
}

export interface AtividadeConsistencia {
  atividadeId: string
  nome: string
  objetivoDescricao: string
  areaId: string
  areaNome: string
  areaCorTema: string | null
  planejado: number
  concluido: number
  taxa: number | null
  streakAtual: number
  maiorSequencia: number
}

export interface ResumoInsights {
  inicio: string
  fim: string
  planejado: number
  concluido: number
  taxa: number | null
  taxaPeriodoAnterior: number | null
  diasComPlano: number
  diasPerfeitos: number
  serie: SerieDia[]
  porArea: AreaConsistencia[]
  porDiaSemana: DiaSemanaConsistencia[]
  atividades: AtividadeConsistencia[]
}
