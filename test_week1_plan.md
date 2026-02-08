# LinkHub — Week 1 Test Plan

> **Scope**: Foundation + Auth + Key Generation  
> **Date**: February 2026

---

## Prerequisites

### 1. Start Infrastructure

```bash
cd /path/to/LinkHub
docker-compose up -d
```

Verify all services are running:

```bash
docker-compose ps
```

| Service    | Port | Expected Status |
|------------|------|-----------------|
| PostgreSQL | 5432 | healthy         |
| Redis      | 6379 | healthy         |
| Kafka      | 9092 | healthy         |
| Zookeeper  | 2181 | running         |

### 2. Build & Start the Backend

```bash
cd backend
mvn clean package -DskipTests
mvn spring-boot:run
```

Expected: Application starts on `http://localhost:8080` with logs showing:
- Flyway migrations executed (V1 through V6)
- Key pool scheduler initialized
- Redis connection established

### 3. Start the Frontend (optional for Week 1)

```bash
cd frontend
npm install
npm run dev
```

Expected: React app starts on `http://localhost:5173`

---

## Test 1: Health & Swagger Endpoints

### 1.1 — Actuator Health

```bash
curl -s http://localhost:8080/actuator/health | jq .
```

**Expected**: HTTP 200

```json
{
  "status": "UP"
}
```

### 1.2 — Swagger UI

Open in browser: `http://localhost:8080/swagger-ui.html`

**Expected**: Swagger UI loads with all auth endpoints visible.

### 1.3 — OpenAPI JSON

```bash
curl -s http://localhost:8080/api-docs | jq .info
```

**Expected**: Returns LinkHub API metadata.

---

## Test 2: Flyway Migrations

### 2.1 — Verify Tables Created

```bash
docker exec -it linkhub-postgres psql -U linkhub -d linkhub -c "\dt"
```

**Expected Tables**:
- `users`
- `key_pool`
- `urls`
- `click_events` (partitioned)
- `click_events_2026_02`
- `click_events_2026_03`
- `refresh_tokens`
- `failed_click_events`
- `flyway_schema_history`

### 2.2 — Verify Indexes

```bash
docker exec -it linkhub-postgres psql -U linkhub -d linkhub -c "\di"
```

**Expected**: Indexes on `users.email`, `key_pool.is_used`, `urls.short_code`, `urls.user_id`, etc.

---

## Test 3: User Registration

### 3.1 — Successful Registration

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123",
    "displayName": "Test User"
  }' | jq .
```

**Expected**: HTTP 201

```json
{
  "accessToken": "<jwt_token>",
  "refreshToken": "<refresh_token>",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user": {
    "id": 1,
    "email": "test@example.com",
    "displayName": "Test User",
    "role": "USER"
  }
}
```

### 3.2 — Duplicate Email Registration

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123",
    "displayName": "Test User"
  }' | jq .
```

**Expected**: HTTP 400

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Email already registered: test@example.com"
}
```

### 3.3 — Validation Errors

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "not-an-email",
    "password": "short"
  }' | jq .
```

**Expected**: HTTP 400 with field-level validation errors for `email` and `password`.

---

## Test 4: User Login

### 4.1 — Successful Login

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }' | jq .
```

**Expected**: HTTP 200 with `accessToken`, `refreshToken`, and `user` object.

### 4.2 — Wrong Password

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "wrongpassword"
  }' | jq .
```

**Expected**: HTTP 401

```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid email or password"
}
```

### 4.3 — Non-existent User

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "nobody@example.com",
    "password": "password123"
  }' | jq .
```

**Expected**: HTTP 401

---

## Test 5: JWT Token Validation

### 5.1 — Access Protected Endpoint Without Token

```bash
curl -s -X GET http://localhost:8080/api/v1/urls | jq .
```

**Expected**: HTTP 401 or 403 (Forbidden).

### 5.2 — Access Protected Endpoint With Token

```bash
# First login to get a token:
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}' | jq -r .accessToken)

# Then use the token:
curl -s -X GET http://localhost:8080/api/v1/urls \
  -H "Authorization: Bearer $TOKEN" | jq .
```

**Expected**: HTTP 200 (even if empty list — endpoint is authenticated successfully).

### 5.3 — Access With Invalid Token

```bash
curl -s -X GET http://localhost:8080/api/v1/urls \
  -H "Authorization: Bearer invalid.token.here" | jq .
```

**Expected**: HTTP 401 or 403.

---

## Test 6: Token Refresh

### 6.1 — Successful Token Refresh

```bash
# Login first:
RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}')

REFRESH_TOKEN=$(echo $RESPONSE | jq -r .refreshToken)

# Refresh:
curl -s -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\": \"$REFRESH_TOKEN\"}" | jq .
```

**Expected**: HTTP 200 with **new** `accessToken` and `refreshToken` (rotation).

### 6.2 — Reuse Old Refresh Token (Rotation Security)

```bash
# Try using the SAME refresh token again:
curl -s -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\": \"$REFRESH_TOKEN\"}" | jq .
```

**Expected**: HTTP 401 — old token revoked, all sessions invalidated (reuse detection).

---

## Test 7: Logout

### 7.1 — Logout Revokes Tokens

```bash
# Login:
RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}')

