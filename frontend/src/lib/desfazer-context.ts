import { createContext } from 'react'

/** Uma exclusão adiada: some da tela na hora e só vai pra API quando o prazo pra desfazer acaba. */
export interface Exclusao {
  /** Identifica o item escondido enquanto a exclusão está pendente. */
  id: string
  mensagem: string
  executar: () => Promise<unknown>
}

export interface DesfazerContextValue {
  excluir: (exclusao: Exclusao) => void
  pendente: (id: string) => boolean
}

export const DesfazerContext = createContext<DesfazerContextValue | null>(null)
