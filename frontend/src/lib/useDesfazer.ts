import { useContext } from 'react'
import { DesfazerContext } from './desfazer-context'

export function useDesfazer() {
  const contexto = useContext(DesfazerContext)
  if (!contexto) {
    throw new Error('useDesfazer precisa ser usado dentro de um DesfazerProvider')
  }
  return contexto
}
