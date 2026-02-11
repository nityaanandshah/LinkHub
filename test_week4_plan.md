# LinkHub — Week 4 Test Plan

## Overview
Week 4 covers: **React Frontend (full UI)**, **Fault Tolerance (Resilience4j)**, **Dockerization (production + frontend)**, **Kubernetes manifests**, **Grafana dashboards**, **CI pipeline**, and **Load testing**.

**Total automated integration tests: 51** (40 from Weeks 1–3 + 11 new Resilience tests)

---

## Prerequisites

```bash
# Start infrastructure
docker-compose up -d postgres redis zookeeper kafka

# Start backend
cd backend && mvn spring-boot:run

# Start frontend dev server
cd frontend && npm run dev
```

Frontend is available at `http://localhost:5173`  
Backend API at `http://localhost:8080`

---

## 1. Frontend — Login / Register Page

### Test 1.1: Register a new user
1. Open `http://localhost:5173/login`
2. Click "Create Account" toggle
3. Fill in Display Name, Email, Password (8+ chars)
4. Click "Create Account"
5. **Expected:** Redirect to `/dashboard`, user info visible in navbar

### Test 1.2: Login with existing credentials
1. Open `http://localhost:5173/login`
2. Enter the credentials from Test 1.1
3. Click "Sign In"
4. **Expected:** Redirect to `/dashboard`

### Test 1.3: Google OAuth2 button
1. Open `http://localhost:5173/login`
2. Click "Continue with Google" button
3. **Expected:** Redirects to Google OAuth2 consent page (or backend OAuth2 endpoint)
4. **Note:** Full Google login requires valid `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` in `application.yml`

### Test 1.4: JWT token storage
1. After login, open browser DevTools → Application → Local Storage
2. **Expected:** `accessToken`, `refreshToken`, `user` keys present

### Test 1.5: Logout
1. Click "Logout" in the navbar
2. **Expected:** Redirect to `/login`, localStorage tokens cleared

### Test 1.6: Auth guard
1. While logged out, navigate to `http://localhost:5173/dashboard`
2. **Expected:** Redirect to `/login`

---

## 2. Frontend — Dashboard

### Test 2.1: Dashboard loads with URL table
1. Login and navigate to `/dashboard`
2. **Expected:** Page shows "My URLs" header, total link count, "Create URL" button

### Test 2.2: Create a short URL
1. Click "Create URL" button
2. Enter a long URL (e.g., `https://www.google.com/search?q=linkhub+url+shortener`)
3. Optionally enter a custom alias (4–10 chars)
4. Optionally set an expiry date
5. Click "Create URL"
6. **Expected:** Toast notification "Short URL created: {code}", URL appears in table

### Test 2.3: URL table displays data correctly
1. After creating URLs, check the table
2. **Expected:** Each row shows: short code, destination (truncated), click count, status (Active/Inactive), created time, action buttons

### Test 2.4: Copy short URL
1. Click the short code in the table
2. **Expected:** Toast "Copied to clipboard!", URL is in clipboard

### Test 2.5: Deactivate / Activate URL
1. Click the toggle (circle-slash) icon on a URL
2. **Expected:** Toast "URL deactivated", status changes to "Inactive"
3. Click again
4. **Expected:** Toast "URL activated", status back to "Active"

### Test 2.6: Delete a URL
1. Click the trash icon on a URL
2. Confirm the prompt
3. **Expected:** Toast "URL deleted", URL removed from table

### Test 2.7: Pagination
1. Create more than 20 URLs (or reduce page size in the API call)
2. **Expected:** Pagination controls appear ("Previous", "Page X of Y", "Next")
3. Click "Next" — next page loads
4. Click "Previous" — previous page loads

---

## 3. Frontend — Analytics Page

### Test 3.1: Navigate to analytics
1. Click the click count or bar chart icon on a URL in the dashboard
2. **Expected:** Navigate to `/analytics/{shortCode}` page

### Test 3.2: Summary cards
1. On the Analytics page
2. **Expected:** "Total Clicks" and "Unique Visitors" cards with numbers

### Test 3.3: Time range selector
1. Click the time range buttons: 7d, 30d, 90d, All
2. **Expected:** Charts and data refresh for the selected range

### Test 3.4: Clicks Over Time chart
1. Check the area chart
2. **Expected:** Recharts area chart with dates on X-axis, click counts on Y-axis
3. Hover over data points — tooltip shows details

### Test 3.5: Top Referrers chart
1. Check the horizontal bar chart
2. **Expected:** Referrer domains listed with bar chart visualization

