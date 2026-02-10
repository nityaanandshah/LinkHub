package com.linkhub.analytics.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for the Dead Letter Queue (failed_click_events table).
 * Used by both the DLQ retry job and the ClickEventProducer.
 */
@Entity
@Table(name = "failed_click_events")
public class FailedClickEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "payload", nullable = false, columnDefinition = "JSONB")
    private String payload;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "retry_count")
    private int retryCount = 0;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    public FailedClickEvent() {}

    public FailedClickEvent(UUID eventId, String payload, String failureReason) {
        this.eventId = eventId;
        this.payload = payload;
        this.failureReason = failureReason;
        this.createdAt = Instant.now();
        this.nextRetryAt = Instant.now().plusSeconds(60);
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }

    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getNextRetryAt() { return nextRetryAt; }
    public void setNextRetryAt(Instant nextRetryAt) { this.nextRetryAt = nextRetryAt; }

    /**
     * Increment retry count and set next retry with exponential backoff.
     * Backoff: 1min, 5min, 25min, 2h, 10h
     */
    public void scheduleRetry() {
        this.retryCount++;
        long backoffSeconds = (long) (60 * Math.pow(5, this.retryCount - 1));
        this.nextRetryAt = Instant.now().plusSeconds(Math.min(backoffSeconds, 36000));
    }
}
