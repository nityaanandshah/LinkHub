# LinkHub — Week 2 Test Plan

> **Week 2 Scope:** URL CRUD, Redirect with Redis caching, QR codes, Bulk creation, Rate limiting, Observability (Actuator/Prometheus/Grafana), Integration tests, Load testing

---

## Prerequisites

### 1. Start Infrastructure

```bash
# Make sure Docker containers from Week 1 are still running
docker-compose up -d postgres redis zookeeper kafka
```

Verify all containers are healthy:
```bash
docker ps --format "table {{.Names}}\t{{.Status}}"
```

### 2. Build & Start Backend

```bash
cd /Users/mac_nit/Desktop/preya/projects/LinkHub
mvn clean compile -pl backend -am
cd backend
mvn spring-boot:run
```

Wait for the log line:
```
Started LinkHubApplication in X.XXX seconds
```

### 3. Get a JWT Token

Register a test user and capture the token:

```bash
REGISTER_RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "week2test@example.com",
    "password": "SecurePass123!",
    "displayName": "Week2 Tester"
  }')

echo "$REGISTER_RESPONSE" | python3 -m json.tool

ACCESS_TOKEN=$(echo "$REGISTER_RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")
echo "Token: $ACCESS_TOKEN"
```

If user already exists, login instead:
```bash
LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "week2test@example.com",
    "password": "SecurePass123!"
  }')

ACCESS_TOKEN=$(echo "$LOGIN_RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")
echo "Token: $ACCESS_TOKEN"
```

---

## Test 1: Create Short URL (Auto-generated Key)

```bash
curl -s -X POST http://localhost:8080/api/v1/urls \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "longUrl": "https://www.google.com/search?q=linkhub+url+shortener"
  }' | python3 -m json.tool
```

**Expected:**
- Status: `201 Created`
- Response contains: `shortCode`, `shortUrl`, `longUrl`, `qrUrl`, `createdAt`
- `isCustomAlias` is `false`
- `shortCode` is 7 characters, alphanumeric

**Save the shortCode:**
```bash
SHORT_CODE=$(curl -s -X POST http://localhost:8080/api/v1/urls \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"longUrl": "https://www.github.com/nityaanandshah/LinkHub"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['shortCode'])")

echo "Short Code: $SHORT_CODE"
```

---

## Test 2: Create Short URL with Custom Alias

```bash
curl -s -X POST http://localhost:8080/api/v1/urls \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "longUrl": "https://www.example.com/my-custom-page",
    "customAlias": "my-link"
  }' | python3 -m json.tool
```

**Expected:**
- Status: `201 Created`
- `shortCode` is `"my-link"`
- `isCustomAlias` is `true`

### 2.1 Reject Duplicate Custom Alias

```bash
curl -s -X POST http://localhost:8080/api/v1/urls \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "longUrl": "https://www.example.com/duplicate",
    "customAlias": "my-link"
  }' | python3 -m json.tool
```

**Expected:**
- Status: `400 Bad Request`
- Message contains: `already taken`

---

## Test 3: Create URL with Expiry

```bash
curl -s -X POST http://localhost:8080/api/v1/urls \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "longUrl": "https://www.example.com/expires-soon",
    "expiresAt": "2026-12-31T23:59:59Z"
  }' | python3 -m json.tool
```

**Expected:**
- Status: `201 Created`
- `expiresAt` is `"2026-12-31T23:59:59Z"`

### 3.1 Reject Past Expiry Date

```bash
curl -s -X POST http://localhost:8080/api/v1/urls \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "longUrl": "https://www.example.com/past",
    "expiresAt": "2020-01-01T00:00:00Z"
  }' | python3 -m json.tool
```

**Expected:**
- Status: `400 Bad Request`
- Message: `Expiry date must be in the future`

---

## Test 4: Input Validation

### 4.1 Missing longUrl

```bash
curl -s -X POST http://localhost:8080/api/v1/urls \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{}' | python3 -m json.tool
```

