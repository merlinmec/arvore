import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useRef, useState, type ReactNode } from 'react'
import { ConfirmarExclusao } from '../components/ConfirmarExclusao'
import { IconDownload, IconUpload } from '../components/icons'
import { api } from '../lib/api'
import { mensagemDeErro, salvarArquivo } from '../lib/arquivos'
import { hojeIso, somarDias } from '../lib/datas'
import type { Backup, ResumoBackup } from '../lib/types'

const PERIODOS = [
  { dias: null, rotulo: 'Tudo' },
  { dias: 30, rotulo: '30 dias' },
  { dias: 90, rotulo: '90 dias' },
  { dias: 365, rotulo: '1 ano' },
]

const formatoGerado = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'long', timeStyle: 'short' })

function Secao({ titulo, descricao, children }: { titulo: string; descricao: ReactNode; children: ReactNode }) {
  return (
    <section className="rounded-xl border border-line bg-surface p-5">
      <h2 className="text-sm font-medium text-ink">{titulo}</h2>
      <p className="mt-1 max-w-xl text-sm text-ink-muted">{descricao}</p>
      <div className="mt-4">{children}</div>
    </section>
  )
}

const botao =
  'inline-flex items-center gap-2 rounded-md border border-line px-3 py-1.5 text-sm text-ink transition-colors hover:border-line-strong hover:bg-surface-2 disabled:opacity-50'

function plural(quantidade: number, singular: string, pluralizado: string) {
  return `${quantidade} ${quantidade === 1 ? singular : pluralizado}`
}

function resumoDoBackup(backup: Backup | ResumoBackup) {
  const contar = (lista: unknown[] | number | undefined) => (typeof lista === 'number' ? lista : (lista?.length ?? 0))
  return [
    plural(contar(backup.areas), 'área', 'áreas'),
    plural(contar(backup.objetivos), 'objetivo', 'objetivos'),
    plural(contar(backup.atividades), 'atividade', 'atividades'),
    plural(contar(backup.conclusoes), 'registro', 'registros'),
    plural(contar(backup.notas), 'nota', 'notas'),
    plural(contar(backup.diario), 'dia de diário', 'dias de diário'),
    plural(contar(backup.revisoes), 'revisão', 'revisões'),
    plural(contar(backup.sessoesFoco), 'sessão de foco', 'sessões de foco'),
  ].join(', ')
}

function ExportarCsv() {
  const [dias, setDias] = useState<number | null>(null)
  const baixar = useMutation({
    mutationFn: async () => {
      const hoje = hojeIso()
      const params = dias === null ? {} : { inicio: somarDias(hoje, -(dias - 1)), fim: hoje }
      const resposta = await api.get<Blob>('/dados/conclusoes.csv', { params, responseType: 'blob' })
      salvarArquivo(resposta.data, `arvore-conclusoes-${hoje}.csv`)
    },
  })

  return (
    <Secao
      titulo="Exportar conclusões"
      descricao="Um CSV com cada atividade feita (e cada recaída de hábito negativo): data, área, objetivo, atividade e valor. Abre direto no Excel e no Google Planilhas."
    >
      <div className="flex flex-wrap items-center gap-3">
        <div className="flex rounded-md border border-line bg-canvas p-0.5" role="group" aria-label="Período do CSV">
          {PERIODOS.map((periodo) => (
            <button
              key={periodo.rotulo}
              type="button"
              aria-pressed={dias === periodo.dias}
              onClick={() => setDias(periodo.dias)}
              className={`rounded-[5px] px-2.5 py-1 text-sm transition-colors duration-150 ${
                dias === periodo.dias ? 'bg-surface-3 text-ink' : 'text-ink-faint hover:text-ink-muted'
              }`}
            >
              {periodo.rotulo}
            </button>
          ))}
        </div>
        <button type="button" onClick={() => baixar.mutate()} disabled={baixar.isPending} className={botao}>
          <IconDownload className="h-4 w-4" />
          {baixar.isPending ? 'Gerando…' : 'Baixar CSV'}
        </button>
      </div>
      {baixar.isError && <p className="mt-3 text-sm text-danger">Não foi possível gerar o CSV.</p>}
    </Secao>
  )
}

