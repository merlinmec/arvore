# Árvore

App de acompanhamento de áreas, objetivos e progresso pessoal. Backend em Spring Boot (Java) e frontend em React (Vite).

## Funcionalidades

- **Áreas em árvore** (ex.: Saúde → Corrida), cada uma com cor, descrição e wallpaper
- **Objetivos** com prazo e status, e **atividades** encaixadas numa grade semanal
- **Hábitos negativos** ("não fumar"): valem todo dia, e você registra só as recaídas
- **Hoje**: plano do dia agrupado por área, navegação por data com a tira da semana, registro retroativo e sequência (streak) por atividade
- **Painel**: taxa de conclusão com variação contra o período anterior, série diária por área, consistência por área e por dia da semana, heatmap anual e ranking de atividades com a força de cada hábito (pontuação com decaimento), com filtro de período e de área
- **Diário do dia**: humor e energia (1 a 5) e uma nota por data, cruzados com a consistência no Painel
- **Revisão semanal** guiada: como foi a semana, o que escapou, uma nota e o ajuste da grade da próxima
- **Notas** em Markdown com tags que cruzam áreas
- **Dados**: CSV das conclusões pra planilha, backup completo em JSON e restauração
- **Temporizador de foco**: ▶ numa atividade começa a contar, e o Painel mostra o tempo por área e atividade
- **Paleta de comandos** (`Ctrl+K`): ir pra qualquer tela ou área e marcar as atividades de hoje pelo teclado; na tela Hoje, `←`/`→` trocam o dia
- **Exclusão com desfazer**: atividade, objetivo e nota somem na hora, com alguns segundos pra desfazer (ou `Ctrl+Z`)

Direção do produto, diretrizes e próximas fases: [`docs/ROADMAP.md`](docs/ROADMAP.md).

## Estrutura

```
arvore/
├── backend/    # API Spring Boot (Java 17, PostgreSQL, Flyway, JWT)
└── frontend/   # SPA React + TypeScript (Vite, Tailwind, React Query)
```

## Rodar com Docker (um comando)

Precisa só do Docker. Copie o `.env.example` para `.env`, preencha o hash bcrypt da senha e o segredo do JWT (entre aspas simples), e suba:

```bash
cp .env.example .env
docker compose up --build
```

O app abre em `http://localhost:8000` (mude com `PORTA` no `.env`). O nginx do frontend repassa `/api` pro backend, e os dados do Postgres ficam no volume `dados-postgres`.

Para desenvolver com hot reload, siga as seções abaixo.

## Pré-requisitos