**Expected:**
- Status: `400 Bad Request`
- Field error for `longUrl`: `Long URL is required`

### 4.2 Invalid URL Format

```bash
curl -s -X POST http://localhost:8080/api/v1/urls \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "longUrl": "not-a-valid-url"
  }' | python3 -m json.tool
```

**Expected:**
- Status: `400 Bad Request`
- Field error: `Must be a valid URL`

### 4.3 Invalid Custom Alias (too short)

```bash
curl -s -X POST http://localhost:8080/api/v1/urls \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "longUrl": "https://www.example.com",
    "customAlias": "ab"
  }' | python3 -m json.tool
```

**Expected:**
- Status: `400 Bad Request`
- Field error for `customAlias`

---

## Test 5: Redirect (302)

### 5.1 Successful Redirect

```bash
curl -s -o /dev/null -w "HTTP Status: %{http_code}\nLocation: %{redirect_url}\n" \
  http://localhost:8080/$SHORT_CODE
```

**Expected:**
- HTTP Status: `302`
- Location: `https://www.github.com/nityaanandshah/LinkHub`

### 5.2 Redirect with Custom Alias

```bash
curl -s -o /dev/null -w "HTTP Status: %{http_code}\nLocation: %{redirect_url}\n" \
  http://localhost:8080/my-link
```

**Expected:**
- HTTP Status: `302`
- Location: `https://www.example.com/my-custom-page`

### 5.3 Unknown Short Code → 404

```bash
curl -s -o /dev/null -w "HTTP Status: %{http_code}\n" \
  http://localhost:8080/zzzzzzzz
```

**Expected:**
- HTTP Status: `404`

---

## Test 6: Redis Cache Verification

### 6.1 Write-Through Cache on Create

After creating a URL, verify it's cached:

```bash
redis-cli GET "url:$SHORT_CODE"
```

**Expected:**
- Returns the long URL (e.g., `https://www.github.com/nityaanandshah/LinkHub`)

### 6.2 Cache-Aside on Redirect

Delete the cache entry and redirect again:

```bash
# Clear cache
redis-cli DEL "url:$SHORT_CODE"

# Verify it's gone
redis-cli GET "url:$SHORT_CODE"
# Expected: (nil)

# Redirect (triggers cache-aside fill)
curl -s -o /dev/null -w "HTTP Status: %{http_code}\n" http://localhost:8080/$SHORT_CODE
# Expected: 302

# Verify cache is repopulated
redis-cli GET "url:$SHORT_CODE"
# Expected: the long URL
```

### 6.3 Click Counter in Redis

```bash
# Perform a few redirects
curl -s -o /dev/null http://localhost:8080/$SHORT_CODE
curl -s -o /dev/null http://localhost:8080/$SHORT_CODE
curl -s -o /dev/null http://localhost:8080/$SHORT_CODE

# Check click counter
redis-cli GET "clicks:$SHORT_CODE"
```

**Expected:**
- A number ≥ 3

---

## Test 7: List User's URLs (Paginated)

```bash
curl -s http://localhost:8080/api/v1/urls?page=0\&size=5 \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  | python3 -m json.tool
```

**Expected:**
- Status: `200 OK`
- Response contains `content` array, `totalElements`, `totalPages`, `number`, `size`
- Each URL in `content` has `shortCode`, `longUrl`, `shortUrl`, `clickCount`, `qrUrl`

---

## Test 8: Get URL Metadata

```bash
curl -s http://localhost:8080/api/v1/urls/$SHORT_CODE \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  | python3 -m json.tool
```

**Expected:**
- Status: `200 OK`
- Response contains full URL metadata including `clickCount`

---

## Test 9: Update URL (PATCH)

### 9.1 Update Expiry

```bash
curl -s -X PATCH http://localhost:8080/api/v1/urls/$SHORT_CODE \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "expiresAt": "2027-06-30T23:59:59Z"
  }' | python3 -m json.tool
```

