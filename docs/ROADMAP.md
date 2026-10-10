# Roadmap do Árvore

Documento vivo com a direção do produto: em que apps parecidos se inspira, as diretrizes que valem pra qualquer feature nova, as metas e as fases de evolução. Atualizado em 10/10/2026.

---

## 1. Onde o Árvore se encaixa

O Árvore junta três coisas que o mercado costuma separar:

| Categoria | Referências | O que fazem bem | O que falta pro nosso caso |
|---|---|---|---|
| Rastreador de hábitos | Habi, Streaks, Loop Habit Tracker | Marcação em um toque, sequência (streak), heatmap anual, frequência flexível ("3x por semana") | Hábito solto, sem ligação com um objetivo maior |
| Metas e OKRs pessoais | Conceito "Goal-Tab", Notion "Life OS" | Meta com prazo, progresso colorido por meta | Meta sem rotina: não diz o que fazer na terça |
| Tempo e produtividade | Clockify, Sunsama, Todoist | Dashboard por projeto, ritual de planejamento diário e revisão semanal | Mede esforço, não consistência |

**Posicionamento:** área → objetivo → atividade recorrente, com o progresso subindo de baixo pra cima. A consistência da atividade vira o progresso do objetivo, que vira o progresso da área. Nenhum dos apps acima faz essa agregação hierárquica.

**O que decidimos não copiar:** gamificação (mascote, pontos, loja da Habitica), camada social (feed, leaderboard) e multiusuário por enquanto. O app é pessoal, e a métrica é honesta: cumprido contra planejado.

---

## 2. Diretrizes

Valem para qualquer feature nova, no backend ou no frontend.

1. **Consistência honesta.** Toda métrica compara o que foi feito com o que estava planejado. Dia sem plano nunca conta como falha. Taxa nunca passa de 100%.
2. **A árvore é o filtro universal.** Toda tela de dados aceita filtro por área, e uma área inclui suas subáreas.
3. **Regra de negócio pura e testada.** Cálculo (streak, taxa, planejado) mora em funções sem banco (`util/Consistencia`, `util/ProgressoCalculo`) e tem teste unitário.
4. **Sem N+1.** Telas que cruzam a árvore inteira carregam tudo em poucas queries (`Catalogo`) e calculam em memória. O volume de um único usuário cabe folgado nisso.
5. **Marcar é instantâneo.** Ações frequentes usam atualização otimista (a UI muda antes da resposta e volta atrás se a API recusar).
6. **Cor é dado.** A cor que o usuário escolhe pra área é a única cor forte das telas de dados. Verde fica pra ação e estado, laranja pra alerta e sequência.
7. **Todo estado tem tela.** Carregando (skeleton), vazio (explica o que fazer), erro (diz o que houve) e dia futuro (só leitura).
8. **Datas como `AAAA-MM-DD` de ponta a ponta,** no fuso `America/Sao_Paulo`. O frontend nunca usa `toISOString()` pra datas civis.

---

## 3. Metas do projeto

| Meta | Critério de pronto |
|---|---|
| Registrar o dia em menos de 10 s | Abrir Hoje e marcar tudo sem sair da tela |
| Responder "estou sendo consistente?" numa tela | Painel com taxa, variação contra o período anterior, por área, padrão semanal e ano |
| Regras de consistência 100% testadas | Todo método público de `util/` com teste unitário |
| Backend com teste de integração | Controllers principais testados com Testcontainers (Postgres real) |
| Nenhuma tela com query por item | Hoje, Painel e Áreas com no máximo ~5 queries por request |
| Rodar com um comando | `docker compose up` sobe banco, backend e frontend |

---

## 4. Fases

### Fase 1 — Consistência visível ✅ (28/09/2026)

- [x] Navegação por data na tela Hoje (`GET /api/hoje?data=`), com tira da semana que enche conforme o dia é cumprido
- [x] Registro retroativo (`POST /api/atividades/{id}/conclusao?data=`); data futura é recusada (400)
- [x] Hoje mostra só atividades de objetivos **ativos**, agrupadas por área
- [x] Endpoint de insights (`GET /api/insights/resumo?dias=&areaId=`): taxa, variação contra o período anterior, dias completos, série diária por área, taxa por área, por dia da semana e por atividade (sequência atual e recorde)
- [x] Heatmap anual (`GET /api/insights/historico`), no Painel e em cada área (6 meses)
- [x] Tela Painel com filtro de período (7/30/90/365) e de área (inclui subáreas)
- [x] Redesenho: navegação lateral com o "termômetro" semanal das áreas, Áreas em grade com a cor de cada área, detalhe com breadcrumb, card de objetivo com prazo em contagem regressiva
- [x] `Catalogo`: retrato da árvore em 4 queries, eliminando o N+1 de Hoje
- [x] Erros HTTP padronizados (`TratadorDeErros`: 404 pra não encontrado, 400 pra entrada inválida)

### Fase 2 — Metas mensuráveis ✅ (09/10/2026)

