package com.linkhub.analytics.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Kafka message DTO for click events.
 * Mirror of the backend's ClickEventMessage for deserialization.
 */
public record ClickEventMessage(
        UUID eventId,
        Long urlId,
        String shortCode,
        Instant clickedAt,
        String ipAddress,
        String userAgent,
        String referrer
) {}
