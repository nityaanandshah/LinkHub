package com.linkhub.analytics.dto;

import java.time.Instant;

/**
 * Summary analytics for a URL.
 */
public record ClickStats(
        String shortCode,
        long totalClicks,
        long uniqueVisitors,
        Instant from,
        Instant to
) {}
