package com.linkhub.url.dto;

import java.time.Instant;

public record CreateUrlResponse(
        String shortCode,
        String shortUrl,
        String longUrl,
        boolean isCustomAlias,
        Instant createdAt,
        Instant expiresAt,
        String qrUrl
) {}
