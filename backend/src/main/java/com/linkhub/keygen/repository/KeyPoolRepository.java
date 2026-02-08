package com.linkhub.keygen.repository;

import com.linkhub.keygen.model.KeyPool;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KeyPoolRepository extends JpaRepository<KeyPool, Long> {

    /**
     * Count available (unused) keys in the pool.
     */
    @Query("SELECT COUNT(k) FROM KeyPool k WHERE k.isUsed = false")
    long countAvailableKeys();

    /**
     * Fetch a batch of unused keys for loading into Redis buffer.
     * Uses native query with row-level locking (FOR UPDATE SKIP LOCKED) for concurrency safety.
     */
    @Query(value = "SELECT * FROM key_pool WHERE is_used = FALSE ORDER BY id LIMIT :limit FOR UPDATE SKIP LOCKED",
            nativeQuery = true)
    List<KeyPool> fetchUnusedKeys(int limit);

    /**
     * Mark a batch of keys as used.
     */
    @Modifying
    @Query("UPDATE KeyPool k SET k.isUsed = true, k.claimedAt = CURRENT_TIMESTAMP WHERE k.id IN :ids")
    void markKeysAsUsed(List<Long> ids);

    /**
     * Check if a specific short key exists.
     */
    boolean existsByShortKey(String shortKey);
}