**Expected:**
- Status: `200 OK`
- `expiresAt` updated to `"2027-06-30T23:59:59Z"`

### 9.2 Deactivate URL

```bash
# Create a URL to deactivate
DEACTIVATE_CODE=$(curl -s -X POST http://localhost:8080/api/v1/urls \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"longUrl": "https://www.example.com/to-deactivate"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['shortCode'])")

echo "Deactivate Code: $DEACTIVATE_CODE"

# Deactivate
curl -s -X PATCH http://localhost:8080/api/v1/urls/$DEACTIVATE_CODE \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"isActive": false}' | python3 -m json.tool
```

**Expected:**
- `isActive` is `false`

### 9.3 Redirect to Deactivated URL → 404

```bash
curl -s -o /dev/null -w "HTTP Status: %{http_code}\n" \
  http://localhost:8080/$DEACTIVATE_CODE
```

**Expected:**
- HTTP Status: `404`

### 9.4 Cache Invalidation on Update

```bash
# Verify cache was invalidated after update
redis-cli GET "url:$SHORT_CODE"
```

**Expected:**
- `(nil)` — cache was invalidated after the PATCH

---

## Test 10: Delete URL (Soft Delete)

```bash
# Create a URL to delete
DELETE_CODE=$(curl -s -X POST http://localhost:8080/api/v1/urls \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"longUrl": "https://www.example.com/to-delete"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['shortCode'])")

echo "Delete Code: $DELETE_CODE"

# Delete
curl -s -o /dev/null -w "HTTP Status: %{http_code}\n" \
  -X DELETE http://localhost:8080/api/v1/urls/$DELETE_CODE \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

**Expected:**
- HTTP Status: `204 No Content`

### 10.1 Redirect to Deleted URL → 404

```bash
curl -s -o /dev/null -w "HTTP Status: %{http_code}\n" \
  http://localhost:8080/$DELETE_CODE
```

**Expected:**
- HTTP Status: `404`

### 10.2 Cache Invalidation on Delete

```bash
redis-cli GET "url:$DELETE_CODE"
```

**Expected:**
- `(nil)` — cache was invalidated

---

## Test 11: Bulk URL Creation

```bash
curl -s -X POST http://localhost:8080/api/v1/urls/bulk \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "urls": [
      {"longUrl": "https://www.google.com"},
      {"longUrl": "https://www.github.com"},
      {"longUrl": "https://www.stackoverflow.com"},
      {"longUrl": "https://www.linkedin.com"},
      {"longUrl": "https://www.twitter.com"}
    ]
  }' | python3 -m json.tool
```

**Expected:**
- Status: `201 Created`
- Response is an array of 5 `CreateUrlResponse` objects
- Each has a unique `shortCode`

---

## Test 12: QR Code Generation

```bash
# Download QR code as PNG
curl -s http://localhost:8080/api/v1/urls/$SHORT_CODE/qr \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -o /tmp/linkhub-qr.png

# Verify it's a valid PNG file
file /tmp/linkhub-qr.png
```

**Expected:**
- File should be identified as `PNG image data, 300 x 300`
- You can open `/tmp/linkhub-qr.png` to see the QR code

### 12.1 Custom QR Size

```bash
curl -s "http://localhost:8080/api/v1/urls/$SHORT_CODE/qr?size=500" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -o /tmp/linkhub-qr-500.png

file /tmp/linkhub-qr-500.png
```

**Expected:**
- `PNG image data, 500 x 500`

---

## Test 13: Rate Limiting

### 13.1 Authenticated User Rate Limit (100/min)

```bash
# Make 5 quick requests — all should succeed
for i in $(seq 1 5); do
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
    http://localhost:8080/api/v1/urls \
    -H "Authorization: Bearer $ACCESS_TOKEN")
  echo "Request $i: HTTP $STATUS"
