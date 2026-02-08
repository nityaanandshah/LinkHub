package com.linkhub.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Rate limiting service using Redis with a Lua script for atomic
 * sliding window counter implementation.
 *
 * <p>Key pattern: {@code rate:{type}:{identifier}} where type is "ip" or "user".
 *
 * <p>When Redis is unavailable, degrades to permissive mode (allow all)
 * to avoid blocking traffic.
 */
@Service
public class RateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);

    /**
     * Lua script for atomic rate limiting.
     * Increments a counter, sets TTL on first call, returns 0 if over limit.
     */
    private static final String RATE_LIMIT_SCRIPT =
            "local key = KEYS[1]\n" +
            "local limit = tonumber(ARGV[1])\n" +
            "local window = tonumber(ARGV[2])\n" +
            "local current = redis.call('INCR', key)\n" +
            "if current == 1 then\n" +
            "    redis.call('EXPIRE', key, window)\n" +
            "end\n" +
            "if current > limit then\n" +
            "    return 0\n" +
            "end\n" +
            "return 1";

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> rateLimitRedisScript;

    public RateLimitService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.rateLimitRedisScript = new DefaultRedisScript<>();
        this.rateLimitRedisScript.setScriptText(RATE_LIMIT_SCRIPT);
        this.rateLimitRedisScript.setResultType(Long.class);
    }

    /**
     * Check if a request is allowed under the rate limit.
     *
     * @param key    the rate limit key (e.g., "rate:ip:192.168.1.1")
     * @param limit  maximum number of requests allowed
     * @param windowSeconds  time window in seconds
     * @return true if the request is allowed, false if rate limited
     */
    public boolean isAllowed(String key, int limit, int windowSeconds) {
        try {
            Long result = redisTemplate.execute(
                    rateLimitRedisScript,
                    Collections.singletonList(key),
                    String.valueOf(limit),
                    String.valueOf(windowSeconds)
            );
            return result != null && result == 1L;
        } catch (Exception e) {
            log.warn("Redis unavailable for rate limiting, degrading to permissive mode: {}", e.getMessage());
            // Graceful degradation: allow request when Redis is down
            return true;
        }
    }

    /**
     * Rate limit by IP address.
     * Anonymous: 20 requests/minute per IP.
     */
    public boolean isAllowedByIp(String ipAddress) {
        return isAllowed("rate:ip:" + ipAddress, 20, 60);
    }

    /**
     * Rate limit by authenticated user.
     * Authenticated: 100 requests/minute per user.
     */
    public boolean isAllowedByUser(Long userId) {
        return isAllowed("rate:user:" + userId, 100, 60);
    }

    /**
     * Rate limit for bulk endpoint.
     * 10 requests/minute per user.
     */
    public boolean isAllowedBulk(Long userId) {
        return isAllowed("rate:bulk:" + userId, 10, 60);
    }
}
