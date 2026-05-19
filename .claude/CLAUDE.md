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

## Anota.AI Integration

MenuBank integrates with the **Anota.AI** platform to import orders and sync the product catalog.
Authentication uses a partner token sent as a header (`Authorization: Bearer <token>`), configured per merchant in the settings.

### Base URLs

| Purpose         | Base URL                                      |
| --------------- | --------------------------------------------- |
| Orders (PDV)    | `https://api-parceiros.anota.ai/partnerauth`  |
| Menu / Catalog  | `https://api-menu.anota.ai/partnerauth`       |

### Endpoints

#### List orders — `GET /ping/list`

Returns a paginated list of pending orders.

```json
{
  "success": true,
  "info": {
    "docs": [
      { "_id": "string", "check": 0, "from": "string", "salesChannel": "string", "updatedAt": "string" }
    ],
    "count": 0, "limit": 0, "currentpage": 0
  }
}
```

#### Get order detail — `GET /ping/get/{orderId}`

Returns full order data. Key fields:

```json
{
  "success": true,
  "info": {
    "_id": "66b6258e890ffb00126c4233",
    "check": 1,
    "type": "LOCAL",
    "salesChannel": "anotaai",
    "total": 10,
    "deliveryFee": 0,
    "customer": { "id": "...", "name": "Teste", "phone": "43123456789" },
    "items": [
      {
        "name": "Refrigerante 1L", "quantity": 1, "price": 10, "total": 10,
        "externalId": "|3|", "internalId": "65d4a428f784bb001956f919",
        "subItems": []
      }
    ],
    "payments": [
      { "name": "money", "code": "money", "value": "10", "prepaid": false }
    ],
    "merchant": { "name": "Beto", "id": "...", "unit": "..." },
    "createdAt": "2024-08-09T14:19:58.182Z",
    "shortReference": 1553
  }
}
```

#### Export catalog — `GET /v2/nm-category/rest/simple-item/export/v2`

Returns the full menu grouped by category. Each category contains items with day-of-week pricing (`week_prices`). `is_additional: true` marks complement/add-on categories.

Key fields per category:

| Field                    | Description                                   |
| ------------------------ | --------------------------------------------- |
| `id`                     | Anota.AI category ID                          |
| `title`                  | Category name                                 |
| `is_additional`          | `true` = complement group, `false` = product  |
| `itens[].id`             | Anota.AI item ID                              |
| `itens[].title`          | Item name                                     |
| `itens[].week_prices`    | Array of `{ price, short_name }` (sun–sat)    |
| `itens[].next_steps`     | Linked complement categories                  |
| `itens[].out`            | `true` = item is out of stock                 |

Abbreviated response example:

```json
{
  "success": true,
  "message": "Menu exportado com sucesso.",
  "data": [
    {
      "title": "Bebidas", "id": "6a0afa28...", "is_additional": false,
      "itens": [
        {
          "id": "6a0afa28...", "title": "Refrigerante 600 ml",
          "week_prices": [{ "price": 5, "short_name": "mon" }, "..."],
          "next_steps": [], "out": false
        }
      ]
    },
    {
      "title": "Turbine seu lanche", "id": "6a0afa28...", "is_additional": true,
      "itens": [
        {
          "id": "6a0afa28...", "title": "Bacon",
          "week_prices": [{ "price": 4, "short_name": "mon" }, "..."],
          "next_steps": []
        }
      ]
    }
  ]
}
```

### Catalog Structure — Flat Array with ID Lookup

The catalog export returns a **single flat `data[]` array** containing all categories — both products (`is_additional: false`) and complement groups (`is_additional: true`). There is no nesting in the response; relationships are expressed via IDs.

To resolve the complements of an item, look up `next_steps[i].category_id` in the same `data[]` array:

```
data[]  ← flat array, all categories mixed together
  ├── { id: "AAA", is_additional: false, itens: [ { next_steps: [{ category_id: "BBB", min: 1, max: 3 }] } ] }
  └── { id: "BBB", is_additional: true,  itens: [ { title: "Creme de Ninho", week_prices: [...] } ] }
```

Resolution flow:

```
item.next_steps[i].category_id
        ↓
data.find(cat => cat.id === category_id)
        ↓
cat.itens[j].week_prices  ← complement prices
```

The full catalog should be loaded once and held in memory as a map (`categoryId → category`) to resolve all relationships locally without additional API calls.

### Integration Notes

- `check` field in orders: `0` = pending, `1` = acknowledged/imported.
- Item price is determined by the current day-of-week from `week_prices`.
- `internalId` on order items maps to the MenuBank product ID; `externalId` is the Anota.AI reference.
- The sync flow: poll `/ping/list` → fetch each order via `/ping/get/{id}` → persist locally → mark as checked.
- Order total = base item price (`week_prices`) + sum of selected complement prices (`subItems[].week_prices`). The `/ping/get/{id}` response already contains resolved `subItems` with calculated prices — no need to re-resolve `next_steps` at import time.

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

