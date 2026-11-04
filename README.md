# Fun Travels Tour - Server

Backend API for the Fun Travels Tour multi-agency travel platform.

**Author:** Hussain Al Aradi - **Tech:** Java 21 | Spring Boot 3.5.16 | PostgreSQL | JWT | Stripe

## Quick start

```bash
createdb fun_travels_tour
cp src/main/resources/application.properties.example src/main/resources/application.properties
# set DB_URL / DB_USERNAME / DB_PASSWORD and JWT_SECRET (32+ characters)
./mvnw test
./mvnw spring-boot:run
```

The API starts at `http://localhost:8080`. With `SEED_ENABLED=true` on an empty database, one account per role is created (`admin`, `owner`, `manager`, `employee`, `customer`, `support`, `developer`, emails `<name>@funtravel.com`); the password is `SEED_PASSWORD` or a generated one written to `seed-credentials.local.txt`. The seed never runs under the `prod` profile.

## Layout

```text
src/main/java/com/server/server/
  config/        database, notification, payment, properties, scheduling, security, web
  constants/     booking rules, code prefixes, role groups, validation patterns
  controllers/   one folder per domain (agency, auth, geography, notification, payment, support, tourmanagement, user)
  dto/  enums/  models/  repositories/  services/   same domain folders
  exceptions/    domain exceptions and the global handler
  middlewares/   JWT authentication filter
  seeds/         development data seeder
  utilities/     API envelope, code generator, pagination, pricing, workflow validation, mappers
```

## Documentation

Endpoints, services, models, security notes, and use cases: [fun-travels-tour-document](https://github.com/HussainALAradi5/fun-travels-tour-document).