### Test 3.6: Device Breakdown charts
1. Check the Device Breakdown section
2. **Expected:** Three donut charts: Device Types, Browsers, Operating Systems
3. Each shows legend with percentages

### Test 3.7: Geographic Breakdown
1. Check the Geo section
2. **Expected:** Countries with progress bars and percentages, Top Cities list

### Test 3.8: Back navigation
1. Click the back arrow
2. **Expected:** Navigate back to `/dashboard`

---

## 4. Frontend — QR Code Modal

### Test 4.1: Open QR code modal
1. On the dashboard, click the QR code icon for any URL
2. **Expected:** Modal opens with QR code image, short URL text

### Test 4.2: Download QR code
1. In the QR modal, click "Download PNG"
2. **Expected:** PNG file downloads with name `{shortCode}-qr.png`

### Test 4.3: Close modal
1. Click the X button or press Escape
2. **Expected:** Modal closes

---

## 5. Frontend — Analytics Lag Indicator

### Test 5.1: Analytics lag endpoint
```bash
curl http://localhost:8080/api/v1/system/analytics-lag
```
**Expected:** JSON response:
```json
{
  "lag": 0,
  "delayed": false,
  "message": "Analytics data is up to date"
}
```

### Test 5.2: Dashboard shows warning when lagged
1. If consumer lag exceeds 1000 events, the Analytics page should show an amber warning banner: "Analytics data may be delayed (X events behind)"
2. **Expected:** Warning disappears when lag drops below threshold

---

## 6. Frontend Dockerfile

### Test 6.1: Build frontend Docker image
```bash
cd frontend
docker build -t linkhub-frontend .
```
**Expected:** Multi-stage build succeeds (npm build → Nginx)

### Test 6.2: Run frontend container
```bash
docker run -d -p 3000:80 --name lh-frontend linkhub-frontend
curl -s -o /dev/null -w "%{http_code}" http://localhost:3000/
```
**Expected:** HTTP 200, serves the React SPA

### Test 6.3: SPA routing works
```bash
curl -s -o /dev/null -w "%{http_code}" http://localhost:3000/dashboard
curl -s -o /dev/null -w "%{http_code}" http://localhost:3000/analytics/test
```
**Expected:** Both return HTTP 200 (Nginx serves `index.html` for all routes)

---

## 7. Resilience4j — Circuit Breaker

### Test 7.1: Circuit breaker on UrlCacheService
1. Verify circuit breaker is registered:
```bash
curl http://localhost:8080/actuator/circuitbreakers
```
**Expected:** `redisCache` and `redisRateLimit` circuit breakers listed

### Test 7.2: Redis down → fallback behavior
1. Stop Redis: `docker-compose stop redis`
2. Create a short URL — it should succeed (writes to DB only)
3. Redirect to a cached URL — should still work (DB fallback)
4. Rate limiting — should allow all requests (permissive mode)
5. **Expected:** Application logs show "Circuit breaker OPEN" warnings but continues functioning

### Test 7.3: Redis recovery
1. Start Redis: `docker-compose start redis`
2. Wait ~30 seconds (circuit breaker `waitDurationInOpenState`)
3. **Expected:** Circuit transitions to HALF_OPEN → CLOSED, normal caching resumes

### Test 7.4: Circuit breaker metrics
```bash
curl http://localhost:8080/actuator/metrics/resilience4j.circuitbreaker.calls
```
**Expected:** Shows call counts for successful, failed, not permitted calls

---

## 8. Rate Limiter Graceful Degradation

### Test 8.1: Rate limiting works with Redis up
```bash
for i in $(seq 1 25); do
  curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/{shortCode}
done
```
**Expected:** First 20 return 302, remaining return 429 (Too Many Requests)

### Test 8.2: Rate limiting permissive mode when Redis down
1. Stop Redis: `docker-compose stop redis`
2. Repeat the 25-request burst above
3. **Expected:** All requests return 302 (permissive mode — no rate limiting)
4. Logs show: "Rate limiter circuit breaker OPEN — permissive mode"

---

## 9. Production Docker Compose

### Test 9.1: Validate production compose
```bash
docker compose -f docker-compose.prod.yml config
```
**Expected:** Valid YAML, all services defined, no errors

### Test 9.2: Environment variable enforcement
```bash
docker compose -f docker-compose.prod.yml up
```
**Expected:** Fails with error requiring `POSTGRES_PASSWORD`, `REDIS_PASSWORD`, `JWT_SECRET`, `GRAFANA_PASSWORD`

### Test 9.3: Start with .env
```bash
cp .env.example .env
# Edit .env with real values
docker compose -f docker-compose.prod.yml up -d
```
**Expected:** All services start, including `linkhub-frontend` on port 80

---

