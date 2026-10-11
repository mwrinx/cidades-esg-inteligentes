param([Parameter(Mandatory=$true)][ValidateSet('staging','production')] [string]$Environment,
      [Parameter(Mandatory=$true)][string]$Image, [string]$Version='local')
$ErrorActionPreference='Stop'
if (-not $env:DB_PASSWORD) { throw 'DB_PASSWORD deve ser definida antes do deploy.' }
$env:APP_IMAGE=$Image
$env:APP_VERSION=$Version
$env:APP_ENV=$Environment
$env:APP_PORT=if($Environment -eq 'staging'){'8081'}else{'8082'}
docker compose -p "esg-$Environment" up -d --no-build --wait --wait-timeout 180
if($LASTEXITCODE -ne 0){throw 'Falha ao subir o ambiente.'}
$base="http://localhost:$($env:APP_PORT)"
$health=Invoke-RestMethod "$base/actuator/health"
if($health.status -ne 'UP'){throw 'Healthcheck falhou.'}
$info=Invoke-RestMethod "$base/api/ambiente"
if($info.ambiente -ne $Environment -or $info.versao -ne $Version){throw 'Ambiente ou versão incorretos.'}
$null=Invoke-RestMethod "$base/api/iniciativas"
Write-Output "DEPLOY OK: $Environment | $base | $Version"
