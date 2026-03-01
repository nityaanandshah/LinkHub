# LinkHub — Production-Grade URL Shortener

A full-stack, production-ready URL shortener built with **Java Spring Boot**, **React**, **PostgreSQL**, **Redis**, **Kafka**, and **Kubernetes**. Designed to demonstrate system design with sub-50ms redirect latency, real-time analytics, fault tolerance, and horizontal scalability.

---

## Architecture

```
┌────────────┐     ┌───────────────┐     ┌──────────┐
│  React SPA │────>│ Spring Boot   │────>│ PostgreSQL│
│  (Vite)    │     │ Backend API   │     │   (16)    │
└────────────┘     │               │     └──────────┘
                   │  ┌──────────┐ │
                   │  │  Redis   │ │  Cache + Rate Limit
                   │  │   (7)    │ │
                   │  └──────────┘ │
                   │               │
                   │  ┌──────────┐ │     ┌─────────────────┐
                   │  │  Kafka   │─┼────>│ Analytics        │
                   │  └──────────┘ │     │ Consumer         │
                   └───────────────┘     │ (GeoIP + UA)     │
                                         └────────┬────────┘
                                                   │
                                              ┌────▼─────┐
                                              │PostgreSQL │
                                              │(click_events)│
                                              └──────────┘
```

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| **Backend** | Java 17, Spring Boot 3.2, Spring Security, JPA/Hibernate |
| **Frontend** | React 18, TypeScript, Vite, Tailwind CSS 4, Recharts |
| **Database** | PostgreSQL 16, Flyway migrations |
| **Cache** | Redis 7 (URL cache, rate limiting, key pool) |
| **Messaging** | Apache Kafka (click event pipeline) |
| **Auth** | JWT (access + refresh tokens), Google OAuth2 |
| **Resilience** | Resilience4j circuit breakers |
| **API Docs** | SpringDoc OpenAPI / Swagger UI |
| **Monitoring** | Prometheus, Grafana, Micrometer |
| **Load Testing** | k6 |
| **CI/CD** | GitHub Actions |
| **Deployment** | Docker Compose, Kubernetes (Ingress, HPA) |

---

## Features

### URL Shortening
- Create short URLs with auto-generated or custom aliases (4-10 chars)
- 302 redirect with Redis-cached lookups for sub-50ms latency
- Optional expiry dates with auto-deactivation
- QR code generation and download
- Activate/deactivate URLs without deletion

### Analytics Dashboard
- Real-time click tracking via Kafka event pipeline
- Time-series click charts (7d / 30d / 90d / All)
- Referrer breakdown (Google, LinkedIn, Twitter/X, Reddit, Facebook, Direct, Others)
- Device analysis (device type, browser, OS) with donut charts
- Geographic breakdown (country, city) via MaxMind GeoIP
- Unique visitor tracking
- Analytics lag indicator for Kafka consumer delays

### Authentication & Security
- Email/password registration and login
- Google OAuth2 integration
- JWT access tokens (15 min) with refresh token rotation (7 days)
- Redis sliding-window rate limiting (20/min anonymous, 100/min authenticated)
- CORS configuration

### Fault Tolerance
- Resilience4j circuit breakers on all Redis operations
- Graceful degradation: cache falls back to DB, rate limiter becomes permissive
- Kafka Dead Letter Queue (DLQ) for failed analytics events
- DLQ retry job for automatic recovery

### Performance
- Pre-generated short-code pool (Base62) to avoid generation bottlenecks
- Redis caching on the redirect hot path
- Batch inserts for click events in analytics consumer
- Connection pooling (HikariCP, Lettuce)

---

## Getting Started

### Prerequisites

- **Java 17+**
- **Node.js 20+**
- **Docker & Docker Compose**
- **Maven 3.9+** (or use the included `mvnw` wrapper)

### Local Development

**1. Start infrastructure services:**

```bash
docker compose up postgres redis zookeeper kafka
```

Wait until all services are healthy.

**2. Start the backend (new terminal):**

```bash
cd backend
mvn spring-boot:run
```

Runs on http://localhost:8080. Wait for `Started LinkhubApplication`.

**3. Start the analytics consumer (new terminal):**

```bash
cd analytics-consumer
mvn spring-boot:run
```

Runs on http://localhost:8081.

**4. Start the frontend (new terminal):**

```bash
cd frontend
npm install
npm run dev
```

Runs on http://localhost:5173.

**5. Open the app:**

