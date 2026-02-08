package com.linkhub.keygen.service;

import com.linkhub.common.util.Base62;
import com.linkhub.keygen.model.KeyPool;
import com.linkhub.keygen.repository.KeyPoolRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class KeyGenService {

    private static final Logger log = LoggerFactory.getLogger(KeyGenService.class);
    private static final String REDIS_KEY_POOL = "keypool:batch";

    private final KeyPoolRepository keyPoolRepository;
    private final StringRedisTemplate redisTemplate;

    @Value("${keygen.key-length:7}")
    private int keyLength;

    @Value("${keygen.batch-size:100000}")
    private int batchSize;

    @Value("${keygen.redis-buffer-size:1000}")
    private int redisBufferSize;

    @Value("${keygen.redis-refill-threshold:200}")
    private int redisRefillThreshold;

    public KeyGenService(KeyPoolRepository keyPoolRepository, StringRedisTemplate redisTemplate) {
        this.keyPoolRepository = keyPoolRepository;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Allocate a short key from the Redis buffer (RPOP).
     * Falls back to database if Redis is empty.
     */
    public String allocateKey() {
        // Try Redis first (O(1) operation)
        String key = redisTemplate.opsForList().rightPop(REDIS_KEY_POOL);

        if (key != null) {
            log.debug("Key allocated from Redis buffer: {}", key);
            checkAndTriggerRefill();
            return key;
        }

        // Redis buffer empty — refill from DB, then retry
        log.warn("Redis key buffer empty, refilling from database...");
        refillRedisBuffer();
        key = redisTemplate.opsForList().rightPop(REDIS_KEY_POOL);

        if (key != null) {
            return key;
        }

        // Last resort: generate a key directly
        log.warn("Key pool exhausted, generating key directly");
        return generateDirectKey();
    }

    /**
     * Generate a batch of unique Base62 keys and store in the key_pool table.
     */
    @Transactional
    public int generateKeyBatch(int count) {
        Set<String> generatedKeys = new HashSet<>();

        while (generatedKeys.size() < count) {
            String key = Base62.generateRandomKey(keyLength);
            if (!keyPoolRepository.existsByShortKey(key)) {
                generatedKeys.add(key);
            }
        }

        List<KeyPool> entities = new ArrayList<>(count);
        for (String key : generatedKeys) {
            entities.add(new KeyPool(key));
        }

        keyPoolRepository.saveAll(entities);
        log.info("Generated {} keys into key_pool table", count);
        return count;
    }

    /**
     * Load unused keys from the database into the Redis buffer.
     */
    @Transactional
    public void refillRedisBuffer() {
        List<KeyPool> keys = keyPoolRepository.fetchUnusedKeys(redisBufferSize);

        if (keys.isEmpty()) {
            log.warn("No unused keys available in database for Redis refill");
            return;
        }

        List<Long> ids = new ArrayList<>();
        List<String> shortKeys = new ArrayList<>();

        for (KeyPool kp : keys) {
            ids.add(kp.getId());
            shortKeys.add(kp.getShortKey());
        }

        // Mark as used in DB
        keyPoolRepository.markKeysAsUsed(ids);

        // Push to Redis list
        redisTemplate.opsForList().leftPushAll(REDIS_KEY_POOL, shortKeys);
        log.info("Refilled Redis buffer with {} keys", shortKeys.size());
    }

    /**
     * Get the current size of the Redis key buffer.
     */
    public long getRedisBufferSize() {
        Long size = redisTemplate.opsForList().size(REDIS_KEY_POOL);
        return size != null ? size : 0;
    }

    /**
     * Get the count of available (unused) keys in the database.
     */
    public long getAvailableKeyCount() {
        return keyPoolRepository.countAvailableKeys();
    }

    private void checkAndTriggerRefill() {
        long currentSize = getRedisBufferSize();
        if (currentSize < redisRefillThreshold) {
            log.info("Redis buffer below threshold ({}/{}), triggering refill", currentSize, redisRefillThreshold);
            refillRedisBuffer();
        }
    }

    private String generateDirectKey() {
        String key;
        int attempts = 0;
        do {
            key = Base62.generateRandomKey(keyLength);
            attempts++;
        } while (keyPoolRepository.existsByShortKey(key) && attempts < 100);

        if (attempts >= 100) {
            throw new IllegalStateException("Failed to generate a unique key after 100 attempts");
        }

        // Save directly to key_pool as used
        KeyPool kp = new KeyPool(key);
        kp.setUsed(true);
        kp.setClaimedAt(java.time.Instant.now());
        keyPoolRepository.save(kp);

        return key;
    }
}
