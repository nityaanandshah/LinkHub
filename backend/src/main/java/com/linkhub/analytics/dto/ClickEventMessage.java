package com.linkhub.analytics.dto;

import java.time.Instant;
import java.util.UUID;

public record ClickEventMessage(
        UUID eventId,
        Long urlId,
        String shortCode,
        Instant clickedAt,
        String ipAddress,
        String userAgent,
        String referrer
) {
    public static ClickEventMessage create(Long urlId, String shortCode,
                                           String ipAddress, String userAgent, String referrer) {
        return new ClickEventMessage(
                UUID.randomUUID(),
                urlId,
                shortCode,
                Instant.now(),
                ipAddress,
                userAgent,
                referrer
        );
    }
}
