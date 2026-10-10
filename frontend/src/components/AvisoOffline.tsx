import { useSyncExternalStore } from 'react'
import { assinarConexao, estaUsandoCopia } from '../lib/conexao'

function assinar(aoMudar: () => void) {
  window.addEventListener('online', aoMudar)
  window.addEventListener('offline', aoMudar)
  return () => {
    window.removeEventListener('online', aoMudar)
    window.removeEventListener('offline', aoMudar)
  }
}

/** Faixa no topo quando a rede cai: o que aparece é a última versão salva, e marcar não grava. */
export function AvisoOffline() {
  const online = useSyncExternalStore(assinar, () => navigator.onLine)
  const usandoCopia = useSyncExternalStore(assinarConexao, estaUsandoCopia)
  if (online && !usandoCopia) return null
  return (
    <p
      role="status"
      className="mb-6 rounded-lg border border-warning/40 bg-warning-soft px-4 py-2.5 text-sm text-warning"
    >
      Sem conexão. Você está vendo a última versão salva; marcar e editar voltam a funcionar quando a rede voltar.
    </p>
  )
}
