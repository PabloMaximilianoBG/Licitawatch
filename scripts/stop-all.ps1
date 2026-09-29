<#
  LicitaWatch - detiene todos los servicios iniciados localmente (puertos 8080-8085 y 5173).
  Uso: powershell -ExecutionPolicy Bypass -File scripts\stop-all.ps1
#>
$puertos = 8080, 8081, 8082, 8083, 8084, 8085, 5173
foreach ($puerto in $puertos) {
    $conexiones = Get-NetTCPConnection -LocalPort $puerto -State Listen -ErrorAction SilentlyContinue
    foreach ($c in $conexiones) {
        try {
            Stop-Process -Id $c.OwningProcess -Force -Confirm:$false -ErrorAction Stop
            Write-Host "Detenido proceso $($c.OwningProcess) (puerto $puerto)"
        } catch { }
    }
}
$root = Split-Path -Parent $PSScriptRoot
Get-ChildItem (Join-Path $root '.run') -Filter '*.pid' -ErrorAction SilentlyContinue | Remove-Item -Force -Confirm:$false
Write-Host 'LicitaWatch detenido.'
