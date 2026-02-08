package com.linkhub.url.dto;

import org.hibernate.validator.constraints.URL;
import java.time.Instant;

public record UpdateUrlRequest(
        @URL(message = "Must be a valid URL")
        String longUrl,
        Instant expiresAt,
        Boolean isActive
) {}
