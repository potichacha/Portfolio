# PortLoko — CLAUDE.md

## Projet

PortLoko est un réseau social de développeurs centré sur le code déployé. Chaque post est une démo live testable en un clic. C'est le croisement entre Instagram (feed visuel), LinkedIn (profil pro, recruteurs) et GitHub (projets réels).

## Stack technique

### Core API (core-api/)
- **Java 21** + **Quarkus 3.x** (framework)
- **Panache ORM** (Hibernate sous le capot) — JAMAIS de SQL concaténé
- **PostgreSQL** via Supabase (MVP) puis VPS (V1)
- **RESTEasy Reactive** (JAX-RS) pour les endpoints
- **SmallRye JWT** pour l'authentification
- **Flyway** pour les migrations DB (resources/db/migration/)
- **JUnit 5 + Mockito** pour les tests unitaires
- **RestAssured + @QuarkusTest** pour les tests d'intégration

### Runner Service (runner-service/)
- **Java 21** + **Quarkus 3.x**
- **Docker Java SDK** pour orchestrer les builds/containers
- **Testcontainers** pour les tests d'intégration
- Templates : static-web (nginx), node-web (node), quarkus-jvm (java)

### AI (core-api/src/.../ai/ au MVP, ai-service/ en V1)
- **MVP** : module Java dans Core API, HttpClient vers Claude API
- **V1** : FastAPI + Python 3.12 + pgvector + Redis

### Frontend (frontend/)
- **React** ou **Next.js** (SPA)
- **TypeScript**
- **Tailwind CSS**

### Infra
- **Traefik v3** (reverse proxy, routage sous-domaines)
- **Docker 24+** (containers projets déployés)
- **GitHub Actions** (CI/CD)
- **Railway/Render** (hosting MVP, 0€)

## Architecture du repo

```
portloko/
├── docker-compose.yml
├── traefik/traefik.yml
├── core-api/                    # Quarkus - Java 21
│   ├── src/main/java/com/portloko/
│   │   ├── auth/                # OAuth GitHub + JWT
│   │   ├── user/                # Profil public
│   │   ├── project/             # CRUD projets
│   │   ├── deployment/          # Deploy + logs
│   │   ├── feed/                # Feed chrono + likes
│   │   ├── ai/                  # Client Claude API
│   │   ├── github/              # GitHub REST client
│   │   └── shared/              # Rate limit, error mapper, CORS, CSP
│   └── resources/db/migration/  # Flyway : V1__users, V2__projects, V3__deployments, V4__likes
├── runner-service/              # Quarkus - Docker Java SDK
│   └── src/.../runner/
│       ├── build/               # Orchestration build
│       ├── template/            # static-web, node-web, quarkus-jvm
│       ├── docker/              # Rolling switch + network isolation
│       └── logs/                # Build logs filesystem
├── frontend/                    # React/Next.js SPA
└── .github/workflows/
    ├── ci.yml
    └── cd.yml
```

## Conventions de code

### Java / Quarkus
- **Packages** : `com.portloko.{module}` (auth, user, project, deployment, feed, ai, github, shared)
- **Entités** : classes Panache avec `@Entity`. Utiliser `PanacheEntityBase` avec UUID comme PK
- **Repositories** : `PanacheRepository<Entity>` — pas de requêtes SQL brutes
- **Resources (controllers)** : suffixe `Resource` (ex: `ProjectResource`), annotations JAX-RS
- **Services** : suffixe `Service` (ex: `DeploymentService`), logique métier uniquement
- **DTOs** : records Java pour les requêtes/réponses (ex: `CreateProjectRequest`, `ProjectResponse`)
- **Validation** : `@Valid` + Bean Validation sur les DTOs
- **Erreurs** : `ExceptionMapper` centralisé dans `shared/`, codes HTTP standards (400, 401, 403, 404, 409, 429)
- **Tests** : 1 classe de test par classe de service. Nommage : `{Classe}Test.java`
- **Pas de Lombok** — utiliser les records Java et les méthodes Panache

### Migrations Flyway
- Nommage : `V{n}__{description}.sql` (double underscore)
- Toujours inclure les `CREATE INDEX` nécessaires
- Jamais de `DROP` en migration — ajouter, ne pas supprimer

### API REST
- Préfixe : `/v1/`
- Pagination : cursor-based (pas d'offset), paramètre `cursor` + `limit` (default 20)
- Auth : JWT dans cookie HttpOnly, middleware vérifie sur chaque endpoint `@Authenticated`
- Réponses : JSON, snake_case pour les champs
- Erreurs : `{ "error": "...", "code": "...", "details": {} }`

### Docker
- Multi-stage builds pour les images
- Chaque container déployé par un user : bridge isolé, --pids-limit 200, --cpus 1, --memory 512m
- Jamais d'accès au Docker socket depuis un container utilisateur

### Git
- Branches : `feature/BACK-{id}-{description}`, `fix/BACK-{id}-{description}`
- Commits : `feat(module): description`, `fix(module): description`, `test(module): description`
- PR obligatoire vers develop, 1 review minimum, CI vert

## Sécurité — Règles absolues

1. **JAMAIS de SQL concaténé** — ORM Panache exclusif
2. **JAMAIS de secrets dans le code** — variables d'env uniquement, .env.example documenté
3. **JAMAIS de log de tokens/secrets** — logger masqué configuré
4. **JWT HttpOnly** — jamais dans localStorage
5. **Containers utilisateurs isolés** — bridge séparé, egress limité, pas d'accès réseau interne
6. **Tokens GitHub chiffrés AES-GCM** au repos en base
7. **Rate limiting** — 60 req/min auth, 20 req/min deploy, 10 req/min IA
8. **CSP strict + CORS whitelist** sur tous les endpoints

## Ce que tu ne dois JAMAIS faire

- Générer du code avec Lombok (@Data, @Builder, etc.)
- Écrire du SQL brut (utiliser Panache)
- Mettre des secrets en dur dans le code
- Créer des endpoints sans auth quand ils en nécessitent
- Ignorer les limites de ressources containers (pids, cpu, memory)
- Utiliser des dépendances sans vérifier leur licence
- Commit directement sur main

## Commandes utiles

```bash
# Setup local
docker-compose up -d

# Core API
cd core-api && mvn quarkus:dev                    # Dev mode (hot reload)
cd core-api && mvn test                           # Tests unitaires
cd core-api && mvn verify                         # Tests + intégration
cd core-api && mvn checkstyle:check               # Lint

# Runner Service
cd runner-service && mvn quarkus:dev
cd runner-service && mvn test

# Migrations
# Les migrations Flyway s'exécutent automatiquement au démarrage de Quarkus

# Docker
docker build -t portloko/core-api ./core-api
docker build -t portloko/runner-service ./runner-service
trivy image portloko/core-api
```
