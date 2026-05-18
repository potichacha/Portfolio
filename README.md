# PortLoko

PortLoko is a developer portfolio platform focused on live, deployable projects. The current repository contains the Quarkus Core API and the CI workflow used to validate it.

## Current Stack

- Java 21
- Quarkus 3.x
- Maven
- Hibernate ORM with Panache
- PostgreSQL in local/dev environments
- H2 for tests
- Flyway migrations
- SmallRye JWT
- GitHub Actions

## Repository Layout

```text
Portfolio/
├── core-api/                 # Quarkus Core API
│   ├── src/main/java/com/portloko/
│   │   ├── github/           # GitHub API client
│   │   ├── project/          # Project model/repository
│   │   ├── shared/           # Shared exceptions and error mappers
│   │   └── user/             # User profile API
│   └── src/main/resources/
│       ├── application.properties
│       └── db/migration/     # Flyway migrations
├── .github/workflows/        # CI workflows
├── install_start.ps1         # Windows setup/test/build/start helper
└── install_start.sh          # Unix/Git Bash setup/test/build/start helper
```

Planned services include a runner service for isolated project builds and deployment orchestration.

## Prerequisites

- Java 21 available in `PATH`
- Maven available in `PATH`
- Git
- GitHub CLI (`gh`) for PR/check commands
- PostgreSQL for running the API locally against a real database

The tests do not require PostgreSQL because the test profile uses H2.

## Configuration

The Core API reads configuration from environment variables with local defaults in `core-api/src/main/resources/application.properties`.

Useful variables:

```text
DATABASE_URL=jdbc:postgresql://localhost:5432/portloko
DB_USER=portloko
DB_PASSWORD=portloko
JWT_PUBLIC_KEY_LOCATION=keys/public.pem
JWT_ISSUER=https://portloko.dev
CORS_ORIGINS=http://localhost:3000
GITHUB_API_BASE_URL=https://api.github.com
GITHUB_ARCHIVE_TEMP_DIR=
GITHUB_ARCHIVE_MAX_BYTES=52428800
```

For tests, no local `.env` is required.

## Validate The Project

From the repository root:

```powershell
cd core-api
mvn -B -ntp test
mvn -B -ntp verify
```

`mvn verify` matches the GitHub Actions validation workflow.

On Windows, you can also use:

```powershell
.\install_start.ps1 -NoStart
```

On Git Bash, WSL, Linux, or macOS:

```bash
NO_START=true ./install_start.sh
```

The `NoStart` / `NO_START=true` mode runs tests and builds the application without starting Quarkus dev mode.

## Start The Core API

Make sure PostgreSQL is available, then run one of:

```powershell
.\install_start.ps1
```

```bash
./install_start.sh
```

Or start Quarkus directly:

```powershell
cd core-api
mvn quarkus:dev
```

The API runs on:

```text
http://localhost:8080
```

Swagger UI is included by Quarkus when the application starts.

## GitHub Integration

The `com.portloko.github` package contains the GitHub API client used by deployment flows.

Implemented capabilities:

- List authenticated user repositories through `GET /user/repos`
- Filter repositories by `permissions.push`
- Paginate GitHub results with `per_page=100`
- Resolve a branch or tag to a full 40-character commit SHA
- Download a repository tarball for a specific commit SHA
- Store archives in a temporary directory
- Clean up downloaded archives after use
- Reject archives larger than `GITHUB_ARCHIVE_MAX_BYTES`

External GitHub calls are covered by unit tests using a local HTTP server, not the real GitHub API.

## Code Conventions

- Packages live under `com.portloko.{module}`
- REST resources end with `Resource`
- Services end with `Service`
- Repositories use Panache repositories
- DTOs are Java records
- Error responses use `{ "error": "...", "code": "..." }`
- Tests live in the same package under `src/test/java`
- Use `@QuarkusTest` for API/integration tests and JUnit + Mockito or local fakes for unit tests
- Do not commit secrets or local `.env` files

## Pull Request Workflow

1. Create a feature branch:

   ```bash
   git switch -c feature/BACK-123-short-description
   ```

2. Implement the change with focused tests.

3. Validate locally:

   ```bash
   cd core-api
   mvn -B -ntp verify
   ```

4. Push and open a PR:

   ```bash
   git push -u origin feature/BACK-123-short-description
   gh pr create --base main --head feature/BACK-123-short-description
   ```

5. Wait for the GitHub Actions check `Validate Quarkus Core API` to pass before merging.

## Useful Commands

```powershell
git status --short
gh pr checks <pr-number> --repo potichacha/Portfolio
gh pr view <pr-number> --repo potichacha/Portfolio
```

## Notes

The local scripts use the installed `mvn` command. If the Maven wrapper jar is missing or invalid locally, use `mvn` directly.
