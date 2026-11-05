# Fun Travels Tour - Server

Backend API for the Fun Travels Tour multi-agency travel platform: tours, per-tour seat inventory, bookings and wallet payments, refunds, support requests, notifications (REST and WebSocket), and server-generated PDF/Excel reports.

**Author:** Hussain Al Aradi - **Tech:** Java 21 | Spring Boot 3.5 | PostgreSQL | JWT | Stripe | OpenPDF | Apache POI

## Quick start

```bash
createdb fun_travels_tour
cp src/main/resources/application.properties.example src/main/resources/application.properties
# set DB_URL / DB_USERNAME / DB_PASSWORD and JWT_SECRET (32+ characters)
./mvnw test
./mvnw spring-boot:run
```

The API starts at `http://localhost:8080/api/v1`. With `SEED_ENABLED=true` on an empty database, one account per role is created (`admin`, `owner`, `manager`, `employee`, `customer`, `support`, `developer`, emails `<name>@funtravel.com`); the password is `SEED_PASSWORD` or a generated one written to `seed-credentials.local.txt`. The seed never runs under the `prod` profile.

## Settings

| Variable | Default | Purpose |
| --- | --- | --- |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | - | PostgreSQL connection |
| `JWT_SECRET` | - | Token signing key, at least 32 characters |
| `SERVER_PORT` | `8080` | HTTP port |
| `API_PREFIX` | `/api/v1` | Prefix of every REST endpoint; the web and mobile apps use the same value |
| `AUTH_RATE_LIMIT`, `AUTH_RATE_WINDOW_SECONDS` | `10`, `60` | Sign-in, registration and password-reset attempts per address in each window |
| `CLIENT_URL` | `http://localhost:3000` | Web app address used in password-reset emails |
| `CORS_ALLOWED_ORIGINS` | local hosts | Allowed browser origins |
| `FILE_UPLOAD_DIR` | `./uploads` | Public profile pictures |
| `MEDIA_DIR` | `./media` | Private generated reports (never served statically) |
| `REPORT_RETENTION_DAYS` | `30` | Reports older than this are deleted nightly |
| `SEED_ENABLED`, `SEED_PASSWORD` | `true`, generated | Development accounts |
| Stripe and mail settings | - | See `application.properties.example` |

## Layout

```text
src/main/java/com/server/server/
  config/        database (auditing), notification, payment, properties, scheduling, security, web
  constants/     common (code prefixes, validation patterns), errors (error codes), report, tourmanagement, user (role groups)
  controllers/   one folder per domain: agency, auth, geography, media, notification, payment, report, support, tourmanagement, user
  dto/           request and response records per domain, built with from(entity)
  enums/ models/ repositories/ services/   same domain folders
  exceptions/    common: the named exceptions services throw
  middlewares/   auth (JWT filter, WebSocket auth), errors (exception handler, security error responses),
                 logging (request ids), security (auth rate limit)
  seeds/         development: one user per role
  utilities/     codes, filters, pagination, phone, pricing, report (PDF and Excel renderers), responses (API envelope)
  validations/   workflow: allowed status transitions
src/main/resources/db/migration/   Flyway scripts
```

Request and response bodies are Java records; the web and mobile apps mirror them as generated TypeScript (`tools/generate-ts-dtos.py` in the documentation repository).

## Database migrations

Flyway owns the schema: versioned scripts live in `src/main/resources/db/migration` and run on startup, and Hibernate only validates that the schema matches the entities (`ddl-auto=validate`). A database created before migrations existed is marked as version 1 and receives only the later scripts. A schema change is a new `V<n>__<description>.sql` file; never edit a script that has already run.

## Checks

`./mvnw test` runs the unit tests (no database needed). GitHub Actions runs them on every push and pull request.

## Documentation

Endpoints, DTOs, services and business rules, models, security review, and verification: [fun-travels-tour-document](https://github.com/HussainALAradi5/fun-travels-tour-document).
