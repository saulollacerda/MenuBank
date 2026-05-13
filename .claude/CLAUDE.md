# MenuBank — Context & Architecture

## Overview

MenuBank is a **financial management system for delivery restaurants**.
It is a **monorepo** containing a **Java Spring Boot backend** and a **Vue 3 frontend**.

- **User-facing language:** Portuguese (pt-BR) — all UI text, labels, messages, and user interactions are in Portuguese.
- **Code language:** English — all code, file names, variables, classes, endpoints, comments, and tooling are in English.

---

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

### Frontend

| Technology            | Version / Details                  |
| --------------------- | ---------------------------------- |
| Vue                   | 3.5+                               |
| Vue Router            | 5.x                               |
| Pinia                 | 3.x (state management)            |
| TypeScript            | 5.9+                               |
| Vite                  | 7.x (build & dev server)          |
| Vitest                | 4.x (unit testing)                |
| ESLint                | 10.x + `eslint-plugin-vue` + `@vue/eslint-config-typescript` |
| OxLint                | 1.50.x (additional linting)       |
| Prettier              | 3.8+ (formatting)                 |
| Node.js               | >=20.19.0 or >=22.12.0            |
| Containerization      | Docker (multi-stage: `node:20-alpine` → `nginx:alpine`) |

### Infrastructure

| Technology            | Details                            |
| --------------------- | ---------------------------------- |
| Docker Compose        | Orchestrates `backend`, `frontend`, and `db` services |
| PostgreSQL            | 16-alpine, persistent volume `postgres_data` |
| Nginx                 | Serves the built frontend SPA      |

---

## Project Structure

```
MenuBank/                         # Repository root (monorepo)
├── .env.example                  # Environment variable template (DB_NAME, DB_USER, DB_PASSWORD)
├── .gitignore
├── docker-compose.yaml           # Docker Compose orchestration
├── README.md
│
├── backend/                      # Spring Boot application
│   ├── Dockerfile                # Multi-stage build (JDK 21 → JRE 21)
│   ├── pom.xml                   # Maven config (Java 21, Spring Boot 4.0.3)
│   ├── mvnw / mvnw.cmd           # Maven Wrapper
│   ├── .mvn/                     # Maven Wrapper config
│   └── src/
│       ├── main/
│       │   ├── java/com/MenuBank/MenuBank/
│       │   │   └── Application.java          # @SpringBootApplication entry point
│       │   └── resources/
│       │       ├── application.properties     # Base config (empty — to be configured)
│       │       └── application-dev.properties # Dev profile config (empty — to be configured)
│       └── test/
│           └── java/com/MenuBank/MenuBank/
│               └── ApplicationTests.java      # Spring Boot context test
│
├── frontend/                     # Vue 3 SPA
│   ├── Dockerfile                # Multi-stage build (Node 20 → Nginx)
│   ├── package.json              # Dependencies & scripts
│   ├── vite.config.ts            # Vite config with Vue plugin + DevTools
│   ├── tsconfig.json             # TypeScript project references
│   ├── tsconfig.app.json         # App-specific TS config
│   ├── tsconfig.node.json        # Node-specific TS config
│   ├── tsconfig.vitest.json      # Vitest TS config
│   ├── eslint.config.ts          # Flat ESLint config (Vue + TS + Vitest + OxLint + Prettier)
│   ├── vitest.config.ts          # Vitest config
│   ├── index.html                # SPA HTML entry point
│   ├── env.d.ts                  # Vite env type declarations
│   ├── public/
│   │   └── favicon.ico
│   └── src/
│       ├── App.vue               # Root component
│       ├── main.ts               # App bootstrap (createApp + Pinia + Router)
│       ├── router/
│       │   └── index.ts          # Vue Router (empty routes — to be configured)
│       ├── stores/
│       │   └── counter.ts        # Example Pinia store (to be replaced)
│       └── __tests__/
│           └── App.spec.ts       # Example test
│
└── .github/
    └── copilot-instructions.md   # This file
```

---

## Backend Architecture

The backend follows a **layered architecture organized by feature/domain** (package-by-feature).
Each domain module lives inside `com.MenuBank.MenuBank.<feature>` and contains:

