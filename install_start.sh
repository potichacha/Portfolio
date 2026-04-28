#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CORE_API_DIR="$ROOT_DIR/core-api"
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

MAVEN_ARGS=()
JAVA_VERSION_OUTPUT="$(java -version 2>&1 || true)"
if [[ "$JAVA_VERSION_OUTPUT" =~ version\ \"([0-9]+) ]]; then
  if [[ "${BASH_REMATCH[1]}" -ge 23 ]]; then
    echo "Java ${BASH_REMATCH[1]} detected; enabling Byte Buddy experimental mode for local tests."
    MAVEN_ARGS+=("-Dnet.bytebuddy.experimental=true")
  fi
fi

cd "$CORE_API_DIR"

info "Running tests"
mvn "${MAVEN_ARGS[@]}" clean test

info "Building application"
mvn "${MAVEN_ARGS[@]}" package -DskipTests

if [[ "$NO_START" == "true" ]]; then
  info "Done"
  echo "Tests and build passed. Start skipped because NO_START=true."
  exit 0
fi

info "Starting Core API"
echo "If PostgreSQL is not available, start it or set DATABASE_URL before running this script."
echo "Core API will run on http://localhost:8080. Press Ctrl+C to stop."
mvn "${MAVEN_ARGS[@]}" quarkus:dev
