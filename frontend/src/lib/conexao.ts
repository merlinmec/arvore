/**
 * Se a última resposta da API veio da rede ou da cópia salva pelo service worker. navigator.onLine
 * sozinho engana (Wi-Fi conectado sem internet, servidor fora do ar), então o aviso de offline
 * também olha pra isso.
 */
let usandoCopia = false
const ouvintes = new Set<() => void>()

export function registrarResposta(veioDaCopia: boolean) {
  if (veioDaCopia === usandoCopia) return
  usandoCopia = veioDaCopia
  ouvintes.forEach((ouvinte) => ouvinte())
}

export function assinarConexao(ouvinte: () => void) {
  ouvintes.add(ouvinte)
  return () => {
    ouvintes.delete(ouvinte)
  }
}

export function estaUsandoCopia() {
  return usandoCopia
}
