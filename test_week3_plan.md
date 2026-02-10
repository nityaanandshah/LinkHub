# Week 3 — Manual Test Plan: Analytics Pipeline, OAuth2 & DLQ

## Prerequisites

### 1. Start Infrastructure
```bash
cd /Users/mac_nit/Desktop/preya/projects/LinkHub
docker-compose up -d postgres redis zookeeper kafka
```

Wait for Kafka to be healthy:
```bash
docker exec linkhub-kafka kafka-broker-api-versions --bootstrap-server localhost:9092 2>&1 | head -1
```
Expected: version info line (no connection error).

### 2. Build & Start Backend
```bash
cd /Users/mac_nit/Desktop/preya/projects/LinkHub
mvn clean compile -pl backend -am -q
cd backend
mvn spring-boot:run &
```

Wait ~15 seconds for the backend to start. Verify:
```bash
curl -s http://localhost:8080/actuator/health | jq .status
```
Expected: `"UP"`

### 3. Get a JWT Token
```bash
# Register a test user
curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email": "week3@test.com", "password": "Test1234!"}' | jq .
```

Save the access token:
```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email": "week3test@test.com", "password": "Test1234!"}' | jq -r '.accessToken')
echo "TOKEN: $TOKEN"
```

If user already exists, login instead:
```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "week3test@test.com", "password": "Test1234!"}' | jq -r '.accessToken')
echo "TOKEN: $TOKEN"
```

### 4. Create a Test URL
```bash
RESULT=$(curl -s -X POST http://localhost:8080/api/v1/urls \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"longUrl": "https://www.google.com/search?q=linkhub+analytics"}')
echo "$RESULT" | jq .
SHORT_CODE=$(echo "$RESULT" | jq -r '.shortCode')
echo "SHORT_CODE: $SHORT_CODE"
```
Expected: 201 Created with shortCode.

---

## Test 1: Analytics Consumer Module Structure

### 1.1 Module compiles
```bash
cd /Users/mac_nit/Desktop/preya/projects/LinkHub
mvn clean compile -pl analytics-consumer -am -q
```
Expected: BUILD SUCCESS (no errors).

### 1.2 Verify analytics-consumer has its own Dockerfile
```bash
ls -la /Users/mac_nit/Desktop/preya/projects/LinkHub/analytics-consumer/Dockerfile
```
Expected: File exists.

### 1.3 Verify analytics-consumer application.yml
```bash
cat /Users/mac_nit/Desktop/preya/projects/LinkHub/analytics-consumer/src/main/resources/application.yml | head -10
```
Expected: Shows `linkhub-analytics-consumer` application name, port 8081.

---

## Test 2: Click Event Kafka Pipeline

### 2.1 Generate Click Events (via redirect)
```bash
# Simulate 5 clicks with different User-Agents and referrers
for i in 1 2 3 4 5; do
  curl -s -o /dev/null -w "Click $i: HTTP %{http_code}, Redirect: %{redirect_url}\n" \
    -H "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0" \
    -H "Referer: https://google.com" \
    "http://localhost:8080/$SHORT_CODE"
done
```
Expected: Each click returns HTTP 302 with redirect to google.com search URL.

### 2.2 Click with Mobile User-Agent
```bash
curl -s -o /dev/null -w "HTTP %{http_code}\n" \
  -H "User-Agent: Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) Safari/605.1.15" \
  -H "Referer: https://twitter.com/post/12345" \
  "http://localhost:8080/$SHORT_CODE"
```
Expected: HTTP 302.

### 2.3 Click with no referrer
```bash
curl -s -o /dev/null -w "HTTP %{http_code}\n" \
  -H "User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) Firefox/120.0" \
  "http://localhost:8080/$SHORT_CODE"
```
Expected: HTTP 302.

### 2.4 Verify Kafka received the events
```bash
docker exec linkhub-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic click-events \
  --from-beginning \
  --timeout-ms 5000 2>/dev/null | head -5
```
Expected: JSON click event messages with `eventId`, `urlId`, `shortCode`, `clickedAt`, `ipAddress`, `userAgent`, `referrer`.

