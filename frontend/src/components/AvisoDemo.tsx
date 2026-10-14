import { ehVisitante } from '../lib/demo'
import { useAuth } from '../lib/useAuth'

/** Faixa no topo pra quem entrou pela demonstração: os dados não são de ninguém e não ficam. */
export function AvisoDemo() {
  const { token } = useAuth()
  if (!ehVisitante(token)) return null
  return (
    <p className="mb-6 rounded-lg border border-accent/30 bg-accent-soft px-4 py-2.5 text-sm text-accent-strong">
      Demonstração: os dados são fictícios, só você os vê e eles somem em 24 horas.
    </p>
  )
}