## 10. Kubernetes Manifests

### Test 10.1: Validate manifests (dry run)
```bash
kubectl apply --dry-run=client -f k8s/namespace.yaml
kubectl apply --dry-run=client -f k8s/configmap.yaml
kubectl apply --dry-run=client -f k8s/secrets.yaml
kubectl apply --dry-run=client -f k8s/deployment.yaml
kubectl apply --dry-run=client -f k8s/service.yaml
kubectl apply --dry-run=client -f k8s/ingress.yaml
kubectl apply --dry-run=client -f k8s/hpa.yaml
```
**Expected:** All pass with "created (dry run)"

### Test 10.2: Deployment structure
- **Backend:** 2 replicas, liveness/readiness/startup probes on `/actuator/health/*`
- **Analytics Consumer:** 1 replica, GeoIP volume mount
- **Frontend:** 2 replicas, health probes on `/`

### Test 10.3: Secrets are properly templated
- Verify `secrets.yaml` references: `POSTGRES_PASSWORD`, `REDIS_PASSWORD`, `JWT_SECRET`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`
- All injected via `secretKeyRef` in deployment env vars

### Test 10.4: HTTPS Ingress
- Verify `ingress.yaml` has `tls` section with `linkhub-tls-secret`
- Annotations for `cert-manager.io/cluster-issuer: "letsencrypt-prod"`
- SSL redirect enabled

### Test 10.5: HPA configuration
- Backend: min 2, max 10 replicas, CPU 60%, Memory 75%
- Frontend: min 2, max 6 replicas, CPU 70%
- Scale-up stabilization: 60s, scale-down: 300s

---

## 11. Load Testing

### Test 11.1: Run k6 load test
```bash
# Prerequisites: install k6 (brew install k6)
# Start backend with a test URL, then:
AUTO_SETUP=true k6 run loadtest/redirect-load-test.js
```
**Expected:** Test runs through warmup → load → spike scenarios

### Test 11.2: Verify SLO thresholds
**Expected output:**
```
╔═══════════════════════════════════════════════════╗
║       LinkHub Redirect Load Test Results          ║
╠═══════════════════════════════════════════════════╣
║  p95 latency:      <50ms  (target: <50ms)        ║
║  p99 latency:     <100ms  (target: <100ms)       ║
║  Result: PASS ✓                                   ║
╚═══════════════════════════════════════════════════╝
```

### Test 11.3: Spike test
- After main load test, spike scenario ramps to 200 VUs in 5 seconds
- **Expected:** No errors, latency remains within thresholds

---

## 12. Grafana Dashboards

### Test 12.1: Dashboard auto-provisioning
1. Start Grafana: `docker-compose up grafana`
2. Open `http://localhost:3001` (admin/admin)
3. Navigate to Dashboards → LinkHub folder
4. **Expected:** "LinkHub — Overview" dashboard is auto-provisioned

### Test 12.2: Dashboard panels
**Expected panels:**
- Request Rate (req/s) — timeseries
- Response Time p95 — timeseries
- Error Rate (%) — stat with green/yellow/red thresholds
- Active DB Connections — stat
- Redis Circuit Breaker State — stat
- Redirect Latency SLO (<50ms) — stat with threshold
- JVM Heap Memory — timeseries
- Kafka Consumer Lag — timeseries
- Analytics Events Processed/Failed/DLQ — timeseries
- HikariCP Connection Pool — timeseries

### Test 12.3: Prometheus data source
1. Go to Configuration → Data Sources
2. **Expected:** Prometheus data source auto-provisioned pointing to `http://prometheus:9090`

---

## 13. CI Pipeline (GitHub Actions)

### Test 13.1: Workflow file structure
```bash
cat .github/workflows/ci.yml
```
**Expected:** Three jobs defined:
1. `backend-test` — Maven build + test with JDK 17
2. `frontend-build` — npm ci + tsc + vite build
3. `docker-build` — Multi-arch Docker build & push to GHCR (only on `main` push)

### Test 13.2: Trigger conditions
- **Push to main/develop:** Runs all jobs
- **PR to main:** Runs test/build jobs (not Docker push)

### Test 13.3: Docker image tags
- Images tagged with: `latest` and `{sha_short}` (first 7 chars of commit SHA)
- Three images: `linkhub-backend`, `linkhub-analytics`, `linkhub-frontend`

---

## 14. Liveness/Readiness Probes

### Test 14.1: Backend probes
```bash
curl http://localhost:8080/actuator/health/liveness
curl http://localhost:8080/actuator/health/readiness
```
**Expected:** Both return `{"status":"UP"}`

