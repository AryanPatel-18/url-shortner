# URL Shortener

A production-ready URL shortening REST API built with Spring Boot, PostgreSQL, Redis, and Spring Security. The application converts long URLs into compact, shareable short links, tracks click analytics, and protects all endpoints with JWT authentication and per-user rate limiting.

## Features

- **URL Shortening** — Convert any long URL into a unique 9-character short code. If the same URL is submitted again, the existing short code is reused.
- **Short Code Collision Handling** — Automatic retry logic (up to 5 attempts) handles the rare case of a generated code conflicting with an existing one at the database constraint level.
- **JWT Authentication** — Stateless authentication using signed JSON Web Tokens. Registration and login endpoints are public; all URL management endpoints require a valid `Bearer` token.
- **Many-to-Many URL Ownership** — A `user_urls` join table allows multiple users to independently own and manage the same shortened URL. Each user sees their own library without affecting others.
- **URL Lifecycle Management** — Users can create, list (with pagination), view, update status (`ACTIVE`/`DISABLED`), and delete URLs from their personal library.
- **Click Tracking with Redis Write-Behind** — Click counts are buffered in a Redis hash and flushed to PostgreSQL in batches every 5 seconds by a scheduled background worker, eliminating per-request `UPDATE` queries on the hot redirect path.
- **Redis Redirect Caching** — Short-code-to-URL lookups are cached in Redis with a 1-hour TTL, drastically reducing database reads for popular links. Cache is automatically invalidated when a URL's status is updated.
- **AOP-Based Rate Limiting** — A Token Bucket algorithm implemented as an atomic Redis Lua script, wired via Spring AOP and a custom `@RateLimit` annotation. Rate limits are enforced per-user, per-operation, and are fully configurable through `application.properties`.
- **Fail-Open Circuit Breaking** — If Redis becomes unavailable, the rate limiter gracefully allows all traffic through rather than blocking legitimate users.
- **Centralized Error Handling** — A `@RestControllerAdvice` maps all domain exceptions to clean JSON error responses with appropriate HTTP status codes, including `429 Too Many Requests` with a calculated `Retry-After` header.
- **Flyway Database Migrations** — Schema changes are versioned and applied automatically on startup.
- **Spring Boot Actuator** — Health and metrics endpoints are exposed at `/actuator/health` and `/actuator/metrics`.

## Technology Stack

| Layer | Technology |
| :--- | :--- |
| Language | Java 25 |
| Framework | Spring Boot 4.1.1 |
| Web | Spring Web MVC |
| Persistence | Spring Data JPA / Hibernate 7 |
| Database | PostgreSQL 18 |
| Migrations | Flyway 12 |
| Caching & Rate Limiting | Redis (via Spring Data Redis) |
| Authentication | Spring Security + JJWT 0.13 |
| Build | Maven Wrapper |
| Infrastructure | Docker Compose |
| Code Generation | Lombok |
| Monitoring | Spring Boot Actuator |

## API Endpoints

The API version prefix is configurable via `api.version` in `application.properties` (default: `v1`).

### Public Endpoints (No Authentication Required)

| Method | Endpoint | Description | Response |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/users/register` | Register a new user | `200` with user ID and email |
| `POST` | `/api/v1/users/login` | Authenticate and receive a JWT | `200` with JWT token |
| `GET` | `/{shortCode}` | Redirect to the original URL | `302` redirect / `410` if disabled |

### Protected Endpoints (JWT Required)

All protected endpoints require the `Authorization: Bearer <token>` header and are individually rate-limited.

| Method | Endpoint | Description | Rate Limit | Response |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/urls` | Create a shortened URL | 10 req / 60s | `201` with short code |
| `GET` | `/api/v1/urls` | List user's URLs (paginated) | 60 req / 60s | `200` with URL list |
| `GET` | `/api/v1/urls/{urlId}` | Get a single URL's details | 60 req / 60s | `200` with URL details |
| `PATCH` | `/api/v1/urls/{urlId}` | Update URL status (ACTIVE/DISABLED) | 30 req / 60s | `200` with updated URL |
| `DELETE` | `/api/v1/urls` | Remove a URL from user's library | 30 req / 60s | `204` no content |

### Error Responses

All errors return a consistent JSON structure:

```json
{
  "status": 409,
  "message": "User already exists with email: user@example.com",
  "timestamp": "2026-09-21T19:00:00"
}
```

| Status | Condition |
| :--- | :--- |
| `400` | Invalid pagination parameters |
| `401` | Invalid credentials or missing/expired JWT |
| `404` | User, URL, or user-URL association not found |
| `409` | Email already registered |
| `410` | Short code exists but URL is disabled |
| `429` | Rate limit exceeded (includes `Retry-After` header) |

## Architecture

### Project Structure

```text
src/main/java/com/aryan/url_shortner/
├── annotation/          Custom annotations (@RateLimit)
├── aspect/              AOP aspects (RateLimitAspect)
├── config/              Security config, rate limit properties, .env loader
├── controller/          REST controllers (User, Url, Redirect)
├── dto/                 Request/response records
├── enums/               UrlStatus, RateLimitOperation
├── exceptions/          Domain exceptions + GlobalExceptionHandler
├── model/               JPA entities (User, ShortenedUrl, UserUrl)
├── repository/          Spring Data JPA repositories
└── service/
    ├── rateLimit/        Token bucket rate limiter (Redis Lua)
    ├── security/         JWT filter, JWT service, UserDetailsService
    ├── url/              URL shortening, redirect caching, click flush worker
    ├── user/             User registration and login
    └── userUrl/          User-URL association management

src/main/resources/
├── db/migration/        Flyway SQL migrations (V1–V3)
├── scripts/             Redis Lua scripts (rate_limiter.lua)
└── application.properties
```