```
com.MenuBank.MenuBank/
├── Application.java              # Entry point
│
├── order/                        # Orders domain
│   ├── OrderController.java      # REST controller — @RestController
│   ├── OrderService.java         # Business logic — @Service
│   ├── OrderRepository.java      # Data access — extends JpaRepository
│   ├── Order.java                # JPA entity — @Entity
│   └── OrderItem.java            # JPA entity — items within an order
│
├── product/                      # Products domain
│   ├── ProductController.java
│   ├── ProductService.java
│   ├── ProductRepository.java
│   └── Product.java              # JPA entity
│
├── category/                     # Categories domain
│   ├── CategoryController.java
│   ├── CategoryService.java
│   ├── CategoryRepository.java
│   └── Category.java             # JPA entity
│
├── ingredient/                   # Ingredients domain
│   ├── IngredientController.java
│   ├── IngredientService.java
│   ├── IngredientRepository.java
│   └── Ingredient.java           # JPA entity
│
├── customer/                     # Customers domain
│   ├── CustomerController.java
│   ├── CustomerService.java
│   ├── CustomerRepository.java
│   └── Customer.java             # JPA entity
│
├── dashboard/                    # Dashboard domain (read-only aggregations)
│   ├── DashboardController.java
│   └── DashboardService.java     # Aggregates data from other services
│
├── user/                         # Users domain (restaurant owner/operator)
│   ├── UserController.java
│   ├── UserService.java
│   ├── UserRepository.java
│   └── User.java                 # JPA entity
│
└── (future domains follow the same pattern)
```

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

### Suggested Frontend Structure (to be built)

```
src/
├── App.vue
├── main.ts
├── router/
│   └── index.ts
├── stores/                       # Pinia stores
│   ├── dashboardStore.ts
│   ├── orderStore.ts
│   ├── productStore.ts
│   ├── categoryStore.ts
│   ├── ingredientStore.ts
│   └── customerStore.ts
├── views/                        # Page-level components (one per route)
│   ├── DashboardView.vue
│   ├── OrdersView.vue
│   ├── ProductsView.vue
│   ├── CategoriesView.vue
│   ├── IngredientsView.vue
│   └── CustomersView.vue
├── components/                   # Reusable UI components
│   ├── common/
│   └── layout/
├── composables/                  # Reusable Composition API logic
├── services/                     # API call modules (axios/fetch wrappers)
│   ├── dashboardService.ts
│   ├── orderService.ts
│   ├── productService.ts
│   ├── categoryService.ts
│   ├── ingredientService.ts
│   └── customerService.ts
├── types/                        # TypeScript interfaces & types
│   ├── Order.ts
│   ├── Product.ts
│   ├── Category.ts
│   ├── Ingredient.ts
│   ├── Customer.ts
│   └── Dashboard.ts
└── assets/                       # Static assets (images, fonts, global CSS)
```

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

## Pages / Tabs

The system has **6 main tabs** in the UI (labels in pt-BR):

| Tab (PT-BR)       | Route (EN)        | View Component          | Description                                         |
| ------------------ | ----------------- | ----------------------- | --------------------------------------------------- |
| **Dashboard**      | `/`               | `DashboardView.vue`     | Overview with KPIs, sales chart, and top products    |
| **Pedidos**        | `/orders`         | `OrdersView.vue`        | CRUD and management of delivery orders               |
| **Produtos**       | `/products`       | `ProductsView.vue`      | CRUD of products with recipe sheet and cost data     |
| **Categorias**     | `/categories`     | `CategoriesView.vue`    | CRUD of product categories                           |
| **Ingredientes**   | `/ingredients`    | `IngredientsView.vue`   | CRUD of ingredients with unit cost tracking          |
| **Clientes**       | `/customers`      | `CustomersView.vue`     | CRUD of customer records                             |

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

## Data Models / Entities

### Order (Pedido)

| Field (EN)        | Field (PT-BR)     | Type                | Description                                       |
| ----------------- | ----------------- | ------------------- | ------------------------------------------------- |
| `id`              | —                 | `UUID` (PK)         | Auto-generated primary key                        |
| `dateTime`        | Data/Hora         | `LocalDateTime`     | Date and time the order was placed                |
| `customer`        | Cliente           | `Customer` (FK)     | Reference to the customer who placed the order    |
| `status`          | Status            | `Enum`              | Order status: `PAID`, `PENDING`, `CANCELLED`      |
| `totalValue`      | Valor Total       | `BigDecimal`        | Total order value in R$                           |
| `estimatedProfit` | Lucro Estimado    | `BigDecimal`        | Estimated profit (totalValue − total cost of items) |
| `items`           | Itens do Pedido   | `List<OrderItem>`   | List of items (products) in this order            |

### OrderItem (Item do Pedido)

| Field (EN)        | Field (PT-BR)     | Type                | Description                                       |
| ----------------- | ----------------- | ------------------- | ------------------------------------------------- |
| `id`              | —                 | `UUID` (PK)         | Auto-generated primary key                        |
| `order`           | Pedido            | `Order` (FK)        | Reference to the parent order                     |
| `product`         | Produto           | `Product` (FK)      | Reference to the product                          |
| `quantity`        | Quantidade        | `Integer`           | Quantity of this product in the order              |
| `unitPrice`       | Preço Unitário    | `BigDecimal`        | Price per unit at the time of the order            |

