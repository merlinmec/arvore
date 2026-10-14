# Atualiza o Árvore que roda no próprio computador (ver docs/DEPLOY.md). Feito para uma tarefa
# agendada do Windows: no login e a cada poucos minutos.
#
# - Só troca a versão quando o master do GitHub mudou. O master só recebe código com a CI verde,
#   então o que chega aqui já passou pelos testes e pelo build das imagens.
# - Antes de trocar a versão, faz um backup extra do banco (as migrations novas rodam no boot).
# - Sempre termina com "up -d": se algum container tiver parado, volta. Sem rede, só isso.
#
# Uso: powershell -NoProfile -ExecutionPolicy Bypass -File local-update.ps1 -ProdDir <clone do master>
param(
  [Parameter(Mandatory = $true)][string]$ProdDir,
  [string]$LogFile = "$env:LOCALAPPDATA\arvore\update.log"
)

# "Continue", não "Stop": no PowerShell 5.1, com "Stop", qualquer linha que git/docker escrevem no
# stderr (avisos, progresso) vira exceção. Falha de verdade é decidida pelo código de saída (Run).
$ErrorActionPreference = "Continue"
New-Item -ItemType Directory -Force (Split-Path $LogFile) | Out-Null
function Log($msg) { "$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss') $msg" | Add-Content -Path $LogFile -Encoding utf8 }

function Run([string]$exe, [string[]]$argv) {
  $out = & $exe @argv 2>&1 | Out-String
  if ($LASTEXITCODE -ne 0) { throw "$exe $($argv -join ' ') falhou ($LASTEXITCODE): $out" }
  return $out.Trim()
}

$compose = @("compose", "-f", "docker-compose.yml", "-f", "docker-compose.local.yml")

try {
  Set-Location $ProdDir -ErrorAction Stop

  # No login o Docker Desktop ainda está iniciando: espera até 5 minutos.
  $ready = $false
  for ($i = 0; $i -lt 60; $i++) {
    & docker info *> $null
    if ($LASTEXITCODE -eq 0) { $ready = $true; break }
    Start-Sleep -Seconds 5
  }
  if (-not $ready) { Log "Docker não respondeu em 5 minutos; tento de novo na próxima execução."; exit 0 }

  # Sem rede o app continua funcionando; só não dá para saber se há versão nova.
  & git fetch --quiet origin master *> $null
  if ($LASTEXITCODE -ne 0) {
    Run docker ($compose + @("up", "-d")) | Out-Null
    exit 0
  }
  $local = Run git @("rev-parse", "HEAD")
  $remote = Run git @("rev-parse", "origin/master")

  if ($local -ne $remote) {
    Log "Nova versão: $($local.Substring(0, 7)) -> $($remote.Substring(0, 7))"
    $stamp = (Get-Date).ToUniversalTime().ToString("yyyyMMdd-HHmmss")
    Run docker ($compose + @("exec", "-T", "backup", "pg_dump", "-Fc", "-f", "/backups/arvore-$stamp-antes-update.dump")) | Out-Null
    Log "Backup antes da atualização: arvore-$stamp-antes-update.dump"
    Run git @("merge", "--ff-only", "--quiet", "origin/master") | Out-Null
    Run docker ($compose + @("up", "-d", "--build", "--remove-orphans")) | Out-Null
    Log "Atualizado para $($remote.Substring(0, 7))."
  } else {
    # Sem versão nova: garante que tudo está de pé (barato quando já está).
    Run docker ($compose + @("up", "-d")) | Out-Null
  }
} catch {
  Log "ERRO: $($_.Exception.Message)"
  exit 1
}