---

## Test 3: Simulate Analytics Consumer (Manual DB Insert)

Since the analytics-consumer is a separate module and might not be running locally, we simulate its work by inserting click events directly into the database.

### 3.1 Insert enriched click events
```bash
# Get the URL ID
URL_ID=$(docker exec linkhub-postgres psql -U linkhub -d linkhub -t -c \
  "SELECT id FROM urls WHERE short_code = '$SHORT_CODE'" | tr -d ' \n')
echo "URL_ID: $URL_ID"

# Insert 10 enriched click events
docker exec linkhub-postgres psql -U linkhub -d linkhub -c "
INSERT INTO click_events (event_id, url_id, short_code, clicked_at, ip_address, user_agent, referrer, device_type, browser, os, country, city, latitude, longitude) VALUES
  (gen_random_uuid(), $URL_ID, '$SHORT_CODE', NOW() - INTERVAL '1 hour', '8.8.8.1', 'Chrome/120', 'https://google.com', 'Desktop', 'Chrome', 'Windows', 'United States', 'New York', 40.7128, -74.0060),
  (gen_random_uuid(), $URL_ID, '$SHORT_CODE', NOW() - INTERVAL '2 hours', '8.8.4.4', 'Safari/17', 'https://twitter.com', 'Mobile', 'Safari', 'iOS', 'United Kingdom', 'London', 51.5074, -0.1278),
  (gen_random_uuid(), $URL_ID, '$SHORT_CODE', NOW() - INTERVAL '3 hours', '1.1.1.1', 'Firefox/120', NULL, 'Desktop', 'Firefox', 'macOS', 'Germany', 'Berlin', 52.5200, 13.4050),
  (gen_random_uuid(), $URL_ID, '$SHORT_CODE', NOW() - INTERVAL '4 hours', '9.9.9.9', 'Chrome/120', 'https://linkedin.com', 'Desktop', 'Chrome', 'Windows', 'United States', 'San Francisco', 37.7749, -122.4194),
  (gen_random_uuid(), $URL_ID, '$SHORT_CODE', NOW() - INTERVAL '1 day', '208.67.222.222', 'Safari/17', 'https://google.com', 'Mobile', 'Safari', 'iOS', 'Japan', 'Tokyo', 35.6762, 139.6503),
  (gen_random_uuid(), $URL_ID, '$SHORT_CODE', NOW() - INTERVAL '1 day 2 hours', '208.67.220.220', 'Edge/120', 'https://bing.com', 'Desktop', 'Edge', 'Windows', 'France', 'Paris', 48.8566, 2.3522),
  (gen_random_uuid(), $URL_ID, '$SHORT_CODE', NOW() - INTERVAL '2 days', '4.4.4.4', 'Chrome/120', 'https://reddit.com', 'Desktop', 'Chrome', 'Linux', 'Canada', 'Toronto', 43.6532, -79.3832),
  (gen_random_uuid(), $URL_ID, '$SHORT_CODE', NOW() - INTERVAL '2 days 3 hours', '64.6.64.6', 'Safari/17', NULL, 'Tablet', 'Safari', 'iPadOS', 'Australia', 'Sydney', -33.8688, 151.2093),
  (gen_random_uuid(), $URL_ID, '$SHORT_CODE', NOW() - INTERVAL '3 days', '77.88.8.8', 'Chrome/120', 'https://google.com', 'Mobile', 'Chrome', 'Android', 'India', 'Mumbai', 19.0760, 72.8777),
  (gen_random_uuid(), $URL_ID, '$SHORT_CODE', NOW() - INTERVAL '3 days 1 hour', '180.76.76.76', 'Firefox/120', 'https://facebook.com', 'Desktop', 'Firefox', 'Windows', 'Brazil', 'São Paulo', -23.5505, -46.6333);
"
```
Expected: `INSERT 0 10`

