package com.linkhub.url.repository;

import com.linkhub.url.model.Url;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface UrlRepository extends JpaRepository<Url, Long> {

    Optional<Url> findByShortCode(String shortCode);

    Page<Url> findByUserIdAndIsActiveTrue(Long userId, Pageable pageable);

    Page<Url> findByUserId(Long userId, Pageable pageable);

    boolean existsByShortCode(String shortCode);

    /**
     * Atomically increment the click count for a URL by a delta amount.
     * Used by the click count flush job.
     */
    @Modifying
    @Query("UPDATE Url u SET u.clickCount = u.clickCount + :delta, u.updatedAt = CURRENT_TIMESTAMP WHERE u.shortCode = :shortCode")
    void incrementClickCount(@Param("shortCode") String shortCode, @Param("delta") long delta);

    /**
     * Find all active URLs that have expired.
     * Used by a cleanup job to deactivate expired URLs.
     */
    @Query("SELECT u FROM Url u WHERE u.isActive = true AND u.expiresAt IS NOT NULL AND u.expiresAt < :now")
    List<Url> findExpiredUrls(@Param("now") Instant now);
}
