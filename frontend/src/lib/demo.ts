import { useQuery } from '@tanstack/react-query'
import { api } from './api'

/**
 * A demonstração pública (perfil demo do backend): cada visitante ganha uma árvore de exemplo só
 * dele, num token que vence em 24 h junto com os dados. Fora da demo, /api/demo responde 404.
 */

interface ConteudoDoToken {
  esquema?: string
  exp?: number
}

function lerToken(token: string): ConteudoDoToken | null {
  try {
    const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
    return JSON.parse(atob(base64)) as ConteudoDoToken
  } catch {
    return null
  }
}

/** Token de visitante: só ele carrega o schema da demonstração. */
export function ehVisitante(token: string | null): boolean {
  return !!token && !!lerToken(token)?.esquema
}

export function visitanteAindaValido(token: string | null): boolean {
  const exp = token ? lerToken(token)?.exp : undefined
  return ehVisitante(token) && !!exp && exp * 1000 > Date.now()
}

export function useDemoAtiva() {
  return useQuery({
    queryKey: ['demo', 'status'],
    queryFn: async () => (await api.get<{ ativo: boolean }>('/demo/status')).data.ativo,
    retry: false,
    staleTime: Infinity,
  })
}

let criando: Promise<string> | null = null

/**
 * Cria o visitante e devolve o token. Duas chamadas seguidas (o StrictMode monta a tela duas vezes)
 * dividem o mesmo pedido, senão cada uma criaria um visitante.
 */
export function criarVisitante(): Promise<string> {
  criando ??= api
    .post<{ token: string }>('/demo/sessao')
    .then((resposta) => resposta.data.token)
    .finally(() => {
      criando = null
    })
  return criando
}
