param(
    [switch]$NoStart
)

$ErrorActionPreference = "Continue"
if (Test-Path Variable:\PSNativeCommandUseErrorActionPreference) {
    $PSNativeCommandUseErrorActionPreference = $false
}

$RootDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$CoreApiDir = Join-Path $RootDir "core-api"
$KeysDir = Join-Path $CoreApiDir "src/main/resources/keys"

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

# Java 23+ needs Byte Buddy experimental mode for the test mocks to load.
$mavenArgs = @()
if (($javaVersionOutput -join "`n") -match 'version "([0-9]+)') {
    $major = [int]$Matches[1]
    if ($major -ge 23) {
        Write-Host "Java $major detected; enabling Byte Buddy experimental mode for local tests." -ForegroundColor Yellow
        $mavenArgs += "-Dnet.bytebuddy.experimental=true"
    }
}

# Generate a development JWT key pair if it is missing. These keys are
# gitignored: every developer has their own local pair.
$publicKey = Join-Path $KeysDir "public.pem"
if (-not (Test-Path $publicKey)) {
    Section "Generating development JWT key pair (keys/ is gitignored)"
    if (Get-Command openssl -ErrorAction SilentlyContinue) {
        New-Item -ItemType Directory -Force -Path $KeysDir | Out-Null
        $privateKey = Join-Path $KeysDir "private.pem"
        & openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out $privateKey 2>$null
        & openssl rsa -pubout -in $privateKey -out $publicKey 2>$null
        Write-Host "Created $KeysDir\{private,public}.pem"
    } else {
        Write-Host "WARNING: openssl not found - cannot create JWT keys. Generate them manually." -ForegroundColor Yellow
    }
}

Section "Running tests (in-memory H2, no database needed)"
Invoke-Maven ($mavenArgs + @("clean", "test"))

Section "Building application"
Invoke-Maven ($mavenArgs + @("package", "-DskipTests"))

if ($NoStart) {
    Section "Done"
    Write-Host "Tests and build passed. Start skipped because -NoStart was provided."
    exit 0
}

# Starting the API for real needs PostgreSQL. Bring it up via docker compose
# when Docker is available; otherwise tell the developer what to do.
Section "Starting PostgreSQL (docker compose)"
if (Get-Command docker -ErrorAction SilentlyContinue) {
    Push-Location $RootDir
    try {
        & docker compose up -d
        Write-Host "Waiting for PostgreSQL to be ready..."
        for ($i = 0; $i -lt 30; $i++) {
            & docker compose exec -T postgres pg_isready -U portloko -d portloko *> $null
            if ($LASTEXITCODE -eq 0) {
                Write-Host "PostgreSQL is ready."
                break
            }
            Start-Sleep -Seconds 1
        }
    } finally {
        Pop-Location
    }
} else {
    Write-Host "WARNING: Docker not found. Start PostgreSQL yourself or set DATABASE_URL, then re-run." -ForegroundColor Yellow
}

Section "Starting Core API"
Write-Host "Core API will run on http://localhost:8080 (Swagger UI at /q/swagger-ui)."
Write-Host "Press Ctrl+C to stop. To stop PostgreSQL afterwards: docker compose down"
Invoke-Maven ($mavenArgs + @("quarkus:dev"))
