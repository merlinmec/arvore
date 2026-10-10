import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter, Route, Routes } from 'react-router-dom'
import { Layout } from './components/Layout'
import { ProtectedRoute } from './components/ProtectedRoute'
import { AuthProvider } from './lib/auth'
import { DesfazerProvider } from './lib/desfazer'
import { AreaDetalhe } from './pages/AreaDetalhe'
import { Dados } from './pages/Dados'
import { Areas } from './pages/Areas'
import { Hoje } from './pages/Hoje'
import { Login } from './pages/Login'
import { NovaArea } from './pages/NovaArea'
import { Painel } from './pages/Painel'
import { Revisao } from './pages/Revisao'
import { TagDetalhe } from './pages/TagDetalhe'

// offlineFirst: sem rede, a consulta ainda é feita, e o service worker responde com a última cópia
// salva. No modo padrão o React Query só esperaria a rede voltar e a tela ficaria carregando.
const queryClient = new QueryClient({
  defaultOptions: {
    queries: { networkMode: 'offlineFirst' },
    mutations: { networkMode: 'offlineFirst' },
  },
})

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <DesfazerProvider>
          <BrowserRouter>
            <Routes>
              <Route path="/login" element={<Login />} />
              <Route element={<ProtectedRoute />}>
                <Route element={<Layout />}>
                  <Route index element={<Hoje />} />
                  <Route path="painel" element={<Painel />} />
                  <Route path="revisao" element={<Revisao />} />
                  <Route path="areas" element={<Areas />} />
                  <Route path="areas/nova" element={<NovaArea />} />
                  <Route path="areas/:id" element={<AreaDetalhe />} />
                  <Route path="tags/:nome" element={<TagDetalhe />} />
                  <Route path="dados" element={<Dados />} />
                </Route>
              </Route>
            </Routes>
          </BrowserRouter>
        </DesfazerProvider>
      </AuthProvider>
    </QueryClientProvider>
  )
}

export default App