### Database Schema

```text
┌──────────────┐       ┌──────────────┐       ┌──────────────────┐
│    users     │       │  user_urls   │       │ shortened_urls   │
├──────────────┤       ├──────────────┤       ├──────────────────┤
│ id (PK)      │──────<│ user_id (FK) │       │ id (PK)          │
│ email        │       │ url_id (FK)  │>──────│ original_url     │
│ password_hash│       │ created_at   │       │ short_code       │
│ created_at   │       │ updated_at   │       │ click_count      │
│ updated_at   │       └──────────────┘       │ status           │
│ total_urls   │                              │ created_at       │
└──────────────┘                              │ updated_at       │
                                              └──────────────────┘
```

### Rate Limiting Architecture

Rate limiting is decoupled from business logic using Spring AOP:

1. Controller methods are annotated with `@RateLimit(operation = RateLimitOperation.CREATE)`.
2. `RateLimitAspect` intercepts the call **before** it reaches the controller, extracts the authenticated user's ID, and queries the Redis rate limiter.
3. `RateLimiterService` executes an atomic Lua script against Redis that implements the Token Bucket algorithm — checking tokens, refilling based on elapsed time, and decrementing atomically in a single round-trip.
4. If the bucket is empty, a `RateLimitExceededException` is thrown and caught by `GlobalExceptionHandler`, which returns `429` with a `Retry-After` header.
5. If Redis is unreachable, the limiter **fails open** to avoid blocking legitimate users.

### Click Count Write-Behind

Instead of issuing a database `UPDATE` on every redirect, clicks are accumulated in a Redis hash (`url:clicks:active`). A scheduled `ClickCountFlushWorker` runs every 5 seconds:

1. Atomically renames the active hash to a flush-specific key (preventing data loss).
2. Reads all accumulated counts from the flush key.
3. Batch-updates PostgreSQL in a single `batchUpdate` call.
4. Deletes the flush key from Redis.

## Requirements

- **JDK 25**
- **Docker** and **Docker Compose**
- **Node.js** (for running the load test scripts)
- **k6** (optional, for heavy concurrency pool stress tests)

## Getting Started

### 1. Clone and Configure

```bash
git clone https://github.com/AryanPatel-18/url-shortner.git
cd url-shortner
cp .env.example .env
```

Edit `.env` with your credentials:

```dotenv
POSTGRES_DB=url_shortener
POSTGRES_USER=postgres
POSTGRES_PASSWORD=your_password

POSTGRES_HOST=localhost
POSTGRES_PORT=5432

REDIS_HOST=localhost
REDIS_PORT=6379

JWT_SECRET=your_secret_key
JWT_EXPIRATION=10000
```

### 2. Start Infrastructure

```bash
docker compose up -d
```

### 3. Run the Application

```bash
.\mvnw spring-boot:run
```

### 4. Run Tests

```bash
.\mvnw clean test
```

## Example API Requests

**Register a user:**

```bash
curl -X POST http://localhost:8080/api/v1/users/register \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"secure-password"}'
```

**Login and get a JWT:**

```bash
curl -X POST http://localhost:8080/api/v1/users/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"secure-password"}'
```

**Create a shortened URL:**

```bash
curl -X POST http://localhost:8080/api/v1/urls \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-jwt-token>" \
  -d '{"originalUrl":"https://example.com/a-very-long-url-to-shorten"}'
```

**List your URLs (paginated):**

```bash
curl http://localhost:8080/api/v1/urls?page=0&size=10 \
  -H "Authorization: Bearer <your-jwt-token>"
```

**Get a single URL:**

```bash
curl http://localhost:8080/api/v1/urls/<url-id> \
  -H "Authorization: Bearer <your-jwt-token>"
```

**Update URL status:**

```bash
curl -X PATCH http://localhost:8080/api/v1/urls/<url-id> \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-jwt-token>" \
  -d '{"status":"DISABLED"}'
```

**Delete a URL from your library:**

```bash
curl -X DELETE http://localhost:8080/api/v1/urls \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-jwt-token>" \
  -d '{"urlId":"<url-id>"}'
```

**Visit a short link:**

```bash
curl -i http://localhost:8080/<short-code>
```

## Make Targets

| Command | Description |
| :--- | :--- |
| `make up` | Start PostgreSQL and Redis containers |
| `make down` | Stop containers |
| `make restart` | Restart containers |
| `make logs` | Tail container logs |
| `make ps` | Show container status |
| `make build` | Build the project (`mvnw clean package`) |
| `make test` | Run tests (`mvnw test`) |
| `make clean` | Clean build artifacts (`mvnw clean`) |
| `make shell` | Open a psql shell inside the PostgreSQL container |

## Load Testing

The project includes a comprehensive load testing suite in the `load-tests/` directory (gitignored):

- **`functional-test.js`** — End-to-end sequential test covering the full user journey: Register → Login → Create → List → Get → Redirect → Update → Delete.
- **`rate-limit-tests/run-all-endpoints.js`** — Verifies that the Token Bucket rate limiter enforces exact capacity limits on all 5 protected endpoints.
- **`pool-stress/`** — k6 scripts that simulate up to 200 concurrent users to stress-test the HikariCP connection pool under extreme load.

The application has been verified to handle **1.1 million+ requests** without a single database connection error or 500-level failure, thanks to the AOP rate limiter rejecting excess traffic before it reaches the database layer.

## License

No license has been added yet.
