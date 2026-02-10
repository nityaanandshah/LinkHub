package com.linkhub.analytics.controller;

import com.linkhub.analytics.dto.*;
import com.linkhub.analytics.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
@Tag(name = "Analytics", description = "URL click analytics endpoints")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/{shortCode}/summary")
    @Operation(summary = "Click summary", description = "Get total clicks and unique visitors for a URL")
    public ResponseEntity<ClickStats> getSummary(
            @PathVariable String shortCode,
            @RequestParam(required = false) Integer days,
            @Parameter(description = "Start of time range (ISO-8601)") @RequestParam(required = false) Instant from,
            @Parameter(description = "End of time range (ISO-8601)") @RequestParam(required = false) Instant to,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        ClickStats stats = analyticsService.getClickSummary(shortCode, userId, days, from, to);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/{shortCode}/clicks")
    @Operation(summary = "Paginated click events", description = "Browse individual click events with pagination")
    public ResponseEntity<AnalyticsPage<ClickEventDto>> getClicks(
            @PathVariable String shortCode,
            @RequestParam(required = false) Integer days,
            @Parameter(description = "Start of time range (ISO-8601)") @RequestParam(required = false) Instant from,
            @Parameter(description = "End of time range (ISO-8601)") @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        // Clamp page size to [1, 200]
        int clampedSize = Math.max(1, Math.min(size, 200));
        AnalyticsPage<ClickEventDto> result = analyticsService.getClickEvents(
                shortCode, userId, days, from, to, page, clampedSize);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{shortCode}/timeseries")
    @Operation(summary = "Click timeseries", description = "Get clicks over time (daily or hourly)")
    public ResponseEntity<List<TimeseriesPoint>> getTimeseries(
            @PathVariable String shortCode,
            @RequestParam(required = false) Integer days,
            @Parameter(description = "Start of time range (ISO-8601)") @RequestParam(required = false) Instant from,
            @Parameter(description = "End of time range (ISO-8601)") @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "day") String granularity,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        List<TimeseriesPoint> timeseries = analyticsService.getTimeseries(
                shortCode, userId, days, from, to, granularity);
        return ResponseEntity.ok(timeseries);
    }

    @GetMapping("/{shortCode}/referrers")
    @Operation(summary = "Top referrers", description = "Get top traffic sources for a URL")
    public ResponseEntity<List<ReferrerStats>> getReferrers(
            @PathVariable String shortCode,
            @RequestParam(required = false) Integer days,
            @Parameter(description = "Start of time range (ISO-8601)") @RequestParam(required = false) Instant from,
            @Parameter(description = "End of time range (ISO-8601)") @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "10") Integer limit,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        List<ReferrerStats> referrers = analyticsService.getTopReferrers(
                shortCode, userId, days, from, to, limit);
        return ResponseEntity.ok(referrers);
    }

    @GetMapping("/{shortCode}/devices")
    @Operation(summary = "Device breakdown", description = "Get device type, browser, and OS breakdown")
    public ResponseEntity<DeviceStats> getDevices(
            @PathVariable String shortCode,
            @RequestParam(required = false) Integer days,
            @Parameter(description = "Start of time range (ISO-8601)") @RequestParam(required = false) Instant from,
            @Parameter(description = "End of time range (ISO-8601)") @RequestParam(required = false) Instant to,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        DeviceStats stats = analyticsService.getDeviceStats(shortCode, userId, days, from, to);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/{shortCode}/geo")
    @Operation(summary = "Geographic breakdown", description = "Get country and city breakdown of clicks")
    public ResponseEntity<GeoStats> getGeo(
            @PathVariable String shortCode,
            @RequestParam(required = false) Integer days,
            @Parameter(description = "Start of time range (ISO-8601)") @RequestParam(required = false) Instant from,
            @Parameter(description = "End of time range (ISO-8601)") @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "20") Integer limit,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        GeoStats stats = analyticsService.getGeoStats(shortCode, userId, days, from, to, limit);
        return ResponseEntity.ok(stats);
    }
}
