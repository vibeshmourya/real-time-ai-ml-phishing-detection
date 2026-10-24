<#
.SYNOPSIS
    Prahar Academic Submission Packaging Wrapper (PowerShell)
.DESCRIPTION
    Executes scripts/package_submission.py to package the repository into a sanitized
    academic submission ZIP archive with automated integrity and secret scanning.
#>

param(
    [string]$OutputFile = "prahar_academic_submission.zip"
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot = Split-Path -Parent $ScriptDir
$PythonScript = Join-Path $ScriptDir "package_submission.py"
$TargetZip = Join-Path $RepoRoot $OutputFile

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "  PRAHAR SUBMISSION PACKAGING (POWERSHELL WRAPPER)" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "Target ZIP: $TargetZip" -ForegroundColor Yellow

if (Get-Command python -ErrorAction SilentlyContinue) {
    python "$PythonScript" "$TargetZip"
} elseif (Get-Command py -ErrorAction SilentlyContinue) {
    py -3 "$PythonScript" "$TargetZip"
} else {
    Write-Error "Python 3 is required to run scripts/package_submission.py."
}
