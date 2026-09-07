# Fun Travels Tour - Server

Backend API for the Fun Travels Tour travel agency management platform.

**Author:** Hussain Al Aradi
**Tech:** Java 21 | Spring Boot 3.4.13 | PostgreSQL | JWT | Stripe

## Quick Start

```bash
# Prerequisites: Java 21+, PostgreSQL 15+

# 1. Create database
createdb fun_travels_tour

# 2. Configure src/main/resources/application.properties
spring.datasource.url=jdbc:postgresql://localhost:5432/fun_travels_tour
spring.datasource.username=your_user
spring.datasource.password=your_pass
spring.jpa.hibernate.ddl-auto=update
jwt.secret=your_secret_key
stripe.api.secret-key=sk_test_xxx
stripe.api.publishable-key=pk_test_xxx

# 3. Run
./mvnw spring-boot:run
```

Server starts at `http://localhost:8080`

## Documentation

Full documentation is in the separate repository: [fun-travels-tour-document](https://github.com/HussainALAradi5/fun-travels-tour-document)

## Tech Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 21 | Language |
| Spring Boot | 3.4.13 | Framework |
| Spring Security | - | Authentication |
| Spring Data JPA | - | ORM |
| PostgreSQL | 15+ | Database |
| JWT (jjwt) | 0.13.0 | Token Auth |
| Stripe | 24.2.0 | Payments |
| Apache POI | 5.2.3 | Excel Import |
| ZXing | 3.5.3 | QR/Barcode |

## Project Structure

```
src/main/java/com/server/server/
├── Config/           # Security, JWT, CORS, Stripe, WebSocket
├── controllers/      # REST endpoints (20 controllers)
├── Models/           # JPA entities (19 entities)
├── repositories/     # Data access (19 repos)
├── services/         # Business logic (27 services)
├── enums/            # Type enums (17 enums)
├── exceptions/       # Error handling
└── utilities/        # ApiResponse wrapper
```
