# MenuBank — Context & Architecture

## Overview

MenuBank is a **financial management system for delivery restaurants**.
It is a **monorepo** containing a **Java Spring Boot backend** and a **Vue 3 frontend**.

- **User-facing language:** Portuguese (pt-BR) — all UI text, labels, messages, and user interactions are in Portuguese.
- **Code language:** English — all code, file names, variables, classes, endpoints, comments, and tooling are in English.

---

## Git & Branch Strategy

- Main branche: `main`
- Feature branche: `feature/<description>`
- Fix branche: `fix/<description>`

- Use the main branch only for production, merging develop into main. Features or fixes are created from the develop branch using pattern above.
- Develop branche should always be the most up-to-date.

## Tech Stack

### Backend

| Technology            | Version / Details                  |
| --------------------- | ---------------------------------- |
| Java                  | 21                                 |
| Spring Boot           | 4.0.3                             |
| Spring Data JPA       | `spring-boot-starter-data-jpa`     |
| Spring Web MVC        | `spring-boot-starter-webmvc`       |
| Spring Security       | `spring-boot-starter-security`     |
| OAuth2 Resource Server | `spring-boot-starter-oauth2-resource-server` |
| OAuth2 Authorization Server | `spring-boot-starter-oauth2-authorization-server` |
| Validation            | `spring-boot-starter-validation`   |
| Lombok                | Annotation processing enabled      |
| H2 Database           | Runtime (dev/test only)            |
| PostgreSQL            | 16 (production via Docker)         |
| Build Tool            | Maven (Maven Wrapper — `mvnw`)     |
| Test — Slices         | `spring-boot-starter-data-jpa-test`, `spring-boot-starter-webmvc-test` |
| Test — Unit           | JUnit 5 (Jupiter), Mockito 5 (`mockito-core`, `mockito-junit-jupiter`) |
| Test — Security       | `spring-security-test`             |
| Containerization      | Docker (multi-stage: `eclipse-temurin:21-jdk` → `eclipse-temurin:21-jre`) |

---

### Layer Responsibilities

| Layer        | Annotation         | Responsibility                                         |
| ------------ | ------------------ | ------------------------------------------------------ |
| Controller   | `@RestController`  | Receives HTTP requests, validates input, delegates to service, returns responses |
| Service      | `@Service`         | Contains business rules and orchestration logic        |
| Repository   | `JpaRepository`    | Data access layer, Spring Data JPA auto-implementation |
| Entity       | `@Entity`          | JPA entity mapping to a PostgreSQL table               |

### Conventions

- Use **Lombok** (`@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`, etc.) to reduce boilerplate.
- DTOs should be used for request/response payloads — never expose entities directly to the API.
- REST endpoints follow the pattern: `/api/<feature>` (e.g., `/api/orders`, `/api/products`, `/api/categories`, `/api/ingredients`, `/api/customers`, `/api/dashboard`).
- Use the `dev` Spring profile for local development (`SPRING_PROFILES_ACTIVE=dev`).

---

## Frontend Architecture

The frontend is a **Vue 3 SPA** using the **Composition API** with `<script setup lang="ts">`.

### Conventions

- **State management:** Pinia stores (Composition API style with `defineStore` + `setup` function).
- **Routing:** Vue Router 5 with `createWebHistory`.
- **Alias:** `@` maps to `./src` (configured in Vite).
- **Formatting:** Prettier.
- **Linting:** ESLint (flat config) + OxLint + Vue + TypeScript rules.
- **Testing:** Vitest + `@vue/test-utils`.

---

## Database

- **DBMS:** PostgreSQL 16
- **Connection:** Configured via environment variables (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`)
- **Dev fallback:** H2 in-memory database available for quick local testing without Docker
- **ORM:** Spring Data JPA (Hibernate) — entities map directly to PostgreSQL tables
- **Migrations:** (To be configured — recommend Flyway or Liquibase)

---

## Docker & Deployment

### Services (docker-compose.yaml)

| Service    | Port  | Description                         |
| ---------- | ----- | ----------------------------------- |
| `backend`  | 8080  | Spring Boot API                     |
| `frontend` | 80    | Nginx serving Vue SPA               |
| `db`       | 5432  | PostgreSQL 16-alpine                |

### Environment Variables (.env)

| Variable      | Description                     |
| ------------- | ------------------------------- |
| `DB_NAME`     | PostgreSQL database name        |
| `DB_USER`     | PostgreSQL username             |
| `DB_PASSWORD` | PostgreSQL password             |

### Running the Project

```bash
# 1. Copy and configure environment variables
cp .env.example .env
# Edit .env with your database credentials

# 2. Start all services
docker compose up --build

# 3. Access
# Frontend: http://localhost
# Backend API: http://localhost:8080
# Database: localhost:5432
```

### Local Development (without Docker)

```bash
# Backend
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Frontend
cd frontend
npm install
npm run dev
```

---


## Dashboard Specifications

The Dashboard is a **read-only aggregation view** that displays KPIs and charts.
All data can be **filtered by date range** (default: today).

### KPI Cards

| KPI (PT-BR)         | KPI (EN)             | Description                                                  |
| -------------------- | -------------------- | ------------------------------------------------------------ |
| Total de Vendas      | Total Sales          | Sum of all order totals (R$) within the selected date range  |
| Quantidade de Pedidos | Order Count         | Number of orders within the selected date range              |
| Ticket Médio         | Average Ticket       | Total Sales ÷ Order Count                                    |
| Lucro Estimado       | Estimated Profit     | Sum of estimated profit across all orders in the date range  |

### Charts & Widgets

| Widget (PT-BR)      | Widget (EN)          | Description                                                  |
| -------------------- | -------------------- | ------------------------------------------------------------ |
| Vendas por Dia       | Sales by Day         | Line/bar chart showing daily sales totals within the date range |
| Top 5 Produtos       | Top 5 Products       | Ranked list of the 5 best-selling products within the date range |

---

## Coding Guidelines

1. **All code in English** — class names, variables, methods, endpoints, comments, commit messages.
2. **All UI text in Portuguese (pt-BR)** — labels, buttons, messages, tooltips, error messages shown to the user.
3. **Backend package-by-feature** — each domain gets its own package with Controller, Service, Repository, and Entity.
4. **Frontend Composition API** — always use `<script setup lang="ts">` and Pinia Composition stores.
5. **RESTful API design** — use proper HTTP methods (GET, POST, PUT, DELETE) and status codes.
6. **DTOs for API communication** — never expose JPA entities directly in REST responses.
7. **TypeScript strict mode** — use proper types and interfaces, avoid `any`.
8. **Test coverage** — write unit tests for services (backend) and components (frontend).
9. **Test-Driven Development (TDD) — strictly enforced:**
   - **NEVER write implementation code before the test exists.** No exceptions.
   - The cycle is always: 🔴 Red (write failing test) → 🟢 Green (write minimal implementation to pass) → 🔵 Refactor.
   - Stub classes (empty methods that `throw new UnsupportedOperationException`) are the only production code allowed before a test.
   - Backend: use **JUnit 5 (Jupiter)** + **Mockito** (`@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks`, `given/then` BDD style).
   - Backend controller tests: use `@WebMvcTest` + `MockMvc` + `@MockitoBean`.
   - Frontend: use **Vitest** + `@vue/test-utils`.
   - If asked to implement a feature, always create the test file first and get explicit confirmation (or follow the TDD cycle within the same response).

