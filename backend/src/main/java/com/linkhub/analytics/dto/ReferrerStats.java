package com.linkhub.analytics.dto;

/**
 * Click count grouped by referrer source.
 */
public record ReferrerStats(
        String referrer,
        long clicks,
        double percentage
) {}
