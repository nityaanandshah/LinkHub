package com.linkhub.url.scheduler;

import com.linkhub.url.cache.UrlCacheService;
import com.linkhub.url.repository.UrlRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Scheduled job that flushes buffered click counts from Redis to PostgreSQL.
 *
 * <p>Redis keys: {@code clicks:{shortCode}} contain atomically incremented counters.
 * This job reads and resets each counter, then applies the delta to the
 * {@code urls.click_count} column — avoiding per-click DB writes.
 *
 * <p>Runs every 60 seconds.
 */
@Component
public class ClickCountFlushJob {

    private static final Logger log = LoggerFactory.getLogger(ClickCountFlushJob.class);
    private static final String CLICK_KEY_PATTERN = "clicks:*";

    private final StringRedisTemplate redisTemplate;
    private final UrlCacheService cacheService;
    private final UrlRepository urlRepository;

    public ClickCountFlushJob(StringRedisTemplate redisTemplate,
                              UrlCacheService cacheService,
                              UrlRepository urlRepository) {
        this.redisTemplate = redisTemplate;
        this.cacheService = cacheService;
        this.urlRepository = urlRepository;
    }

    @Scheduled(fixedRate = 60_000) // every 60 seconds
    @Transactional
    public void flushClickCounts() {
        try {
            Set<String> keys = redisTemplate.keys(CLICK_KEY_PATTERN);
            if (keys == null || keys.isEmpty()) {
                return;
            }

            int flushed = 0;
            for (String key : keys) {
                // Extract shortCode from key pattern "clicks:{shortCode}"
                String shortCode = key.substring("clicks:".length());
                long delta = cacheService.getAndResetClickCount(shortCode);

                if (delta > 0) {
                    urlRepository.incrementClickCount(shortCode, delta);
                    flushed++;
                    log.debug("Flushed {} clicks for shortCode={}", delta, shortCode);
                }
            }

            if (flushed > 0) {
                log.info("Click count flush completed: {} URLs updated", flushed);
            }
        } catch (Exception e) {
            log.error("Click count flush job failed: {}", e.getMessage(), e);
        }
    }
}
