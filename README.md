# MenuBank

A financial management system for delivery restaurants — built as a monorepo with a **Java Spring Boot** backend and a **Vue 3** frontend.

---

## Tech Stack

| Layer          | Technologies                                                    |
| -------------- | --------------------------------------------------------------- |
| **Backend**    | Java 21, Spring Boot 4.0.3, Spring Data JPA, Lombok, Maven     |
| **Frontend**   | Vue 3.5+, TypeScript 5.9+, Pinia 3, Vue Router 5, Vite 7       |
| **Database**   | PostgreSQL 16 (production) · H2 (dev/test)                      |
| **Infra**      | Docker, Docker Compose, Nginx                                   |

---

## Getting Started

### Prerequisites

- [Docker](https://www.docker.com/) & Docker Compose
- (Optional for local dev) Java 21, Node.js ≥20.19.0

### Running with Docker

```bash
# 1. Configure environment variables
cp .env.example .env
# Edit .env with your database credentials

# 2. Start all services
docker compose up --build
```

| Service    | URL                      |
| ---------- | ------------------------ |
| Frontend   | http://localhost         |
| Backend API| http://localhost:8080    |
| Database   | localhost:5432           |

### Running Locally (without Docker)

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

## Project Structure

```
MenuBank/
├── backend/          # Spring Boot API
│   ├── src/main/java/com/MenuBank/MenuBank/
│   │   ├── order/        # Orders domain
│   │   ├── product/      # Products domain
│   │   ├── category/     # Categories domain
│   │   ├── ingredient/   # Ingredients domain
│   │   ├── customer/     # Customers domain
│   │   └── dashboard/    # Dashboard aggregations
│   └── src/main/resources/
│       ├── application.properties
│       └── application-dev.properties
│
├── frontend/         # Vue 3 SPA
│   └── src/
│       ├── views/        # Page components
│       ├── stores/       # Pinia stores
│       ├── services/     # API call modules
│       ├── types/        # TypeScript interfaces
│       ├── components/   # Reusable UI components
│       └── composables/  # Composition API logic
│
└── docker-compose.yaml
```

---

## Architecture

### Backend — Layered, Package-by-Feature

Each domain follows the pattern:

```
<feature>/
├── FeatureController.java    # @RestController — HTTP layer
├── FeatureService.java       # @Service — Business logic
├── FeatureRepository.java    # JpaRepository — Data access
└── Feature.java              # @Entity — JPA entity
```

**API pattern:** `/api/<feature>` (e.g., `/api/orders`, `/api/products`)

### Frontend — Vue 3 Composition API

- `<script setup lang="ts">` in all components
- Pinia stores with Composition API (`defineStore` + `setup`)
- Vue Router 5 with `createWebHistory`

---

## Pages

| Page            | Route           | Description                                    |
| --------------- | --------------- | ---------------------------------------------- |
| Dashboard       | `/`             | KPIs, sales chart, top 5 products              |
| Orders          | `/orders`       | Order management (CRUD)                        |
| Products        | `/products`     | Products with recipe sheet and cost data       |
| Categories      | `/categories`   | Product categories (CRUD)                      |
| Ingredients     | `/ingredients`  | Ingredients with unit cost tracking            |
| Customers       | `/customers`    | Customer records (CRUD)                        |

---

## Data Model

```
Customer 1 ──── N Order
Order    1 ──── N OrderItem
Product  1 ──── N OrderItem
Product  M ──── N Category        (join table)
Product  1 ──── N RecipeItem
Ingredient 1 ── N RecipeItem
```

---

## Environment Variables

| Variable      | Description              |
| ------------- | ------------------------ |
| `DB_NAME`     | PostgreSQL database name |
| `DB_USER`     | PostgreSQL username      |
| `DB_PASSWORD` | PostgreSQL password      |

---

## License

This project is private and not licensed for public use.