### 3.2 Verify data in the database
```bash
docker exec linkhub-postgres psql -U linkhub -d linkhub -c \
  "SELECT COUNT(*) AS total_clicks, COUNT(DISTINCT ip_address) AS unique_visitors FROM click_events WHERE short_code = '$SHORT_CODE'"
```
Expected: `total_clicks` >= 10, `unique_visitors` >= 10.

---

## Test 4: Analytics Endpoints — Click Summary

### 4.1 Get click summary (last 30 days)
```bash
curl -s http://localhost:8080/api/v1/analytics/$SHORT_CODE/summary?days=30 \
  -H "Authorization: Bearer $TOKEN" | jq .
```
Expected:
```json
{
  "shortCode": "<SHORT_CODE>",
  "totalClicks": 10,
  "uniqueVisitors": 10,
  "from": "...",
  "to": "..."
}
```

### 4.2 Get click summary with different time range
```bash
curl -s http://localhost:8080/api/v1/analytics/$SHORT_CODE/summary?days=1 \
  -H "Authorization: Bearer $TOKEN" | jq .
```
Expected: `totalClicks` should be less than the 30-day total (only recent clicks).

---

## Test 5: Analytics Endpoints — Timeseries

### 5.1 Daily timeseries
```bash
curl -s http://localhost:8080/api/v1/analytics/$SHORT_CODE/timeseries?days=30\&granularity=day \
  -H "Authorization: Bearer $TOKEN" | jq .
```
Expected: Array of `{"timestamp": "YYYY-MM-DD", "clicks": N}` objects, multiple days represented.

### 5.2 Hourly timeseries
```bash
curl -s http://localhost:8080/api/v1/analytics/$SHORT_CODE/timeseries?days=1\&granularity=hour \
  -H "Authorization: Bearer $TOKEN" | jq .
```
Expected: Array with hourly timestamps like `"YYYY-MM-DD HH:00"`.

---

## Test 6: Analytics Endpoints — Referrers

### 6.1 Top referrers
```bash
curl -s http://localhost:8080/api/v1/analytics/$SHORT_CODE/referrers?days=30 \
  -H "Authorization: Bearer $TOKEN" | jq .
```
Expected: Array of `{"referrer": "...", "clicks": N, "percentage": N.N}` objects.
Should include "https://google.com", "Direct", "https://twitter.com", etc.

### 6.2 Referrers with limit
```bash
curl -s http://localhost:8080/api/v1/analytics/$SHORT_CODE/referrers?days=30\&limit=3 \
  -H "Authorization: Bearer $TOKEN" | jq .
```
Expected: Array with at most 3 entries.

---

## Test 7: Analytics Endpoints — Device Stats

### 7.1 Device breakdown
```bash
curl -s http://localhost:8080/api/v1/analytics/$SHORT_CODE/devices?days=30 \
  -H "Authorization: Bearer $TOKEN" | jq .
```
Expected:
```json
{
  "deviceTypes": [{"name": "Desktop", "clicks": N, "percentage": N.N}, ...],
  "browsers": [{"name": "Chrome", "clicks": N, "percentage": N.N}, ...],
  "operatingSystems": [{"name": "Windows", "clicks": N, "percentage": N.N}, ...]
}
```
Should include Desktop, Mobile, Tablet; Chrome, Safari, Firefox, Edge; Windows, iOS, macOS, etc.

---

## Test 8: Analytics Endpoints — Geographic Stats

### 8.1 Geo breakdown
```bash
curl -s http://localhost:8080/api/v1/analytics/$SHORT_CODE/geo?days=30 \
  -H "Authorization: Bearer $TOKEN" | jq .
```
Expected:
```json
{
  "countries": [{"country": "United States", "clicks": N, "percentage": N.N}, ...],
  "cities": [{"city": "New York", "country": "United States", "clicks": N, "latitude": 40.71, "longitude": -74.00}, ...]
}
```
Should include multiple countries and cities.

### 8.2 Geo with limit
```bash
curl -s http://localhost:8080/api/v1/analytics/$SHORT_CODE/geo?days=30\&limit=3 \
  -H "Authorization: Bearer $TOKEN" | jq .
```
Expected: At most 3 countries and 3 cities.

