import axios from 'axios'

/** Salva um Blob como arquivo: a API exige o token no cabeçalho, então não dá pra usar um link direto. */
export function salvarArquivo(conteudo: Blob, nome: string) {
  const url = URL.createObjectURL(conteudo)
  const link = document.createElement('a')
  link.href = url
  link.download = nome
  document.body.appendChild(link)
  link.click()
  link.remove()
  // o clique dispara o download de forma assíncrona; libera a URL depois
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}

/** Mensagem de erro que o backend mandou ({"erro": ...}), ou um texto padrão. */
export function mensagemDeErro(erro: unknown, padrao: string): string {
  if (axios.isAxiosError(erro)) {
    const corpo = erro.response?.data as { erro?: unknown } | undefined
    if (typeof corpo?.erro === 'string') return corpo.erro
  }
  return padrao
}
