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
  /** Hábito negativo ("não fumar"): vale todo dia, sem grade, e o registro é a recaída. */
  negativa: boolean
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
  /** Há registro nesse dia. Num hábito negativo o registro é a recaída: use cumprido(). */
  concluido: boolean
  negativa: boolean
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
  /** Força do hábito hoje (0–100, com decaimento), independente do período escolhido. */
  forca: number | null
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

export interface AtividadeRevisao {
  /** Traz a grade atual, pra ajustar a próxima semana ali mesmo. */
  atividade: Atividade
  objetivoDescricao: string
  areaId: string
  areaNome: string
  areaCorTema: string | null
  planejado: number
  concluido: number
  /** Dias passados em que algo planejado escapou; hoje ainda pode ser feito e não entra. */
  diasPerdidos: string[]
}

/** Retrato de uma semana (segunda a domingo), contado até apuradoAte. */
export interface RevisaoSemana {
  semana: string
  fim: string
  apuradoAte: string
  planejado: number
  concluido: number
  taxa: number | null
  taxaSemanaAnterior: number | null
  dias: DiaConsistencia[]
  atividades: AtividadeRevisao[]
  /** Null enquanto a semana não foi revisada. */
  nota: string | null
}

export interface Revisao {
  semana: string
  nota: string
  atualizadoEm: string
}

/** Diário de uma data; dia sem registro vem com tudo nulo. */
export interface Diario {
  data: string
  humor: number | null
  energia: number | null
  nota: string | null
}

export interface NivelConsistencia {
  nivel: number
  dias: number
  planejado: number
  concluido: number
  taxa: number | null
}

/** Taxa de conclusão por nível (1 a 5) de humor e de energia registrados no diário. */
export interface DiarioConsistencia {
  diasRegistrados: number
  porHumor: NivelConsistencia[]
  porEnergia: NivelConsistencia[]
}

/** Backup completo; o frontend só lê o necessário pra mostrar o resumo antes de restaurar. */
export interface Backup {
  versao: number
  geradoEm: string
  areas?: unknown[]
  objetivos?: unknown[]
  atividades?: unknown[]
  conclusoes?: unknown[]
  notas?: unknown[]
  revisoes?: unknown[]
  diario?: unknown[]
  sessoesFoco?: unknown[]
}

export interface ResumoBackup {
  areas: number
  notas: number
  objetivos: number
  atividades: number
  conclusoes: number
  revisoes: number
  diario: number
  sessoesFoco: number
}

/** Sessão de foco; fim nulo é a que está contando, com os minutos até agora. */
export interface SessaoFoco {
  id: string
  atividadeId: string
  atividadeNome: string
  areaId: string
  areaNome: string
  areaCorTema: string | null
  inicio: string
  fim: string | null
  minutos: number
}

export interface TempoFoco {
  inicio: string
  fim: string
  totalMinutos: number
  sessoes: number
  dias: { data: string; minutos: number }[]
  porArea: { areaId: string; nome: string; corTema: string | null; minutos: number }[]
  atividades: {
    atividadeId: string
    nome: string
    areaNome: string
    areaCorTema: string | null
    minutos: number
    sessoes: number
  }[]
}
