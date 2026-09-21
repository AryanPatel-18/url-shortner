# URL Shortener — Complete Project Documentation

This document provides a comprehensive explanation of the entire URL Shortener project. It covers the architecture, every endpoint, all database interactions, caching strategies, authentication flow, and performance optimizations that have been implemented. Use this as context when working with an AI or onboarding a new developer.

---

## Table of Contents

1. [Tech Stack](#tech-stack)
2. [Infrastructure & Environment](#infrastructure--environment)
3. [Database Schema](#database-schema)
4. [Project Structure](#project-structure)
5. [Authentication & Security](#authentication--security)
6. [API Endpoints](#api-endpoints)
7. [Service Layer Architecture](#service-layer-architecture)
8. [Caching Strategy](#caching-strategy)
9. [Click Count System](#click-count-system)
10. [Performance Optimizations](#performance-optimizations)
11. [Load Testing Infrastructure](#load-testing-infrastructure)
12. [Known Considerations & Future Work](#known-considerations--future-work)

---

## Tech Stack

| Component       | Technology                          |
|-----------------|-------------------------------------|
| Language        | Java 25                             |
| Framework       | Spring Boot 4.1.1                   |
| Database        | PostgreSQL 18                       |
| Cache           | Redis (latest, volatile-lfu policy) |
| ORM             | Spring Data JPA / Hibernate         |
| Migrations      | Flyway                              |
| Authentication  | JWT (jjwt library)                  |
| Build Tool      | Maven (Maven Wrapper included)      |
| Containerization| Docker Compose                      |
| Load Testing    | k6 + Node.js monitor scripts        |
| Password Hashing| BCrypt                              |

---

## Infrastructure & Environment

### Docker Compose (`docker-compose.yml`)

The project runs PostgreSQL and Redis as Docker containers:

- **PostgreSQL 18**: Exposed on `${POSTGRES_PORT}` (mapped to internal 5432). Data persisted via `postgres_data` volume.
- **Redis**: Exposed on `${REDIS_PORT}` (mapped to internal 6379). Configured with `--maxmemory 64mb` and `--maxmemory-policy volatile-lfu`. Data persisted via `redis_data` volume.

### Environment Variables (loaded via `.env` file)

The project uses a custom `DotenvEnvironmentPostProcessor` (registered as a Spring `EnvironmentPostProcessor`) to load variables from a `.env` file at the project root. Required variables:

| Variable            | Purpose                              |
|---------------------|--------------------------------------|
| `POSTGRES_HOST`     | PostgreSQL hostname                  |
| `POSTGRES_PORT`     | PostgreSQL port                      |
| `POSTGRES_DB`       | Database name                        |
| `POSTGRES_USER`     | Database username                    |
| `POSTGRES_PASSWORD` | Database password                    |
| `REDIS_HOST`        | Redis hostname                       |
| `REDIS_PORT`        | Redis port                           |
| `JWT_SECRET`        | Base64-encoded HMAC secret key       |
| `JWT_EXPIRATION`    | Token expiration in milliseconds     |

### Application Properties (`application.properties`)

```properties
spring.application.name=url-shortner

spring.datasource.url=jdbc:postgresql://${POSTGRES_HOST}:${POSTGRES_PORT}/${POSTGRES_DB}
spring.datasource.username=${POSTGRES_USER}
spring.datasource.password=${POSTGRES_PASSWORD}

# HikariCP Connection Pool
spring.datasource.hikari.maximum-pool-size=25
spring.datasource.hikari.minimum-idle=10
spring.datasource.hikari.connection-timeout=30000

spring.jpa.hibernate.ddl-auto=validate

spring.data.redis.host=${REDIS_HOST}
spring.data.redis.port=${REDIS_PORT}

jwt.secret=${JWT_SECRET}
jwt.expiration=${JWT_EXPIRATION}

spring.autoconfigure.exclude=org.springframework.boot.actuate.autoconfigure.endpoint.jackson.Jackson2EndpointAutoConfiguration
management.endpoints.web.exposure.include=health,metrics

api.version=v1
```

Key notes:
- `ddl-auto=validate` means Hibernate only validates the schema against entity mappings; it does NOT create or modify tables. Flyway handles all migrations.
- HikariCP pool is set to 25 max connections (upgraded from the default of 10 after performance testing revealed it as a bottleneck).
- Spring Boot Actuator is enabled for `health` and `metrics` endpoints (used by the load test monitor).

---

## Database Schema

The schema is managed by Flyway with 3 migration files:

### V1: Initial Tables (`V1__create_tables.sql`)
Creates `users` and an initial `shortened_urls` table (with `user_id` foreign key — later refactored in V2).

### V2: Refactored Schema (`V2__create_user_urls.sql`)
Drops the original `shortened_urls` table and creates the current normalized schema:

- **`shortened_urls`**: Stores the actual shortened URL data. `original_url` is globally unique (TEXT, UNIQUE). `short_code` is unique (VARCHAR(20), UNIQUE).
- **`user_urls`**: Join table linking users to their shortened URLs. Contains `user_id` (FK → users) and `url_id` (FK → shortened_urls). This allows multiple users to share the same shortened URL without duplicating `shortened_urls` rows.

### V3: Status Migration (`V3__move_status_to_shortened_urls.sql`)
Moves the `status` column from `user_urls` to `shortened_urls`, making URL status (ACTIVE/DISABLED) a global property of the shortened URL itself rather than per-user.

### Final Schema

```
┌──────────────────────┐     ┌──────────────────────┐     ┌──────────────────────┐
│       users          │     │     user_urls         │     │   shortened_urls     │
├──────────────────────┤     ├──────────────────────┤     ├──────────────────────┤
│ id (UUID, PK)        │◄────│ user_id (UUID, FK)   │     │ id (UUID, PK)        │
│ email (UNIQUE)       │     │ url_id (UUID, FK)    │────►│ original_url (UNIQUE)│
│ password_hash        │     │ id (UUID, PK)        │     │ short_code (UNIQUE)  │
│ created_at           │     │ created_at           │     │ click_count          │
│ updated_at           │     │ updated_at           │     │ status (ACTIVE/      │
│ total_urls_created   │     └──────────────────────┘     │         DISABLED)    │
└──────────────────────┘                                   │ created_at           │
                                                           │ updated_at           │
                                                           └──────────────────────┘
```

---

## Project Structure

```
src/main/java/com/aryan/url_shortner/
├── config/
│   ├── DotenvEnvironmentPostProcessor.java   # Loads .env file into Spring Environment
│   └── SecurityConfig.java                   # Spring Security configuration
├── controller/
│   ├── RedirectController.java               # GET /{shortCode} — public redirect
│   ├── UrlController.java                    # CRUD endpoints for user URLs
│   └── UserController.java                   # Registration and login
├── dto/
│   ├── ApiErrorResponse.java                 # Standard error response format
│   ├── CreateShortUrlRequest.java            # POST /urls request body
│   ├── DeleteUrlRequest.java                 # DELETE /urls request body
│   ├── LoginRequest.java                     # Login request body
│   ├── LoginResponse.java                    # Login response (includes JWT token)
│   ├── RedirectCacheDTO.java                 # Cached redirect data (id, url, status)
│   ├── RegisterUserRequest.java              # Registration request body
│   ├── ShortUrlResponse.java                 # POST /urls response
│   ├── UpdateUrlStatusRequest.java           # PATCH /urls/{id} request body
│   ├── UserResponse.java                     # Registration response
│   ├── UserUrlResponse.java                  # Single URL detail response
│   └── UserUrlsResponse.java                 # Paginated URL list response
├── enums/
│   └── UrlStatus.java                        # ACTIVE, DISABLED
├── exceptions/
│   ├── GlobalExceptionHandler.java           # @RestControllerAdvice handler
│   ├── InvalidCredentialsException.java      # → 401
│   ├── ShortenedUrlNotFoundException.java    # → 404
│   ├── UserAlreadyExistsException.java       # → 409
│   ├── UserNotFoundException.java            # → 404
│   └── UserUrlNotFoundException.java         # → 404
├── model/
│   ├── CustomUserDetails.java                # Spring Security UserDetails wrapper
│   ├── ShortenedUrl.java                     # JPA entity
│   ├── User.java                             # JPA entity
│   └── UserUrl.java                          # JPA entity (join table)
├── repository/
│   ├── ShortenedUrlRepository.java
│   ├── UserRepository.java
│   └── UserUrlRepository.java
└── service/
    ├── security/
    │   ├── CustomUserDetailsService.java     # Loads user from DB (used during login)
    │   ├── ICustomUserDetailsService.java
    │   ├── IJwtService.java
    │   ├── JwtAuthenticationFilter.java      # Stateless JWT filter (no DB lookup)
    │   └── JwtService.java                   # JWT generation + validation
    ├── url/
    │   ├── ClickCountFlushWorker.java        # Scheduled Redis → DB click flusher
    │   ├── IShortenedUrlService.java
    │   └── ShortenedUrlService.java          # URL shortening + redirect caching
    ├── user/
    │   ├── IUserService.java
    │   └── UserService.java                  # Registration + login
    └── userUrl/
        ├── IUserUrlService.java
        └── UserUrlService.java               # User-URL CRUD + cache management
```

---

## Authentication & Security

### Overview

The application uses **stateless JWT authentication**. After a user logs in, they receive a JWT token containing their `email` (as the subject) and their `userId` (as a custom claim). All subsequent API requests include this token in the `Authorization: Bearer <token>` header.

### How It Works (Step by Step)

#### 1. Login Flow
1. User sends `POST /api/v1/users/login` with `{ email, password }`.
2. `UserService.loginUser()` authenticates via Spring Security's `AuthenticationManager`.
3. During authentication, `CustomUserDetailsService.loadUserByEmail()` queries the database for the user (this is the ONLY time the DB is queried for auth).
4. If credentials are valid, `JwtService.generateToken()` creates a JWT with:
   - `sub` (subject) = user's email
   - `userId` = user's UUID (custom claim)
   - `iat` = issued-at timestamp
   - `exp` = expiration timestamp
5. The token is returned in the `LoginResponse`.

#### 2. Request Authentication (Stateless — No DB Lookup)
1. `JwtAuthenticationFilter` intercepts every request.
2. If the `Authorization` header contains a valid `Bearer` token:
   - Extracts the `email` and `userId` directly from the token payload.
   - Constructs a lightweight `User` object **in memory** (no database query).
   - Wraps it in `CustomUserDetails` and sets it in the `SecurityContextHolder`.
3. Controllers access the authenticated user via `authentication.getPrincipal()` → `CustomUserDetails` → `User`.

#### 3. Public Endpoints (No Auth Required)
Configured in `SecurityConfig.java`:
- `POST /api/v1/users/register`
- `POST /api/v1/users/login`
- `GET /{shortCode}` (redirect)

All other endpoints require a valid JWT.

### Security Configuration
- CSRF is disabled (stateless API).
- Password encoding uses BCrypt.
- `DaoAuthenticationProvider` handles login validation.

---

## API Endpoints

### User Endpoints (`/api/v1/users`)

#### `POST /api/v1/users/register`
- **Auth**: None
- **Body**: `{ "email": "...", "password": "..." }`
- **Response**: `{ "id": "uuid", "email": "..." }`
- **Errors**: 409 if email already exists

#### `POST /api/v1/users/login`
- **Auth**: None
- **Body**: `{ "email": "...", "password": "..." }`
- **Response**: `{ "userId": "uuid", "email": "...", "token": "jwt-token" }`
- **Errors**: 401 if credentials are invalid

---

### URL Management Endpoints (`/api/v1/urls`)

#### `POST /api/v1/urls` — Create Short URL
- **Auth**: Required (JWT)
- **Body**: `{ "originalUrl": "https://example.com/very-long-url" }`
- **Response**: `{ "id": "uuid", "originalUrl": "...", "shortCode": "AbCdEfGhI" }`
- **Flow**:
  1. `UserUrlService.createUserUrl()` → `ShortenedUrlService.getOrCreateShortenedUrl()`
  2. Checks if `originalUrl` already exists in `shortened_urls` (deduplication).
  3. If not, generates a random 9-character alphanumeric short code and saves it.
  4. Collision handling: If the generated short code collides with an existing one (unique constraint violation), it retries up to 5 times with a new random code.
  5. Creates a `user_urls` entry linking the authenticated user to the shortened URL.
  6. If the user already has this URL linked, returns the existing mapping without creating a duplicate.
  7. Clears the user's paginated URL cache in Redis.

#### `GET /api/v1/urls` — List User's URLs (Paginated)
- **Auth**: Required (JWT)
- **Query Params**: `page` (default 0), `size` (default 20, max 20)
- **Response**: `{ "urls": [ { "id", "originalUrl", "shortCode", "status", "clickCount", "createdAt", "updatedAt" }, ... ] }`
- **Flow**:
  1. Validates pagination params (page ≥ 0, 1 ≤ size ≤ 20).
  2. Checks Redis cache for key `user:urls:{userId}:page:{page}:size:{size}`.
  3. On cache hit: deserializes JSON and returns immediately (no DB query).
  4. On cache miss: executes a `JOIN FETCH` JPQL query to load `UserUrl` entities with their `ShortenedUrl` relationships in a **single query** (eliminates N+1 problem).
  5. Serializes the response to JSON and caches it in Redis with a 2-minute TTL.

#### `GET /api/v1/urls/{urlId}` — Get Single URL
- **Auth**: Required (JWT)
- **Response**: `{ "id", "originalUrl", "shortCode", "status", "clickCount", "createdAt", "updatedAt" }`
- **Flow**: Queries `user_urls` by `userId` and `urlId`. Returns 404 if not found.

#### `PATCH /api/v1/urls/{urlId}` — Update URL Status
- **Auth**: Required (JWT)
- **Body**: `{ "status": "ACTIVE" }` or `{ "status": "DISABLED" }`
- **Response**: Updated URL details
- **Flow**: Finds the `UserUrl`, updates the associated `ShortenedUrl`'s status, clears the user's URL cache, and saves.

#### `DELETE /api/v1/urls` — Remove URL from User
- **Auth**: Required (JWT)
- **Body**: `{ "urlId": "uuid" }`
- **Response**: 204 No Content
- **Flow**: Finds and deletes the `user_urls` mapping. Clears the user's URL cache. Does NOT delete the `shortened_urls` entry (other users may still reference it).

---

### Redirect Endpoint

#### `GET /{shortCode}` — Redirect to Original URL
- **Auth**: None (public)
- **Response**: HTTP 302 redirect with `Location` header, or 410 Gone if URL is disabled
- **Flow**:
  1. `ShortenedUrlService.getByShortCode()` checks Redis cache first (key: `redirect:{shortCode}`).
  2. Cache format: `{id}:::{status}:::{originalUrl}` (high-performance string splitting, no JSON overhead).
  3. On cache miss: queries PostgreSQL, caches the result for 1 hour.
  4. If the URL status is `DISABLED`, returns HTTP 410 Gone.
  5. If `ACTIVE`, increments click count via Redis hash (`url:clicks:active`) and returns HTTP 302 redirect.

---

## Service Layer Architecture

### ShortenedUrlService

Handles the core URL shortening logic and redirect caching.

- **`getOrCreateShortenedUrl(originalUrl)`**: Deduplicates URLs. Checks if `originalUrl` exists; if so returns it, otherwise creates a new shortened URL.
- **`shortenUrl(originalUrl)`**: Generates a random 9-character alphanumeric code (62^9 = 13.5 quadrillion combinations). Uses a retry loop (max 5 attempts) with `DataIntegrityViolationException` catch for the astronomically rare collision case.
- **`getByShortCode(shortCode)`**: Cache-aside pattern. Returns a `RedirectCacheDTO` from Redis or falls back to PostgreSQL.
- **`incrementClickCount(urlId)`**: Writes to a Redis hash (`url:clicks:active`). Falls back to direct DB update if Redis fails.

### UserUrlService

Manages the relationship between users and their shortened URLs.

- **`createUserUrl(userId, originalUrl)`**: Orchestrates the full creation flow.
- **`addUrlToUser(userId, shortenedUrl)`**: Links a user to a shortened URL using `UserUrl`. Uses `userRepository.getReferenceById(userId)` to avoid an unnecessary `SELECT` query on the `users` table.
- **`getUserUrls(userId, page, size)`**: Paginated listing with Redis caching (Jackson serialization).
- **`getUserUrl(userId, urlId)`**: Single URL lookup.
- **`updateStatus(userId, urlId, status)`**: Updates URL status with `@Transactional`.
- **`removeUserUrl(userId, urlId)`**: Deletes the user-URL mapping.
- **`clearUserUrlsCache(userId)`**: Invalidates all paginated cache entries for a user using Redis `KEYS` pattern matching.

### UserService

Handles user registration and login.

- **`registerUser(request)`**: Checks for duplicate email, hashes password with BCrypt, saves user.
- **`loginUser(request)`**: Uses Spring Security's `AuthenticationManager` to validate credentials, generates JWT token.

### JwtService

Handles all JWT operations.

- **`generateToken(userDetails)`**: Creates JWT with email as subject and userId as custom claim.
- **`extractUsername(token)`**: Extracts email from JWT subject.
- **`extractUserId(token)`**: Extracts userId from custom claim.
- **`isTokenValid(token)`**: Validates signature and checks expiration without requiring a `UserDetails` parameter (stateless).

### ClickCountFlushWorker

A scheduled background worker (`@Scheduled(fixedDelay = 5000)`) that flushes accumulated click counts from Redis to PostgreSQL every 5 seconds.

**How it works**:
1. Atomically renames the `url:clicks:active` Redis hash to a unique `url:clicks:flush:{uuid}` key (prevents writes during flush).
2. Reads all entries from the flush key.
3. Executes a JDBC batch update to increment `click_count` in `shortened_urls` for each URL.
4. Deletes the flush key.

This design ensures that the high-frequency redirect endpoint never blocks on database writes for click counting.

---

## Caching Strategy

### Redis Cache Keys

| Key Pattern                                    | Value Format                    | TTL      | Purpose                          |
|------------------------------------------------|---------------------------------|----------|----------------------------------|
| `redirect:{shortCode}`                         | `{id}:::{status}:::{url}`      | 1 hour   | Redirect lookup cache            |
| `user:urls:{userId}:page:{p}:size:{s}`         | JSON (Jackson serialized)       | 2 min    | Paginated user URL list cache    |
| `url:clicks:active`                            | Redis Hash: `{urlId} → count`   | None     | Accumulated click counts         |
| `url:clicks:flush:{uuid}`                      | Redis Hash (same as above)      | Transient| Snapshot during flush            |

### Cache Invalidation

- **User URL list cache**: Invalidated on every write operation (create, update, delete) using `clearUserUrlsCache()` which uses Redis `KEYS` pattern matching to find and delete all paginated entries for a user.
- **Redirect cache**: Entries expire naturally after 1 hour. Not explicitly invalidated on status changes (this means a disabled URL may still redirect for up to 1 hour until the cache expires).

---

## Click Count System

The click counting system uses a **write-behind** pattern to avoid database writes on every redirect:

1. **On redirect**: Click count is incremented in a Redis hash (`url:clicks:active`) — O(1) operation, no DB involved.
2. **Every 5 seconds**: `ClickCountFlushWorker` atomically swaps the hash to a flush key and batch-updates PostgreSQL.
3. **Fallback**: If Redis is unavailable during a redirect, the click count is written directly to the database using `ShortenedUrlService.incrementClickCount()`.

This means click counts in the database may lag behind by up to 5 seconds, but this is acceptable for analytics purposes and dramatically reduces database load.

---

## Performance Optimizations

The following optimizations were implemented and verified through load testing:

### 1. Stateless JWT Authentication
- **Problem**: The original `JwtAuthenticationFilter` queried the database (`SELECT * FROM users WHERE email = ?`) on EVERY authenticated request to load the user.
- **Fix**: The `userId` is now stored as a custom claim inside the JWT. The filter extracts both `email` and `userId` directly from the token and constructs a lightweight `User` object in memory. Zero database queries during authentication.
- **Impact**: Eliminated one DB query per request across ALL authenticated endpoints.

### 2. N+1 Query Elimination (JOIN FETCH)
- **Problem**: `getUserUrls()` fetched `UserUrl` entities, then lazy-loaded each `ShortenedUrl` individually (N+1 queries).
- **Fix**: Added a custom JPQL query in `UserUrlRepository` with `JOIN FETCH uu.shortenedUrl` to load everything in a single query.
- **Impact**: Reduced queries from N+1 to exactly 1. The `GET /api/v1/urls` endpoint went from 2.37 req/s to 671 req/s (283x improvement).

### 3. Pagination
- **Problem**: `getUserUrls()` loaded ALL of a user's URLs in a single response.
- **Fix**: Added server-side pagination with `page` and `size` query parameters (max 20 per page).
- **Impact**: Reduced response payload size and database load for users with many URLs.

### 4. High-Performance Redirect Caching
- **Problem**: The redirect cache originally used `Map<Object, Object>` Redis Hash storage, which was slow.
- **Fix**: Replaced with a simple string format (`id:::status:::originalUrl`) using `String.split()` for deserialization instead of Jackson/Map operations.
- **Impact**: Reduced Redis overhead and CPU usage during redirect lookups.

### 5. Proxy User Reference (getReferenceById)
- **Problem**: `addUrlToUser()` called `userService.getUser(userId)` which triggered a `SELECT * FROM users` query just to set a foreign key.
- **Fix**: Replaced with `userRepository.getReferenceById(userId)` which creates a JPA proxy object in memory without touching the database.
- **Impact**: Eliminated one unnecessary DB query per URL creation.

### 6. Redundant Save Removal
- **Problem**: `getOrCreateShortenedUrl()` called `shortenUrl()` (which already does `save()`), then called `save()` again on the returned entity.
- **Fix**: Removed the redundant second `save()`.
- **Impact**: Eliminated an unnecessary Hibernate dirty-check and potential UPDATE query per creation.

### 7. HikariCP Pool Size Increase
- **Problem**: The default pool size of 10 connections was saturating at ~25 concurrent users.
- **Fix**: Increased `maximum-pool-size` to 25 with `minimum-idle` of 10.
- **Impact**: Pool saturation point pushed much higher, allowing more concurrent requests.

### 8. Short Code Length Increase
- **Problem**: 6-character codes (62^6 = 56.8 billion) were experiencing rare collisions under heavy load testing.
- **Fix**: Increased to 9 characters (62^9 = 13.5 quadrillion combinations).
- **Impact**: Collisions are now mathematically impossible at any realistic scale.

---

## Load Testing Infrastructure

The project includes a comprehensive k6 + Node.js load testing setup in `load-tests/pool-stress/`:

### Scripts

- **`config.js`**: Shared configuration (base URL, credentials, VU ramp-up stages).
- **`auth.js`**: Login helper for k6 scripts.
- **`pool-monitor.js`**: Node.js script that polls Spring Boot Actuator every 2 seconds for HikariCP connection pool metrics, CPU usage, JVM memory, and thread count. Saves snapshots to JSON.
- **`pool-stress-create-url.js`**: Stress test for `POST /api/v1/urls`. Generates unique URLs per iteration.
- **`pool-stress-get-urls.js`**: Stress test for `GET /api/v1/urls`. Randomizes page parameter (0-3).
- **`pool-stress-redirect.js`**: Stress test for `GET /{shortCode}`.
- **`pool-stress-get-url-by-id.js`**: Stress test for `GET /api/v1/urls/{urlId}`.
- **`pool-stress-update-url.js`**: Stress test for `PATCH /api/v1/urls/{urlId}`.
- **`run-pool-tests.ps1` / `run-pool-tests.bat`**: PowerShell/batch scripts to orchestrate tests.

### VU Ramp-Up Stages
```
10 VUs (40s) → 25 VUs (40s) → 50 VUs (40s) → 100 VUs (40s) → 150 VUs (40s) → 200 VUs (40s) → Cooldown (10s)
Total: ~4 minutes 10 seconds
```

### Running Tests
```powershell
.\load-tests\pool-stress\run-pool-tests.bat -TestName pool-stress-create-url
```
Results are saved as JSON files in the `results/` directory.

---

## Known Considerations & Future Work

### Current Limitations

1. **Redis `KEYS` Command**: The `clearUserUrlsCache()` method uses `redisTemplate.keys("user:urls:{userId}:page:*")` which scans the entire Redis keyspace. This is blocking and should be replaced with a cache versioning strategy for production use.

2. **Redirect Cache Staleness**: When a URL's status is changed to `DISABLED`, the redirect cache (`redirect:{shortCode}`) is not explicitly invalidated. The cached entry will continue serving redirects until it naturally expires after 1 hour.

3. **No Rate Limiting**: There is no rate limiting on any endpoint. Under extreme load, the API relies solely on the HikariCP pool and Redis to manage throughput.

4. **No Input Validation**: URL format validation is not implemented. Any string can be submitted as an `originalUrl`.

5. **`UserUrlRepository` Generic Type**: The repository extends `JpaRepository<UserUrl, Long>` but the `UserUrl` entity uses `UUID` as its primary key type. This should be `JpaRepository<UserUrl, UUID>`.

### Potential Improvements

1. **Cache Versioning**: Replace `KEYS` pattern matching with an atomic Redis `INCREMENT` on a version key (e.g., `user:cache_version:{userId}`). Cache keys would include the version number, and invalidation becomes O(1).

2. **JOIN FETCH for Single URL Lookups**: `getUserUrl()` and `updateStatus()` still use `findByUserIdAndId()` which triggers a lazy-loading N+1 query when accessing `getShortenedUrl()`. A `findByUserIdAndIdWithShortenedUrl()` method with `JOIN FETCH` would eliminate this.

3. **Explicit Redirect Cache Invalidation**: When `updateStatus()` is called, the redirect cache for the affected short code should be explicitly deleted or updated.

4. **URL Validation**: Add validation for `originalUrl` to ensure it's a valid HTTP/HTTPS URL.

5. **Bulk URL Creation**: A `POST /api/v1/urls/batch` endpoint could leverage JDBC batch inserts for users submitting multiple URLs.
