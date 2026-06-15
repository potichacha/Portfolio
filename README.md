# PortLoko

PortLoko is a developer portfolio platform focused on live, deployable projects:
every project can be deployed and shown as a working demo, not just a repo link.

This repository currently contains the **Core API** (Quarkus / Java 21) and its
CI workflow. The Runner service, the AI module and the frontend are tracked in
the project's issues and will land incrementally.

---

## TL;DR

```bash
# 1. Just run the tests (no database needed — uses in-memory H2)
cd core-api && mvn clean verify

# 2. Or do everything in one command (tests + build + Postgres + start the API)
#    macOS / Linux / Git Bash:
./install_start.sh
#    Windows PowerShell:
.\install_start.ps1
```

If `mvn clean verify` ends with `BUILD SUCCESS`, your environment is correct.

---

## Contents

1. [Stack](#stack)
2. [Prerequisites](#prerequisites)
3. [Two ways to work](#two-ways-to-work)
4. [Option A — Validate only (contributors)](#option-a--validate-only-contributors)
5. [Option B — Run the API for real](#option-b--run-the-api-for-real)
6. [Configuration](#configuration)
7. [Project layout](#project-layout)
8. [API endpoints](#api-endpoints)
9. [Contributing (branch + PR workflow)](#contributing-branch--pr-workflow)
10. [Troubleshooting](#troubleshooting)

---

## Stack

| Layer | Tech |
|------|------|
| Language | Java 21 |
| Framework | Quarkus 3.9 |
| Build | Maven |
| ORM | Hibernate ORM with Panache |
| Database | PostgreSQL (local/dev) · H2 (tests only) |
| Migrations | Flyway |
| Auth | SmallRye JWT |
| CI | GitHub Actions |

---

## Prerequisites

**To run the tests** (the most common case), you only need:

- **Java 21** (`java -version` should print 21)
- **Maven** (`mvn -v`)

**To start the API for real**, you additionally need:

- **Docker** (to run PostgreSQL via `docker compose`)
- **openssl** (to generate a local JWT key pair — the start script does it for you)

> Using Java 23+? It works, but Mockito needs an extra flag. The `install_start`
> scripts add it automatically. If you run Maven by hand, append
> `-Dnet.bytebuddy.experimental=true`. The recommended setup is **Java 21**.

---

## Two ways to work

PortLoko has two distinct modes, and most contributors only ever need the first:

- **Option A — Validate only.** Compile, run tests, build the jar. **No database, no Docker, no keys.** This is all you need to work on most tickets and to make CI pass.
- **Option B — Run the API for real.** Actually start the HTTP server and call endpoints. This needs PostgreSQL and a JWT key.

---

## Option A — Validate only (contributors)

This is what the CI runs and what you need for almost every ticket.

```bash
cd core-api
mvn clean verify
```

That compiles the code and runs **all tests** against an in-memory H2 database.
No PostgreSQL, no Docker, no `.env`, no keys required. Finish on `BUILD SUCCESS`
and you're good to open a PR.

Shortcut that also checks tooling and builds the jar without starting the server:

```bash
# macOS / Linux / Git Bash
NO_START=true ./install_start.sh

# Windows PowerShell
.\install_start.ps1 -NoStart
```

---

## Option B — Run the API for real

Use this when you want to actually hit the endpoints (Swagger, curl, frontend).

### One command (recommended)

```bash
# macOS / Linux / Git Bash
./install_start.sh

# Windows PowerShell
.\install_start.ps1
```

The script will, in order:

1. Check Java + Maven.
2. Generate a development JWT key pair into `core-api/src/main/resources/keys/` **if it's missing** (these keys are gitignored — each dev has their own).
3. Run the tests and build the jar.
4. Start **PostgreSQL** with `docker compose up -d` and wait until it's ready.
5. Start the API in dev mode (hot reload).

The API then runs at **http://localhost:8080**, with Swagger UI at
**http://localhost:8080/q/swagger-ui** and health at
**http://localhost:8080/q/health**.

Stop the API with `Ctrl+C`. Stop the database with `docker compose down`
(add `-v` to also delete its data).

### Manual steps (if you prefer not to use the script)

```bash
# 1. Start PostgreSQL
docker compose up -d

# 2. Generate a dev JWT key pair (once)
mkdir -p core-api/src/main/resources/keys
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 \
  -out core-api/src/main/resources/keys/private.pem
openssl rsa -pubout \
  -in core-api/src/main/resources/keys/private.pem \
  -out core-api/src/main/resources/keys/public.pem

# 3. Start the API
cd core-api
mvn quarkus:dev
```

---

## Configuration

All settings have working local defaults in
`core-api/src/main/resources/application.properties`, so you usually don't need
to set anything. To override, copy `.env.example` to `.env` and edit it.

| Variable | Default | Purpose |
|----------|---------|---------|
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/portloko` | Postgres JDBC URL |
| `DB_USER` / `DB_PASSWORD` | `portloko` / `portloko` | DB credentials (match `docker-compose.yml`) |
| `JWT_PUBLIC_KEY_LOCATION` | `keys/public.pem` | Public key used to verify JWTs |
| `JWT_ISSUER` | `https://portloko.dev` | Expected token issuer |
| `CORS_ORIGINS` | `http://localhost:3000` | Allowed frontend origin |
| `GITHUB_CLIENT_ID` / `GITHUB_CLIENT_SECRET` | dev placeholders | GitHub OAuth app |
| `CLAUDE_API_KEY` | dev placeholder | AI features (BACK-19/20) |
| `RUNNER_URL` | `http://localhost:8090` | Runner service (not built yet) |

> Tests need **none** of these — the test profile uses H2 and an embedded key.

---

## Project layout

```text
Portfolio/
├── docker-compose.yml          # Local PostgreSQL (dev only)
├── .env.example                # Copy to .env to override defaults
├── install_start.sh / .ps1     # Setup → test → build → (start) helper
├── README.md
├── .github/workflows/          # CI (Core API validation)
└── core-api/                   # Quarkus Core API
    ├── pom.xml
    └── src/
        ├── main/java/com/portloko/
        │   ├── auth/           # JWT identity (CurrentUser) + logout
        │   ├── user/           # User profile API
        │   ├── project/        # Project model/repository
        │   ├── github/         # GitHub API client (repos, SHA, archive)
        │   ├── shared/         # Config, error mappers, shared exceptions
        │   ├── ai/             # (planned) AI features
        │   ├── deployment/     # (planned) deployment orchestration
        │   └── feed/           # (planned) chronological feed
        └── main/resources/
            ├── application.properties
            ├── db/migration/   # Flyway migrations (V1, V2, V3…)
            └── keys/           # Dev JWT keys (gitignored, auto-generated)
```

---

## API endpoints

Currently implemented:

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/v1/me` | JWT | Current user's full profile |
| PATCH | `/v1/me/profile` | JWT | Update bio / handle / avatar |
| GET | `/v1/users/{handle}` | public | Public profile + public projects |
| POST | `/v1/auth/logout` | JWT | Clear the JWT cookie |
| GET | `/q/health` | public | Liveness/readiness |
| GET | `/q/swagger-ui` | public | Interactive API docs |

The `com.portloko.github` package also provides a GitHub client (list repos,
resolve a branch/tag to a commit SHA, download a repo archive) used by upcoming
deployment flows.

---

## Contributing (branch + PR workflow)

Never commit to `main`. One branch + one PR per ticket.

```bash
# 1. Branch from up-to-date main
git switch main && git pull
git switch -c feature/BACK-123-short-description

# 2. Make your change with focused tests, then validate
cd core-api && mvn clean verify

# 3. Push and open a PR
git push -u origin feature/BACK-123-short-description
gh pr create --base main --head feature/BACK-123-short-description
```

In the PR description, write `Closes #<issue-number>` so the issue closes
automatically on merge. Wait for the **Validate Quarkus Core API** check to be
green before merging.

**Conventions:**

- Packages under `com.portloko.{module}`.
- REST resources end with `Resource`, services with `Service`.
- Repositories use Panache — **never** build SQL by string concatenation.
- DTOs are Java `record`s.
- Error responses use `{ "error": "...", "code": "..." }`.
- Tests live in the same package under `src/test/java`.
- Never commit secrets, `.env`, or the `keys/` directory.

---

## Troubleshooting

| Symptom | Cause / Fix |
|---------|-------------|
| `mvn` not found | Install Maven and put it on your `PATH`. |
| Tests fail with `Mockito cannot mock ...` | You're on Java 23+. Use Java 21, or add `-Dnet.bytebuddy.experimental=true` (the start scripts do this automatically). |
| API won't start: connection refused on 5432 | PostgreSQL isn't running. `docker compose up -d`, or use `./install_start.sh`. |
| API won't start: missing JWT public key | `keys/public.pem` is absent. Re-run `./install_start.sh`, or generate it manually (see Option B). |
| Port 8080 already in use | Stop the other process, or set `quarkus.http.port`. |
| Want to wipe the DB | `docker compose down -v` then `docker compose up -d`. |
