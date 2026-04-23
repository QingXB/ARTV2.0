# ============================================
# ART Project - Stop All Local Services
# ============================================

$ProjectDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$PID_FILE = "$ProjectDir\deploy.pids"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "     ART Project - Stop All Services" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

if (Test-Path $PID_FILE) {
    $pids = Get-Content $PID_FILE | ConvertFrom-Json

    if ($pids.Python) {
        Write-Host "[INFO] Stopping Python AI (PID: $($pids.Python))..." -ForegroundColor Cyan
        Stop-Process -Id $pids.Python -Force -ErrorAction SilentlyContinue
    }

    if ($pids.Java) {
        Write-Host "[INFO] Stopping Java backend (PID: $($pids.Java))..." -ForegroundColor Cyan
        Stop-Process -Id $pids.Java -Force -ErrorAction SilentlyContinue
    }

    if ($pids.Frontend) {
        Write-Host "[INFO] Stopping Vue frontend (PID: $($pids.Frontend))..." -ForegroundColor Cyan
        Stop-Process -Id $pids.Frontend -Force -ErrorAction SilentlyContinue
    }

    Remove-Item $PID_FILE -Force -ErrorAction SilentlyContinue
}

$ports = @(8000, 8080, 5173)
foreach ($port in $ports) {
    $proc = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess
    if ($proc) {
        Write-Host "[INFO] Cleaning up port $port..." -ForegroundColor Cyan
        Stop-Process -Id $proc -Force -ErrorAction SilentlyContinue
    }
}

Write-Host ""
Write-Host "[OK] All services stopped" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
