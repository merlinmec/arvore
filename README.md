# Árvore

App de acompanhamento de áreas, objetivos e progresso pessoal. Backend em Spring Boot (Java) e frontend em React (Vite).

## Funcionalidades

- **Áreas em árvore** (ex.: Saúde → Corrida), cada uma com cor, descrição e wallpaper
- **Objetivos** com prazo e status, e **atividades** encaixadas numa grade semanal
- **Hoje**: plano do dia agrupado por área, navegação por data com a tira da semana, registro retroativo e sequência (streak) por atividade
- **Painel**: taxa de conclusão com variação contra o período anterior, série diária por área, consistência por área e por dia da semana, heatmap anual e ranking de atividades, com filtro de período e de área
- **Notas** em Markdown com tags que cruzam áreas

Direção do produto, diretrizes e próximas fases: [`docs/ROADMAP.md`](docs/ROADMAP.md).

## Estrutura

```
arvore/
├── backend/    # API Spring Boot (Java 17, PostgreSQL, Flyway, JWT)
└── frontend/   # SPA React + TypeScript (Vite, Tailwind, React Query)
```

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
./mvnw test
```

### Principais endpoints

| Método | Rota | O que faz |
|---|---|---|
| `GET` | `/api/hoje?data=AAAA-MM-DD` | Plano do dia (padrão: hoje), com `editavel=false` para datas futuras |
| `POST`/`DELETE` | `/api/atividades/{id}/conclusao?data=&valor=` | Marca/desmarca a conclusão (aceita dias passados, recusa futuros); `valor` opcional soma na meta quantitativa do objetivo, e marcar de novo com valor corrige o do dia |
| `GET` | `/api/insights/resumo?dias=30&areaId=` | Consistência do período (1–365 dias), opcionalmente filtrada por área e subáreas |
| `GET` | `/api/insights/historico?inicio=&fim=&areaId=` | Planejado x concluído por dia (padrão: últimas 53 semanas) |
| `POST`/`DELETE` | `/api/atividades/{id}/agenda/{DIA}` | Coloca/tira um dia fixo da grade (vale a partir de hoje) |
| `PUT`/`DELETE` | `/api/atividades/{id}/agenda/frequencia` | Define (`{"vezes": 1-7}`) ou remove a meta de N vezes por semana; troca de modo encerra os dias fixos |
| `GET` | `/api/areas` | Árvore de áreas com o progresso da semana |

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
