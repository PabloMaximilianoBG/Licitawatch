<#
  LicitaWatch - arranque on-premise de todos los servicios (Windows, sin Docker).
  Orden: API Usuarios -> Licitaciones -> Ventas -> Notificaciones -> Asistente -> Gateway -> Frontend.

  Uso (desde la raíz del proyecto):
    powershell -ExecutionPolicy Bypass -File scripts\start-all.ps1            # compila si falta el jar y levanta todo
    powershell -ExecutionPolicy Bypass -File scripts\start-all.ps1 -Build     # fuerza recompilación
    powershell -ExecutionPolicy Bypass -File scripts\start-all.ps1 -SinFrontend

  Requisitos: PostgreSQL corriendo con las 4 bases creadas (db\crear-bases.sql) y el archivo .env en la raíz.
  Logs: carpeta .run\ (un archivo por servicio).
#>
param(
    [switch]$Build,
    [switch]$SinFrontend
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

if (-not (Test-Path (Join-Path $root '.env'))) {
    Write-Host 'Falta el archivo .env en la raiz. Copia .env.example como .env y completalo.' -ForegroundColor Red
    exit 1
}
New-Item -ItemType Directory -Force (Join-Path $root '.run') | Out-Null
# Directorio corto y SIN espacios para los sockets internos (AF_UNIX) de la JVM en Windows.
# Si la ruta de TEMP del perfil contiene espacios, Java falla con "Unable to establish loopback connection".
$tmp = Join-Path $env:SystemDrive 'licitawatch-tmp'
try { New-Item -ItemType Directory -Force $tmp -ErrorAction Stop | Out-Null }
catch { $tmp = Join-Path $root '.run\tmp'; New-Item -ItemType Directory -Force $tmp | Out-Null }

$servicios = @(
    @{ nombre = 'api-usuarios';       puerto = 8081 },
    @{ nombre = 'api-licitaciones';   puerto = 8082 },
    @{ nombre = 'api-ventas';         puerto = 8083 },
    @{ nombre = 'api-notificaciones'; puerto = 8084 },
    @{ nombre = 'api-asistente';      puerto = 8085 },
    @{ nombre = 'api-gateway';        puerto = 8080 }
)

function Esperar-Salud([int]$puerto, [int]$segundos = 120) {
    $limite = (Get-Date).AddSeconds($segundos)
    while ((Get-Date) -lt $limite) {
        try {
            $r = Invoke-RestMethod -Uri "http://localhost:$puerto/actuator/health" -TimeoutSec 3
            if ($r.status -eq 'UP') { return $true }
        } catch { }
        Start-Sleep -Seconds 2
    }
    return $false
}

$faltanJars = $servicios | Where-Object { -not (Test-Path (Join-Path $root "$($_.nombre)\target\$($_.nombre)-1.0.0.jar")) }
if ($Build -or $faltanJars) {
    Write-Host 'Compilando todos los microservicios (mvn -DskipTests package)...' -ForegroundColor Cyan
    & mvn -q -DskipTests package
    if ($LASTEXITCODE -ne 0) { Write-Host 'Error de compilacion' -ForegroundColor Red; exit 1 }
}

foreach ($s in $servicios) {
    $enUso = Get-NetTCPConnection -LocalPort $s.puerto -State Listen -ErrorAction SilentlyContinue
    if ($enUso) {
        Write-Host ("{0,-20} ya esta corriendo en {1}" -f $s.nombre, $s.puerto) -ForegroundColor Yellow
        continue
    }
    $dir = Join-Path $root $s.nombre
    $jar = "target\$($s.nombre)-1.0.0.jar"
    $log = Join-Path $root ".run\$($s.nombre).log"
    Write-Host ("Iniciando {0,-20} puerto {1} ..." -f $s.nombre, $s.puerto) -NoNewline
    $p = Start-Process -FilePath 'java' -ArgumentList "-Djdk.net.unixdomain.tmpdir=$tmp", '-jar', $jar `
        -WorkingDirectory $dir -RedirectStandardOutput $log -RedirectStandardError "$log.err" -WindowStyle Hidden -PassThru
    $p.Id | Out-File (Join-Path $root ".run\$($s.nombre).pid")
    if (Esperar-Salud $s.puerto) { Write-Host ' OK' -ForegroundColor Green }
    else { Write-Host " NO RESPONDE (revisa .run\$($s.nombre).log)" -ForegroundColor Red }
}

if (-not $SinFrontend) {
    $front = Join-Path $root 'frontend'
    if (Test-Path $front) {
        if (-not (Test-Path (Join-Path $front 'node_modules'))) {
            Write-Host 'Instalando dependencias del frontend (npm install)...' -ForegroundColor Cyan
            Push-Location $front; & npm install; Pop-Location
        }
        if (-not (Get-NetTCPConnection -LocalPort 5173 -State Listen -ErrorAction SilentlyContinue)) {
            Write-Host 'Iniciando frontend en http://localhost:5173 ...'
            $p = Start-Process -FilePath 'cmd.exe' -ArgumentList '/c', 'npm run dev' -WorkingDirectory $front `
                -RedirectStandardOutput (Join-Path $root '.run\frontend.log') -RedirectStandardError (Join-Path $root '.run\frontend.log.err') `
                -WindowStyle Hidden -PassThru
            $p.Id | Out-File (Join-Path $root '.run\frontend.pid')
        }
    }
}

Write-Host ''
Write-Host 'LicitaWatch en ejecucion:' -ForegroundColor Cyan
Write-Host '  Frontend : http://localhost:5173'
Write-Host '  Gateway  : http://localhost:8080'
Write-Host '  Swagger  : http://localhost:8081/swagger-ui.html (8082, 8083, 8084, 8085)'
Write-Host '  Detener  : powershell -ExecutionPolicy Bypass -File scripts\stop-all.ps1'
