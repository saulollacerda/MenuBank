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

### `check` Field — Status Values

| Value | Meaning                                  |
| ----- | ---------------------------------------- |
| `0`   | Pending — not yet imported               |
| `1`   | Acknowledged / imported by MenuBank      |
| `2`   | In preparation / dispatched (iFood flow) |
| `3`   | Completed / delivered                    |

---

### Endpoints

#### List orders — `GET /ping/list`

Returns a paginated list of orders. The `from` field indicates the origin platform; `salesChannel` indicates the sales channel.

```json
{
  "success": true,
  "info": {
    "docs": [
      {
        "_id": "6a0e28468e8bf5733592fb7a",
        "check": 2,
        "from": "ifood-marketplace",
        "salesChannel": "ifood",
        "updatedAt": "2026-05-20T20:57:32.968Z"
      },
      {
        "_id": "6a0e094aa2335ae5e05c5eae",
        "check": 3,
        "from": "menu-share-adm",
        "salesChannel": "anotaai",
        "updatedAt": "2026-05-20T20:41:00.884Z"
      }
    ],
    "count": 4,
    "limit": 100,
    "currentpage": 1
  }
}
```

#### Get order detail — `GET /ping/get/{orderId}`

Returns full order data. The response structure is the same for all channels, but there are relevant behavioral differences between **iFood** and **Anota.AI** orders — detailed below.

##### iFood order example

`from: "ifood-marketplace"` / `salesChannel: "ifood"`

```json
{
  "success": true,
  "info": {
    "_id": "6a0e28468e8bf5733592fb7a",
    "check": 2,
    "type": "DELIVERY",
    "salesChannel": "ifood",
    "from": "ifood-marketplace",
    "shortReference": 7452,
    "createdAt": "2026-05-20T21:31:50.786Z",
    "total": 14.48,
    "deliveryFee": 4,
    "additionalFees": [
      { "type": "RESTAURANT_SERVICE_FEE_...", "description": "Taxa de serviço", "value": 0.99 }
    ],
    "discounts": [
      { "amount": 9, "tag": "Total de descontos - iFood", "target": "CART" }
    ],
    "customer": {
      "id": null,
      "name": "João Silva",
      "phone": "11987654321",
      "localizer": "12345678"
    },
    "deliveryAddress": {
      "formattedAddress": "Rua das Flores, 123, Apto 4",
      "city": "São Paulo", "state": "SP", "postalCode": "01310100",
      "neighborhood": "Vila Madalena",
      "coordinates": { "latitude": -23.550520, "longitude": -46.633308 },
      "ifood_pickup_code": "4321"
    },
    "items": [
      {
        "_id": "...", "id": 0,
        "name": "Açaí 330 ml", "quantity": 1, "price": 18.49, "total": 18.49,
        "externalId": "", "internalId": "",
        "subItems": [
          { "name": "Açaí Zero", "quantity": 1, "price": 0, "total": 0, "externalId": "", "internalId": "" },
          { "name": "Banana",    "quantity": 1, "price": 0, "total": 0, "externalId": "", "internalId": "" }
        ]
      }
    ],
    "payments": [
      { "name": "pix-ifood", "code": "pix-ifood", "value": "14.48", "prepaid": true }
    ],
    "merchant": { "name": "Restaurante Exemplo", "id": "aabb1122...", "unit": "ccdd3344..." },
    "ifood_order_integration": { "order_v4": false },
    "observation": ""
  }
}
```

**iFood total formula:**
```
total = sum(items[].price × quantity) + deliveryFee
      - sum(discounts[].amount)
      + sum(additionalFees[].value)
// Example: 18.49 + 4.00 - 9.00 + 0.99 = 14.48
```

##### Anota.AI order example

`from: "menu-share-adm"` / `salesChannel: "anotaai"`

