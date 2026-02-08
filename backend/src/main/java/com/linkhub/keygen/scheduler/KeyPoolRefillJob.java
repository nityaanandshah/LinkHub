package com.linkhub.keygen.scheduler;

import com.linkhub.keygen.service.KeyGenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class KeyPoolRefillJob {

    private static final Logger log = LoggerFactory.getLogger(KeyPoolRefillJob.class);

    private final KeyGenService keyGenService;

    @Value("${keygen.pool-size-watermark:100000}")
    private long poolSizeWatermark;

    @Value("${keygen.batch-size:100000}")
    private int batchSize;

    public KeyPoolRefillJob(KeyGenService keyGenService) {
        this.keyGenService = keyGenService;
    }

    /**
     * Check every 5 minutes if the key pool needs replenishing.
     * If available keys drop below the watermark, generate a new batch.
     */
    @Scheduled(fixedRate = 300_000, initialDelay = 10_000) // 5 minutes, 10s initial delay
    public void checkAndRefillKeyPool() {
        long availableKeys = keyGenService.getAvailableKeyCount();
        log.info("Key pool check — available keys: {}, watermark: {}", availableKeys, poolSizeWatermark);

        if (availableKeys < poolSizeWatermark) {
            log.info("Key pool below watermark, generating {} new keys...", batchSize);
            try {
                int generated = keyGenService.generateKeyBatch(batchSize);
                log.info("Successfully generated {} keys", generated);
            } catch (Exception e) {
                log.error("Failed to generate key batch: {}", e.getMessage(), e);
            }
        }

        // Also ensure Redis buffer is topped up
        long redisSize = keyGenService.getRedisBufferSize();
        log.info("Redis key buffer size: {}", redisSize);
        if (redisSize < 200) {
            log.info("Redis buffer low, triggering refill...");
            try {
                keyGenService.refillRedisBuffer();
            } catch (Exception e) {
                log.error("Failed to refill Redis buffer: {}", e.getMessage(), e);
            }
        }
    }
}