### Test 14.2: Analytics consumer probes
```bash
curl http://localhost:8081/actuator/health/liveness
curl http://localhost:8081/actuator/health/readiness
```
**Expected:** Both return `{"status":"UP"}`

### Test 14.3: Frontend health
```bash
curl -s -o /dev/null -w "%{http_code}" http://localhost:3000/
```
**Expected:** HTTP 200

---

## 15. Automated Integration Tests Summary

Run all integration tests:
```bash
cd /path/to/LinkHub
mvn test -pl backend -am
```

**Expected: 51 tests, 0 failures**

| Test Class | Tests | Description |
|------------|-------|-------------|
| `UrlCreationIntegrationTest` | 7 | URL CRUD, custom alias, bulk, pagination |
| `RedirectIntegrationTest` | 4 | Redirect, cache, expiry, click counting |
| `RateLimitIntegrationTest` | 4 | IP, user, bulk rate limiting |
| `AnalyticsIntegrationTest` | 18 | Analytics endpoints, pagination, time-range, DLQ |
| `OAuth2IntegrationTest` | 4 | OAuth2 flow, user creation, account linking |
| `ResilienceIntegrationTest` | 11 | Circuit breakers, cache fallback, rate limiter degradation |

---

## Key Files Created/Modified in Week 4

### Frontend (New)
- `frontend/src/pages/Login.tsx` — Tailwind Login/Register with Google OAuth2
- `frontend/src/pages/Dashboard.tsx` — URL table, create modal, pagination
- `frontend/src/pages/Analytics.tsx` — Charts, geo, lag indicator
- `frontend/src/components/layout/Navbar.tsx` — Navigation bar
- `frontend/src/components/layout/AppLayout.tsx` — Layout wrapper
- `frontend/src/components/UrlTable.tsx` — URL table with actions
- `frontend/src/components/CreateUrlModal.tsx` — Create URL form modal
- `frontend/src/components/QrCodeModal.tsx` — QR code display + download
- `frontend/src/components/analytics/ClickChart.tsx` — Recharts area chart
- `frontend/src/components/analytics/ReferrerChart.tsx` — Horizontal bar chart
- `frontend/src/components/analytics/DeviceCharts.tsx` — Donut charts
- `frontend/src/components/analytics/GeoTable.tsx` — Country/city tables
- `frontend/src/components/ui/Modal.tsx` — Reusable modal component
- `frontend/src/hooks/useAuth.ts` — Auth context with OAuth callback
- `frontend/src/hooks/useUrls.ts` — URL CRUD hook
- `frontend/src/hooks/useAnalytics.ts` — Analytics data hook
- `frontend/src/hooks/useAnalyticsLag.ts` — Consumer lag check
- `frontend/src/types/api.ts` — TypeScript type definitions
- `frontend/Dockerfile` — Multi-stage Nginx build
- `frontend/nginx.conf` — SPA routing + API proxy

### Backend (Modified/New)
- `backend/pom.xml` — Resilience4j + AOP dependencies
- `backend/src/main/java/.../url/cache/UrlCacheService.java` — `@CircuitBreaker` annotations
- `backend/src/main/java/.../ratelimit/RateLimitService.java` — `@CircuitBreaker` annotations
- `backend/src/main/java/.../analytics/controller/HealthInfoController.java` — Analytics lag endpoint
- `backend/src/main/java/.../config/SecurityConfig.java` — System endpoints public access
- `backend/src/main/resources/application.yml` — Resilience4j circuit breaker config

### Infrastructure (New)
- `docker-compose.prod.yml` — Production compose with secrets, resource limits, networks
- `.env.example` — Environment variables template
- `k8s/namespace.yaml` — Kubernetes namespace
- `k8s/configmap.yaml` — Non-secret configuration
- `k8s/secrets.yaml` — Sensitive configuration (base64-encoded)
- `k8s/deployment.yaml` — Backend, analytics-consumer, frontend deployments
- `k8s/service.yaml` — ClusterIP services
- `k8s/ingress.yaml` — HTTPS Ingress with TLS + cert-manager
- `k8s/hpa.yaml` — Horizontal Pod Autoscaler for backend + frontend

### Monitoring & CI (New)
- `monitoring/grafana/provisioning/dashboards/linkhub-overview.json` — Grafana dashboard
- `monitoring/grafana/provisioning/dashboards/dashboards.yml` — Dashboard provisioning
- `.github/workflows/ci.yml` — GitHub Actions CI pipeline
- `loadtest/redirect-load-test.js` — Enhanced k6 load test with warmup + spike

### Tests (New)
- `backend/src/test/java/.../integration/ResilienceIntegrationTest.java` — 11 circuit breaker tests
