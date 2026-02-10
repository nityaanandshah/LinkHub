package com.linkhub.analytics.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Read-only JPA entity for querying the click_events partitioned table.
 * Writes are handled exclusively by the analytics-consumer module.
 */
@Entity
@Table(name = "click_events")
@IdClass(ClickEventId.class)
public class ClickEvent {

    @Id
    @Column(name = "id")
    private Long id;

    @Id
    @Column(name = "clicked_at", nullable = false)
    private Instant clickedAt;

    @Column(name = "event_id", nullable = false)
    private UUID eventId;

    @Column(name = "url_id", nullable = false)
    private Long urlId;

    @Column(name = "short_code", nullable = false, length = 10)
    private String shortCode;

    @Column(name = "ip_address", columnDefinition = "INET")
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "referrer", columnDefinition = "TEXT")
    private String referrer;

    @Column(name = "device_type", length = 20)
    private String deviceType;

    @Column(name = "browser", length = 50)
    private String browser;

    @Column(name = "os", length = 50)
    private String os;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    public ClickEvent() {}

    // Getters
    public Long getId() { return id; }
    public UUID getEventId() { return eventId; }
    public Long getUrlId() { return urlId; }
    public String getShortCode() { return shortCode; }
    public Instant getClickedAt() { return clickedAt; }
    public String getIpAddress() { return ipAddress; }
    public String getUserAgent() { return userAgent; }
    public String getReferrer() { return referrer; }
    public String getDeviceType() { return deviceType; }
    public String getBrowser() { return browser; }
    public String getOs() { return os; }
    public String getCountry() { return country; }
    public String getCity() { return city; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
}
