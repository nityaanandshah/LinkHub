package com.linkhub.analytics.service;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Optional;

/**
 * Service to resolve IP addresses to geographic locations using MaxMind GeoLite2.
 * Gracefully degrades when the database file is not available.
 */
@Service
public class GeoIpService {

    private static final Logger log = LoggerFactory.getLogger(GeoIpService.class);

    @Value("${geoip.database-path:#{null}}")
    private String databasePath;

    private DatabaseReader databaseReader;
    private boolean available = false;

    @PostConstruct
    public void init() {
        if (databasePath == null || databasePath.isBlank()) {
            log.warn("GeoIP database path not configured. Geographic enrichment disabled.");
            return;
        }

        File dbFile = new File(databasePath);
        if (!dbFile.exists()) {
            log.warn("GeoIP database file not found at: {}. Geographic enrichment disabled.", databasePath);
            return;
        }

        try {
            databaseReader = new DatabaseReader.Builder(dbFile).build();
            available = true;
            log.info("GeoIP database loaded from: {}", databasePath);
        } catch (IOException e) {
            log.error("Failed to load GeoIP database: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void destroy() {
        if (databaseReader != null) {
            try {
                databaseReader.close();
            } catch (IOException e) {
                log.warn("Error closing GeoIP database: {}", e.getMessage());
            }
        }
    }

    /**
     * Resolve an IP address to a GeoLocation.
     * Returns empty if the database is not loaded or the IP cannot be resolved.
     */
    public Optional<GeoLocation> resolve(String ipAddress) {
        if (!available || ipAddress == null || ipAddress.isBlank()) {
            return Optional.empty();
        }

        try {
            // Skip private/loopback addresses
            InetAddress inet = InetAddress.getByName(ipAddress);
            if (inet.isLoopbackAddress() || inet.isSiteLocalAddress() || inet.isLinkLocalAddress()) {
                return Optional.empty();
            }

            CityResponse response = databaseReader.city(inet);

            String country = response.getCountry() != null ? response.getCountry().getName() : null;
            String city = response.getCity() != null ? response.getCity().getName() : null;
            Double latitude = response.getLocation() != null ? response.getLocation().getLatitude() : null;
            Double longitude = response.getLocation() != null ? response.getLocation().getLongitude() : null;

            return Optional.of(new GeoLocation(country, city, latitude, longitude));
        } catch (GeoIp2Exception e) {
            log.debug("GeoIP lookup failed for IP {}: {}", ipAddress, e.getMessage());
            return Optional.empty();
        } catch (IOException e) {
            log.warn("GeoIP I/O error for IP {}: {}", ipAddress, e.getMessage());
            return Optional.empty();
        }
    }

    public boolean isAvailable() {
        return available;
    }

    /**
     * GeoLocation result record.
     */
    public record GeoLocation(String country, String city, Double latitude, Double longitude) {}
}
