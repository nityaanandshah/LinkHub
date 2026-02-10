package com.linkhub.analytics.repository;

import com.linkhub.analytics.model.FailedClickEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface FailedClickEventRepository extends JpaRepository<FailedClickEvent, Long> {

    /**
     * Find events eligible for retry: retry_count < max and next_retry_at has passed.
     */
    @Query("SELECT f FROM FailedClickEvent f WHERE f.retryCount < :maxRetries AND f.nextRetryAt <= :now ORDER BY f.nextRetryAt ASC")
    List<FailedClickEvent> findRetryableEvents(@Param("maxRetries") int maxRetries, @Param("now") Instant now);
}
