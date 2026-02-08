package com.linkhub.url.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkhub.url.model.Url;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis cache service for URL operations.
 *
 * <p>Key patterns:
 * <ul>
 *   <li>{@code url:{shortCode}} → longUrl (redirect hot path, TTL 1h default / 24h hot)</li>
 *   <li>{@code url:meta:{shortCode}} → JSON (full URL metadata, TTL 1h)</li>
 * </ul>
 *
 * <p>All Redis calls are wrapped in try-catch for graceful degradation.
 * If Redis is down, the service degrades transparently to DB-only mode.
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
    public void cacheOnCreate(Url url) {
        try {
            // Redirect cache: url:{shortCode} → longUrl
            redisTemplate.opsForValue().set(
                    REDIRECT_KEY_PREFIX + url.getShortCode(),
                    url.getLongUrl(),
                    DEFAULT_TTL
            );

            // Metadata cache: url:meta:{shortCode} → JSON
            String json = objectMapper.writeValueAsString(url);
            redisTemplate.opsForValue().set(
                    META_KEY_PREFIX + url.getShortCode(),
                    json,
                    DEFAULT_TTL
            );

            log.debug("Cache populated for shortCode={}", url.getShortCode());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize URL for cache: {}", e.getMessage());
        } catch (Exception e) {
            log.warn("Redis unavailable during cache write for shortCode={}: {}",
                    url.getShortCode(), e.getMessage());
            // Graceful degradation: URL is already in DB, so this is non-critical
        }
    }

    // ────────── Cache-Aside (on redirect) ──────────

    /**
     * Look up the long URL from Redis cache.
     * Returns empty if cache miss or Redis is down.
     */
    public Optional<String> getRedirectUrl(String shortCode) {
        try {
            String longUrl = redisTemplate.opsForValue().get(REDIRECT_KEY_PREFIX + shortCode);
            return Optional.ofNullable(longUrl);
        } catch (Exception e) {
            log.warn("Redis unavailable during redirect lookup for shortCode={}: {}",
                    shortCode, e.getMessage());
            return Optional.empty(); // Fallback to DB
        }
    }

    /**
     * Populate redirect cache after a DB lookup (cache-aside fill).
     */
    public void cacheRedirectUrl(String shortCode, String longUrl) {
        try {
            redisTemplate.opsForValue().set(
                    REDIRECT_KEY_PREFIX + shortCode,
                    longUrl,
                    DEFAULT_TTL
            );
        } catch (Exception e) {
            log.warn("Redis unavailable during cache fill for shortCode={}: {}",
                    shortCode, e.getMessage());
        }
    }

    // ────────── Eager Invalidation (on update/delete) ──────────

    /**
     * Invalidate all cached data for a short code.
     * Called on URL update, delete, or expiry.
     */
    public void invalidate(String shortCode) {
        try {
            redisTemplate.delete(REDIRECT_KEY_PREFIX + shortCode);
            redisTemplate.delete(META_KEY_PREFIX + shortCode);
            redisTemplate.delete(CLICK_COUNTER_PREFIX + shortCode);
            log.debug("Cache invalidated for shortCode={}", shortCode);
        } catch (Exception e) {
            log.warn("Redis unavailable during cache invalidation for shortCode={}: {}",
                    shortCode, e.getMessage());
        }
    }

    // ────────── Click Counter Buffering ──────────

    /**
     * Atomically increment the click counter in Redis.
     * A scheduled job will flush these to PostgreSQL periodically.
     */
    public void incrementClickCount(String shortCode) {
        try {
            Long count = redisTemplate.opsForValue().increment(CLICK_COUNTER_PREFIX + shortCode);
            if (count != null && count == 1) {
                // First click — set TTL so counters don't leak
                redisTemplate.expire(CLICK_COUNTER_PREFIX + shortCode, Duration.ofHours(2));
            }

            // Hot URL promotion: extend redirect cache TTL
            if (count != null && count >= HOT_THRESHOLD) {
                redisTemplate.expire(REDIRECT_KEY_PREFIX + shortCode, HOT_TTL);
                log.debug("Hot URL promotion for shortCode={}, clicks={}", shortCode, count);
            }
        } catch (Exception e) {
            log.warn("Redis unavailable during click increment for shortCode={}: {}",
                    shortCode, e.getMessage());
        }
    }

    /**
     * Read and reset the buffered click count for a short code.
     * Used by the flush job to sync counters back to PostgreSQL.
     */
    public long getAndResetClickCount(String shortCode) {
        try {
            String value = redisTemplate.opsForValue().getAndDelete(CLICK_COUNTER_PREFIX + shortCode);
            return value != null ? Long.parseLong(value) : 0;
        } catch (Exception e) {
            log.warn("Redis unavailable during click count reset for shortCode={}: {}",
                    shortCode, e.getMessage());
            return 0;
        }
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
