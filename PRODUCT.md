# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

Um único usuário (o próprio autor), que usa o app principalmente no navegador do desktop: abre pra revisar o dia, marcar o que fez e planejar a semana. Não há outros papéis nem contas.

## Product Purpose

Organizar a vida em **áreas** hierárquicas (a "árvore": ex. Saúde → Corrida), cada uma com **objetivos**, **atividades** encaixadas numa grade semanal e **notas** em Markdown com tags. O app mostra o que está planejado pra cada dia, registra o que foi concluído e devolve isso como consistência (streak, histórico, taxa por área). Sucesso é o usuário conseguir responder "estou sendo consistente no que disse que importava?" olhando uma tela.

## Positioning

Apps de hábito (Habi, Streaks) tratam hábitos como lista plana; apps de metas tratam metas sem rotina. O Árvore liga os três níveis — área → objetivo → atividade recorrente — e mede progresso de baixo pra cima: a consistência da atividade vira o progresso do objetivo, que vira o progresso da área.

## Operating Context

- Ritual diário: tela Hoje, marcar atividades; ocasionalmente registrar retroativamente um dia anterior.
- Ritual semanal: revisar o painel de consistência, ajustar a grade semanal das atividades, escrever notas.
- Benchmarks trazidos pelo usuário: Clockify (dashboard de tempo), Habi (hábitos com áreas de vida, heatmap de streak), conceito "Goal-Tab" (metas mensais com progresso colorido).

## Capabilities and Constraints

- Backend Spring Boot 4 / Java 17+ / PostgreSQL / Flyway / JWT com 1 usuário configurado; frontend React 19 + Vite + Tailwind v4 + React Query.
- Single-user por decisão (confirmada em 2026-09-28). Multiusuário fica como fase futura do roadmap.
- A grade semanal (`agenda_semanal`) não é versionada: o "planejado" de dias passados é reconstruído a partir da grade atual.
- Descartados: gamificação (mascote, pontos), camada social.

## Brand Commitments

- Nome "árvore", em minúsculas, com o ícone de árvore em traço.
- Tema escuro com acento verde já estabelecido no código; cada área tem uma cor escolhida pelo usuário, que é a principal fonte de cor dos dados.
- Tom minimalista, sem gamificação exagerada.

## Evidence on Hand

Dados reais só existem no banco local do usuário. Não inventar números de uso nem depoimentos.

## Product Principles

1. Consistência real acima de motivação artificial: medir o que foi feito contra o que foi planejado, nunca inflar.
2. A hierarquia é o produto: toda visão deve permitir filtrar/agregar por área.
3. Registro rápido: marcar uma atividade deve ser um clique, inclusive em dias anteriores.
4. Cálculo no backend, regras puras testáveis: streak, taxa e planejado vivem em funções sem banco.
