package com.linkhub.analytics.dto;

/**
 * A single data point in a timeseries (clicks per time bucket).
 */
public record TimeseriesPoint(
        String timestamp,
        long clicks
) {}
