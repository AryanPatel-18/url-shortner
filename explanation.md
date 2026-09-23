# URL Shortener — Complete Project Context

This file is the maintained, implementation-oriented description of the project. It is intended to give a developer or coding agent enough context to understand the current system without first reading every source file.

The code is the final authority when this document and another document disagree. This snapshot was audited against the working tree on **2026-09-23**.

## Table of contents

1. [Current status and source of truth](#current-status-and-source-of-truth)
2. [What the application does](#what-the-application-does)
3. [Technology and runtime](#technology-and-runtime)
4. [Repository layout](#repository-layout)
5. [Local setup and commands](#local-setup-and-commands)
6. [Configuration](#configuration)
7. [API contract](#api-contract)
8. [Authentication and security](#authentication-and-security)
9. [Rate limiting](#rate-limiting)
10. [Data model and migrations](#data-model-and-migrations)
11. [Request flows](#request-flows)
12. [Caching and click counting](#caching-and-click-counting)
13. [Service and repository responsibilities](#service-and-repository-responsibilities)
14. [Error behavior](#error-behavior)
15. [Testing and load testing](#testing-and-load-testing)
16. [Current limitations and next work](#current-limitations-and-next-work)

## Current status and source of truth

### Implemented capabilities

- User registration with BCrypt password hashing.
- Login with signed JWTs containing the email and user UUID.
- Stateless JWT authentication for protected requests; the request filter reconstructs the authenticated user from token claims without a database lookup.
- Global URL deduplication by exact `originalUrl` string.
- Random nine-character alphanumeric short-code generation with database-backed uniqueness and up to five collision retries.
- User-to-URL association management through `user_urls`.
- Paginated URL listing, single-item lookup, status updates, and association deletion.
- Public active-link redirects with HTTP `302 Found`.
- Global URL lifecycle status: `ACTIVE` or `DISABLED`; disabled links return HTTP `410 Gone`.
- Redis redirect caching with one-hour TTL and explicit invalidation on status changes.
- Redis-backed user URL-list caching with versioned keys and two-minute entry TTL.
- Redis write-behind click counting with a five-second scheduled PostgreSQL flush.
- Per-user, per-operation token-bucket rate limiting implemented by a Redis Lua script and Spring AOP.
- Flyway migrations and Hibernate schema validation.
- Spring Boot Actuator health and metrics exposure.
- Docker Compose definitions for PostgreSQL and Redis.

### Important state of this workspace

- The branch is `main`.
- The working tree contains many uncommitted source/config changes and this `explanation.md` is currently an untracked documentation file. Do not reset or discard those changes while working on the project.
- `load-tests/` and `results/` exist locally but are ignored by Git. They are useful local artifacts, not version-controlled application source.
- The committed test source contains only one `@SpringBootTest` context smoke test. The more extensive Node.js and k6 tests are local/ignored scripts.
- A local rate-limit report dated 2026-09-21 records the expected capacity and `429` behavior for all five protected URL operations. Treat that report as historical evidence, not as a replacement for rerunning tests after code changes.

### Verification status of this audit

Static source and configuration inspection was completed. Automated Maven verification could not be run in this environment because Java is unavailable, and Docker integration is unavailable in the WSL distribution. The normal commands are documented below.

## What the application does

The service accepts a long URL, creates or reuses a globally unique shortened URL, and associates it with the authenticated user. Anyone can visit `/{shortCode}`. Active links redirect to the original URL and record a click; disabled links are blocked. Authenticated users manage only their own `user_urls` associations, although the underlying shortened URL and its status can be shared by multiple users.

The application has two related concepts:

1. `shortened_urls` is the globally deduplicated URL record. It owns the short code, original URL, status, and global click count.
2. `user_urls` is a user-library association. It lets multiple users reference the same shortened URL and gives each user a separate association ID for management endpoints.

Because status lives on `shortened_urls`, disabling a URL disables the public short link for every user who references it.

## Technology and runtime

| Area | Current implementation |
|---|---|
| Language | Java 25 |
| Framework | Spring Boot 4.1.1 |
| Web | Spring MVC (`spring-boot-starter-webmvc`) |
| Persistence | Spring Data JPA / Hibernate |
| Database | PostgreSQL 18 |
| Migrations | Flyway managed by Spring Boot |
| Cache / distributed coordination | Redis through Spring Data Redis |
| Authentication | Spring Security and JJWT 0.13.0 |
| Password hashing | BCrypt |
| Build | Maven Wrapper 3.9.16 distribution |
| Infrastructure | Docker Compose |
| Monitoring | Spring Boot Actuator |
| Load testing | Node.js scripts and k6 scripts in the local ignored `load-tests/` directory |
| Boilerplate | Lombok |

The main class is `src/main/java/com/aryan/url_shortner/UrlShortnerApplication.java`. It enables scheduling with `@EnableScheduling` and sets the JVM default timezone to `Asia/Kolkata` during class initialization.

## Repository layout

```text
.
├── pom.xml                         Maven dependencies and Java version
├── mvnw / mvnw.cmd                 Maven Wrapper entry points
├── Makefile                        Docker, build, test, and database shell targets
├── docker-compose.yml              PostgreSQL and Redis containers
├── .env.example                    Local environment-variable template
├── explanation.md                  This complete project context
├── readme.md                       Shorter user-facing README
├── run-all-tests.ps1               Local master test runner (untracked)
├── parse-results.js                Local result parser (ignored)
├── load-tests/                     Local Node/k6 scripts (ignored)
├── results/                        Local load-test output (ignored)
└── src/
    ├── main/java/com/aryan/url_shortner/
    │   ├── UrlShortnerApplication.java
    │   ├── annotation/             @RateLimit
    │   ├── aspect/                 RateLimitAspect
    │   ├── config/                 .env, security, and rate-limit config
    │   ├── controller/              User, URL-management, and redirect controllers
    │   ├── dto/                     Request/response records
    │   ├── enums/                   URL status and rate-limit operation enums
    │   ├── exceptions/              Domain exceptions and global handler
    │   ├── model/                   JPA entities and CustomUserDetails
    │   ├── repository/              Spring Data repositories
    │   └── service/                 Security, URL, user, user-URL, and rate-limit services
    ├── main/resources/
    │   ├── application.properties
    │   ├── META-INF/spring.factories
    │   ├── db/migration/V1__create_tables.sql
    │   ├── db/migration/V2__create_user_urls.sql
    │   ├── db/migration/V3__move_status_to_shortened_urls.sql
    │   └── scripts/rate_limiter.lua
    └── test/java/.../UrlShortnerApplicationTests.java
```

### Java source inventory

- `controller/UserController`: `register` and `login` endpoints.
- `controller/UrlController`: authenticated URL create/list/get/update/delete endpoints.
- `controller/RedirectController`: public short-code redirect.
- `config/DotenvEnvironmentPostProcessor`: loads a root `.env` file if present.
- `config/SecurityConfig`: disables CSRF for the stateless API, installs the JWT filter, configures public routes, BCrypt, and authentication manager.
- `config/RateLimitProperties`: binds `rate-limit.*` properties.
- `aspect/RateLimitAspect` and `annotation/RateLimit`: enforce operation-specific limits before controller execution.
- `service/security/*`: login-time user lookup, JWT creation/validation, and request authentication.
- `service/url/ShortenedUrlService`: global URL deduplication, code generation, redirect cache, and click increment.
- `service/url/ClickCountFlushWorker`: scheduled Redis-to-PostgreSQL click flush.
- `service/user/UserService`: registration, user lookup, BCrypt hashing, login, and token creation.
- `service/userUrl/UserUrlService`: user-library association, pagination, status changes, and cache versions.
- `service/rateLimit/RateLimiterService`: executes the Redis Lua token bucket and fails open if Redis/rate-limit execution fails.
- `repository/*`: persistence queries for users, shortened URLs, and user associations.
- `exceptions/GlobalExceptionHandler`: maps selected domain exceptions to JSON errors or `429` responses.

## Local setup and commands

### Prerequisites

- JDK 25 available on `PATH`.
- Docker Desktop/Docker Engine with Compose support.
- Node.js for local functional/rate-limit/monitor scripts.
- k6 is optional and only needed for the heavy stress suites.

### Configure environment

```bash
cp .env.example .env
```

Edit `.env` with real local values. Do not commit `.env`. The JWT secret must be a sufficiently long Base64-encoded HMAC key; the placeholder `RandomKey` in `.env.example` is only a placeholder and is not suitable for production or reliable JJWT startup.

### Start infrastructure

```bash
docker compose up -d
docker compose ps
```

This starts PostgreSQL and Redis. Then start the Spring application:

```bash
./mvnw spring-boot:run       # Linux/macOS/WSL
./mvnw.cmd spring-boot:run   # Windows PowerShell/cmd
```

The default HTTP port is Spring Boot's `8080` because no server port override is configured.

### Build and tests

```bash
./mvnw clean package
./mvnw test
```

Equivalent Make targets:

| Target | Action |
|---|---|
| `make up` | Start Docker Compose services |
| `make down` | Stop Docker Compose services |
| `make restart` | Recreate Docker Compose services |
| `make health` | Run `docker ps -a` |
| `make logs` | Follow Compose logs |
| `make ps` | Show Compose service status |
| `make build` | Run `./mvnw clean package` |
| `make test` | Run `./mvnw test` |
| `make clean` | Run `./mvnw clean` |
| `make shell` | Open `psql` in the PostgreSQL container using the default database/user names |

## Configuration

### Environment variables

The custom `DotenvEnvironmentPostProcessor` reads these keys from `.env` when present and adds them to the Spring environment:

| Variable | Used for |
|---|---|
| `POSTGRES_HOST` | PostgreSQL host |
| `POSTGRES_PORT` | PostgreSQL host port |
| `POSTGRES_DB` | Database name |
| `POSTGRES_USER` | Database user |
| `POSTGRES_PASSWORD` | Database password |
| `REDIS_HOST` | Redis host |
| `REDIS_PORT` | Redis port |
| `JWT_SECRET` | Base64 signing key for JWTs |
| `JWT_EXPIRATION` | JWT lifetime in milliseconds |

The loader uses `ignoreIfMissing()`. Missing variables will still cause unresolved datasource/Redis/JWT configuration failures when the corresponding beans initialize.

### Application properties

The current important properties are:

```properties
spring.application.name=url-shortner
spring.jpa.hibernate.ddl-auto=validate
spring.datasource.hikari.maximum-pool-size=25
spring.datasource.hikari.minimum-idle=10
spring.datasource.hikari.connection-timeout=30000
api.version=v1
management.endpoints.web.exposure.include=health,metrics
```

The datasource URL sets the JDBC session timezone to UTC. The application class sets the JVM default timezone to Asia/Kolkata; entity timestamps therefore use a mixture of `Instant` and `LocalDateTime` as described in the data model section.

Hibernate does not create or alter tables. Flyway migrations run on startup, and Hibernate validates the resulting schema.

### Docker Compose

- PostgreSQL uses `postgres:18`, container name `url-shortener-postgres`, host port `${POSTGRES_PORT}`, and the `postgres_data` named volume.
- Redis uses `redis:latest`, container name `url-shortener-redis`, host port `${REDIS_PORT}`, and the `redis_data` named volume.
- Redis starts with `--maxmemory 64mb --maxmemory-policy volatile-lfu`.

## API contract

The configured API prefix is `/api/${api.version}`. With the current `application.properties`, it is `/api/v1`. The API version can be changed in configuration, but clients and the security allow-list must use the same configured value.

All UUIDs are JSON strings. Request records do not use Bean Validation annotations, so the application currently does not reject malformed/blank URLs or weak passwords through declarative validation.

### Endpoint summary

| Method | Route | Auth | Rate limit | Current success response |
|---|---|---|---|---|
| `POST` | `/api/v1/users/register` | Public | None | `200` with user ID/email |
| `POST` | `/api/v1/users/login` | Public | None | `200` with user ID/email/JWT |
| `POST` | `/api/v1/urls` | JWT | CREATE | `201` with association ID/original URL/short code |
| `GET` | `/api/v1/urls` | JWT | LIST | `200` with `urls` array |
| `GET` | `/api/v1/urls/{urlId}` | JWT | GET | `200` with URL association details |
| `PATCH` | `/api/v1/urls/{urlId}` | JWT | UPDATE | `200` with updated association details |
| `DELETE` | `/api/v1/urls` | JWT | DELETE | `204` with no body |
| `GET` | `/{shortCode}` | Public | None | `302` to original URL or `410` if disabled |
| `GET` | `/actuator/health` | JWT under current security chain | None | Actuator health response |
| `GET` | `/actuator/metrics` and `/actuator/metrics/{name}` | JWT under current security chain | None | Actuator metric response |

The redirect route is a single path segment. `/api/v1/...` routes are more specific controller mappings; the security configuration explicitly permits the three public route patterns.

### User endpoints

#### Register — `POST /api/v1/users/register`

Request:

```json
{
  "email": "user@example.com",
  "password": "secure-password"
}
```

The controller returns HTTP `200` with:

```json
{
  "id": "<user-uuid>",
  "email": "user@example.com"
}
```

The service checks the email, BCrypt-hashes the password, initializes user timestamps and `totalUrlsCreated = 0`, and persists the user. Duplicate email is handled as `409`.

#### Login — `POST /api/v1/users/login`

Request:

```json
{
  "email": "user@example.com",
  "password": "secure-password"
}
```

Success (`200`):

```json
{
  "userId": "<user-uuid>",
  "email": "user@example.com",
  "token": "<jwt>"
}
```

`AuthenticationManager` uses `CustomUserDetailsService` and BCrypt. The token subject is the email and it contains a `userId` string claim.

### URL-management endpoints

All of these require `Authorization: Bearer <jwt>`.

#### Create — `POST /api/v1/urls`

Request:

```json
{
  "originalUrl": "https://example.com/a-long-path"
}
```

Success (`201`):

```json
{
  "id": "<user-url-association-uuid>",
  "originalUrl": "https://example.com/a-long-path",
  "shortCode": "AbCdEf123"
}
```

The returned `id` is the `user_urls.id`, not the `shortened_urls.id`.

Behavior:

1. Find an existing `shortened_urls` row by exact `originalUrl`.
2. If absent, generate a random 9-character code from `[A-Za-z0-9]` and insert the row.
3. If the database reports the named `uk_short_code` constraint, retry with a new code up to five attempts.
4. Find or create the current user's association.
5. Advance the user's URL-list cache version when a new association is created.

The same URL submitted by different users reuses the same global short code.

#### List — `GET /api/v1/urls?page=0&size=20`

Query parameters:

- `page`: zero-based page index; default `0`; must be `>= 0`.
- `size`: default `20`; must be between `1` and `20` inclusive.

Invalid pagination returns an empty `400 Bad Request` response from the controller. Valid requests return:

```json
{
  "urls": [
    {
      "id": "<user-url-association-uuid>",
      "originalUrl": "https://example.com/a-long-path",
      "shortCode": "AbCdEf123",
      "status": "ACTIVE",
      "clickCount": 3,
      "createdAt": "2026-09-23T10:00:00",
      "updatedAt": "2026-09-23T10:00:00"
    }
  ]
}
```

Results are sorted by `user_urls.createdAt` descending. The repository uses a `JOIN FETCH` for `shortenedUrl` to avoid an N+1 query on the list path.

#### Get one — `GET /api/v1/urls/{urlId}`

`urlId` is the current user's association ID. The service scopes the lookup by both authenticated user ID and association ID. A missing association returns `404`.

Success is the same `UserUrlResponse` object shown in the list response.

#### Update status — `PATCH /api/v1/urls/{urlId}`

Request:

```json
{
  "status": "DISABLED"
}
```

`status` is an enum and must be `ACTIVE` or `DISABLED`. The update changes the associated global `shortened_urls.status`, invalidates `redirect:{shortCode}`, advances the user's list-cache version, and returns the updated `UserUrlResponse` with `200`.

#### Delete association — `DELETE /api/v1/urls`

Request:

```json
{
  "urlId": "<user-url-association-uuid>"
}
```

The service deletes only the current user's `user_urls` row, advances the list-cache version, and returns `204`. It deliberately does not delete the global `shortened_urls` row, even if no users reference it afterward.

### Public redirect — `GET /{shortCode}`

No JWT is required.

- If the code does not exist: `404` with the shortened-url-not-found error body.
- If the URL is `DISABLED`: `410 Gone`, empty body, no click increment.
- If the URL is `ACTIVE`: increment click count and return `302 Found` with a `Location` header pointing to `originalUrl`.

The original URL is passed to `URI.create`. Because there is no URL validation or allow-list, the current implementation accepts any string that reaches a syntactically valid URI at redirect time.

## Authentication and security

### Login-time flow

1. The client posts email/password to `/login`.
2. `AuthenticationManager` delegates to `DaoAuthenticationProvider`.
3. `CustomUserDetailsService` performs the database lookup by email.
4. BCrypt verifies the stored hash.
5. `JwtService` signs a token with:
   - `sub`: email
   - `userId`: user UUID string
   - `iat`: issued-at date
   - `exp`: current time plus `jwt.expiration` milliseconds

The JWT secret is Base64-decoded and converted into an HMAC signing key.

### Authenticated request flow

`JwtAuthenticationFilter` runs once per request before `UsernamePasswordAuthenticationFilter`:

1. No `Authorization` header or no `Bearer ` prefix: continue without setting authentication.
2. A bearer token is parsed and signature/expiration validated.
3. Email and user UUID are extracted directly from claims.
4. A lightweight in-memory `User` and `CustomUserDetails` are created.
5. A `UsernamePasswordAuthenticationToken` is placed in the security context.

There is intentionally no database lookup during normal JWT request authentication. The filter does not confirm that the user still exists or has changed state after token issuance.

### Security rules

- CSRF is disabled because the application is designed as a stateless API.
- BCrypt is the configured password encoder.
- Registration, login, and the one-segment redirect route are public.
- Everything else is authenticated, including the exposed Actuator endpoints under the current `SecurityConfig`.
- Invalid/expired bearer tokens are terminated by the filter with HTTP `401` and a small JSON object. This response is not the same shape as `ApiErrorResponse`.
- No roles/authorities are currently assigned; `CustomUserDetails#getAuthorities()` returns an empty collection.
- CORS is not explicitly configured.

## Rate limiting

Rate limiting applies to the five methods in `UrlController`, not to registration, login, redirects, or Actuator.

### Policies

| Operation | Endpoint | Capacity | Refill | Effective bucket TTL |
|---|---|---:|---:|---:|
| `CREATE` | `POST /api/v1/urls` | 10 tokens | 1 token / 6 seconds | 60 seconds |
| `UPDATE` | `PATCH /api/v1/urls/{urlId}` | 30 tokens | 1 token / 2 seconds | 60 seconds |
| `DELETE` | `DELETE /api/v1/urls` | 30 tokens | 1 token / 2 seconds | 60 seconds |
| `LIST` | `GET /api/v1/urls` | 60 tokens | 1 token / second | 60 seconds |
| `GET` | `GET /api/v1/urls/{urlId}` | 60 tokens | 1 token / second | 60 seconds |

These values are configured in `src/main/resources/application.properties` under `rate-limit.policies.*`. The bucket key is:

```text
rate_limit:user:{userId}:{operation-lowercase}
```

### Implementation

1. `@RateLimit(operation = ...)` marks a controller method.
2. `RateLimitAspect` runs before the method, reads the authenticated user UUID, and loads the matching policy.
3. `RateLimiterService` executes `src/main/resources/scripts/rate_limiter.lua` atomically in Redis.
4. The Lua script stores `tokens` and `last_refill`, refills whole tokens based on elapsed milliseconds, consumes one token when available, and returns allowed/remaining/retry-after values.
5. An empty bucket throws `RateLimitExceededException`.
6. The exception handler returns `429 Too Many Requests` with an empty body and a `Retry-After` header in seconds.
7. If Redis or the script fails, the service fails open and allows the request with an unknown remaining-token value (`-1`).

## Data model and migrations

### Final logical schema

```text
users (1) ────────< user_urls >──────── (1) shortened_urls
```

`user_urls` is the association table. The current migrations do not declare a composite unique constraint on `(user_id, url_id)`; the service checks for an existing association before inserting.

### `users`

| Column | Type/constraint | Entity field |
|---|---|---|
| `id` | UUID primary key | `User.id` |
| `email` | `VARCHAR(255) NOT NULL UNIQUE` | `email` |
| `password_hash` | `VARCHAR(255) NOT NULL` | `passwordHash` |
| `created_at` | `TIMESTAMP WITH TIME ZONE NOT NULL` | `Instant createdAt` |
| `updated_at` | `TIMESTAMP WITH TIME ZONE NOT NULL` | `Instant updatedAt` |
| `total_urls_created` | integer, default `0` | `Integer totalUrlsCreated` |

`User` sets timestamps using `Instant`. `totalUrlsCreated` is initialized but is not incremented by the current URL-creation flow.

### `shortened_urls`

| Column | Type/constraint | Entity field |
|---|---|---|
| `id` | UUID primary key | `ShortenedUrl.id` |
| `original_url` | `TEXT NOT NULL UNIQUE` (`uk_original_url`) | `originalUrl` |
| `short_code` | `VARCHAR(20) NOT NULL UNIQUE` (`uk_short_code`) | `shortCode` |
| `click_count` | `BIGINT NOT NULL DEFAULT 0` | `long clickCount` |
| `status` | `VARCHAR(50) NOT NULL DEFAULT 'ACTIVE'` | `UrlStatus status` |
| `created_at` | `TIMESTAMP NOT NULL` | `LocalDateTime createdAt` |
| `updated_at` | `TIMESTAMP NOT NULL` | `LocalDateTime updatedAt` |

`ShortenedUrl` defaults status to `ACTIVE` in `@PrePersist`, timestamps in `@PrePersist`, and updates `updatedAt` in `@PreUpdate`.

### `user_urls`

| Column | Type/constraint | Entity field |
|---|---|---|
| `id` | UUID primary key | `UserUrl.id` |
| `user_id` | UUID foreign key to `users.id` | lazy `User user` |
| `url_id` | UUID foreign key to `shortened_urls.id` | lazy `ShortenedUrl shortenedUrl` |
| `created_at` | `TIMESTAMP NOT NULL` | `LocalDateTime createdAt` |
| `updated_at` | `TIMESTAMP NOT NULL` | `LocalDateTime updatedAt` |

`UserUrl` timestamps are set with `LocalDateTime`. The model has no JPA cascade declaration on either `ManyToOne` relationship.

### Flyway history

- **V1 — `create_tables.sql`** creates `users` and the original `shortened_urls` design with a direct `user_id` foreign key.
- **V2 — `create_user_urls.sql`** drops/recreates `shortened_urls` in normalized form and adds `user_urls`; it temporarily places `status` on `user_urls`.
- **V3 — `move_status_to_shortened_urls.sql`** adds `shortened_urls.status` with default `ACTIVE` and drops `user_urls.status`.

Flyway history is append-only for an already-initialized database. Do not edit an already-applied migration casually; add a new version for future schema changes.

## Request flows

### Create or reuse a URL

```text
POST /api/v1/urls
  -> JwtAuthenticationFilter
  -> RateLimitAspect(CREATE)
  -> UrlController
  -> UserUrlService.createUserUrl
  -> ShortenedUrlService.getOrCreateShortenedUrl
       -> findByOriginalUrl
       -> insert generated code, retry on uk_short_code collision
  -> UserUrlService.addUrlToUser
       -> find existing user/shortened-url association
       -> getReferenceById(userId) for a proxy when inserting a new association
       -> advance list cache version
  -> 201 ShortUrlResponse
```

### List a user's URLs

```text
GET /api/v1/urls
  -> JWT authentication
  -> RateLimitAspect(LIST)
  -> validate page/size
  -> read user:cache_version:{userId}
  -> read user:urls:{userId}:v:{version}:page:{page}:size:{size}
  -> on miss: one JOIN FETCH query, map to UserUrlResponse, cache for 2 minutes
```

### Redirect

```text
GET /{shortCode}
  -> ShortenedUrlService.getByShortCode
       -> read redirect:{shortCode}
       -> on miss: findByShortCode and cache for 1 hour
  -> if DISABLED: 410
  -> if ACTIVE: increment Redis click hash and return 302 Location
```

### Status update

The update is transactional at the service method. It finds the association scoped to the authenticated user, changes the shared `ShortenedUrl.status`, deletes `redirect:{shortCode}`, advances the user's URL-list cache version, and saves the association.

### Delete

Delete is an association operation, not global URL deletion. The global shortened URL, its short code, status, and click count remain unless removed by a future cleanup process (none exists currently).

## Caching and click counting

### Redis key inventory

| Key | Value | TTL | Purpose |
|---|---|---:|---|
| `redirect:{shortCode}` | `{urlUuid}:::{status}:::{originalUrl}` | 1 hour | Redirect lookup cache |
| `user:cache_version:{userId}` | integer version | 7 days after write | O(1) list-cache invalidation |
| `user:urls:{userId}:v:{version}:page:{p}:size:{s}` | Jackson JSON for `UserUrlsResponse` | 2 minutes | Paginated user list cache |
| `url:clicks:active` | Redis hash `{shortenedUrlUuid -> increment}` | no explicit TTL | Current click-write buffer |
| `url:clicks:flush:{uuid}` | Redis hash snapshot | deleted after successful flush | In-flight click snapshot |
| `rate_limit:user:{userId}:{operation}` | Redis hash with token state | capacity × refill interval | Token bucket state |

### User-list cache versioning

The service does not scan Redis with `KEYS`. It reads a numeric version and includes that version in every list key. On create, update, or delete, it increments `user:cache_version:{userId}` and gives the version key a seven-day TTL. Older list entries are left to expire naturally and are no longer read.

### Redirect cache

The redirect cache is a compact delimiter string, parsed with `split(":::", 3)`. On a cache miss, the database record is cached for one hour. On a status update, the exact short-code key is explicitly deleted, so a disabled URL does not remain active in cache for the full TTL.

The original URL is embedded directly in the value. The parser limits the split to three parts, so the remainder of the original URL stays in the third part; URL input validation/encoding is still not implemented.

### Click write-behind

1. A successful active redirect increments `url:clicks:active[urlId]` in Redis.
2. Every five seconds, `ClickCountFlushWorker` checks whether the active key exists.
3. It atomically renames the active hash to `url:clicks:flush:{randomUuid}`. New clicks can immediately write to a fresh active hash.
4. It reads the snapshot and runs a JDBC batch update:

   ```sql
   UPDATE shortened_urls
   SET click_count = click_count + ?
   WHERE id = ?
   ```

5. On success it deletes the flush snapshot.

If Redis increment fails during a redirect, `ShortenedUrlService` attempts a direct repository increment instead. If a flush fails, the worker logs to stderr and leaves the renamed flush key in Redis; there is no explicit retry/dead-letter mechanism. PostgreSQL click counts can normally lag recent redirects by up to five seconds.

## Service and repository responsibilities

### Repositories

- `UserRepository`: `existsByEmail`, `findByEmail`, and standard JPA user operations.
- `ShortenedUrlRepository`: lookup by short code/original URL, code existence check, and a JPQL bulk increment method.
- `UserUrlRepository`: association lookup by user and shortened URL, user-scoped lookup, a `JOIN FETCH` paginated list query, and standard delete/save operations.

### Services

- `UserService.registerUser`: duplicate check, BCrypt hash, save.
- `UserService.loginUser`: authenticate, read `CustomUserDetails`, generate `LoginResponse`.
- `ShortenedUrlService.getOrCreateShortenedUrl`: exact-string deduplication.
- `ShortenedUrlService.shortenUrl`: code generation and collision retry.
- `ShortenedUrlService.getByShortCode`: redirect cache-aside lookup.
- `ShortenedUrlService.incrementClickCount`: Redis buffer with direct database fallback.
- `UserUrlService.createUserUrl`: orchestrates shortened-url lookup and association.
- `UserUrlService.getUserUrls`: versioned cache plus paginated `JOIN FETCH` query.
- `UserUrlService.getUserUrl`: user-scoped association lookup and mapping.
- `UserUrlService.updateStatus`: global status mutation plus both cache invalidations.
- `UserUrlService.removeUserUrl`: association-only delete plus list-cache invalidation.

### DTOs and enums

Request records:

- `RegisterUserRequest(email, password)`
- `LoginRequest(email, password)`
- `CreateShortUrlRequest(originalUrl)`
- `DeleteUrlRequest(urlId)`
- `UpdateUrlStatusRequest(status)`

Response/internal records:

- `UserResponse(id, email)`
- `LoginResponse(userId, email, token)`
- `ShortUrlResponse(id, originalUrl, shortCode)`
- `UserUrlResponse(id, originalUrl, shortCode, status, clickCount, createdAt, updatedAt)`
- `UserUrlsResponse(urls)`
- `ApiErrorResponse(status, message, timestamp)`
- `RedirectCacheDTO(id, originalUrl, status)`
- `RateLimitPolicy(operation, capacity, refillInterval)`
- `RateLimitResult(allowed, remainingTokens, retryAfterSeconds)`

`CreateUrlRequest` and `ShortenedUrlResponse` are present in the source tree but are not used by the current controllers/services. They should not be treated as the active API contract.

Enums:

- `UrlStatus`: `ACTIVE`, `DISABLED`.
- `RateLimitOperation`: `CREATE`, `UPDATE`, `DELETE`, `LIST`, `GET`.

## Error behavior

### Standard domain error shape

For exceptions handled by `GlobalExceptionHandler`:

```json
{
  "status": 404,
  "message": "URL not found for user",
  "timestamp": "2026-09-23T10:00:00"
}
```

Mappings:

| Exception | Status | Typical cause |
|---|---:|---|
| `UserAlreadyExistsException` | `409` | Registration email already exists |
| `InvalidCredentialsException` | `401` | Custom invalid-credentials path, if thrown |
| `UserNotFoundException` | `404` | User lookup missing |
| `ShortenedUrlNotFoundException` | `404` | Unknown public short code |
| `UserUrlNotFoundException` | `404` | Association is absent or belongs to another user |

Other response paths:

- `RateLimitExceededException`: `429`, empty body, `Retry-After` header.
- Invalid JWT caught in `JwtAuthenticationFilter`: `401` with `{ "status": 401, "message": "Invalid or expired JWT token" }` and no timestamp.
- Missing credentials on a protected route are handled by Spring Security's default entry-point behavior; no project-specific JSON entry point is configured.
- Invalid enum/path/body conversion and malformed request JSON use Spring MVC's default error handling; there is no global `MethodArgumentNotValidException`/binding handler.
- Disabled public URL is not an exception: it returns `410` with an empty body.

The `InvalidCredentialsException` class exists, but the current login implementation calls `AuthenticationManager` directly and does not explicitly translate every Spring `AuthenticationException` into that custom exception. Verify exact login error payloads with an integration test before relying on them as a stable contract.

## Testing and load testing

### Checked-in test coverage

`src/test/java/com/aryan/url_shortner/UrlShortnerApplicationTests.java` contains only:

```java
@SpringBootTest
void contextLoads()
```

It requires a working application configuration and external PostgreSQL/Redis services unless test replacements are supplied. There are no checked-in controller, repository, service, security, cache, rate-limit, or redirect tests.

### Local functional test

`load-tests/functional-test.js` attempts the flow:

```text
register -> login -> create -> list -> get -> active redirect
         -> disable -> disabled redirect -> delete -> verify deleted
```

It is currently stale relative to the Java code: it expects registration `201` while the controller returns `200`, and it expects a disabled redirect of `400/404` while the controller returns `410`. Treat it as a starting point that needs alignment before using it as a pass/fail gate.

### Local rate-limit tests

`load-tests/rate-limit-tests/run-all-endpoints.js` sends concurrent bursts against all five protected URL operations and then sends one excess request. The local report dated 2026-09-21 records:

- CREATE: 10 allowed, request 11 returned `429`.
- LIST: 60 allowed, request 61 returned `429`.
- GET: 60 allowed, request 61 returned `429`.
- UPDATE: 30 allowed, request 31 returned `429`.
- DELETE: 30 allowed, request 31 returned `429`.

The older `run-tests.js` also checks six-second CREATE refill and 50-request concurrency atomicity. These scripts contain stale success-code assumptions and should be updated to expect the current controller responses.

### k6 stress tests

The local ignored directories contain:

- `load-tests/stress/`: endpoint stress scripts using shared login/config helpers.
- `load-tests/pool-stress/`: endpoint-specific pool stress scripts for create, list, redirect, get-by-id, update, and delete.
- `load-tests/pool-stress/pool-monitor.js`: polls Actuator HikariCP, CPU, JVM memory, and thread metrics every two seconds.
- `run-stress-tests.ps1/.bat` and `run-pool-tests.ps1/.bat`: sequential runners that write timestamped JSON under `results/`.
- `run-all-tests.ps1/.bat`: local master runner for functional tests, rate-limit tests, database seeding, and optional pool tests.

Pool tests ramp through 10, 25, 50, 100, 150, and 200 VUs. Each level ramps for ten seconds and holds for thirty seconds, followed by a ten-second cooldown: approximately four minutes ten seconds per test.

The local pool runner comments still say the Hikari maximum is 10, but the current application configuration is 25. Update those comments/reports before treating them as authoritative.

### Result-artifact cautions

The `results/` directory contains very large local k6 output files. `parse-results.js` expects line-delimited k6 points, while the existing artifacts may not match that exact format; do not infer a complete benchmark summary from the parser's lack of output. Use k6's own summary or a parser matched to the actual artifact format.

## Current limitations and next work

These are implementation observations, not claims that they have already been fixed:

1. **No request validation.** Add `jakarta.validation` annotations and a consistent binding-error handler for email, password, URL, UUID, pagination, and status inputs.
2. **Repository ID generic mismatch.** `UserUrlRepository` currently extends `JpaRepository<UserUrl, Long>` even though `UserUrl.id` is a `UUID`. Change the generic ID type to `UUID` and add/adjust tests.
3. **No association database uniqueness constraint.** Concurrent duplicate creates for the same user and URL can race because uniqueness is enforced only by a service-level read. Add a composite unique constraint and handle the conflict if duplicate associations are forbidden.
4. **Global URL orphan cleanup.** Deleting a user's association never deletes unreferenced `shortened_urls`; add an explicit retention/cleanup policy if desired.
5. **`total_urls_created` is not maintained.** Decide whether it is a real counter; if so, increment it transactionally during association creation.
6. **Status is global.** This is intentional in the current schema, but if each owner should be able to disable independently, status must move back to `user_urls` and the redirect semantics must be redesigned.
7. **Single-item association queries are not fetch joins.** `getUserUrl` and `updateStatus` access the lazy shortened URL after a normal association lookup; add a user-scoped `JOIN FETCH` query if query count matters.
8. **Click-flush failure recovery is incomplete.** Failed flush snapshots are logged and retained without an explicit retry/alert policy. Make the worker observable and idempotently retryable.
9. **Redis availability behavior is asymmetric.** Rate limiting fails open and click increments have a database fallback, but redirect-cache reads/writes and user-list cache-version calls are not comprehensively wrapped. Decide whether cache outages should always degrade to PostgreSQL.
10. **Cross-user list-cache staleness.** Status is global, but an update advances only the updating user's list-cache version. Another owner's cached list can show the old status for up to two minutes.
11. **JWT revocation/user deletion.** Stateless request auth does not check that the user still exists, and there is no revocation list or refresh-token flow.
12. **Authentication error contract.** Add an explicit Spring Security entry point and authentication-exception translation if clients need the same `ApiErrorResponse` shape for all `401` responses.
13. **Actuator exposure/security.** Only health/metrics are exposed, but they currently fall under the authenticated catch-all and have no role separation or management port.
14. **Test coverage and script drift.** Add integration tests with Testcontainers or a controlled PostgreSQL/Redis environment, then align the local Node/k6 scripts with the actual `200/201/204/410` contract.
15. **Secrets in local load scripts.** The local scripts contain hard-coded test credentials and tokens/configuration. Keep those files ignored and replace secrets with environment variables before sharing or CI use.
16. **Configuration hardening.** Use a generated strong Base64 JWT secret, avoid `redis:latest` in reproducible deployments, and make the database/Redis health checks explicit in Compose.

### Recommended next implementation order

1. Fix the `UserUrlRepository` UUID generic and add integration tests for all endpoint status codes.
2. Add request validation and a consistent error contract.
3. Align or replace the local functional/rate-limit/k6 scripts with the current API.
4. Add concurrency-safe association uniqueness and global orphan cleanup policy.
5. Improve click-flush retry/metrics and decide the desired Redis outage behavior.
6. Add production hardening for secrets, Actuator authorization, CORS, and reproducible container versions.

## Quick agent handoff

When modifying this project, use this order:

1. Read `application.properties`, `SecurityConfig`, the three controllers, and this document.
2. Check Flyway migrations before changing entities or repository queries.
3. Preserve the distinction between global `shortened_urls.id` and user-facing `user_urls.id`.
4. Preserve JWT statelessness unless the task explicitly changes the authentication design.
5. Preserve cache invalidation on status/list mutations and the five-second click-write-behind behavior unless the task changes those guarantees.
6. Treat the current Java controller status codes as authoritative: register `200`, login `200`, create `201`, list/get/update `200`, delete `204`, active redirect `302`, disabled redirect `410`.
7. Run `./mvnw test` with PostgreSQL and Redis available, then run focused integration/load tests relevant to the change.
8. Update this file whenever routes, request/response records, migrations, cache keys, rate policies, setup commands, tests, or known limitations change.
