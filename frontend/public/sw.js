// Service worker do Árvore: deixa o app abrir sem rede, com a última versão salva da tela Hoje.
//
// - assets com hash no nome (/assets/*): cache primeiro, porque nunca mudam;
// - navegação: rede primeiro, caindo no index.html salvo quando não há conexão;
// - GETs da API que a tela Hoje e a lateral usam: rede primeiro, caindo na última resposta salva.
// Escritas (POST, PUT, DELETE) passam direto: offline elas falham e a tela desfaz a mudança otimista.

const VERSAO = 'v1'
const CACHE_APP = `arvore-app-${VERSAO}`
const CACHE_API = `arvore-api-${VERSAO}`
const API_OFFLINE = [
  /^\/api\/hoje/,
  /^\/api\/areas$/,
  /^\/api\/insights\/historico/,
  /^\/api\/diario\//,
  /^\/api\/foco\/atual$/,
]

self.addEventListener('install', (evento) => {
  evento.waitUntil(
    caches
      .open(CACHE_APP)
      .then((cache) => cache.addAll(['/', '/index.html', '/manifest.webmanifest', '/icone.svg', '/icone-192.png']))
      .then(() => self.skipWaiting()),
  )
})

self.addEventListener('activate', (evento) => {
  evento.waitUntil(
    caches
      .keys()
      .then((nomes) =>
        Promise.all(
          nomes
            .filter((nome) => nome.startsWith('arvore-') && !nome.endsWith(VERSAO))
            .map((nome) => caches.delete(nome)),
        ),
      )
      .then(() => self.clients.claim()),
  )
})

// clicar num lembrete traz o app pra frente (ou abre, se estava fechado)
self.addEventListener('notificationclick', (evento) => {
  evento.notification.close()
  const destino = evento.notification.data?.url ?? '/'
  evento.waitUntil(
    self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then((janelas) => {
      const aberta = janelas.find((janela) => new URL(janela.url).origin === self.location.origin)
      return aberta ? aberta.focus() : self.clients.openWindow(destino)
    }),
  )
})

// o app pede pra esquecer os dados da API ao sair da conta
self.addEventListener('message', (evento) => {
  if (evento.data === 'limpar-api') evento.waitUntil(caches.delete(CACHE_API))
})

async function redePrimeiro(requisicao, nomeCache, reserva) {
  const cache = await caches.open(nomeCache)
  try {
    const resposta = await fetch(requisicao)
    if (resposta.ok) cache.put(reserva ?? requisicao, resposta.clone())
    return resposta
  } catch (erro) {
    const salva = await cache.match(reserva ?? requisicao)
    if (!salva) throw erro
    // avisa o app que é a cópia salva, não a resposta de agora
    const cabecalhos = new Headers(salva.headers)
    cabecalhos.set('X-Arvore-Copia', '1')
    return new Response(salva.body, { status: salva.status, statusText: salva.statusText, headers: cabecalhos })
  }
}

async function cachePrimeiro(requisicao) {
  const cache = await caches.open(CACHE_APP)
  const salva = await cache.match(requisicao)
  if (salva) return salva
  const resposta = await fetch(requisicao)
  if (resposta.ok) cache.put(requisicao, resposta.clone())
  return resposta
}

self.addEventListener('fetch', (evento) => {
  const requisicao = evento.request
  if (requisicao.method !== 'GET') return
  const url = new URL(requisicao.url)
  if (url.origin !== self.location.origin) return

  if (requisicao.mode === 'navigate') {
    // toda rota do SPA é o mesmo index.html
    evento.respondWith(redePrimeiro(requisicao, CACHE_APP, '/index.html'))
  } else if (url.pathname.startsWith('/assets/')) {
    evento.respondWith(cachePrimeiro(requisicao))
  } else if (API_OFFLINE.some((padrao) => padrao.test(url.pathname))) {
    evento.respondWith(redePrimeiro(requisicao, CACHE_API))
  }
})
