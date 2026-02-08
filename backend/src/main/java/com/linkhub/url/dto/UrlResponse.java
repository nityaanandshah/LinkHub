package com.linkhub.url.dto;

import com.linkhub.url.model.Url;

import java.time.Instant;

public record UrlResponse(
        Long id,
        String shortCode,
        String shortUrl,
        String longUrl,
        boolean isCustomAlias,
        boolean isActive,
        long clickCount,
        Instant expiresAt,
        Instant createdAt,
        Instant updatedAt,
        String qrUrl
) {
    public static UrlResponse from(Url url, String baseUrl) {
        return new UrlResponse(
                url.getId(),
                url.getShortCode(),
                baseUrl + "/" + url.getShortCode(),
                url.getLongUrl(),
                url.isCustomAlias(),
                url.isActive(),
                url.getClickCount(),
                url.getExpiresAt(),
                url.getCreatedAt(),
                url.getUpdatedAt(),
                "/api/v1/urls/" + url.getShortCode() + "/qr"
        );
    }
}
