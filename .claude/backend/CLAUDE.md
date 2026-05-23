# Backend — MenuBank

## Tech Stack

| Technology | Version / Details |
|---|---|
| Java | 21 |
| Spring Boot | 4.0.3 |
| Spring Data JPA | `spring-boot-starter-data-jpa` |
| Spring Web MVC | `spring-boot-starter-webmvc` |
| Spring Security | `spring-boot-starter-security` |
| OAuth2 Resource Server | `spring-boot-starter-oauth2-resource-server` |
| OAuth2 Authorization Server | `spring-boot-starter-oauth2-authorization-server` |
| Validation | `spring-boot-starter-validation` |
| Lombok | Annotation processing enabled |
| H2 Database | Runtime (dev/test only) |
| PostgreSQL | 16 (production via Docker) |
| Build Tool | Maven Wrapper (`mvnw`) |
| Test — Slices | `spring-boot-starter-data-jpa-test`, `spring-boot-starter-webmvc-test` |
| Test — Unit | JUnit 5 (Jupiter), Mockito 5 (`mockito-core`, `mockito-junit-jupiter`) |
| Test — Security | `spring-security-test` |
| Containerization | Docker multi-stage: `eclipse-temurin:21-jdk` → `eclipse-temurin:21-jre` |

## Layer Responsibilities

| Layer | Annotation | Responsibility |
|---|---|---|
| Controller | `@RestController` | Receives HTTP requests, validates input, delegates to service, returns responses |
| Service | `@Service` | Business rules and orchestration logic |
| Repository | `JpaRepository` | Data access, Spring Data JPA auto-implementation |
| Entity | `@Entity` | JPA entity mapping to a PostgreSQL table |

## Conventions

- **Package-by-feature** — each domain gets its own package: Controller, Service, Repository, Entity.
- **Lombok** (`@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`) to reduce boilerplate.
- **DTOs** for all request/response payloads — never expose entities directly.
- **REST endpoints:** `/api/<feature>` (e.g. `/api/orders`, `/api/products`, `/api/dashboard`).
- **Dev profile:** `SPRING_PROFILES_ACTIVE=dev` for local development with H2.

## Running locally

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```