package com.linkhub.analytics.dto;

import java.util.List;

/**
 * Device analytics breakdown (device type, browser, OS).
 */
public record DeviceStats(
        List<Breakdown> deviceTypes,
        List<Breakdown> browsers,
        List<Breakdown> operatingSystems
) {
    public record Breakdown(String name, long clicks, double percentage) {}
}