done
```

**Expected:**
- All 5 return `200`

### 13.2 Simulate Rate Limit Exceeded

Extract the userId from your JWT and set the counter to the limit:

```bash
# Decode the JWT to get userId
USER_ID=$(echo "$ACCESS_TOKEN" | cut -d'.' -f2 | base64 -d 2>/dev/null | python3 -c "import sys,json; print(json.load(sys.stdin)['userId'])")
echo "User ID: $USER_ID"

# Set the rate counter to the limit
redis-cli SET "rate:user:$USER_ID" "100"

# Now try a request — should be rate limited
curl -s http://localhost:8080/api/v1/urls \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  | python3 -m json.tool
```

**Expected:**
- Status: `429 Too Many Requests`
- Response body: `{"status": 429, "error": "Too Many Requests", "message": "Rate limit exceeded..."}`

### 13.3 Reset Rate Limit and Verify Recovery

```bash
redis-cli DEL "rate:user:$USER_ID"

curl -s -o /dev/null -w "HTTP Status: %{http_code}\n" \
  http://localhost:8080/api/v1/urls \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

**Expected:**
- HTTP Status: `200` — rate limit recovered

---

## Test 14: Kafka Click Events

### 14.1 Verify Click Events Topic Exists

```bash
docker exec linkhub-kafka kafka-topics --bootstrap-server localhost:9092 --list | grep click
```

**Expected:**
- `click-events` topic listed

### 14.2 Consume Click Events

Open a new terminal and start a consumer:

```bash
docker exec linkhub-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic click-events \
  --from-beginning \
  --max-messages 5
```

Then in the original terminal, perform a redirect:

```bash
curl -s -o /dev/null http://localhost:8080/$SHORT_CODE
```

**Expected:**
- The consumer should show a JSON message with `eventId`, `urlId`, `shortCode`, `clickedAt`, `ipAddress`, `userAgent`

---

## Test 15: Observability

### 15.1 Actuator Health Endpoint

```bash
curl -s http://localhost:8080/actuator/health | python3 -m json.tool
```

**Expected:**
- Status: `200 OK`
- `{"status": "UP"}`

### 15.2 Readiness Probe

```bash
curl -s http://localhost:8080/actuator/health/readiness | python3 -m json.tool
```

**Expected:**
- `{"status": "UP"}`

### 15.3 Prometheus Metrics Endpoint

```bash
curl -s http://localhost:8080/actuator/prometheus | head -30
```

**Expected:**
- Prometheus text format metrics
- Should include: `http_server_requests_seconds`, `jvm_memory_used_bytes`, `system_cpu_usage`

### 15.4 Custom Metrics

```bash
# Check for HTTP request latency histograms
curl -s http://localhost:8080/actuator/prometheus | grep "http_server_requests"
```

**Expected:**
- Histogram buckets with latency percentiles

### 15.5 Metrics Endpoint

```bash
curl -s http://localhost:8080/actuator/metrics | python3 -m json.tool
```

**Expected:**
- List of available metric names

---

## Test 16: Prometheus & Grafana (Docker)

> ⚠️ **Note:** For local development testing, run the backend locally (not in Docker).
> Prometheus and Grafana should be started separately using:

```bash
docker-compose up -d prometheus grafana
```

### 16.1 Prometheus Dashboard

