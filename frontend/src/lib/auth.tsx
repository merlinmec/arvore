import { useState, type ReactNode } from 'react'
import { api, TOKEN_STORAGE_KEY } from './api'
import { AuthContext } from './auth-context'

export function AuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() =>
    localStorage.getItem(TOKEN_STORAGE_KEY),
  )

  async function entrar(usuario: string, senha: string) {
    const resposta = await api.post<{ token: string }>('/auth/login', { usuario, senha })
    localStorage.setItem(TOKEN_STORAGE_KEY, resposta.data.token)
    setToken(resposta.data.token)
  }

  function sair() {
    localStorage.removeItem(TOKEN_STORAGE_KEY)
    setToken(null)
  }

  return <AuthContext.Provider value={{ token, entrar, sair }}>{children}</AuthContext.Provider>
}
