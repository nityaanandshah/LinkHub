package com.linkhub.analytics.dto;

import com.linkhub.analytics.model.ClickEvent;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for individual click event data returned via paginated endpoint.
 */
public record ClickEventDto(
        UUID eventId,
        String shortCode,
        Instant clickedAt,
        String ipAddress,
        String referrer,
        String deviceType,
        String browser,
        String os,
        String country,
        String city
) {
    public static ClickEventDto from(ClickEvent ce) {
        return new ClickEventDto(
                ce.getEventId(),
                ce.getShortCode(),
                ce.getClickedAt(),
                ce.getIpAddress(),
                ce.getReferrer(),
                ce.getDeviceType(),
                ce.getBrowser(),
                ce.getOs(),
                ce.getCountry(),
                ce.getCity()
        );
    }
}
