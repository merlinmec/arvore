import { useEffect, useState } from 'react'
import { hojeIso } from './datas'
import { useDia } from './queries'
import type { ItemHoje } from './types'

const CHAVE_AVISADOS = 'arvore.lembretes.avisados'
// aberto bem depois do horário, o app não dispara uma pilha de lembretes velhos
const JANELA_MINUTOS = 60

/** "07:30:00" ou "07:30" → "07:30". */
export function horario(lembrete: string) {
  return lembrete.slice(0, 5)
}

function minutosDoDia(hhmm: string) {
  const [horas, minutos] = hhmm.split(':').map(Number)
  return horas * 60 + minutos
}

function lerAvisados(hoje: string): Set<string> {
  try {
    const salvo = JSON.parse(localStorage.getItem(CHAVE_AVISADOS) ?? 'null') as { data: string; ids: string[] } | null
    return new Set(salvo?.data === hoje ? salvo.ids : [])
  } catch {
    return new Set()
  }
}

function salvarAvisados(hoje: string, ids: Set<string>) {
  try {
    localStorage.setItem(CHAVE_AVISADOS, JSON.stringify({ data: hoje, ids: [...ids] }))
  } catch {
    // sem localStorage o pior caso é avisar de novo ao recarregar
  }
}

/** Lembretes que já passaram da hora (até uma hora atrás), de atividades ainda não feitas hoje. */
export function lembretesVencidos(itens: ItemHoje[], agora: Date, avisados: Set<string>): ItemHoje[] {
  const minutoAtual = agora.getHours() * 60 + agora.getMinutes()
  return itens.filter((item) => {
    if (!item.lembrete || item.negativa || item.concluido || avisados.has(item.atividadeId)) return false
    const atraso = minutoAtual - minutosDoDia(horario(item.lembrete))
    return atraso >= 0 && atraso <= JANELA_MINUTOS
  })
}

async function notificar(item: ItemHoje) {
  const titulo = item.atividadeNome
  const opcoes: NotificationOptions = {
    body: `${horario(item.lembrete!)} · ${item.objetivoDescricao}`,
    tag: `lembrete-${item.atividadeId}`,
    icon: '/icone-192.png',
    data: { url: '/' },
  }
  // pelo service worker a notificação funciona também com o app instalado e em segundo plano
  const registro = await navigator.serviceWorker?.getRegistration()
  if (registro) {
    await registro.showNotification(titulo, opcoes)
  } else {
    const notificacao = new Notification(titulo, opcoes)
    notificacao.onclick = () => window.focus()
  }
}

/**
 * Dispara os lembretes do dia enquanto o app está aberto (numa aba ou instalado). Não há servidor de
 * push: com o app fechado, nada chega. Confere a cada 30 s.
 */
export function useLembretes() {
  const [hoje, setHoje] = useState(hojeIso)
  const { data: dia } = useDia(hoje)

  useEffect(() => {
    function verificar() {
      const agora = new Date()
      const dataAtual = hojeIso()
      if (dataAtual !== hoje) {
        setHoje(dataAtual)
        return
      }
      if (!dia || !('Notification' in window) || Notification.permission !== 'granted') return
      const avisados = lerAvisados(dataAtual)
      const vencidos = lembretesVencidos(dia.itens, agora, avisados)
      if (vencidos.length === 0) return
      vencidos.forEach((item) => {
        avisados.add(item.atividadeId)
        void notificar(item)
      })
      salvarAvisados(dataAtual, avisados)
    }
    verificar()
    const intervalo = window.setInterval(verificar, 30_000)
    return () => window.clearInterval(intervalo)
  }, [dia, hoje])
}
