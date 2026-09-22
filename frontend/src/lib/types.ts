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
}

export interface ObjetivoComAtividades {
  id: string
  areaId: string
  descricao: string
  prazo: string | null
  status: StatusObjetivo
  progresso: Progresso
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
  objetivoDescricao: string
  areaId: string
  areaNome: string
  areaCorTema: string | null
  concluidoHoje: boolean
}