Inspiração: Loop Habit Tracker, Streaks, "Goal-Tab".

- [x] **Grade semanal versionada** (29/09/2026): `agenda_semanal` ganhou `vigente_de`/`vigente_ate`. Mudar os dias de uma atividade vale a partir de hoje e não reescreve mais o "planejado" do passado
- [x] **Frequência flexível** (29/09/2026): além de dias fixos, "N vezes por semana" em qualquer dia (`frequencia_semanal`, com vigência). Cada dia feito conta 1 de 1 até a meta, e a falha cai no dia em que deixou de dar tempo de cumprir a semana, então heatmap, taxa e streak continuam somando por dia
- [x] **Meta quantitativa no objetivo** (30/09/2026): ex. "correr 100 km até dezembro". A conclusão registra um valor opcional (5 km), e o card mostra o acumulado contra a meta, onde estaria num ritmo constante, a situação (adiantada, no ritmo, atrasada) e quanto falta por semana até o prazo
- [x] **Hábito negativo** (09/10/2026): "não fumar", criado com `negativa: true`. Vale todo dia desde a criação, sem grade, e o registro é a recaída. Dia sem recaída até hoje conta como cumprido (hoje vale enquanto não houver recaída), dia futuro fica planejado e não cumprido, e a sequência é de dias limpos
- [x] **Força do hábito** (09/10/2026), pontuação com decaimento estilo Loop: média móvel exponencial (0–100) das ocorrências planejadas, com meia-vida de 13 ocorrências. Uma falha isolada depois de meses de consistência custa uns 5 pontos, enquanto a sequência zera. Aparece no ranking de atividades do Painel

### Fase 3 — Rituais

Inspiração: Sunsama, Clockify, Habi.

- [x] **Revisão semanal guiada** (09/10/2026): tela Revisão em 4 passos — como foi a semana (taxa contra a anterior e os dias), o que escapou (dias perdidos por atividade, recaídas à parte), uma nota guardada por semana e o ajuste da grade. Domingo, a tela Hoje convida pra revisar enquanto a semana não tem nota. O ajuste da grade vale a partir de hoje, como qualquer mudança de grade (vigência futura ficou de fora)
- [x] **Diário do dia** (10/10/2026): humor e energia (1–5) e uma nota curta por data, no fim da tela Hoje. Os níveis salvam no clique e a nota ao sair do campo; tudo vazio apaga o registro. No Painel, um bloco mostra a taxa de conclusão por nível de humor e de energia no período (com o filtro de área), e compara os dias de nível alto (4–5) com os de nível baixo (1–2)
- [ ] **Temporizador de foco** por atividade, registrando minutos (dashboard de tempo por área, estilo Clockify)
- [ ] **Lembretes** por atividade (notificação do navegador/PWA)

### Fase 4 — Plataforma e qualidade

- [x] **Testes de integração e CI** (09/10/2026): Testcontainers com Postgres real, passando pelo login JWT (autenticação e fluxo da tela Hoje); GitHub Actions roda os testes do backend, lint e build do frontend e o build das imagens. Faltam testes de Painel, Áreas e agenda
- [x] **`docker-compose.yml`** (09/10/2026) com Postgres, backend e frontend (nginx servindo o build e repassando `/api`)
- [ ] PWA instalável e com cache offline da tela Hoje
- [ ] Atalhos de teclado e command palette (`Ctrl+K`: ir pra área, marcar atividade)
- [ ] Exportar CSV das conclusões e backup/restauração em JSON
- [ ] Trocar `window.confirm` por confirmação inline com desfazer

### Fase 5 — Multiusuário (só se o projeto mudar de escopo)

- [ ] Tabela `usuario`, cadastro e `usuario_id` em todas as tabelas raiz
- [ ] Isolamento por usuário em todos os repositórios, com teste que garanta que um usuário não lê dado de outro

---

## 5. Dívida técnica conhecida

| Onde | Problema | Correção sugerida |
|---|---|---|
| Tabela `tag` | Tag que ficou sem nenhuma nota continua no banco e aparece em `GET /api/tags` | Apagar tags órfãs ao editar/excluir nota, ou listar só as que têm nota |

### Resolvida (29/09/2026)

- **N+1 em Áreas e Objetivos:** a árvore de áreas e a lista de objetivos agora usam o `Catalogo` e uma query de conclusões da semana (`ProgressoService.semanaAtual`). Antes eram várias queries por objetivo e por atividade.
- **Progresso semanal alinhado com Hoje e Painel:** passa por `Consistencia.progresso`. Na área, só contam objetivos ativos; nenhum dia antes da criação da atividade conta como planejado; conclusão em dia fora da grade não conta.
- **Grade sem histórico:** resolvida pela grade versionada da Fase 2.
- **Busca de notas por tag:** feita no banco (subquery em `nota_tag`, tags carregadas junto, mais recentes primeiro), e os nomes das áreas vêm numa query só.
