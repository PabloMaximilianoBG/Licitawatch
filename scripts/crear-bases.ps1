<#
  Crea las 4 bases de LicitaWatch en el PostgreSQL local usando las credenciales de .env.
  Uso: powershell -ExecutionPolicy Bypass -File scripts\crear-bases.ps1
  Las tablas NO se crean aquí: las crea Flyway al arrancar cada microservicio.
#>
$root = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $root '.env'
$vars = @{}
Get-Content $envFile | Where-Object { $_ -match '^\s*[A-Z_]+=' } | ForEach-Object {
    $k, $v = $_ -split '=', 2
    $vars[$k.Trim()] = $v.Trim()
}
$psql = (Get-Command psql -ErrorAction SilentlyContinue).Source
if (-not $psql) {
    $psql = Get-ChildItem 'C:\Program Files\PostgreSQL\*\bin\psql.exe' -ErrorAction SilentlyContinue |
        Sort-Object FullName -Descending | Select-Object -First 1 -ExpandProperty FullName
}
if (-not $psql) { Write-Host 'No se encontro psql.exe. Instala PostgreSQL.' -ForegroundColor Red; exit 1 }

$env:PGPASSWORD = $vars['POSTGRES_PASSWORD']
& $psql -U $vars['POSTGRES_USER'] -h $vars['POSTGRES_HOST'] -p $vars['POSTGRES_PORT'] -f (Join-Path $root 'db\crear-bases.sql')
& $psql -U $vars['POSTGRES_USER'] -h $vars['POSTGRES_HOST'] -p $vars['POSTGRES_PORT'] -Atc "select datname from pg_database where datname like 'licitawatch%' order by 1"
Remove-Item Env:\PGPASSWORD
