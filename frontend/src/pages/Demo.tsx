import { useEffect, useState } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { IconTree } from '../components/icons'
import { mensagemDeErro } from '../lib/arquivos'
import { criarVisitante, visitanteAindaValido } from '../lib/demo'
import { useAuth } from '../lib/useAuth'

// o servidor gratuito dorme sem acesso; acordar leva quase um minuto, e vale avisar
const AVISO_DEMORA_MS = 6000

/**
 * Destino do botão "Ver demo" do portfólio: cria um visitante com dados de exemplo e entra direto
 * na tela Hoje. Quem volta com um visitante ainda válido reaproveita o mesmo.
 */
export function Demo() {
  const { token, entrarComToken } = useAuth()
  const pronto = visitanteAindaValido(token)
  const [erro, setErro] = useState<string | null>(null)
  const [demorando, setDemorando] = useState(false)
  const [tentativa, setTentativa] = useState(0)

  useEffect(() => {
    // com o token novo guardado, a tela vira o redirecionamento lá embaixo
    if (pronto) return
    let ativo = true
    const aviso = window.setTimeout(() => setDemorando(true), AVISO_DEMORA_MS)
    criarVisitante()
      .then((novo) => {
        if (ativo) entrarComToken(novo)
      })
      .catch((falha) => {
        if (ativo) setErro(mensagemDeErro(falha, 'Não foi possível abrir a demonstração agora.'))
      })
      .finally(() => window.clearTimeout(aviso))
    return () => {
      ativo = false
      window.clearTimeout(aviso)
    }
  }, [pronto, entrarComToken, tentativa])

  function tentarDeNovo() {
    setErro(null)
    setDemorando(false)
    setTentativa((n) => n + 1)
  }

  if (pronto) return <Navigate to="/" replace />

  return (
    <div className="relative flex min-h-screen items-center justify-center overflow-hidden bg-canvas px-4">
      <div
        className="pointer-events-none absolute left-1/2 top-1/2 h-[36rem] w-[36rem] -translate-x-1/2 -translate-y-1/2 rounded-full opacity-[0.15] blur-3xl"
        style={{ background: 'radial-gradient(circle, var(--color-accent), transparent 65%)' }}
      />

      <div
        className="relative w-full max-w-sm space-y-4 rounded-xl border border-line bg-surface p-7 text-center shadow-2xl shadow-black/40"
        aria-live="polite"
      >
        <span className="mx-auto flex h-11 w-11 items-center justify-center rounded-full bg-accent-soft text-accent-strong">
          <IconTree className={`h-6 w-6 ${erro ? '' : 'motion-safe:animate-pulse'}`} />
        </span>

        {erro ? (
          <>
            <h1 className="text-lg font-semibold text-ink">A demonstração não abriu</h1>
            <p className="text-sm text-ink-muted">{erro}</p>
            <div className="flex flex-col gap-2 pt-1">
              <button
                type="button"
                onClick={tentarDeNovo}
                className="w-full rounded-md bg-accent px-3 py-2 font-medium text-accent-ink transition-colors hover:bg-accent-strong"
              >
                Tentar de novo
              </button>
              <Link to="/login" className="text-sm text-ink-faint transition-colors hover:text-ink-muted">
                Voltar
              </Link>
            </div>
          </>
        ) : (
          <>
            <h1 className="text-lg font-semibold text-ink">Preparando sua árvore de exemplo</h1>
            <p className="text-sm text-ink-muted">
              Dez semanas de hábitos, metas e diário, só suas. Mexa à vontade: tudo some em 24 horas.
            </p>
            {demorando && (
              <p className="text-xs text-ink-faint">
                O servidor gratuito estava dormindo e leva até um minuto para acordar. Já está quase.
              </p>
            )}
          </>
        )}
      </div>
    </div>
  )
}
