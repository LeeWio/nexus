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

## Coding Conventions
### Hutool Utility Library
This project strongly encourages the use of **Hutool** to maintain clean, readable, and less error-prone code. When writing or refactoring code, always prefer Hutool utilities over native Java boilerplate. Please adhere to the following rules:

1. **Exception and State Assertion (`Assert`):**
   - Replace traditional `if (condition) { throw new Exception(...) }` blocks with `cn.hutool.core.lang.Assert`.
   - Use lambda expressions for exception messages to ensure lazy evaluation (e.g., `Assert.isTrue(condition, () -> new BusinessException(...))`).
2. **String Manipulation and Checks (`StrUtil`):**
   - Use `StrUtil.isBlank()`, `StrUtil.isNotBlank()`, `StrUtil.isEmpty()`, etc., instead of manual `null` and `""` checks.
   - Use `StrUtil.equals(str1, str2)` for null-safe string comparisons.
   - Use `StrUtil.format()` instead of string concatenation (`+`) or `String.format()`.
3. **Collection Checks (`CollUtil`):**
   - Use `CollUtil.isNotEmpty(collection)` or `CollUtil.isEmpty(collection)` instead of checking for `null` and `size() == 0`.
4. **ID and Random Number Generation (`IdUtil`, `RandomUtil`):**
   - Use `IdUtil.fastSimpleUUID()` or `IdUtil.fastUUID()` instead of `UUID.randomUUID().toString().replace("-", "")`.
   - Use `IdUtil.getSnowflakeNextId()` for distributed sequences.
   - Use `RandomUtil.randomNumbers()` instead of `new java.util.Random()`.
5. **Tree Structures (`TreeUtil`):**
   - When building hierarchical trees (e.g., menus), use `cn.hutool.core.lang.tree.TreeUtil` along with `TreeNodeConfig` instead of manual recursive loops and grouping maps.
6. **Map and Dictionary Creation (`Dict`):**
   - For simple inline maps (like parameters for templates or JSON responses), use `cn.hutool.core.lang.Dict.create().set("key", value)` instead of instantiating and manually populating a `java.util.HashMap`.