### Product (Produto)

| Field (EN)        | Field (PT-BR)     | Type                      | Description                                       |
| ----------------- | ----------------- | ------------------------- | ------------------------------------------------- |
| `id`              | —                 | `UUID` (PK)               | Auto-generated primary key                        |
| `name`            | Nome              | `String`                  | Product name                                      |
| `price`           | Preço             | `BigDecimal`              | Selling price in R$                               |
| `estimatedCost`   | Custo Estimado    | `BigDecimal`              | Estimated cost based on ingredient recipe sheet   |
| `margin`          | Margem            | `BigDecimal`              | Profit margin (price − estimatedCost)             |
| `status`          | Status            | `Enum`                    | `ACTIVE` or `INACTIVE`                            |
| `cmv`             | CMV               | `BigDecimal`              | Cost of Goods Sold (Custo de Mercadoria Vendida)  |
| `categories`      | Categorias        | `List<Category>` (M:N)    | Categories this product belongs to                |
| `recipeItems`     | Ficha Técnica     | `List<RecipeItem>` (1:N)  | Recipe sheet — ingredients and quantities needed  |

### RecipeItem (Item da Ficha Técnica)

| Field (EN)        | Field (PT-BR)     | Type                | Description                                       |
| ----------------- | ----------------- | ------------------- | ------------------------------------------------- |
| `id`              | —                 | `UUID` (PK)         | Auto-generated primary key                        |
| `product`         | Produto           | `Product` (FK)      | Reference to the parent product                   |
| `ingredient`      | Ingrediente       | `Ingredient` (FK)   | Reference to the ingredient                       |
| `quantity`        | Quantidade        | `BigDecimal`        | Amount of the ingredient needed                   |

### Category (Categoria)

| Field (EN)        | Field (PT-BR)     | Type                | Description                                       |
| ----------------- | ----------------- | ------------------- | ------------------------------------------------- |
| `id`              | —                 | `UUID` (PK)         | Auto-generated primary key                        |
| `name`            | Nome              | `String`            | Category name                                     |

### Ingredient (Ingrediente)

| Field (EN)        | Field (PT-BR)     | Type                | Description                                       |
| ----------------- | ----------------- | ------------------- | ------------------------------------------------- |
| `id`              | —                 | `UUID` (PK)         | Auto-generated primary key                        |
| `name`            | Nome              | `String`            | Ingredient name                                   |
| `unit`            | Unidade           | `String`            | Unit of measurement (e.g., "kg", "L", "un")       |
| `costPerUnit`     | Custo/Unidade     | `BigDecimal`        | Cost per unit of measurement in R$                |
| `status`          | Status            | `Enum`              | `ACTIVE` or `INACTIVE`                            |

### Customer (Cliente)

| Field (EN)        | Field (PT-BR)     | Type                | Description                                       |
| ----------------- | ----------------- | ------------------- | ------------------------------------------------- |
| `id`              | —                 | `UUID` (PK)         | Auto-generated primary key                        |
| `name`            | Nome              | `String`            | Customer name                                     |
| `phone`           | Telefone          | `String`            | Phone number                                      |
| `email`           | Email             | `String`            | Email address                                     |

### User (Usuário / Restaurante)

| Field (EN)        | Field (PT-BR)       | Type                | Description                                             |
| ----------------- | ------------------- | ------------------- | ------------------------------------------------------- |
| `id`              | —                   | `UUID` (PK)         | Auto-generated primary key                              |
| `restaurantName`  | Nome do Restaurante | `String`            | Legal or trade name of the restaurant                   |
| `cnpj`            | CNPJ                | `String`            | Brazilian company tax ID (unique, 14 digits, formatted) |
| `email`           | Email               | `String`            | Login email address (unique)                            |
| `password`        | Senha               | `String`            | Bcrypt-hashed password — never returned in responses    |
| `phone`           | Telefone            | `String`            | Restaurant contact phone number                         |
| `status`          | Status              | `Enum`              | `ACTIVE` or `INACTIVE`                                  |
| `createdAt`       | Data de Cadastro    | `LocalDateTime`     | Account creation timestamp                              |

### Entity Relationship Summary

```
Customer 1 ──── N Order
Order    1 ──── N OrderItem
Product  1 ──── N OrderItem
Product  M ──── N Category       (join table: product_category)
Product  1 ──── N RecipeItem
Ingredient 1 ── N RecipeItem
```

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