Navigate to **http://localhost:5173** — register an account and start creating short URLs.

### Docker Compose (Full Stack)

Run everything with a single command:

```bash
docker compose up --build
```

| Service | URL |
|---------|-----|
| Frontend | http://localhost:3000 |
| Backend API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Grafana | http://localhost:3001 (admin/admin) |
| Prometheus | http://localhost:9090 |

---

## API Endpoints

### Authentication
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/auth/register` | Register new user |
| POST | `/api/v1/auth/login` | Login with email/password |
| POST | `/api/v1/auth/refresh` | Refresh access token |
| GET | `/oauth2/authorization/google` | Google OAuth2 login |

### URLs
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/urls` | Create short URL |
| GET | `/api/v1/urls` | List user's URLs (paginated) |
| GET | `/api/v1/urls/{shortCode}` | Get URL details |
| PUT | `/api/v1/urls/{shortCode}` | Update URL |
| DELETE | `/api/v1/urls/{shortCode}` | Delete URL |
| PATCH | `/api/v1/urls/{shortCode}/toggle` | Activate/deactivate |
| GET | `/api/v1/urls/{shortCode}/qr` | Download QR code |
| GET | `/{shortCode}` | Redirect (public) |

### Analytics
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/analytics/{shortCode}/summary` | Click summary |
| GET | `/api/v1/analytics/{shortCode}/timeseries` | Clicks over time |
| GET | `/api/v1/analytics/{shortCode}/referrers` | Top referrers |
| GET | `/api/v1/analytics/{shortCode}/devices` | Device breakdown |
| GET | `/api/v1/analytics/{shortCode}/geo` | Geographic data |
| GET | `/api/v1/system/analytics-lag` | Consumer lag info |

Full API documentation available at `/swagger-ui.html` when the backend is running.

---

## Database Migrations

Managed by Flyway, auto-applied on startup:

| Version | Description |
|---------|-------------|
| V1 | Users table |
| V2 | Key pool table |
| V3 | URLs table |
| V4 | Click events table |
| V5 | Refresh tokens table |
| V6 | Failed click events (DLQ) table |
| V7 | URL constraints and indexes |
| V8 | Widen short_code column |

---

## Testing

### Integration Tests (51 tests)

```bash
cd backend
mvn clean verify -DskipITs=false
```

Uses **Testcontainers** (PostgreSQL, Redis, Kafka) for realistic integration testing:

- **RedirectIntegrationTest** — redirect flow, caching, inactive/expired URLs
- **UrlCreationIntegrationTest** — create, custom alias, validation
- **OAuth2IntegrationTest** — Google OAuth2 flow
- **AnalyticsIntegrationTest** — click events, analytics API
- **RateLimitIntegrationTest** — rate limiting enforcement
- **ResilienceIntegrationTest** — circuit breaker behavior

---

## Kubernetes Deployment

```bash
# Create namespace and apply all manifests
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secrets.yaml
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
kubectl apply -f k8s/ingress.yaml
kubectl apply -f k8s/hpa.yaml
```

Includes:
- **Deployments** with liveness, readiness, and startup probes
- **HPA** — backend scales 2-10 pods, frontend scales 2-6 pods
- **HTTPS Ingress** with cert-manager TLS termination
- **Secrets** for database credentials, JWT, and OAuth2

---

## Monitoring

- **Prometheus** scrapes `/actuator/prometheus` from backend and analytics-consumer
- **Grafana** dashboard (auto-provisioned) with:
  - Request rate and response time (p95)
  - Error rate and HTTP status breakdown
  - Redis circuit breaker state
  - JVM heap memory usage
  - Kafka consumer lag
  - HikariCP connection pool metrics
  - Analytics events processed/failed/DLQ

---

## CI/CD

GitHub Actions pipeline (`.github/workflows/ci.yml`):

| Job | Trigger | Steps |
|-----|---------|-------|
| **backend-test** | Push/PR to main | JDK 17 setup, `mvn verify` with integration tests |
| **frontend-build** | Push/PR to main | Node 20, type check, Vite build |
| **docker-build** | Push to main | Build & push images to GHCR |

---

## Environment Variables

Copy `.env.example` to `.env` and configure for production:

```bash
cp .env.example .env
```

Key variables:
- `POSTGRES_PASSWORD` — Database password
- `REDIS_PASSWORD` — Redis password
- `JWT_SECRET` — JWT signing secret (min 64 chars)
- `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` — OAuth2 credentials

---

## License

This project is for portfolio and educational purposes.
