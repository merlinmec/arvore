#!/bin/sh
# Backup diário do Postgres (serviço "backup" do docker-compose.local.yml).
# - pg_dump em formato custom (-Fc): comprimido e restaurável tabela a tabela;
# - cada dump é conferido com pg_restore --list logo depois de gerado (arquivo truncado = falha já);
# - mantém BACKUP_KEEP_DAYS dias.
set -eu

KEEP_DAYS="${BACKUP_KEEP_DAYS:-30}"
DIR=/backups

fazer_backup() {
  arquivo="$DIR/arvore-$(date +%Y%m%d-%H%M%S).dump"
  echo "[backup] gerando $arquivo"
  pg_dump -Fc -f "$arquivo.tmp" &&
    pg_restore --list "$arquivo.tmp" > /dev/null &&
    mv "$arquivo.tmp" "$arquivo" &&
    find "$DIR" -name 'arvore-*.dump' -mtime +"$KEEP_DAYS" -print -delete &&
    echo "[backup] ok ($(du -h "$arquivo" | cut -f1))"
}

mkdir -p "$DIR"
until pg_isready -q; do sleep 2; done
while true; do
  fazer_backup || echo "[backup] FALHOU — veja os logs do serviço backup" >&2
  sleep 86400
done