TOKEN=$(echo $RESPONSE | jq -r .accessToken)
REFRESH=$(echo $RESPONSE | jq -r .refreshToken)

# Logout (use -w to see status code since 204 has no body):
curl -s -o /dev/null -w "HTTP Status: %{http_code}\n" \
  -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Authorization: Bearer $TOKEN"
```

**Expected**: Prints `HTTP Status: 204` (no response body — that's correct for 204 No Content).

```bash
# Try refreshing after logout:
curl -s -X POST http://localhost:8080/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d "{\"refreshToken\": \"$REFRESH\"}" | jq .
```

**Expected**: HTTP 401 — "Refresh token has been revoked. All sessions invalidated."

---

## Test 8: Key Pool Generation

### 8.1 — Verify Scheduler Generates Keys

After the app has been running for ~15 seconds (initial delay = 10s), check:

```bash
docker exec -it linkhub-postgres psql -U linkhub -d linkhub \
  -c "SELECT COUNT(*) AS total_keys, COUNT(*) FILTER (WHERE is_used = FALSE) AS available_keys FROM key_pool;"
```

**Expected**: `total_keys` should be ~100,000 (batch size), `available_keys` should be close to that.

### 8.2 — Verify Redis Buffer is Populated

```bash
docker exec -it linkhub-redis redis-cli LLEN keypool:batch
```

**Expected**: Returns a number around 1000 (Redis buffer size).

### 8.3 — Test Key Allocation

```bash
docker exec -it linkhub-redis redis-cli RPOP keypool:batch
```

**Expected**: Returns a 7-character Base62 string (e.g., `a8Kx2Bp`).

### 8.4 — Verify Key Format

```bash
# Pop a key and verify format (strip quotes and whitespace from redis-cli output)
KEY=$(docker exec -it linkhub-redis redis-cli RPOP keypool:batch | tr -d '\r\n"')
echo "Key: $KEY"
echo "Length: ${#KEY}"
echo "Format valid: $(echo $KEY | grep -cE '^[0-9a-zA-Z]{7}$')"
```

**Expected**: Length = 7, Format valid = 1.

---

## Test 9: Database Verification

### 9.1 — Check User Was Created

```bash
docker exec -it linkhub-postgres psql -U linkhub -d linkhub \
  -c "SELECT id, email, display_name, provider, role, created_at FROM users;"
```

**Expected**: Row for `test@example.com`.

### 9.2 — Check Password is Hashed (not plaintext)

```bash
docker exec -it linkhub-postgres psql -U linkhub -d linkhub \
  -c "SELECT email, password_hash FROM users WHERE email = 'test@example.com';"
```

**Expected**: `password_hash` starts with `$2a$12$` (BCrypt format, 12 rounds).

### 9.3 — Check Refresh Tokens

```bash
docker exec -it linkhub-postgres psql -U linkhub -d linkhub \
  -c "SELECT id, user_id, token_hash, revoked, expires_at FROM refresh_tokens ORDER BY id DESC LIMIT 5;"
```

**Expected**: Rows showing hashed tokens, some revoked after logout/refresh.

---

## Test 10: CORS Configuration

### 10.1 — Preflight Request

```bash
curl -s -X OPTIONS http://localhost:8080/api/v1/auth/login \
  -H "Origin: http://localhost:5173" \
  -H "Access-Control-Request-Method: POST" \
  -H "Access-Control-Request-Headers: Content-Type" \
  -D - -o /dev/null
```

**Expected Headers**:
- `Access-Control-Allow-Origin: http://localhost:5173`
- `Access-Control-Allow-Methods` includes `POST`
- `Access-Control-Allow-Credentials: true`

---

## Test 11: Frontend (Basic Smoke Test)

### 11.1 — Login Page Loads

Open `http://localhost:5173` in browser.

**Expected**: Redirected to `/login`, shows "LinkHub" title, email/password form.

### 11.2 — Register Flow

1. Click "Register" toggle
2. Fill in email, password (8+ chars), display name
3. Submit

**Expected**: Redirected to `/dashboard`, shows welcome message.

### 11.3 — Logout Flow

Click "Logout" button on dashboard.

**Expected**: Redirected back to `/login`.

---

## Summary Checklist

| #  | Test                              | Pass? |
|----|-----------------------------------|-------|
| 1  | Actuator health returns UP        | ☐     |
| 2  | Swagger UI loads                  | ☐     |
| 3  | All DB tables created by Flyway   | ☐     |
| 4  | User registration (success)       | ☐     |
| 5  | User registration (duplicate)     | ☐     |
| 6  | User registration (validation)    | ☐     |
| 7  | Login (success)                   | ☐     |
| 8  | Login (wrong password)            | ☐     |
| 9  | JWT protects endpoints            | ☐     |
| 10 | Token refresh works               | ☐     |
| 11 | Token rotation detects reuse      | ☐     |
| 12 | Logout revokes tokens             | ☐     |
| 13 | Key pool generated (~100K keys)   | ☐     |
| 14 | Redis buffer populated (~1K keys) | ☐     |
| 15 | Keys are valid Base62, 7 chars    | ☐     |
| 16 | Passwords stored as BCrypt hash   | ☐     |
| 17 | CORS preflight works              | ☐     |
| 18 | Frontend login page loads         | ☐     |
