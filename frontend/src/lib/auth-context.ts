import { createContext } from 'react'

export interface AuthContextValue {
  token: string | null
  entrar: (usuario: string, senha: string) => Promise<void>
  sair: () => void
}

export const AuthContext = createContext<AuthContextValue | null>(null)
