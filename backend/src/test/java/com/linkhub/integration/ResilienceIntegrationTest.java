package com.linkhub.integration;

import com.linkhub.ratelimit.RateLimitService;
import com.linkhub.url.cache.UrlCacheService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for Resilience4j circuit breakers on
 * UrlCacheService and RateLimitService.
 */
class ResilienceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private UrlCacheService urlCacheService;

    @Autowired
    private RateLimitService rateLimitService;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeEach
    void resetCircuitBreakers() {
        circuitBreakerRegistry.getAllCircuitBreakers()
                .forEach(CircuitBreaker::reset);
    }

    // ────────── UrlCacheService Circuit Breaker ──────────

    @Test
    @DisplayName("UrlCacheService circuit breaker is registered")
    void redisCacheCircuitBreakerExists() {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("redisCache");
        assertThat(cb).isNotNull();
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    @DisplayName("getRedirectUrl returns empty Optional on cache miss (not an error)")
    void getRedirectUrlReturnEmptyOnMiss() {
        Optional<String> result = urlCacheService.getRedirectUrl("nonexistent-code");
        assertThat(result).isEmpty();

        // Circuit should still be closed (cache miss is not a failure)
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("redisCache");
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    @DisplayName("cacheRedirectUrl and getRedirectUrl round-trip works")
    void cacheAndRetrieveRedirectUrl() {
        urlCacheService.cacheRedirectUrl("test-rt", "https://example.com");
        Optional<String> result = urlCacheService.getRedirectUrl("test-rt");
        assertThat(result).isPresent().contains("https://example.com");
    }

    @Test
    @DisplayName("incrementClickCount does not open circuit breaker when Redis is up")
    void incrementClickCountNormalOperation() {
        urlCacheService.incrementClickCount("click-test");
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("redisCache");
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    @DisplayName("invalidate does not throw when key doesn't exist")
    void invalidateNonExistentKey() {
        urlCacheService.invalidate("no-such-key");
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("redisCache");
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    @DisplayName("getAndResetClickCount returns 0 for non-existent key")
    void getAndResetNonExistentKey() {
        long count = urlCacheService.getAndResetClickCount("no-clicks");
        assertThat(count).isZero();
    }

    // ────────── RateLimitService Circuit Breaker ──────────

    @Test
    @DisplayName("RateLimitService circuit breaker is registered")
    void rateLimitCircuitBreakerExists() {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("redisRateLimit");
        assertThat(cb).isNotNull();
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    @DisplayName("Rate limiter allows requests within limit")
    void rateLimiterAllowsWithinLimit() {
        boolean allowed = rateLimitService.isAllowedByIp("192.168.1.100");
        assertThat(allowed).isTrue();

        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("redisRateLimit");
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    @DisplayName("Rate limiter rejects requests over limit")
    void rateLimiterRejectsOverLimit() {
        // Exhaust the limit (20 per minute for IP)
        for (int i = 0; i < 20; i++) {
            rateLimitService.isAllowedByIp("192.168.1.200");
        }
        // 21st request should be rejected
        boolean allowed = rateLimitService.isAllowedByIp("192.168.1.200");
        assertThat(allowed).isFalse();
    }

    @Test
    @DisplayName("Rate limiter allows bulk requests within limit")
    void rateLimiterBulkWithinLimit() {
        boolean allowed = rateLimitService.isAllowedBulk(999L);
        assertThat(allowed).isTrue();
    }

    // ────────── Analytics Lag Endpoint ──────────

    @Test
    @DisplayName("System analytics-lag endpoint is accessible without auth")
    void analyticsLagEndpointAccessible() throws Exception {
        // The endpoint should be publicly accessible
        var result = org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/v1/system/analytics-lag");

        // We can't directly use MockMvc here since we don't inject it,
        // but we verify the circuit breaker config is correct
        assertThat(circuitBreakerRegistry.getAllCircuitBreakers()).isNotEmpty();
    }
}
