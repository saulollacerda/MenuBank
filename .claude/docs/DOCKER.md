# Docker & Deployment

## Services

| Service | Port | Description |
|---|---|---|
| `backend` | 8080 | Spring Boot API |
| `frontend` | 80 | Nginx serving Vue SPA |
| `db` | 5432 | PostgreSQL 16-alpine |

## Environment Variables (.env)

| Variable | Description |
|---|---|
| `DB_NAME` | PostgreSQL database name |
| `DB_USER` | PostgreSQL username |
| `DB_PASSWORD` | PostgreSQL password |

## Running with Docker

```bash
# 1. Configure environment
cp .env.example .env
# Edit .env with your database credentials

# 2. Start all services
docker compose up --build

# 3. Access
# Frontend:    http://localhost
# Backend API: http://localhost:8080
# Database:    localhost:5432
```