- [Java 17](https://adoptium.net/) (JDK)
- [Node.js 20+](https://nodejs.org/) e npm
- [PostgreSQL](https://www.postgresql.org/) rodando localmente (ou acessível via URL de conexão)
- Não é necessário instalar Maven: o projeto usa o Maven Wrapper (`mvnw` / `mvnw.cmd`)

## Backend (`backend/`)

### 1. Banco de dados

Crie um banco e um usuário no PostgreSQL (valores padrão usados pelo projeto):

```sql
CREATE DATABASE arvore;
CREATE USER arvore WITH PASSWORD 'arvore';
GRANT ALL PRIVILEGES ON DATABASE arvore TO arvore;
```

As migrations do Flyway (`src/main/resources/db/migration`) rodam automaticamente na primeira inicialização.

### 2. Configuração local

Copie o arquivo de exemplo e preencha com valores reais (esse arquivo é git-ignorado):

```bash
cd backend
cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
```

Edite `application-local.yml` e defina:

- `arvore.auth.usuario`: usuário de login
- `arvore.auth.senha-hash`: hash bcrypt da senha (gere com `htpasswd -bnBC 10 "" "sua-senha" | tr -d ':\n' | sed 's/^\$2y/\$2a/'` ou outro gerador bcrypt)
- `arvore.jwt.segredo`: string aleatória com pelo menos 32 caracteres

Alternativamente, esses valores podem ser passados via variáveis de ambiente (`DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `ARVORE_USUARIO`, `ARVORE_SENHA_HASH`, `ARVORE_JWT_SEGREDO`, `ARVORE_JWT_EXPIRACAO_MINUTOS`) em vez do profile `local`.

### 3. Rodar

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

No Windows (PowerShell/cmd): `mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local`

A API sobe em `http://localhost:8080`.

### Testes

```bash
./mvnw verify
```

Os testes unitários ficam em `util/`. Os de integração (`integracao/`) sobem um Postgres real com [Testcontainers](https://testcontainers.com/), então precisam do Docker rodando. O GitHub Actions (`.github/workflows/ci.yml`) roda tudo a cada push e PR.

### Principais endpoints

| Método | Rota | O que faz |
|---|---|---|
| `GET` | `/api/hoje?data=AAAA-MM-DD` | Plano do dia (padrão: hoje), com `editavel=false` para datas futuras |
| `POST`/`DELETE` | `/api/atividades/{id}/conclusao?data=&valor=` | Marca/desmarca a conclusão (aceita dias passados, recusa futuros); `valor` opcional soma na meta quantitativa do objetivo, e marcar de novo com valor corrige o do dia |
| `GET` | `/api/insights/resumo?dias=30&areaId=` | Consistência do período (1–365 dias), opcionalmente filtrada por área e subáreas |
| `GET` | `/api/insights/historico?inicio=&fim=&areaId=` | Planejado x concluído por dia (padrão: últimas 53 semanas) |
| `POST` | `/api/objetivos/{id}/atividades` | Cria atividade (`{"nome", "negativa"}`); `negativa: true` cria um hábito negativo, que vale todo dia e cujo registro de conclusão é a recaída |
| `POST`/`DELETE` | `/api/atividades/{id}/agenda/{DIA}` | Coloca/tira um dia fixo da grade (vale a partir de hoje) |
| `PUT`/`DELETE` | `/api/atividades/{id}/agenda/frequencia` | Define (`{"vezes": 1-7}`) ou remove a meta de N vezes por semana; troca de modo encerra os dias fixos |
| `GET` | `/api/areas` | Árvore de áreas com o progresso da semana |
| `GET` | `/api/revisoes/{data}` | Retrato da semana que contém a data (até hoje): taxa contra a anterior, dias, o que escapou por atividade e a nota |
| `PUT` | `/api/revisoes/{data}` | Salva a nota da revisão da semana (`{"nota"}`); `GET /api/revisoes` lista as revisões feitas |
| `GET`/`PUT`/`DELETE` | `/api/diario/{data}` | Diário do dia (`{"humor", "energia", "nota"}`, níveis 1–5). O `PUT` substitui o registro inteiro, e tudo vazio apaga; dia futuro dá 400. `GET /api/diario?inicio=&fim=` lista o período |
| `GET` | `/api/insights/diario?dias=30&areaId=` | Taxa de conclusão por nível de humor e de energia registrados no diário |
| `POST` | `/api/foco/iniciar` · `/api/foco/parar` | Começa (`{"atividadeId"}`, encerrando a anterior) ou para a sessão de foco; `GET /api/foco/atual` devolve a que está contando (204 se nenhuma) e `GET /api/foco?inicio=&fim=` lista as sessões |
| `GET` | `/api/insights/tempo?dias=30&areaId=` | Tempo de foco do período: total, dia a dia, por área e por atividade |
| `GET` | `/api/dados/conclusoes.csv?inicio=&fim=` | CSV das conclusões (padrão: tudo até hoje), com `;`, vírgula decimal e BOM pro Excel |
| `GET`/`PUT` | `/api/dados/backup` | Baixa o backup completo em JSON / substitui todos os dados por um backup (inconsistente dá 400 e nada muda) |

## Frontend (`frontend/`)

### 1. Instalar dependências

```bash
cd frontend
npm install
```

### 2. Rodar em modo dev

```bash
npm run dev
```

A aplicação sobe em `http://localhost:5173` (padrão do Vite) e faz proxy de `/api` para `http://localhost:8080` (backend precisa estar rodando).

### Outros scripts

```bash
npm run build    # build de produção (type-check + bundle)
npm run preview  # serve o build de produção localmente
npm run lint      # lint com oxlint
```

## Ordem recomendada para rodar localmente

1. Suba o PostgreSQL e crie o banco/usuário `arvore`.
2. Configure `backend/src/main/resources/application-local.yml`.
3. Rode o backend (`./mvnw spring-boot:run -Dspring-boot.run.profiles=local`).
4. Rode o frontend (`npm run dev` dentro de `frontend/`).
5. Acesse `http://localhost:5173` e faça login com o usuário/senha configurados no backend.