Open: [http://localhost:9090](http://localhost:9090)

Navigate to **Status > Targets**. If running the backend in Docker too, you should see `linkhub-backend` as a target.

> If running the backend locally (outside Docker), update `monitoring/prometheus/prometheus.yml`:
> Change `linkhub-backend:8080` to `host.docker.internal:8080`

### 16.2 Grafana Dashboard

Open: [http://localhost:3001](http://localhost:3001)

Login: `admin` / `admin`

1. Go to **Connections > Data Sources** — Prometheus should be pre-configured
2. Go to **Explore** → select Prometheus → query: `http_server_requests_seconds_count`
3. You should see request count metrics

---

## Test 17: Integration Tests (Automated)

> ⚠️ **Requires Docker running** (Testcontainers will spin up PostgreSQL, Redis, and Kafka containers).

```bash
cd /Users/mac_nit/Desktop/preya/projects/LinkHub
mvn test -pl backend -Dtest="com.linkhub.integration.*" 2>&1 | tail -30
```

**Expected:**
- All tests pass:
  - `UrlCreationIntegrationTest` — 9 tests (create, custom alias, bulk, validation, etc.)
  - `RedirectIntegrationTest` — 5 tests (redirect, cache-aside, click counter, cache invalidation)
  - `RateLimitIntegrationTest` — 3 tests (allow within limit, block exceeding, error format)

---

## Test 18: Load Test (k6)

### 18.1 Install k6

```bash
brew install k6
```

### 18.2 Prepare a Test URL

Create a URL and export the short code:

```bash
export SHORT_CODE=$(curl -s -X POST http://localhost:8080/api/v1/urls \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"longUrl": "https://www.example.com/load-test-target"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['shortCode'])")

echo "Load test target: $SHORT_CODE"
```

### 18.3 Run Load Test

```bash
cd /Users/mac_nit/Desktop/preya/projects/LinkHub
SHORT_CODE=$SHORT_CODE k6 run loadtest/redirect-load-test.js
```

**Expected:**
- p95 latency < 50ms
- p99 latency < 100ms
- Error rate < 1%
- Summary shows:
  ```
  ═══════════════════════════════════════
    LinkHub Redirect Load Test Results
  ═══════════════════════════════════════
    p50 latency: Xms
    p95 latency: Xms
    p99 latency: Xms
    Target: p95 < 50ms
  ═══════════════════════════════════════
  ```

---

## Test 19: Swagger API Documentation

Open: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

**Verify:**
- All URL endpoints are documented:
  - `POST /api/v1/urls` — Create short URL
  - `POST /api/v1/urls/bulk` — Bulk create
  - `GET /api/v1/urls` — List URLs
  - `GET /api/v1/urls/{shortCode}` — Get metadata
  - `PATCH /api/v1/urls/{shortCode}` — Update
  - `DELETE /api/v1/urls/{shortCode}` — Delete
  - `GET /api/v1/urls/{shortCode}/qr` — QR code
  - `GET /{shortCode}` — Redirect

---

## Summary Checklist

| # | Test | Expected |
|---|------|----------|
| 1 | Create URL (auto key) | 201, shortCode returned |
| 2 | Create URL (custom alias) | 201, shortCode matches alias |
| 2.1 | Duplicate alias rejection | 400, "already taken" |
| 3 | Create URL with expiry | 201, expiresAt set |
| 3.1 | Past expiry rejection | 400 |
| 4 | Input validation | 400, field errors |
| 5 | Redirect (302) | 302, Location header |
| 5.3 | Unknown shortCode | 404 |
| 6 | Redis write-through cache | Key present in Redis |
| 6.2 | Cache-aside on miss | Key repopulated after redirect |
| 6.3 | Click counter | Counter incremented in Redis |
| 7 | List URLs (paginated) | 200, paginated response |
| 8 | Get URL metadata | 200, full metadata |
| 9 | Update URL (PATCH) | 200, fields updated |
| 9.3 | Redirect deactivated → 404 | 404 |
| 9.4 | Cache invalidation on update | Cache cleared |
| 10 | Delete URL (soft) | 204 |
| 10.1 | Redirect deleted → 404 | 404 |
| 10.2 | Cache invalidation on delete | Cache cleared |
| 11 | Bulk create | 201, array of responses |
| 12 | QR code PNG | Valid PNG image |
| 13 | Rate limiting | 429 when exceeded |
| 14 | Kafka click events | Messages in topic |
| 15 | Actuator/Prometheus | Endpoints respond |
| 16 | Prometheus + Grafana UI | Dashboards accessible |
| 17 | Integration tests | All pass |
| 18 | Load test (k6) | p95 < 50ms |
| 19 | Swagger docs | All endpoints documented |
