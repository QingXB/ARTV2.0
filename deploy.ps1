# ============================================
# ART Project - One-Click Local Deploy Script
# ============================================

$ErrorActionPreference = "Stop"

function Write-Step { param($Msg) Write-Host "[INFO] $Msg" -ForegroundColor Cyan }
function Write-Success { param($Msg) Write-Host "[OK] $Msg" -ForegroundColor Green }
function Write-Warn { param($Msg) Write-Host "[WARN] $Msg" -ForegroundColor Yellow }
function Write-Err { param($Msg) Write-Host "[ERROR] $Msg" -ForegroundColor Red }

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectDir = $ScriptDir
$PID_FILE = "$ProjectDir\deploy.pids"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "     ART Project - One-Click Deploy" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

function Kill-Port {
    param($Port)
    $proc = Get-NetTCPConnection -LocalPort $Port -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess
    if ($proc) {
        Write-Warn "Port $Port is in use, closing..."
        Stop-Process -Id $proc -Force -ErrorAction SilentlyContinue
        Start-Sleep -Milliseconds 500
    }
}

Write-Step "Checking port availability..."
Kill-Port 8000
Kill-Port 8080
Kill-Port 5173

Write-Step "Starting Python AI service (ART_P)..."
$PythonDir = "$ProjectDir\ART_P"
if (-not (Test-Path "$PythonDir\venv")) {
    Write-Err "Python venv not found: $PythonDir\venv"
    exit 1
}
$PythonProcess = Start-Process -FilePath "powershell" -ArgumentList "-NoExit", "-Command", "cd '$PythonDir'; & .\venv\Scripts\python.exe .\main.py" -PassThru -WindowStyle Normal
Write-Success "Python AI service started (PID: $($PythonProcess.Id))"

Write-Step "Starting Java backend service (ART_H)..."
$JavaDir = "$ProjectDir\ART_H\art"
$JavaProcess = Start-Process -FilePath "powershell" -ArgumentList "-NoExit", "-Command", "cd '$JavaDir'; & .\mvnw.cmd spring-boot:run" -PassThru -WindowStyle Normal
Write-Success "Java backend service started (PID: $($JavaProcess.Id))"

Write-Step "Starting Vue frontend service (ART_Q)..."
$FrontendDir = "$ProjectDir\ART_Q"
$FrontendProcess = Start-Process -FilePath "powershell" -ArgumentList "-NoExit", "-Command", "cd '$FrontendDir'; npm run dev" -PassThru -WindowStyle Normal
Write-Success "Vue frontend service started (PID: $($FrontendProcess.Id))"

$Processes = @{
    Python = $PythonProcess.Id
    Java = $JavaProcess.Id
    Frontend = $FrontendProcess.Id
} | ConvertTo-Json
$Processes | Out-File -FilePath $PID_FILE -Encoding UTF8

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Success "All services started successfully!"
Write-Host ""
Write-Host "Access URLs:" -ForegroundColor Yellow
Write-Host "  - Frontend: http://localhost:5173"
Write-Host "  - Backend:  http://localhost:8080"
Write-Host "  - AI API:   http://localhost:8000/docs"
Write-Host ""
Write-Host "Process IDs:" -ForegroundColor Yellow
Write-Host "  - Python AI: $($PythonProcess.Id)"
Write-Host "  - Java:      $($JavaProcess.Id)"
Write-Host "  - Frontend:  $($FrontendProcess.Id)"
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "NOTE: Closing this window will NOT stop services" -ForegroundColor Yellow
Write-Host "To stop services, run: .\stop.ps1" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Cyan

Start-Sleep -Seconds 2
