package com.linkhub.url.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkhub.url.model.Url;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis cache service for URL operations with Resilience4j circuit breaker.
 *
 * <p>Key patterns:
 * <ul>
 *   <li>{@code url:{shortCode}} → longUrl (redirect hot path, TTL 1h default / 24h hot)</li>
 *   <li>{@code url:meta:{shortCode}} → JSON (full URL metadata, TTL 1h)</li>
 * </ul>
 *
 * <p>When Redis is down, the circuit breaker opens and methods fall back
 * gracefully — redirect lookups return empty (DB fallback), writes are no-ops,
 * and click counters are silently skipped (flushed from DB later).
 */
@Service
public class UrlCacheService {

    private static final Logger log = LoggerFactory.getLogger(UrlCacheService.class);

    private static final String REDIRECT_KEY_PREFIX = "url:";
    private static final String META_KEY_PREFIX = "url:meta:";
    private static final String CLICK_COUNTER_PREFIX = "clicks:";

    private static final Duration DEFAULT_TTL = Duration.ofHours(1);
    private static final Duration HOT_TTL = Duration.ofHours(24);
    private static final long HOT_THRESHOLD = 100; // clicks/hour to promote to hot

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public UrlCacheService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    // ────────── Write-Through (on URL creation) ──────────

    /**
     * Cache URL on creation (write-through).
     * Stores both the redirect mapping and full metadata.
     */
    @CircuitBreaker(name = "redisCache", fallbackMethod = "cacheOnCreateFallback")
    public void cacheOnCreate(Url url) {
        // Redirect cache: url:{shortCode} → longUrl
        redisTemplate.opsForValue().set(
                REDIRECT_KEY_PREFIX + url.getShortCode(),
                url.getLongUrl(),
                DEFAULT_TTL
        );

        // Metadata cache: url:meta:{shortCode} → JSON
        try {
            String json = objectMapper.writeValueAsString(url);
            redisTemplate.opsForValue().set(
                    META_KEY_PREFIX + url.getShortCode(),
                    json,
                    DEFAULT_TTL
            );
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize URL for cache: {}", e.getMessage());
        }

        log.debug("Cache populated for shortCode={}", url.getShortCode());
    }

    @SuppressWarnings("unused")
    private void cacheOnCreateFallback(Url url, Throwable t) {
        log.warn("Circuit breaker OPEN — skipping cache write for shortCode={}: {}",
                url.getShortCode(), t.getMessage());
    }

    // ────────── Cache-Aside (on redirect) ──────────

    /**
     * Look up the long URL from Redis cache.
     * Returns empty if cache miss or Redis is down.
     */
    @CircuitBreaker(name = "redisCache", fallbackMethod = "getRedirectUrlFallback")
    public Optional<String> getRedirectUrl(String shortCode) {
        String longUrl = redisTemplate.opsForValue().get(REDIRECT_KEY_PREFIX + shortCode);
        return Optional.ofNullable(longUrl);
    }

    @SuppressWarnings("unused")
    private Optional<String> getRedirectUrlFallback(String shortCode, Throwable t) {
        log.warn("Circuit breaker OPEN — cache miss fallback for shortCode={}: {}", shortCode, t.getMessage());
        return Optional.empty(); // Fallback to DB
    }

    /**
     * Populate redirect cache after a DB lookup (cache-aside fill).
     */
    @CircuitBreaker(name = "redisCache", fallbackMethod = "cacheRedirectUrlFallback")
    public void cacheRedirectUrl(String shortCode, String longUrl) {
        redisTemplate.opsForValue().set(
                REDIRECT_KEY_PREFIX + shortCode,
                longUrl,
                DEFAULT_TTL
        );
    }

    @SuppressWarnings("unused")
    private void cacheRedirectUrlFallback(String shortCode, String longUrl, Throwable t) {
        log.warn("Circuit breaker OPEN — skipping cache fill for shortCode={}: {}", shortCode, t.getMessage());
    }

    // ────────── Eager Invalidation (on update/delete) ──────────

    /**
     * Invalidate all cached data for a short code.
     * Called on URL update, delete, or expiry.
     */
    @CircuitBreaker(name = "redisCache", fallbackMethod = "invalidateFallback")
    public void invalidate(String shortCode) {
        redisTemplate.delete(REDIRECT_KEY_PREFIX + shortCode);
        redisTemplate.delete(META_KEY_PREFIX + shortCode);
        redisTemplate.delete(CLICK_COUNTER_PREFIX + shortCode);
        log.debug("Cache invalidated for shortCode={}", shortCode);
    }

    @SuppressWarnings("unused")
    private void invalidateFallback(String shortCode, Throwable t) {
        log.warn("Circuit breaker OPEN — skipping cache invalidation for shortCode={}: {}", shortCode, t.getMessage());
    }

    // ────────── Click Counter Buffering ──────────

    /**
     * Atomically increment the click counter in Redis.
     * A scheduled job will flush these to PostgreSQL periodically.
     */
    @CircuitBreaker(name = "redisCache", fallbackMethod = "incrementClickCountFallback")
    public void incrementClickCount(String shortCode) {
        Long count = redisTemplate.opsForValue().increment(CLICK_COUNTER_PREFIX + shortCode);
        if (count != null && count == 1) {
            redisTemplate.expire(CLICK_COUNTER_PREFIX + shortCode, Duration.ofHours(2));
        }

        // Hot URL promotion: extend redirect cache TTL
        if (count != null && count >= HOT_THRESHOLD) {
            redisTemplate.expire(REDIRECT_KEY_PREFIX + shortCode, HOT_TTL);
            log.debug("Hot URL promotion for shortCode={}, clicks={}", shortCode, count);
        }
    }

    @SuppressWarnings("unused")
    private void incrementClickCountFallback(String shortCode, Throwable t) {
        log.warn("Circuit breaker OPEN — skipping click counter for shortCode={}: {}", shortCode, t.getMessage());
    }

    /**
     * Read and reset the buffered click count for a short code.
     * Used by the flush job to sync counters back to PostgreSQL.
     */
    @CircuitBreaker(name = "redisCache", fallbackMethod = "getAndResetClickCountFallback")
    public long getAndResetClickCount(String shortCode) {
        String value = redisTemplate.opsForValue().getAndDelete(CLICK_COUNTER_PREFIX + shortCode);
        return value != null ? Long.parseLong(value) : 0;
    }

    @SuppressWarnings("unused")
    private long getAndResetClickCountFallback(String shortCode, Throwable t) {
        log.warn("Circuit breaker OPEN — returning 0 clicks for shortCode={}: {}", shortCode, t.getMessage());
        return 0;
    }

    /**
     * Check if Redis is available (used by health checks and fallback logic).
     */
    public boolean isAvailable() {
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
