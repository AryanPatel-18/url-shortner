# URL Shortener

URL Shortener is a Spring Boot REST API for converting long URLs into compact, shareable links. The project is being built with a PostgreSQL-backed data layer, Flyway migrations, Spring Data JPA, and Spring Security.

The current implementation establishes the core URL-shortening flow and the initial user/account model. Authentication, user-owned URL management, caching, and production hardening are still being developed.

## Current progress

### Implemented

- Spring Boot application using Java 25 and Maven.
- PostgreSQL persistence through Spring Data JPA.
- Flyway database migration support.
- Environment-based configuration with optional `.env` loading.
- User registration with unique email validation.
- BCrypt password hashing.
- User login with password verification.
- Creation and reuse of shortened URLs for the same original URL.
- Random six-character short-code generation using letters and numbers.
- Short-code collision handling at the database level.
- Redirecting from a short code to the original URL with HTTP `302 Found`.
- Click-count incrementing when a shortened URL is visited.
- Centralized API error responses for known application exceptions.
- Initial user-to-URL relationship model with `ACTIVE` and `DISABLED` URL statuses.
- Docker Compose services for PostgreSQL and Redis.
- Make targets for common development and Docker tasks.

### Available endpoints

The configured API version is `v1`.

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `POST` | `/api/v1/users/register` | Register a user with an email and password |
| `POST` | `/api/v1/users/login` | Verify a user's credentials |
| `POST` | `/api/v1/urls` | Create or retrieve a shortened URL |
| `GET` | `/{shortCode}` | Redirect to the original URL and increment its click count |

The URL creation request currently accepts an `originalUrl` and a `userId`. The response contains the URL identifier, original URL, and generated short code.

## Technology stack

- Java 25
- Spring Boot 4.1.1
- Spring Web MVC
- Spring Data JPA / Hibernate
- Spring Security
- PostgreSQL
- Flyway
- Redis (configured for planned caching work)
- Lombok
- Docker Compose
- Maven Wrapper

## Project structure

```text
src/
├── main/java/com/aryan/url_shortner/
│   ├── config/          Application and security configuration
│   ├── controller/      REST controllers
│   ├── dto/             Request and response objects
│   ├── enums/           Domain enums such as URL status
│   ├── exceptions/      Application exceptions and error handling
│   ├── model/           JPA entities
│   ├── repository/      Spring Data repositories
│   └── service/         User, URL, and user-URL business logic
└── main/resources/
    ├── db/migration/    Flyway database migrations
    └── application.properties
```

## Requirements

- JDK 25
- Docker and Docker Compose
- A terminal with permission to run the Maven Wrapper

## Configuration

The application reads database and Redis settings from environment variables. A local `.env` file can also be used; it is loaded when present.

Example configuration:

```dotenv
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_DB=url_shortner
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres

REDIS_HOST=localhost
REDIS_PORT=6379
```

## Running locally

Start the supporting services:

```bash
make up
```

Check the container status:

```bash
make ps
```

Run the application with the Maven Wrapper:

```bash
./mvnw spring-boot:run
```

Build the project:

```bash
make build
```

Run tests:

```bash
make test
```

Stop the supporting services:

```bash
make down
```

## Example API requests

Register a user:

```bash
curl -X POST http://localhost:8080/api/v1/users/register \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"change-me"}'
```

Create a shortened URL:

```bash
curl -X POST http://localhost:8080/api/v1/urls \
  -H "Content-Type: application/json" \
  -d '{"userId":"<user-id>","originalUrl":"https://example.com/a-very-long-url"}'
```

Visit the generated short URL:

```bash
curl -i http://localhost:8080/<short-code>
```

## Future planning

### Short term

1. Resolve and verify the Flyway schema migrations.
2. Correctly associate newly created URLs with authenticated users.
3. Add request validation and consistent status handling for active and disabled URLs.
4. Add REST endpoints for a user's URL library.
5. Add integration tests for registration, login, URL creation, redirects, click counts, and error cases.

### Medium term

1. Introduce JWT or another stateless authentication mechanism.
2. Use Redis to cache short-code lookups and reduce database reads during redirects.
3. Add URL expiration, disabling, deletion, and custom aliases.
4. Add pagination and filtering for user URL lists.
5. Add API documentation with OpenAPI/Swagger.

### Long term

1. Add analytics such as clicks over time, referrer data, and device or location summaries.
2. Add rate limiting and abuse protection.
3. Add structured logging, metrics, health checks, and tracing.
4. Add CI checks for formatting, tests, security scanning, and container builds.
5. Prepare a production deployment with managed PostgreSQL, Redis, secrets management, backups, and horizontal scaling.

## License

No license has been added yet. Add a license before accepting external contributions or publishing the project for reuse.
