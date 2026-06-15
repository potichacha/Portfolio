#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CORE_API_DIR="$ROOT_DIR/core-api"
KEYS_DIR="$CORE_API_DIR/src/main/resources/keys"
NO_START="${NO_START:-false}"

info() {
  printf '\n==> %s\n' "$1"
}

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "$1 is required"
    exit 1
  }
}

info "Checking required tools"
require_cmd java
require_cmd mvn
java -version

# Java 23+ needs Byte Buddy experimental mode for the test mocks to load.
MAVEN_ARGS=()
JAVA_VERSION_OUTPUT="$(java -version 2>&1 || true)"
if [[ "$JAVA_VERSION_OUTPUT" =~ version\ \"([0-9]+) ]]; then
  if [[ "${BASH_REMATCH[1]}" -ge 23 ]]; then
    echo "Java ${BASH_REMATCH[1]} detected; enabling Byte Buddy experimental mode for local tests."
    MAVEN_ARGS+=("-Dnet.bytebuddy.experimental=true")
  fi
fi

# Generate a development JWT key pair if it is missing. These keys are
# gitignored: every developer has their own local pair.
if [[ ! -f "$KEYS_DIR/public.pem" ]]; then
  info "Generating development JWT key pair (keys/ is gitignored)"
  if command -v openssl >/dev/null 2>&1; then
    mkdir -p "$KEYS_DIR"
    openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$KEYS_DIR/private.pem" 2>/dev/null
    openssl rsa -pubout -in "$KEYS_DIR/private.pem" -out "$KEYS_DIR/public.pem" 2>/dev/null
    echo "Created $KEYS_DIR/{private,public}.pem"
  else
    echo "WARNING: openssl not found — cannot create JWT keys. Generate them manually:"
    echo "  openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out $KEYS_DIR/private.pem"
    echo "  openssl rsa -pubout -in $KEYS_DIR/private.pem -out $KEYS_DIR/public.pem"
  fi
fi

cd "$CORE_API_DIR"

info "Running tests (in-memory H2, no database needed)"
mvn "${MAVEN_ARGS[@]}" clean test

info "Building application"
mvn "${MAVEN_ARGS[@]}" package -DskipTests

if [[ "$NO_START" == "true" ]]; then
  info "Done"
  echo "Tests and build passed. Start skipped because NO_START=true."
  exit 0
fi

# Starting the API for real needs PostgreSQL. Bring it up via docker compose
# when Docker is available; otherwise tell the developer what to do.
info "Starting PostgreSQL (docker compose)"
if command -v docker >/dev/null 2>&1; then
  (cd "$ROOT_DIR" && docker compose up -d)
  echo "Waiting for PostgreSQL to be ready..."
  for _ in $(seq 1 30); do
    if (cd "$ROOT_DIR" && docker compose exec -T postgres pg_isready -U portloko -d portloko >/dev/null 2>&1); then
      echo "PostgreSQL is ready."
      break
    fi
    sleep 1
  done
else
  echo "WARNING: Docker not found. Start PostgreSQL yourself or set DATABASE_URL,"
  echo "then re-run. The API will fail to start without a reachable database."
fi

info "Starting Core API"
echo "Core API will run on http://localhost:8080 (Swagger UI at /q/swagger-ui)."
echo "Press Ctrl+C to stop. To stop PostgreSQL afterwards: docker compose down"
mvn "${MAVEN_ARGS[@]}" quarkus:dev