```json
{
  "success": true,
  "info": {
    "_id": "6a0e094aa2335ae5e05c5eae",
    "check": 3,
    "type": "DELIVERY",
    "salesChannel": "anotaai",
    "from": "menu-share-adm",
    "shortReference": 5173,
    "createdAt": "2026-05-20T19:19:38.368Z",
    "total": 25.99,
    "deliveryFee": 0,
    "additionalFees": [],
    "discounts": [],
    "customer": {
      "id": "aabbcc112233...",
      "name": "Maria Santos",
      "phone": "11912345678",
      "taxPayerIdentificationNumber": null
    },
    "deliveryAddress": {
      "formattedAddress": "Rua das Palmeiras, 45, Mocambinho II, Nº 45",
      "city": "São Paulo", "state": "SP", "postalCode": "01310-100",
      "neighborhood": "Vila Madalena",
      "coordinates": { "latitude": -23.561684, "longitude": -46.655981 },
      "reference": "Próximo à escola",
      "complement": "Próximo à escola"
    },
    "items": [
      {
        "_id": "...", "id": 0,
        "name": "Açaí 500 ml", "quantity": 1, "price": 21.99, "total": 25.99,
        "internalId": "66c3adfc0fae7c422a4a6c9a",
        "backoffice_id": "1968578289",
        "subItems": [
          { "name": "Açaí Zero", "quantity": 1, "price": 0,   "total": 0,   "internalId": "679ab6c7207b65c7415b6614" },
          { "name": "Pistache",  "quantity": 1, "price": 1.5, "total": 1.5, "internalId": "693de3b9d994310a1d532f45" },
          { "name": "Morango",   "quantity": 1, "price": 1.5, "total": 1.5, "internalId": "6871b1adcc7d0f025fde518a" },
          { "name": "Cereja",    "quantity": 1, "price": 1.0, "total": 1.0, "internalId": "6871b1adcc7d0f025fde524b" }
        ]
      }
    ],
    "payments": [
      { "name": "ifood-online-pix-payin", "code": "ifood-online-pix-payin", "value": "25.99", "prepaid": true }
    ],
    "merchant": { "name": "Restaurante Exemplo", "id": "aabb1122...", "unit": "ccdd3344..." },
    "ifood_order_integration": { "order_v4": true, "merchant_id": "dddd-eeee-ffff-..." },
    "observation": null
  }
}
```

**Anota.AI total formula:**
```
total = item.price + sum(subItems[].total)
// Example: 21.99 + 0 + 1.5 + 0 + 0 + 0 + 0 + 1.5 + 1.0 + 0 = 25.99
// item.total already includes subItems — use item.total directly
```

---

### iFood vs Anota.AI Order Differences

| Field                             | iFood (`ifood-marketplace`)              | Anota.AI (`menu-share-adm`)                      |
| --------------------------------- | ---------------------------------------- | ------------------------------------------------ |
| `from`                            | `"ifood-marketplace"`                    | `"menu-share-adm"`                               |
| `salesChannel`                    | `"ifood"`                                | `"anotaai"`                                      |
| `additionalFees`                  | Has service fees                         | Empty array                                      |
| `discounts`                       | Has iFood promotional discounts          | Empty array                                      |
| `items[].internalId`              | Empty string — no MenuBank mapping       | MenuBank product ID                              |
| `items[].subItems[].internalId`   | Empty string                             | MenuBank ingredient/complement ID                |
| `items[].backoffice_id`           | Empty                                    | Anota.AI backoffice numeric ID                   |
| `customer.localizer`              | Present (pickup code)                    | Absent                                           |
| `deliveryAddress.ifood_pickup_code` | Present                                | Absent                                           |
| `ifood_order_integration.order_v4` | `false`                                 | `true` (includes `merchant_id`)                  |
| `total` composition               | items + delivery - discounts + fees      | item.total (already includes subItems)           |
| `observation`                     | `""` (empty string)                      | `null`                                           |

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

- `check` field in orders: `0` = pending, `1` = acknowledged/imported, `2` = in preparation/dispatched (iFood), `3` = completed/delivered. Import only orders with `check: 0`.
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

