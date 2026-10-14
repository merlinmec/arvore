# Rodar no próprio computador

Guia para deixar o Árvore sempre disponível no seu computador, sem servidor: a stack do
`docker-compose.yml` (Postgres, backend e o nginx do frontend) sobe sozinha quando o computador liga,
faz backup diário numa pasta e se atualiza quando o `master` do GitHub muda.

```
navegador ──8000──▶ frontend (nginx) ──/api──▶ backend (Spring Boot) ──▶ db (Postgres)
                                                                            ▲
                                                   backup (pg_dump diário) ─┘──▶ pasta do computador
```

`docker-compose.local.yml` só acrescenta duas coisas ao `docker-compose.yml`: `restart: unless-stopped`
em todos os serviços e o serviço `backup`.

Não precisa de HTTPS: o navegador trata `http://localhost` como contexto seguro, então instalar o app
(PWA) e receber as notificações dos lembretes funcionam assim mesmo. E o token de login fica no
`localStorage`, que é separado por porta, então outro projeto em outra porta não enxerga a sessão.

## Configuração (uma vez só)

1. Use um clone **só para a produção**, sempre no `master`, separado da pasta onde você desenvolve.
   Assim, uma branch em andamento nunca é publicada:

   ```bash
   git clone --branch master https://github.com/merlinmec/arvore.git arvore-producao
   cd arvore-producao && cp .env.example .env
   ```

2. No `.env`, além da senha e do segredo do JWT (ver o README):
   - `BACKUP_DIR`: uma pasta do computador para os backups. Numa pasta sincronizada (OneDrive,
     Google Drive), o backup ganha cópia fora da máquina de graça. Os dumps têm todos os seus dados:
     use uma conta de nuvem só sua;
   - `POSTGRES_PASSWORD`: uma senha de verdade (o banco não publica porta, mas não custa);
   - `PORTA`, se a 8000 já estiver em uso.

3. Suba:

   ```bash
   docker compose -f docker-compose.yml -f docker-compose.local.yml up -d --build
   ```

   Abra `http://localhost:8000` (ou a `PORTA` escolhida). Para instalar como app, use o ícone de
   instalar na barra de endereço do Chrome/Edge.

4. No Docker Desktop, ligue "Start Docker Desktop when you sign in to your computer". Com o
   `restart: unless-stopped`, os containers voltam sozinhos quando o Docker inicia.

5. Atualização automática (Windows): registra uma tarefa do seu usuário, sem administrador, que roda
   `deploy/local-update.ps1` no login e a cada 15 minutos:

   ```bash
   powershell -NoProfile -ExecutionPolicy Bypass -File deploy/install-local-autoupdate.ps1
   ```

   Quando o `master` do GitHub muda, ela faz um backup extra do banco, baixa a versão nova e
   reconstrói as imagens. O `master` só recebe código com a CI verde, então o que chega já passou
   pelos testes e pelo build. Sem versão nova (ou sem rede), ela só garante que os containers estão
   de pé. O histórico fica em `%LOCALAPPDATA%/arvore/update.log`. Para remover a tarefa:
   `Unregister-ScheduledTask -TaskName "arvore atualizacao" -Confirm:$false`.

## Trazer dados que já existem

Pela própria tela **Dados**: baixe o backup em JSON no app antigo e restaure no novo. Ele leva tudo,
inclusive as datas de criação, que a consistência dos dias passados usa.

## Backup e restauração

- O serviço `backup` faz `pg_dump` diário em `BACKUP_DIR`, confere cada arquivo com
  `pg_restore --list` e mantém `BACKUP_KEEP_DAYS` dias (padrão: 30). Cada atualização automática
  também deixa um `arvore-<data>-antes-update.dump`.
- Restaurar um dump (desastre, ou trocar de computador):

  ```bash
  docker compose -f docker-compose.yml -f docker-compose.local.yml stop backend
  docker compose -f docker-compose.yml -f docker-compose.local.yml exec -T backup \
    pg_restore --clean --if-exists -d arvore /backups/arvore-AAAAMMDD-HHMMSS.dump
  docker compose -f docker-compose.yml -f docker-compose.local.yml start backend
  ```

- Os dados ficam no volume `arvore-producao_dados-postgres`. `docker compose down` mantém o volume;
  só `down -v` apaga.