---

## Test 9: Analytics — Ownership Verification

### 9.1 Register a different user
```bash
TOKEN2=$(curl -s -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email": "other-week3@test.com", "password": "Test1234!"}' | jq -r '.accessToken')
echo "TOKEN2: $TOKEN2"
```

### 9.2 Try to access analytics for another user's URL
```bash
curl -s -w "\nHTTP Status: %{http_code}\n" \
  http://localhost:8080/api/v1/analytics/$SHORT_CODE/summary?days=30 \
  -H "Authorization: Bearer $TOKEN2"
```
Expected: HTTP 404 (Not Found — ownership check fails).

### 9.3 Unauthenticated access
```bash
curl -s -o /dev/null -w "HTTP Status: %{http_code}\n" \
  http://localhost:8080/api/v1/analytics/$SHORT_CODE/summary?days=30
```
Expected: HTTP 302 (redirect to OAuth2 login) or 401/403 — NOT 200.

---

## Test 10: Google OAuth2 Configuration

### 10.1 OAuth2 authorization endpoint exists
```bash
curl -s -o /dev/null -w "HTTP Status: %{http_code}\n" \
  http://localhost:8080/oauth2/authorization/google
```
Expected: HTTP 302 (redirects to Google's OAuth2 authorization URL).

### 10.2 Verify redirect URL
```bash
curl -s -o /dev/null -w "Redirect: %{redirect_url}\n" \
  http://localhost:8080/oauth2/authorization/google
```
Expected: Redirect URL starts with `https://accounts.google.com/o/oauth2/v2/auth`.

### 10.3 OAuth2 callback endpoint exists
```bash
curl -s -o /dev/null -w "HTTP Status: %{http_code}\n" \
  "http://localhost:8080/login/oauth2/code/google?code=fake&state=fake"
```
Expected: HTTP 302 (redirect to /login?error) — NOT 403 (the path is accessible, just the code is invalid).

**Note**: Full end-to-end Google OAuth2 testing requires:
1. A valid Google Cloud project with OAuth2 credentials
2. Setting `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` environment variables
3. Adding `http://localhost:8080/login/oauth2/code/google` as an authorized redirect URI in Google Cloud Console
4. Testing in a browser at: `http://localhost:8080/oauth2/authorization/google`

---

## Test 11: DLQ (Dead Letter Queue) Mechanism

### 11.1 Insert a failed event into DLQ
```bash
docker exec linkhub-postgres psql -U linkhub -d linkhub -c "
INSERT INTO failed_click_events (event_id, payload, failure_reason, retry_count, created_at, next_retry_at)
VALUES (
  gen_random_uuid(),
  '{\"eventId\": \"$(uuidgen)\", \"urlId\": 1, \"shortCode\": \"test123\", \"clickedAt\": \"2026-02-10T10:00:00Z\", \"ipAddress\": \"1.2.3.4\", \"userAgent\": \"test\", \"referrer\": null}',
  'Test failure',
  0,
  NOW(),
  NOW() - INTERVAL '1 minute'
);
"
```
Expected: `INSERT 0 1`

### 11.2 Verify DLQ entry
```bash
docker exec linkhub-postgres psql -U linkhub -d linkhub -c \
  "SELECT id, event_id, failure_reason, retry_count, next_retry_at FROM failed_click_events LIMIT 5"
```
Expected: Shows the DLQ entry with retry_count=0.

### 11.3 Verify DLQ retry job runs
The DLQ retry job runs every 2 minutes. Wait for it, then check:
```bash
sleep 130
docker exec linkhub-postgres psql -U linkhub -d linkhub -c \
  "SELECT id, retry_count, failure_reason FROM failed_click_events WHERE retry_count > 0 LIMIT 5"
```
Expected: `retry_count` should have increased (the retry attempted but likely failed since the event data is fabricated).

---

## Test 12: Analytics Consumer — Kafka Consumer Config

### 12.1 Verify Kafka consumer config
```bash
cat /Users/mac_nit/Desktop/preya/projects/LinkHub/analytics-consumer/src/main/resources/application.yml | grep -A10 "kafka:"
```
Expected: Shows `bootstrap-servers`, `group-id: analytics-consumer-group`, `auto-offset-reset: earliest`, `max-poll-records: 100`.

### 12.2 Verify batch listener configuration
```bash
grep -r "setBatchListener" /Users/mac_nit/Desktop/preya/projects/LinkHub/analytics-consumer/
```
Expected: `factory.setBatchListener(true)` — batch processing enabled.

### 12.3 Verify idempotent insert
```bash
grep "ON CONFLICT" /Users/mac_nit/Desktop/preya/projects/LinkHub/analytics-consumer/src/main/java/com/linkhub/analytics/consumer/ClickEventConsumer.java
```
Expected: `ON CONFLICT (event_id, clicked_at) DO NOTHING`

---

## Test 13: GeoIP Service

### 13.1 Verify graceful degradation (no GeoIP DB)
The GeoIP service should log a warning but not crash when the database file is missing:
```bash
grep -r "Geographic enrichment disabled" /Users/mac_nit/Desktop/preya/projects/LinkHub/analytics-consumer/
```
Expected: Warning message in GeoIpService.java.

### 13.2 Verify GeoIP config
```bash
grep "database-path" /Users/mac_nit/Desktop/preya/projects/LinkHub/analytics-consumer/src/main/resources/application.yml
```
Expected: `database-path: /data/geoip/GeoLite2-City.mmdb`

---

## Test 14: User-Agent Parser

### 14.1 Verify UA parser exists
```bash
grep -r "class UserAgentParser" /Users/mac_nit/Desktop/preya/projects/LinkHub/analytics-consumer/
```
Expected: `UserAgentParser` service class found.

### 14.2 Verify device type inference
```bash
grep -A5 "inferDeviceType" /Users/mac_nit/Desktop/preya/projects/LinkHub/analytics-consumer/src/main/java/com/linkhub/analytics/service/UserAgentParser.java | head -10
```
Expected: Method that classifies into Desktop, Mobile, Tablet, Bot, Other.

---

## Test 15: Docker Compose — Analytics Consumer Service

### 15.1 Verify analytics-consumer in docker-compose
```bash
grep -A15 "analytics-consumer:" /Users/mac_nit/Desktop/preya/projects/LinkHub/docker-compose.yml
```
Expected: Service definition with Dockerfile, depends_on postgres + kafka, port 8081, GeoIP volume mount.

### 15.2 Verify GeoIP volume defined
```bash
grep "geoip_data" /Users/mac_nit/Desktop/preya/projects/LinkHub/docker-compose.yml
```
Expected: `geoip_data` volume defined.

---

## Test 16: Swagger / API Documentation

### 16.1 Verify analytics endpoints in Swagger
```bash
curl -s http://localhost:8080/api-docs | jq '.paths | keys | map(select(startswith("/api/v1/analytics")))' 
```
Expected: Array containing paths like:
- `/api/v1/analytics/{shortCode}/summary`
- `/api/v1/analytics/{shortCode}/timeseries`
- `/api/v1/analytics/{shortCode}/referrers`
- `/api/v1/analytics/{shortCode}/devices`
- `/api/v1/analytics/{shortCode}/geo`

---

## Test 17: Integration Tests

### 17.1 Run all integration tests
```bash
cd /Users/mac_nit/Desktop/preya/projects/LinkHub
mvn test -pl backend \
  -Dtest="com.linkhub.integration.AnalyticsIntegrationTest,com.linkhub.integration.OAuth2IntegrationTest,com.linkhub.integration.UrlCreationIntegrationTest,com.linkhub.integration.RedirectIntegrationTest,com.linkhub.integration.RateLimitIntegrationTest"
```
Expected: `Tests run: 40, Failures: 0, Errors: 0, Skipped: 0` — BUILD SUCCESS.

### 17.2 Verify test coverage
- AnalyticsIntegrationTest: 18 tests (summary, timeseries, referrers, devices, geo, pagination, time-range, auth checks, Kafka, DLQ, partitions)
- OAuth2IntegrationTest: 4 tests (endpoint accessible, Google user creation, account linking, callback)
- UrlCreationIntegrationTest: 8 tests (existing from Week 2)
- RedirectIntegrationTest: 5 tests (existing from Week 2)
- RateLimitIntegrationTest: 5 tests (existing from Week 2)

---

## Test 18: ClickEventProducer — DLQ Write on Failure

### 18.1 Verify DLQ write logic in producer
```bash
grep -A10 "writeToDlq" /Users/mac_nit/Desktop/preya/projects/LinkHub/backend/src/main/java/com/linkhub/analytics/producer/ClickEventProducer.java
```
Expected: Method that serializes the event to JSON and saves to `failed_click_events` table.

### 18.2 Verify producer writes to DLQ on Kafka failure
The producer's `whenComplete` callback now calls `writeToDlq` instead of just logging:
```bash
grep "writeToDlq" /Users/mac_nit/Desktop/preya/projects/LinkHub/backend/src/main/java/com/linkhub/analytics/producer/ClickEventProducer.java
```
Expected: Two calls — one in the `whenComplete` error handler, one in the catch block.

---

---

## Test 19: Partition Management Automation

### 19.1 Verify existing partitions
```bash
docker exec linkhub-postgres psql -U linkhub -d linkhub -c "
SELECT inhrelid::regclass::text AS partition_name
FROM pg_inherits
WHERE inhparent = 'click_events'::regclass
ORDER BY partition_name;
"
```
Expected: At least `click_events_2026_02` and `click_events_2026_03` (created in V4 migration).

### 19.2 Verify partition management job creates future partitions
The PartitionManagementJob creates partitions 3 months ahead. Trigger it manually by calling the backend health first (it runs on startup schedule), then check:
```bash
# The job runs daily at 00:05 UTC. To test manually, we can verify the code:
grep -A3 "MONTHS_AHEAD" /Users/mac_nit/Desktop/preya/projects/LinkHub/backend/src/main/java/com/linkhub/analytics/scheduler/PartitionManagementJob.java
```
Expected: `MONTHS_AHEAD = 3` and `RETENTION_MONTHS = 12`.

### 19.3 Verify partition stats query
```bash
docker exec linkhub-postgres psql -U linkhub -d linkhub -c "
SELECT
    inhrelid::regclass::text AS partition_name,
    pg_size_pretty(pg_relation_size(inhrelid)) AS size,
    (SELECT reltuples::bigint FROM pg_class WHERE oid = inhrelid) AS row_estimate
FROM pg_inherits
WHERE inhparent = 'click_events'::regclass
ORDER BY partition_name;
"
```
Expected: Table with partition names, sizes, and row estimates.

### 19.4 Verify old partition detachment logic
```bash
grep "RETENTION_MONTHS" /Users/mac_nit/Desktop/preya/projects/LinkHub/backend/src/main/java/com/linkhub/analytics/scheduler/PartitionManagementJob.java
```
Expected: `RETENTION_MONTHS = 12` — partitions older than 12 months are detached.

---

## Test 20: Kafka DLQ Topic

### 20.1 Verify DLQ topic is configured
```bash
grep -A5 "CLICK_EVENTS_DLQ_TOPIC" /Users/mac_nit/Desktop/preya/projects/LinkHub/backend/src/main/java/com/linkhub/config/KafkaConfig.java
```
Expected: `CLICK_EVENTS_DLQ_TOPIC = "click-events-dlq"` with 7-day retention.

### 20.2 Verify DLQ topic exists in Kafka
```bash
docker exec linkhub-kafka kafka-topics --list --bootstrap-server localhost:9092 2>/dev/null | grep dlq
```
Expected: `click-events-dlq` topic listed.

### 20.3 Verify consumer error handling strategy
```bash
grep -B2 -A5 "addNotRetryableExceptions" /Users/mac_nit/Desktop/preya/projects/LinkHub/analytics-consumer/src/main/java/com/linkhub/analytics/config/KafkaConsumerConfig.java
```
Expected: `JsonProcessingException`, `SerializationException`, `ClassCastException`, `NullPointerException` listed as non-retryable.

### 20.4 Verify retry backoff config
```bash
grep "FixedBackOff" /Users/mac_nit/Desktop/preya/projects/LinkHub/analytics-consumer/src/main/java/com/linkhub/analytics/config/KafkaConsumerConfig.java
```
Expected: `new FixedBackOff(1000L, 3L)` — 1s interval, 3 max attempts.

---

## Test 21: Batch Insert Tuning

### 21.1 Verify JDBC batch insert in consumer
```bash
grep "batchUpdate" /Users/mac_nit/Desktop/preya/projects/LinkHub/analytics-consumer/src/main/java/com/linkhub/analytics/consumer/ClickEventConsumer.java
```
Expected: `jdbcTemplate.batchUpdate(BATCH_INSERT_SQL, ...)` — batch insert instead of individual inserts.

### 21.2 Verify fallback to individual inserts
```bash
grep -A3 "Fall back to individual" /Users/mac_nit/Desktop/preya/projects/LinkHub/analytics-consumer/src/main/java/com/linkhub/analytics/consumer/ClickEventConsumer.java
```
Expected: Fallback logic that tries individual inserts when batch fails.

### 21.3 Verify Hibernate batch settings
```bash
grep -A5 "batch_size" /Users/mac_nit/Desktop/preya/projects/LinkHub/analytics-consumer/src/main/resources/application.yml
```
Expected: `batch_size: 100`, `order_inserts: true`.

### 21.4 Verify HikariCP tuning
```bash
grep -A6 "hikari:" /Users/mac_nit/Desktop/preya/projects/LinkHub/analytics-consumer/src/main/resources/application.yml
```
Expected: `idle-timeout`, `max-lifetime`, `leak-detection-threshold` configured.

---

## Test 22: Observability Metrics for Analytics Consumer

### 22.1 Verify custom metrics in code
```bash
grep -E "Counter|Timer|Gauge" /Users/mac_nit/Desktop/preya/projects/LinkHub/analytics-consumer/src/main/java/com/linkhub/analytics/consumer/ClickEventConsumer.java | head -10
```
Expected: Counters for `analytics.events.processed`, `analytics.events.failed`, `analytics.events.dlq`, `analytics.events.duplicates`; Timers for `analytics.batch.processing.time`, `analytics.enrichment.time`.

### 22.2 Verify gauge metrics
```bash
grep "Gauge.builder" /Users/mac_nit/Desktop/preya/projects/LinkHub/analytics-consumer/src/main/java/com/linkhub/analytics/config/ConsumerMetricsConfig.java
```
Expected: Gauges for `analytics.click_events.total`, `analytics.dlq.pending`, `analytics.dlq.exhausted`, `analytics.partitions.count`.

### 22.3 Verify Prometheus scrape config includes analytics-consumer
```bash
grep -A5 "analytics-consumer" /Users/mac_nit/Desktop/preya/projects/LinkHub/monitoring/prometheus/prometheus.yml
```
Expected: Scrape target for `host.docker.internal:8081`.

### 22.4 Verify SLO histograms configured
```bash
grep -A6 "percentiles-histogram" /Users/mac_nit/Desktop/preya/projects/LinkHub/analytics-consumer/src/main/resources/application.yml
```
Expected: `analytics.batch.processing.time: true`, `analytics.enrichment.time: true` with SLO buckets.

---

## Test 23: Pagination and Time-Range Filtering

### 23.1 Paginated click events (page 0, size 3)
```bash
curl -s "http://localhost:8080/api/v1/analytics/$SHORT_CODE/clicks?days=30&page=0&size=3" \
  -H "Authorization: Bearer $TOKEN" | jq .
```
Expected:
```json
{
  "content": [ ... 3 click events ... ],
  "page": 0,
  "size": 3,
  "totalElements": 10,
  "totalPages": 4,
  "hasNext": true
}
```

### 23.2 Paginated click events (last page)
```bash
curl -s "http://localhost:8080/api/v1/analytics/$SHORT_CODE/clicks?days=30&page=3&size=3" \
  -H "Authorization: Bearer $TOKEN" | jq .
```
Expected: `hasNext: false`, `content` has 1 element (10 total, 3 per page, 4th page has remainder).

### 23.3 Click events with explicit time range (ISO-8601)
```bash
FROM=$(date -u -v-1d +%Y-%m-%dT%H:%M:%SZ)
TO=$(date -u +%Y-%m-%dT%H:%M:%SZ)
curl -s "http://localhost:8080/api/v1/analytics/$SHORT_CODE/clicks?from=$FROM&to=$TO&page=0&size=50" \
  -H "Authorization: Bearer $TOKEN" | jq .
```
Expected: Only click events within the last 24 hours. `totalElements` < total.

### 23.4 Summary with explicit time range
```bash
FROM=$(date -u -v-1d +%Y-%m-%dT%H:%M:%SZ)
TO=$(date -u +%Y-%m-%dT%H:%M:%SZ)
curl -s "http://localhost:8080/api/v1/analytics/$SHORT_CODE/summary?from=$FROM&to=$TO" \
  -H "Authorization: Bearer $TOKEN" | jq .
```
Expected: `from` and `to` in the response match the request parameters, `totalClicks` < 10.

### 23.5 Timeseries with explicit time range
```bash
FROM=$(date -u -v-7d +%Y-%m-%dT%H:%M:%SZ)
TO=$(date -u +%Y-%m-%dT%H:%M:%SZ)
curl -s "http://localhost:8080/api/v1/analytics/$SHORT_CODE/timeseries?from=$FROM&to=$TO&granularity=day" \
  -H "Authorization: Bearer $TOKEN" | jq .
```
Expected: Only days within the 7-day window are returned.

### 23.6 Page size clamping (max 200)
```bash
curl -s "http://localhost:8080/api/v1/analytics/$SHORT_CODE/clicks?days=30&page=0&size=999" \
  -H "Authorization: Bearer $TOKEN" | jq .size
```
Expected: `200` (clamped from 999).

### 23.7 Click event DTO fields
```bash
curl -s "http://localhost:8080/api/v1/analytics/$SHORT_CODE/clicks?days=30&page=0&size=1" \
  -H "Authorization: Bearer $TOKEN" | jq '.content[0]'
```
Expected: Object with fields: `eventId`, `shortCode`, `clickedAt`, `ipAddress`, `referrer`, `deviceType`, `browser`, `os`, `country`, `city`.

---

## Summary Checklist

| # | Feature | Status |
|---|---------|--------|
| 1 | Analytics-consumer module compiles | |
| 2 | Click events sent to Kafka via redirect | |
| 3 | Click events enriched in DB (simulated) | |
| 4 | Analytics summary endpoint | |
| 5 | Analytics timeseries endpoint (daily + hourly) | |
| 6 | Analytics referrers endpoint | |
| 7 | Analytics device stats endpoint | |
| 8 | Analytics geo stats endpoint | |
| 9 | Analytics ownership verification | |
| 10 | Google OAuth2 configuration | |
| 11 | DLQ mechanism (insert + retry) | |
| 12 | Kafka consumer config (batch, idempotent) | |
| 13 | GeoIP service (graceful degradation) | |
| 14 | User-Agent parser | |
| 15 | Docker Compose updated | |
| 16 | Swagger docs include analytics | |
| 17 | All 40 integration tests pass | |
| 18 | ClickEventProducer DLQ write on failure | |
| 19 | Partition management automation | |
| 20 | Kafka DLQ topic + error handling strategy | |
| 21 | Batch insert tuning + performance configs | |
| 22 | Observability metrics for analytics-consumer | |
| 23 | Pagination + time-range filtering for analytics | |
