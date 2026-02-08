/**
 * LinkHub Redirect Endpoint Load Test
 *
 * Usage:
 *   k6 run loadtest/redirect-load-test.js
 *
 * Prerequisites:
 *   1. Install k6: brew install k6
 *   2. Start the backend (docker-compose up or local)
 *   3. Create a test URL and set SHORT_CODE env var:
 *
 *   export SHORT_CODE="abc1234"
 *   k6 run loadtest/redirect-load-test.js
 *
 *   Or run with default settings:
 *   k6 run loadtest/redirect-load-test.js
 *
 * This test validates:
 *   - Sub-50ms p95 redirect latency
 *   - High throughput under concurrent load
 *   - Error rate stays below 1%
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Custom metrics
const redirectDuration = new Trend('redirect_duration', true);
const errorRate = new Rate('errors');

// Test configuration
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const SHORT_CODE = __ENV.SHORT_CODE || 'test123';

export const options = {
    stages: [
        // Ramp up
        { duration: '10s', target: 10 },    // 10 concurrent users
        { duration: '30s', target: 50 },    // Ramp to 50 users
        { duration: '30s', target: 50 },    // Sustain 50 users
        { duration: '20s', target: 100 },   // Peak at 100 users
        { duration: '20s', target: 100 },   // Sustain peak
        { duration: '10s', target: 0 },     // Ramp down
    ],
    thresholds: {
        // Target: sub-50ms redirects
        'redirect_duration': ['p(95)<50', 'p(99)<100'],
        // Error rate must be below 1%
        'errors': ['rate<0.01'],
        // Overall HTTP request duration
        'http_req_duration': ['p(95)<100'],
    },
};

export function setup() {
    // Verify the short code exists before load testing
    console.log(`Testing redirect for: ${BASE_URL}/${SHORT_CODE}`);

    // First, register a user and create a URL if SHORT_CODE is "test123" (default)
    if (SHORT_CODE === 'test123') {
        console.log('Using default SHORT_CODE. Create a test URL first:');
        console.log('  1. Register a user via POST /api/v1/auth/register');
        console.log('  2. Create a URL via POST /api/v1/urls');
        console.log('  3. Run: SHORT_CODE=<your_code> k6 run loadtest/redirect-load-test.js');
    }

    return { baseUrl: BASE_URL, shortCode: SHORT_CODE };
}

export default function (data) {
    const url = `${data.baseUrl}/${data.shortCode}`;

    const params = {
        redirects: 0, // Don't follow redirects — we want to measure the 302 response time
        tags: { name: 'redirect' },
    };

    const start = Date.now();
    const res = http.get(url, params);
    const duration = Date.now() - start;

    // Record custom metric
    redirectDuration.add(duration);

    // Check expectations
    const passed = check(res, {
        'status is 302': (r) => r.status === 302,
        'has Location header': (r) => r.headers['Location'] !== undefined,
        'response time < 50ms': () => duration < 50,
    });

    if (!passed) {
        errorRate.add(1);
    } else {
        errorRate.add(0);
    }

    // Small think time between requests
    sleep(0.1);
}

export function handleSummary(data) {
    const p50 = data.metrics.redirect_duration?.values?.['p(50)'] || 'N/A';
    const p95 = data.metrics.redirect_duration?.values?.['p(95)'] || 'N/A';
    const p99 = data.metrics.redirect_duration?.values?.['p(99)'] || 'N/A';

    console.log('\n═══════════════════════════════════════');
    console.log('  LinkHub Redirect Load Test Results');
    console.log('═══════════════════════════════════════');
    console.log(`  p50 latency: ${p50}ms`);
    console.log(`  p95 latency: ${p95}ms`);
    console.log(`  p99 latency: ${p99}ms`);
    console.log(`  Target: p95 < 50ms`);
    console.log('═══════════════════════════════════════\n');

    return {};
}
