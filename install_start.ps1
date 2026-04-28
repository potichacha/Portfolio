param(
    [switch]$NoStart
)

$ErrorActionPreference = "Continue"
if (Test-Path Variable:\PSNativeCommandUseErrorActionPreference) {
    $PSNativeCommandUseErrorActionPreference = $false
}

$RootDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$CoreApiDir = Join-Path $RootDir "core-api"

function Section($Message) {
    Write-Host ""
    Write-Host "==> $Message" -ForegroundColor Cyan
}

function Require-Command($Name) {
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "$Name is required"
    }
}

function Invoke-Maven($Arguments) {
    Push-Location $CoreApiDir
    try {
        & mvn @Arguments
        if ($LASTEXITCODE -ne 0) {
            throw "Maven command failed: mvn $($Arguments -join ' ')"
        }
    } finally {
        Pop-Location
    }
}

Section "Checking required tools"
Require-Command java
Require-Command mvn

$javaVersionOutput = & java -version 2>&1
Write-Host ($javaVersionOutput -join "`n")

$mavenArgs = @()
if (($javaVersionOutput -join "`n") -match 'version "([0-9]+)') {
    $major = [int]$Matches[1]
    if ($major -ge 23) {
        Write-Host "Java $major detected; enabling Byte Buddy experimental mode for local tests." -ForegroundColor Yellow
        $mavenArgs += "-Dnet.bytebuddy.experimental=true"
    }
}

Section "Running tests"
Invoke-Maven ($mavenArgs + @("clean", "test"))

Section "Building application"
Invoke-Maven ($mavenArgs + @("package", "-DskipTests"))

if ($NoStart) {
    Section "Done"
    Write-Host "Tests and build passed. Start skipped because -NoStart was provided."
    exit 0
}

Section "Starting Core API"
Write-Host "If PostgreSQL is not available, start it or set DATABASE_URL before running this script." -ForegroundColor Yellow
Write-Host "Core API will run on http://localhost:8080. Press Ctrl+C to stop."
Invoke-Maven ($mavenArgs + @("quarkus:dev"))