function BaixarBackup() {
  const baixar = useMutation({
    mutationFn: async () => {
      const resposta = await api.get<Blob>('/dados/backup', { responseType: 'blob' })
      salvarArquivo(resposta.data, `arvore-backup-${hojeIso()}.json`)
    },
  })

  return (
    <Secao
      titulo="Backup"
      descricao="Tudo num arquivo JSON: áreas, objetivos, atividades com o histórico da grade, registros, notas, diário e revisões. Guarde de vez em quando; ele também serve pra levar os dados pra outra instalação."
    >
      <button type="button" onClick={() => baixar.mutate()} disabled={baixar.isPending} className={botao}>
        <IconDownload className="h-4 w-4" />
        {baixar.isPending ? 'Gerando…' : 'Baixar backup'}
      </button>
      {baixar.isError && <p className="mt-3 text-sm text-danger">Não foi possível gerar o backup.</p>}
    </Secao>
  )
}

function Restaurar() {
  const queryClient = useQueryClient()
  const campo = useRef<HTMLInputElement>(null)
  const [arquivo, setArquivo] = useState<{ nome: string; backup: Backup } | null>(null)
  const [erroLeitura, setErroLeitura] = useState<string | null>(null)

  const restaurar = useMutation({
    mutationFn: async (backup: Backup) => (await api.put<ResumoBackup>('/dados/backup', backup)).data,
    onSuccess: async () => {
      setArquivo(null)
      // tudo mudou: nenhuma tela pode continuar mostrando o que estava em cache
      await queryClient.invalidateQueries()
    },
  })

  async function aoEscolher(lista: FileList | null) {
    const escolhido = lista?.[0]
    if (campo.current) campo.current.value = ''
    if (!escolhido) return
    setErroLeitura(null)
    restaurar.reset()
    try {
      const backup = JSON.parse(await escolhido.text()) as Backup
      if (typeof backup !== 'object' || backup === null || typeof backup.versao !== 'number') {
        throw new Error('formato')
      }
      setArquivo({ nome: escolhido.name, backup })
    } catch {
      setArquivo(null)
      setErroLeitura(`"${escolhido.name}" não parece um backup do Árvore.`)
    }
  }

  return (
    <Secao
      titulo="Restaurar backup"
      descricao={
        <>
          Substitui <span className="text-ink">todos</span> os dados atuais pelos do arquivo. Se o arquivo tiver algum
          problema, nada é alterado.
        </>
      }
    >
      <input
        ref={campo}
        type="file"
        accept="application/json,.json"
        className="sr-only"
        id="arquivo-backup"
        onChange={(evento) => aoEscolher(evento.target.files)}
      />
      {!arquivo && (
        <label htmlFor="arquivo-backup" className={`${botao} cursor-pointer`}>
          <IconUpload className="h-4 w-4" />
          Escolher arquivo
        </label>
      )}

      {arquivo && (
        <div className="space-y-3">
          <div className="rounded-lg border border-line bg-canvas px-4 py-3 text-sm">
            <p className="text-ink">{arquivo.nome}</p>
            <p className="mt-1 text-ink-muted">
              {arquivo.backup.geradoEm ? `Gerado em ${formatoGerado.format(new Date(arquivo.backup.geradoEm))}. ` : ''}
              {resumoDoBackup(arquivo.backup)}.
            </p>
          </div>
          <ConfirmarExclusao
            pergunta="Apagar os dados atuais e colocar os do backup no lugar?"
            rotuloConfirmar="Restaurar"
            ocupado={restaurar.isPending}
            aoConfirmar={() => restaurar.mutate(arquivo.backup)}
            aoCancelar={() => {
              setArquivo(null)
              restaurar.reset()
            }}
          />
        </div>
      )}

      {erroLeitura && <p className="mt-3 text-sm text-danger">{erroLeitura}</p>}
      {restaurar.isError && (
        <p className="mt-3 text-sm text-danger">
          {mensagemDeErro(restaurar.error, 'Não foi possível restaurar.')} Os dados atuais continuam como estavam.
        </p>
      )}
      {restaurar.isSuccess && (
        <p className="mt-3 text-sm text-accent-strong" role="status">
          Backup restaurado: {resumoDoBackup(restaurar.data)}.
        </p>
      )}
    </Secao>
  )
}

export function Dados() {
  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-3xl font-semibold tracking-tight text-ink">Dados</h1>
        <p className="mt-1 text-sm text-ink-faint">Exportar, guardar e restaurar o que você registrou.</p>
      </header>
      <ExportarCsv />
      <BaixarBackup />
      <Restaurar />
    </div>
  )
}
