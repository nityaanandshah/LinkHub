package com.linkhub.analytics.service;

import com.linkhub.analytics.dto.*;
import com.linkhub.analytics.model.ClickEvent;
import com.linkhub.analytics.repository.ClickEventRepository;
import com.linkhub.common.exception.ResourceNotFoundException;
import com.linkhub.url.model.Url;
import com.linkhub.url.repository.UrlRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsService.class);

    private final ClickEventRepository clickEventRepository;
    private final UrlRepository urlRepository;

    public AnalyticsService(ClickEventRepository clickEventRepository,
                            UrlRepository urlRepository) {
        this.clickEventRepository = clickEventRepository;
        this.urlRepository = urlRepository;
    }

    // ────────── Click Summary ──────────

    public ClickStats getClickSummary(String shortCode, Long userId, Integer days, Instant from, Instant to) {
        validateOwnership(shortCode, userId);
        TimeRange range = resolveTimeRange(days, from, to);

        long totalClicks = clickEventRepository.countByShortCodeAndTimeRange(shortCode, range.from, range.to);
        long uniqueVisitors = clickEventRepository.countUniqueVisitors(shortCode, range.from, range.to);

        return new ClickStats(shortCode, totalClicks, uniqueVisitors, range.from, range.to);
    }

    // ────────── Paginated Clicks ──────────

    public AnalyticsPage<ClickEventDto> getClickEvents(String shortCode, Long userId,
                                                        Integer days, Instant from, Instant to,
                                                        int page, int size) {
        validateOwnership(shortCode, userId);
        TimeRange range = resolveTimeRange(days, from, to);

        Page<ClickEvent> pageResult = clickEventRepository.findByShortCodeAndTimeRange(
                shortCode, range.from, range.to, PageRequest.of(page, size));

        List<ClickEventDto> content = pageResult.getContent().stream()
                .map(ClickEventDto::from)
                .toList();

        return AnalyticsPage.of(content, page, size, pageResult.getTotalElements());
    }

    // ────────── Timeseries ──────────

    public List<TimeseriesPoint> getTimeseries(String shortCode, Long userId,
                                                Integer days, Instant from, Instant to,
                                                String granularity) {
        validateOwnership(shortCode, userId);
        TimeRange range = resolveTimeRange(days, from, to);

        List<Object[]> results;
        if ("hour".equalsIgnoreCase(granularity)) {
            results = clickEventRepository.getTimeseriesByHour(shortCode, range.from, range.to);
        } else {
            results = clickEventRepository.getTimeseriesByDay(shortCode, range.from, range.to);
        }

        return results.stream()
                .map(row -> new TimeseriesPoint(
                        (String) row[0],
                        ((Number) row[1]).longValue()
                ))
                .toList();
    }

    // ────────── Referrers ──────────

    public List<ReferrerStats> getTopReferrers(String shortCode, Long userId,
                                               Integer days, Instant from, Instant to,
                                               Integer limit) {
        validateOwnership(shortCode, userId);
        TimeRange range = resolveTimeRange(days, from, to);
        int resultLimit = limit != null ? limit : 10;

        List<Object[]> results = clickEventRepository.getTopReferrers(shortCode, range.from, range.to, resultLimit);
        long totalClicks = results.stream().mapToLong(r -> ((Number) r[1]).longValue()).sum();

        return results.stream()
                .map(row -> new ReferrerStats(
                        (String) row[0],
                        ((Number) row[1]).longValue(),
                        totalClicks > 0 ? Math.round(((Number) row[1]).doubleValue() / totalClicks * 10000.0) / 100.0 : 0
                ))
                .toList();
    }

    // ────────── Device Stats ──────────

    public DeviceStats getDeviceStats(String shortCode, Long userId, Integer days, Instant from, Instant to) {
        validateOwnership(shortCode, userId);
        TimeRange range = resolveTimeRange(days, from, to);

        List<Object[]> deviceTypes = clickEventRepository.getDeviceBreakdown(shortCode, range.from, range.to);
        List<Object[]> browsers = clickEventRepository.getBrowserBreakdown(shortCode, range.from, range.to, 10);
        List<Object[]> operatingSystems = clickEventRepository.getOsBreakdown(shortCode, range.from, range.to, 10);

        return new DeviceStats(
                toBreakdown(deviceTypes),
                toBreakdown(browsers),
                toBreakdown(operatingSystems)
        );
    }

    // ────────── Geographic Stats ──────────

    public GeoStats getGeoStats(String shortCode, Long userId,
                                 Integer days, Instant from, Instant to,
                                 Integer limit) {
        validateOwnership(shortCode, userId);
        TimeRange range = resolveTimeRange(days, from, to);
        int resultLimit = limit != null ? limit : 20;

        List<Object[]> countryResults = clickEventRepository.getCountryBreakdown(shortCode, range.from, range.to, resultLimit);
        List<Object[]> cityResults = clickEventRepository.getCityBreakdown(shortCode, range.from, range.to, resultLimit);

        long totalByCountry = countryResults.stream().mapToLong(r -> ((Number) r[1]).longValue()).sum();

        List<GeoStats.CountryData> countries = countryResults.stream()
                .map(row -> new GeoStats.CountryData(
                        (String) row[0],
                        ((Number) row[1]).longValue(),
                        totalByCountry > 0 ? Math.round(((Number) row[1]).doubleValue() / totalByCountry * 10000.0) / 100.0 : 0
                ))
                .toList();

        List<GeoStats.CityData> cities = cityResults.stream()
                .map(row -> new GeoStats.CityData(
                        (String) row[0],
                        (String) row[1],
                        ((Number) row[2]).longValue(),
                        row[3] != null ? ((Number) row[3]).doubleValue() : null,
                        row[4] != null ? ((Number) row[4]).doubleValue() : null
                ))
                .toList();

        return new GeoStats(countries, cities);
    }

    // ────────── Helpers ──────────

    private Url validateOwnership(String shortCode, Long userId) {
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("URL", "shortCode", shortCode));

        if (!url.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("URL", "shortCode", shortCode);
        }

        return url;
    }

    /**
     * Resolve the time range from explicit from/to or days parameter.
     * Priority: explicit from/to > days > default 30 days.
     */
    private TimeRange resolveTimeRange(Integer days, Instant from, Instant to) {
        if (from != null && to != null) {
            return new TimeRange(from, to);
        }
        Instant resolvedTo = to != null ? to : Instant.now();
        Instant resolvedFrom = from != null ? from : resolvedTo.minus(days != null ? days : 30, ChronoUnit.DAYS);
        return new TimeRange(resolvedFrom, resolvedTo);
    }

    private List<DeviceStats.Breakdown> toBreakdown(List<Object[]> results) {
        long total = results.stream().mapToLong(r -> ((Number) r[1]).longValue()).sum();

        return results.stream()
                .map(row -> new DeviceStats.Breakdown(
                        (String) row[0],
                        ((Number) row[1]).longValue(),
                        total > 0 ? Math.round(((Number) row[1]).doubleValue() / total * 10000.0) / 100.0 : 0
                ))
                .toList();
    }

    private record TimeRange(Instant from, Instant to) {}
}
