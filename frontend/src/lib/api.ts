import axios from 'axios'
import { registrarResposta } from './conexao'

export const TOKEN_STORAGE_KEY = 'arvore.token'

export const api = axios.create({
  baseURL: '/api',
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_STORAGE_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (resposta) => {
    registrarResposta(resposta.headers['x-arvore-copia'] === '1')
    return resposta
  },
  (erro) => {
    // sem resposta nenhuma é falha de rede: o servidor não foi alcançado
    if (!erro.response) registrarResposta(true)
    if (erro.response?.status === 401) {
      localStorage.removeItem(TOKEN_STORAGE_KEY)
      if (window.location.pathname !== '/login') {
        window.location.assign('/login')
      }
    }
    return Promise.reject(erro)
  },
)
