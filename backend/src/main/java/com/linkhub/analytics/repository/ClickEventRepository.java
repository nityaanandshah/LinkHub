package com.linkhub.analytics.repository;

import com.linkhub.analytics.model.ClickEvent;
import com.linkhub.analytics.model.ClickEventId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ClickEventRepository extends JpaRepository<ClickEvent, ClickEventId> {

    /**
     * Paginated click events for a short code within a time range.
     */
    @Query("SELECT ce FROM ClickEvent ce WHERE ce.shortCode = :shortCode AND ce.clickedAt BETWEEN :from AND :to ORDER BY ce.clickedAt DESC")
    Page<ClickEvent> findByShortCodeAndTimeRange(@Param("shortCode") String shortCode,
                                                  @Param("from") Instant from,
                                                  @Param("to") Instant to,
                                                  Pageable pageable);

    /**
     * Total click count for a short code within a time range.
     */
    @Query("SELECT COUNT(ce) FROM ClickEvent ce WHERE ce.shortCode = :shortCode AND ce.clickedAt BETWEEN :from AND :to")
    long countByShortCodeAndTimeRange(@Param("shortCode") String shortCode,
                                      @Param("from") Instant from,
                                      @Param("to") Instant to);

    /**
     * Count unique IPs (unique visitors) for a short code within a time range.
     */
    @Query("SELECT COUNT(DISTINCT ce.ipAddress) FROM ClickEvent ce WHERE ce.shortCode = :shortCode AND ce.clickedAt BETWEEN :from AND :to")
    long countUniqueVisitors(@Param("shortCode") String shortCode,
                             @Param("from") Instant from,
                             @Param("to") Instant to);

    /**
     * Timeseries data: clicks per day for a short code.
     * Returns Object[] arrays of [date_string, count].
     */
    @Query(value = """
            SELECT TO_CHAR(clicked_at AT TIME ZONE 'UTC', 'YYYY-MM-DD') AS day,
                   COUNT(*) AS clicks
            FROM click_events
            WHERE short_code = :shortCode
              AND clicked_at BETWEEN :from AND :to
            GROUP BY day
            ORDER BY day
            """, nativeQuery = true)
    List<Object[]> getTimeseriesByDay(@Param("shortCode") String shortCode,
                                      @Param("from") Instant from,
                                      @Param("to") Instant to);

    /**
     * Timeseries data: clicks per hour for a short code.
     */
    @Query(value = """
            SELECT TO_CHAR(clicked_at AT TIME ZONE 'UTC', 'YYYY-MM-DD HH24:00') AS hour,
                   COUNT(*) AS clicks
            FROM click_events
            WHERE short_code = :shortCode
              AND clicked_at BETWEEN :from AND :to
            GROUP BY hour
            ORDER BY hour
            """, nativeQuery = true)
    List<Object[]> getTimeseriesByHour(@Param("shortCode") String shortCode,
                                       @Param("from") Instant from,
                                       @Param("to") Instant to);

    /**
     * Top referrers for a short code.
     * Returns Object[] arrays of [referrer, count].
     */
    @Query(value = """
            SELECT COALESCE(referrer, 'Direct') AS referrer_source,
                   COUNT(*) AS clicks
            FROM click_events
            WHERE short_code = :shortCode
              AND clicked_at BETWEEN :from AND :to
            GROUP BY referrer_source
            ORDER BY clicks DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> getTopReferrers(@Param("shortCode") String shortCode,
                                   @Param("from") Instant from,
                                   @Param("to") Instant to,
                                   @Param("limit") int limit);

    /**
     * Device type breakdown for a short code.
     * Returns Object[] arrays of [device_type, count].
     */
    @Query(value = """
            SELECT COALESCE(device_type, 'Unknown') AS device,
                   COUNT(*) AS clicks
            FROM click_events
            WHERE short_code = :shortCode
              AND clicked_at BETWEEN :from AND :to
            GROUP BY device
            ORDER BY clicks DESC
            """, nativeQuery = true)
    List<Object[]> getDeviceBreakdown(@Param("shortCode") String shortCode,
                                      @Param("from") Instant from,
                                      @Param("to") Instant to);

    /**
     * Browser breakdown for a short code.
     * Returns Object[] arrays of [browser, count].
     */
    @Query(value = """
            SELECT COALESCE(browser, 'Unknown') AS browser_name,
                   COUNT(*) AS clicks
            FROM click_events
            WHERE short_code = :shortCode
              AND clicked_at BETWEEN :from AND :to
            GROUP BY browser_name
            ORDER BY clicks DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> getBrowserBreakdown(@Param("shortCode") String shortCode,
                                       @Param("from") Instant from,
                                       @Param("to") Instant to,
                                       @Param("limit") int limit);

    /**
     * OS breakdown for a short code.
     * Returns Object[] arrays of [os, count].
     */
    @Query(value = """
            SELECT COALESCE(os, 'Unknown') AS os_name,
                   COUNT(*) AS clicks
            FROM click_events
            WHERE short_code = :shortCode
              AND clicked_at BETWEEN :from AND :to
            GROUP BY os_name
            ORDER BY clicks DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> getOsBreakdown(@Param("shortCode") String shortCode,
                                   @Param("from") Instant from,
                                   @Param("to") Instant to,
                                   @Param("limit") int limit);

    /**
     * Geographic breakdown by country for a short code.
     * Returns Object[] arrays of [country, count].
     */
    @Query(value = """
            SELECT COALESCE(country, 'Unknown') AS country_name,
                   COUNT(*) AS clicks
            FROM click_events
            WHERE short_code = :shortCode
              AND clicked_at BETWEEN :from AND :to
            GROUP BY country_name
            ORDER BY clicks DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> getCountryBreakdown(@Param("shortCode") String shortCode,
                                       @Param("from") Instant from,
                                       @Param("to") Instant to,
                                       @Param("limit") int limit);

    /**
     * Geographic breakdown by city for a short code.
     * Returns Object[] arrays of [city, country, count, avg_lat, avg_lng].
     */
    @Query(value = """
            SELECT COALESCE(city, 'Unknown') AS city_name,
                   COALESCE(country, 'Unknown') AS country_name,
                   COUNT(*) AS clicks,
                   AVG(latitude) AS avg_lat,
                   AVG(longitude) AS avg_lng
            FROM click_events
            WHERE short_code = :shortCode
              AND clicked_at BETWEEN :from AND :to
            GROUP BY city_name, country_name
            ORDER BY clicks DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> getCityBreakdown(@Param("shortCode") String shortCode,
                                    @Param("from") Instant from,
                                    @Param("to") Instant to,
                                    @Param("limit") int limit);
}
