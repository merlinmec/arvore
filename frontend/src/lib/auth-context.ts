import { createContext } from 'react'

export interface AuthContextValue {
  token: string | null
  entrar: (usuario: string, senha: string) => Promise<void>
  /** Na demonstração: guarda o token do visitante recém-criado. */
  entrarComToken: (token: string) => void
  sair: () => void
}

export const AuthContext = createContext<AuthContextValue | null>(null)
