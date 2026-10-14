import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { IconTree } from '../components/icons'
import { useDemoAtiva } from '../lib/demo'
import { useAuth } from '../lib/useAuth'

export function Login() {
  const [usuario, setUsuario] = useState('')
  const [senha, setSenha] = useState('')
  const [erro, setErro] = useState<string | null>(null)
  const { entrar } = useAuth()
  const navegar = useNavigate()
  // até saber se é a demonstração, nem formulário nem convite: senão um pisca antes do outro
  const { data: demoAtiva, isPending: conferindoDemo } = useDemoAtiva()

  async function aoEnviar(evento: FormEvent) {
    evento.preventDefault()
    setErro(null)
    try {
      await entrar(usuario, senha)
      navegar('/')
    } catch {
      setErro('Usuário ou senha inválidos.')
    }
  }

  return (
    <div className="relative flex min-h-screen items-center justify-center overflow-hidden bg-canvas px-4">
      <div
        className="pointer-events-none absolute left-1/2 top-1/2 h-[36rem] w-[36rem] -translate-x-1/2 -translate-y-1/2 rounded-full opacity-[0.15] blur-3xl"
        style={{ background: 'radial-gradient(circle, var(--color-accent), transparent 65%)' }}
      />

      {conferindoDemo ? null : demoAtiva ? (
        <ConviteDaDemo />
      ) : (
        <form
          onSubmit={aoEnviar}
          className="relative w-full max-w-sm space-y-5 rounded-xl border border-line bg-surface p-7 shadow-2xl shadow-black/40"
        >
          <Marca />

          <div className="space-y-1">
            <label className="text-sm text-ink-muted" htmlFor="usuario">
              Usuário
            </label>
            <input
              id="usuario"
              value={usuario}
              onChange={(evento) => setUsuario(evento.target.value)}
              className="w-full rounded-md border border-line bg-canvas px-3 py-2 text-ink focus:border-accent"
            />
          </div>

          <div className="space-y-1">
            <label className="text-sm text-ink-muted" htmlFor="senha">
              Senha
            </label>
            <input
              id="senha"
              type="password"
              value={senha}
              onChange={(evento) => setSenha(evento.target.value)}
              className="w-full rounded-md border border-line bg-canvas px-3 py-2 text-ink focus:border-accent"
            />
          </div>

          {erro && <p className="text-sm text-danger">{erro}</p>}

          <button
            type="submit"
            className="w-full rounded-md bg-accent px-3 py-2 font-medium text-accent-ink transition-colors hover:bg-accent-strong"
          >
            Entrar
          </button>
        </form>
      )}
    </div>
  )
}

function Marca() {
  return (
    <div className="flex flex-col items-center gap-2 pb-1 text-center">
      <span className="flex h-11 w-11 items-center justify-center rounded-full bg-accent-soft text-accent-strong">
        <IconTree className="h-6 w-6" />
      </span>
      <h1 className="text-lg font-semibold text-ink">árvore</h1>
      <p className="text-xs text-ink-faint">seu progresso, por área</p>
    </div>
  )
}

/** Na demonstração não existe conta de verdade: o login vira o convite pra entrar como visitante. */
function ConviteDaDemo() {
  return (
    <div className="relative w-full max-w-sm space-y-5 rounded-xl border border-line bg-surface p-7 text-center shadow-2xl shadow-black/40">
      <Marca />
      <p className="text-sm text-ink-muted">
        Esta é a demonstração do Árvore. Você entra numa árvore de exemplo com dez semanas de hábitos, metas e diário,
        só sua. Mexa à vontade: tudo some em 24 horas.
      </p>
      <Link
        to="/demo"
        className="block w-full rounded-md bg-accent px-3 py-2 font-medium text-accent-ink transition-colors hover:bg-accent-strong"
      >
        Entrar na demonstração
      </Link>
    </div>
  )
}
