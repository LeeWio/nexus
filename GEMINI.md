# Nexus Project

## Project Overview
Nexus is a modern blog/CMS system backend built with Java and Spring Boot. It provides features for managing users, roles, categories, posts, and comments (including an approval workflow).

## Technology Stack
- **Language:** Java 21
- **Framework:** Spring Boot
- **Data Access:** Spring Data JPA (Hibernate), MySQL 9.6
- **Caching:** Spring Data Redis, Redis 7.4
- **Security:** Spring Security with JWT (JSON Web Tokens)
- **Database Migrations:** Flyway
- **Mapping & Utilities:** MapStruct, Hutool, Lombok
- **API Documentation:** Springdoc OpenAPI
- **Monitoring:** Spring Boot Actuator, Micrometer (Prometheus)

## Key Directories and Architecture
- `src/main/java/space/nebula/nexus/` - Main source directory containing the Spring Boot application.
  - `controller/` - REST API endpoints (Admin and Public).
  - `service/` - Business logic and interfaces.
  - `repository/` - Spring Data JPA repositories.
  - `entity/` - JPA domain models.
  - `security/` - JWT filters, exception handlers, and configuration.
  - `mapper/` - MapStruct mappers for DTO-Entity conversion.
  - `payload/` - Request/Response DTOs.
- `src/main/resources/` - Configuration files.
  - `application.yaml` - Main Spring configuration (DB, Redis, Mail, JWT).
  - `db/migration/` - Flyway SQL migration scripts.
- `docker-compose.yml` & `Dockerfile` - Container orchestration for the app, MySQL, and Redis.
- `pom.xml` - Maven configuration and dependencies.

## Building and Running
### Using Docker (Recommended)
You can build and run the entire stack (App, MySQL, Redis) using Docker Compose. A deployment script is provided:
```bash
./deploy.sh
```
Or manually:
```bash
docker compose up --build -d
```
The application will be available at `http://localhost:8080`.

### Local Development
To run locally, ensure you have a MySQL and Redis instance running and configured in `application.yaml` (or override via environment variables).

By default, the application requires Elasticsearch for search functionality. If you want to run the application without Elasticsearch, you can switch to database-backed search:
```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--app.search.type=database"
```
Or set the environment variable `SEARCH_TYPE=database`.

```bash
./mvnw clean package
./mvnw spring-boot:run
```

## Testing
- **Unit & Integration Tests:** Run using Maven:
  ```bash
  ./mvnw test
  ```
- **API E2E Testing:** A shell script is provided to simulate user actions (register, login, post creation, comment submission, and approval) against a running server:
  ```bash
  ./test-api.sh
  ```