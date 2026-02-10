package com.linkhub.analytics.dto;

import java.util.List;

/**
 * Geographic analytics breakdown.
 */
public record GeoStats(
        List<CountryData> countries,
        List<CityData> cities
) {
    public record CountryData(String country, long clicks, double percentage) {}

    public record CityData(String city, String country, long clicks, Double latitude, Double longitude) {}
}
