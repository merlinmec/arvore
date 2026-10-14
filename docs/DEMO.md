# Demonstração pública (custo zero)

A demonstração deixa qualquer pessoa testar o Árvore sem conta: o link `/demo` cria um visitante com
uma árvore de exemplo (dez semanas de hábitos, metas, diário, revisões e sessões de foco, com as
datas relativas a hoje) e entra direto na tela Hoje. É o destino do botão "Ver demo" do portfólio.

## Como funciona

O Árvore é de um usuário só: não existe tabela de usuário nem `usuario_id` nas tabelas (ver a Fase 5
do roadmap). Em vez de transformar o app em multiusuário só para a demo, **cada visitante ganha um
schema próprio do Postgres**, com as mesmas tabelas do app:

- **Perfil `demo`** (`SPRING_PROFILES_ACTIVE=demo`) liga o pacote `demo`. Fora dele, nada disso
  existe (`/api/demo/*` responde 404 e o login mostra o formulário de sempre).
- **`POST /api/demo/sessao`** cria o schema `visitante_<segundos>_<aleatório>`, roda nele as mesmas
  migrations do Flyway e grava os dados de `DemoDados` pela restauração de backup (que preserva as
  datas de criação no passado; pela API, tudo nasceria hoje). Devolve um JWT com o schema numa claim
  assinada, válido por 24 h.
- **`DataSourceDoVisitante`** aponta cada conexão emprestada do pool para o schema do visitante da
  requisição (o `search_path`), ou para o `public` sem visitante. O resto do código (JPA, JDBC,
  regras de consistência) não sabe que existe mais de um. O HikariCP **não** devolve o schema ao
  original quando a conexão volta ao pool, por isso ele é definido em todo empréstimo; o teste de
  isolamento pega essa regressão.
- **Apagado depois de 24 h** com `drop schema ... cascade`. A idade vem do próprio nome do schema,
  sem tabela de controle. A limpeza roda de hora em hora e antes de cada visitante novo, porque o
  servidor gratuito pode passar horas parado.
- **Bloqueado na demo**: login de verdade (sem `ARVORE_SENHA_HASH`, nunca passa), token sem schema,
  restaurar backup e corpo de requisição acima de 64 KB (`FiltroDoVisitante`).
- **Limites do banco gratuito**: no máximo 200 visitantes ativos e 10 por IP por hora
  (`ARVORE_DEMO_MAX_VISITANTES`, `ARVORE_DEMO_POR_IP_POR_HORA`).
- **Um serviço só**: `Dockerfile.demo` põe o build do Vite dentro do jar (`classpath:/static`) e o
  backend serve o SPA. Na hospedagem gratuita, dois serviços seriam dois servidores dormindo.
- **Aquecimento**: ao subir, o servidor cria e apaga um visitante em segundo plano, para o primeiro
  visitante de verdade não pagar o Flyway e o JIT ainda frios.

Medido localmente com os limites do plano gratuito do Render (512 MB, 0,1 CPU): a aplicação usa
cerca de 275 MB, sobe em pouco mais de 2 minutos, e cada visitante novo fica pronto em 2 a 4
segundos. Com três visitantes, o banco inteiro tinha 11 MB (o plano gratuito do Neon dá 0,5 GB).

## Passo a passo

### 1. Banco no Neon (gratuito)

1. Em https://neon.tech, crie um projeto chamado `arvore-demo`, na região
   **AWS US West 2 (Oregon)**, a mesma do serviço no Render.
2. Em **Connect**, desligue "Connection pooling" e copie os dados da conexão **direta** (o host
   **não** pode ter `-pooler`: o Flyway usa travas de sessão que não passam pelo PgBouncer).
3. Monte a URL JDBC: `jdbc:postgresql://<host>/<banco>?sslmode=require`
   (ex.: `jdbc:postgresql://ep-xxx-123.us-west-2.aws.neon.tech/neondb?sslmode=require`).
   Guarde também o usuário e a senha.

### 2. Serviço no Render (gratuito)

1. Em https://render.com: **New → Blueprint**, escolha o repositório `arvore`. O Render lê o
   `render.yaml` da raiz. (O repositório pode continuar privado: o Render acessa pelo app do GitHub.)
2. Ele pede os segredos:
   - `DATABASE_URL`: a URL JDBC do passo 1;
   - `DATABASE_USERNAME` e `DATABASE_PASSWORD`: do Neon.
   O `ARVORE_JWT_SEGREDO` é gerado pelo próprio Render.
3. Espere o primeiro deploy e abra `https://<seu-endereço>/demo`.

Depois disso, todo merge no `master` publica a demo sozinho (`autoDeploy`).

### 3. Manter a demo acordada (gratuito)

O plano gratuito do Render dorme após 15 minutos sem acesso, e acordar leva uns 2 minutos. Um ping
no `https://<seu-endereço>/api/demo/status` (não consulta o banco, então o Neon continua dormindo)
evita isso, num cron job do https://cron-job.org a cada 10 minutos.

**Atenção às horas gratuitas:** as 750 horas por mês do Render são **por workspace**, não por
serviço. Dois serviços acordados o mês inteiro (o fin-mec e o Árvore) somam cerca de 1.490 horas, e
o Render suspende os dois quando a cota acaba. Restrinja o ping dos dois a **segunda a sexta, das
8h às 20h** (horário de Brasília): cerca de 265 horas cada, com folga para os acessos fora dele, que
só esperam o servidor acordar. No cron-job.org isso fica em "Advanced → Days of week / Hours".

### 4. Botão no portfólio

Preencha `demoUrl` do `arvore` em `src/data/projects.js` do repositório `portfolio` com
`https://<seu-endereço>/demo`.
