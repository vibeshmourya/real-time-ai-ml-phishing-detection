<#
.SYNOPSIS
    Prahar Local Development Orchestrator
    Starts FastAPI ML Service, Spring Boot Backend, and React Frontend securely.

.DESCRIPTION
    Launches all three Prahar services locally for testing and development.
    Never hardcodes, logs, or echoes secret API keys.
#>

param(
    [switch]$SkipFrontend,
    [switch]$SkipBackend,
    [switch]$SkipML
)

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  PRAHAR: Real-Time AI/ML Phishing Detection Platform     " -ForegroundColor Cyan
Write-Host "  Local Multi-Service Development Orchestrator            " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host ""

$Root = $PSScriptRoot

# --- 1. Python FastAPI ML Service ---
if (-not $SkipML) {
    Write-Host "[1/3] Checking Python ML Microservice..." -ForegroundColor Yellow
    $MLDir = Join-Path $Root "ml-service"
    
    if (Test-Path $MLDir) {
        Write-Host "  Starting FastAPI ML service on http://127.0.0.1:8000..." -ForegroundColor Green
        Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$MLDir'; Write-Host 'Prahar FastAPI ML Microservice' -ForegroundColor Cyan; python -m uvicorn app.main:app --host 127.0.0.1 --port 8000 --reload"
        Start-Sleep -Seconds 2
    } else {
        Write-Host "  [WARN] ml-service directory not found at $MLDir" -ForegroundColor Red
    }
}

# --- 2. Spring Boot Backend ---
if (-not $SkipBackend) {
    Write-Host "[2/3] Configuring Spring Boot Backend..." -ForegroundColor Yellow
    $BackendDir = Join-Path $Root "backend"

    $ConfigureAi = Read-Host "  Would you like to configure Google Gemini AI provider? (y/N)"
    $AiProvider = "none"
    $AiModel = "gemini-2.5-flash"
    $AiUrl = "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions"

    if ($ConfigureAi -match "^[yY]") {
        $AiProvider = "gemini"
        Write-Host "  Enter your Gemini API key (input will NOT be echoed on screen):" -ForegroundColor Cyan
        $SecureKey = Read-Host -AsSecureString
        $BSTR = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($SecureKey)
        $PlainKey = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto($BSTR)
        [System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($BSTR)

        $env:AI_PROVIDER = $AiProvider
        $env:AI_API_KEY = $PlainKey
        $env:AI_API_URL = $AiUrl
        $env:AI_MODEL = $AiModel
        $env:AI_TIMEOUT_MS = "15000"
        Write-Host "  [OK] Gemini provider configured securely in process environment." -ForegroundColor Green
    } else {
        $env:AI_PROVIDER = "none"
        Write-Host "  [INFO] Running backend with deterministic 4-layer heuristic safety engine." -ForegroundColor Gray
    }

    $env:ML_SERVICE_URL = "http://localhost:8000"
    $env:ML_SERVICE_TIMEOUT_MS = "3000"

    Write-Host "  Starting Spring Boot backend on http://127.0.0.1:8080..." -ForegroundColor Green
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$BackendDir'; Write-Host 'Prahar Spring Boot Backend' -ForegroundColor Cyan; mvn spring-boot:run '-Dspring-boot.run.profiles=dev'"
    Start-Sleep -Seconds 3
}

# --- 3. React Frontend ---
if (-not $SkipFrontend) {
    Write-Host "[3/3] Starting React Frontend..." -ForegroundColor Yellow
    $FrontendDir = Join-Path $Root "frontend"

    if (Test-Path $FrontendDir) {
        Write-Host "  Starting Vite dev server on http://localhost:5173..." -ForegroundColor Green
        Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$FrontendDir'; Write-Host 'Prahar Frontend Dev Server' -ForegroundColor Cyan; npm run dev"
    } else {
        Write-Host "  [WARN] frontend directory not found at $FrontendDir" -ForegroundColor Red
    }
}

Write-Host ""
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  All services launched in separate terminal windows!     " -ForegroundColor Green
Write-Host "  - ML Service:   http://127.0.0.1:8000/health             " -ForegroundColor White
Write-Host "  - Backend API:  http://127.0.0.1:8080/api/scans/ml-status" -ForegroundColor White
Write-Host "  - Frontend UI:  http://localhost:5173                    " -ForegroundColor White
Write-Host "==========================================================" -ForegroundColor Cyan
