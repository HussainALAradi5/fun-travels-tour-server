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

The API starts at `http://localhost:8080/api`. With `SEED_ENABLED=true` on an empty database, one account per role is created (`admin`, `owner`, `manager`, `employee`, `customer`, `support`, `developer`, emails `<name>@funtravel.com`); the password is `SEED_PASSWORD` or a generated one written to `seed-credentials.local.txt`. The seed never runs under the `prod` profile.

## Settings

| Variable | Default | Purpose |
| --- | --- | --- |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | - | PostgreSQL connection |
| `JWT_SECRET` | - | Token signing key, at least 32 characters |
| `SERVER_PORT` | `8080` | HTTP port |
| `API_PREFIX` | `/api` | Prefix of every REST endpoint; the web and mobile apps use the same value |
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

## Deploy (free: Render + Neon + Vercel)

`render.yaml` describes the API as a free Docker web service. The database is Neon's free PostgreSQL, because a Render workspace gets only one free database.

1. On [Neon](https://neon.tech): create a project and copy its connection string (`postgresql://user:password@host/dbname?sslmode=require`).
2. On [Render](https://render.com): New, then Blueprint, then this repository. Enter `DB_URL` (the Neon string) and, for now, `CLIENT_URL` and `CORS_ALLOWED_ORIGINS` as `https://*.vercel.app`.
3. On [Vercel](https://vercel.com): import `fun-travels-tour-client` and set `NEXT_PUBLIC_API_BASE_URL` to `https://<render-service>.onrender.com/api`.
4. Back on Render, set `CLIENT_URL` and `CORS_ALLOWED_ORIGINS` to the exact Vercel address.

On first start, Flyway creates the schema, the seed adds one user per role (Render generates `SEED_PASSWORD`; it is under the service's Environment tab), and every country is imported from the country directory.

The image builds with the Maven wrapper, runs as a non-root user, and `docker-entrypoint.sh` maps Render's `PORT` to `SERVER_PORT` and a `postgresql://` connection string to a JDBC URL with user and password. Client addresses come from `X-Forwarded-For`, so the sign-in rate limit works behind the proxy.

Free-plan limits: the Render service sleeps after 15 minutes without traffic (the next request takes about a minute), Neon's free database holds 0.5 GB, and uploaded files and reports are lost on each Render redeploy.

## Checks

`./mvnw test` runs the unit tests (no database needed). GitHub Actions runs them on every push and pull request.

## Documentation

Endpoints, DTOs, services and business rules, models, security review, and verification: [fun-travels-tour-document](https://github.com/HussainALAradi5/fun-travels-tour-document).
