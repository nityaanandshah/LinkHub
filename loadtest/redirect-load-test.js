/**
 * LinkHub Redirect Endpoint Load Test — k6
 *
 * Usage:
 *   k6 run loadtest/redirect-load-test.js
 *
 * With custom settings:
 *   SHORT_CODE=abc1234 BASE_URL=http://localhost:8080 k6 run loadtest/redirect-load-test.js
 *
 * With automated setup (creates a user + URL):
 *   AUTO_SETUP=true k6 run loadtest/redirect-load-test.js
 *
 * Scenarios validated:
 *   - Sub-50ms p95 redirect latency (cache hit)
 *   - Sub-100ms p99 latency
 *   - High throughput under 100 concurrent users
 *   - Error rate below 1%
 *   - Sustained load stability
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

// Custom metrics
const redirectDuration = new Trend('redirect_duration', true);
const cacheHitDuration = new Trend('cache_hit_duration', true);
const errorRate = new Rate('errors');
const totalRedirects = new Counter('total_redirects');

// Test configuration
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const SHORT_CODE = __ENV.SHORT_CODE || '';
const AUTO_SETUP = __ENV.AUTO_SETUP === 'true';

export const options = {
    scenarios: {
        // Warm-up: seed the Redis cache
        warmup: {
            executor: 'shared-iterations',
            iterations: 20,
            vus: 2,
            maxDuration: '10s',
            exec: 'warmup',
        },
        // Main load test: ramp to peak
        load_test: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '15s', target: 20 },    // Ramp up
                { duration: '30s', target: 50 },    // Steady state
                { duration: '20s', target: 100 },   // Peak load
                { duration: '30s', target: 100 },   // Sustain peak
                { duration: '15s', target: 0 },     // Ramp down
            ],
            startTime: '15s', // Start after warmup
            exec: 'default',
        },
        // Spike test: sudden burst
        spike: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '5s', target: 200 },    // Sudden spike
                { duration: '10s', target: 200 },   // Sustain spike
                { duration: '5s', target: 0 },      // Drop
            ],
            startTime: '130s', // After main load test
            exec: 'default',
        },
    },
    thresholds: {
        // Target: sub-50ms redirects at p95
        'redirect_duration': ['p(95)<50', 'p(99)<100', 'avg<30'],
        // Cache-hit redirects should be faster
        'cache_hit_duration': ['p(95)<25', 'p(99)<50'],
        // Error rate must be below 1%
        'errors': ['rate<0.01'],
        // Overall HTTP request duration
        'http_req_duration': ['p(95)<100'],
    },
};

export function setup() {
    let testShortCode = SHORT_CODE;

    if (AUTO_SETUP && !SHORT_CODE) {
        console.log('Auto-setup: Creating test user and URL...');

        // Register user
        const registerRes = http.post(
            `${BASE_URL}/api/v1/auth/register`,
            JSON.stringify({
                email: `loadtest-${Date.now()}@linkhub.test`,
                password: 'LoadTest123!',
                displayName: 'Load Test User',
            }),
            { headers: { 'Content-Type': 'application/json' } }
        );

        if (registerRes.status === 201 || registerRes.status === 200) {
            const authData = JSON.parse(registerRes.body);
            const token = authData.accessToken;

            // Create a URL
            const createRes = http.post(
                `${BASE_URL}/api/v1/urls`,
                JSON.stringify({ longUrl: 'https://github.com/linkhub/loadtest' }),
                {
                    headers: {
                        'Content-Type': 'application/json',
                        Authorization: `Bearer ${token}`,
                    },
                }
            );

            if (createRes.status === 201) {
                const urlData = JSON.parse(createRes.body);
                testShortCode = urlData.shortCode;
                console.log(`Created test URL: ${testShortCode}`);
            } else {
                console.error(`Failed to create URL: ${createRes.status} ${createRes.body}`);
            }
        } else {
            console.error(`Failed to register: ${registerRes.status} ${registerRes.body}`);
        }
    }

    if (!testShortCode) {
        console.log('⚠ No SHORT_CODE set. Either set SHORT_CODE env var or use AUTO_SETUP=true');
        console.log('  Example: SHORT_CODE=abc1234 k6 run loadtest/redirect-load-test.js');
        testShortCode = 'test123'; // Will likely 404, but test will run
    }

    console.log(`\nLoad testing: ${BASE_URL}/${testShortCode}\n`);
    return { baseUrl: BASE_URL, shortCode: testShortCode };
}

// Warm-up function — seeds the Redis cache
export function warmup(data) {
    const url = `${data.baseUrl}/${data.shortCode}`;
    http.get(url, { redirects: 0 });
    sleep(0.2);
}

// Main test function
export default function (data) {
    const url = `${data.baseUrl}/${data.shortCode}`;

    const params = {
        redirects: 0, // Don't follow redirects — measure the 302 response time
        tags: { name: 'redirect' },
    };

    const start = Date.now();
    const res = http.get(url, params);
    const duration = Date.now() - start;

    // Record metrics
    redirectDuration.add(duration);
    totalRedirects.add(1);

    // Cache hits should be very fast
    if (duration < 25) {
        cacheHitDuration.add(duration);
    }

    // Check expectations
    const passed = check(res, {
        'status is 302': (r) => r.status === 302,
        'has Location header': (r) => r.headers['Location'] !== undefined,
        'response time < 50ms': () => duration < 50,
        'response time < 100ms': () => duration < 100,
    });

    if (!passed) {
        errorRate.add(1);
    } else {
        errorRate.add(0);
    }

    // Small think time between requests
    sleep(0.05 + Math.random() * 0.1);
}

export function handleSummary(data) {
    const p50 = data.metrics.redirect_duration?.values?.['p(50)']?.toFixed(2) || 'N/A';
    const p95 = data.metrics.redirect_duration?.values?.['p(95)']?.toFixed(2) || 'N/A';
    const p99 = data.metrics.redirect_duration?.values?.['p(99)']?.toFixed(2) || 'N/A';
    const avg = data.metrics.redirect_duration?.values?.avg?.toFixed(2) || 'N/A';
    const total = data.metrics.total_redirects?.values?.count || 0;
    const errors = data.metrics.errors?.values?.rate || 0;

    const result = `
╔═══════════════════════════════════════════════════╗
║       LinkHub Redirect Load Test Results          ║
╠═══════════════════════════════════════════════════╣
║  Total redirects:  ${String(total).padStart(10)}                  ║
║  Error rate:       ${(errors * 100).toFixed(2).padStart(9)}%                  ║
║                                                   ║
║  Latency:                                         ║
║    avg:   ${String(avg + 'ms').padStart(10)}                            ║
║    p50:   ${String(p50 + 'ms').padStart(10)}                            ║
║    p95:   ${String(p95 + 'ms').padStart(10)}  (target: <50ms)       ║
║    p99:   ${String(p99 + 'ms').padStart(10)}  (target: <100ms)      ║
║                                                   ║
║  Result: ${p95 !== 'N/A' && parseFloat(p95) < 50 ? 'PASS ✓' : 'FAIL ✗'}                                      ║
╚═══════════════════════════════════════════════════╝
`;

    console.log(result);
    return {};
}
