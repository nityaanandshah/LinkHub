package com.linkhub.url.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

import java.time.Instant;

public record CreateUrlRequest(

        @NotBlank(message = "Long URL is required")
        @URL(message = "Must be a valid URL")
        @Size(max = 2048, message = "URL must not exceed 2048 characters")
        String longUrl,

        @Pattern(regexp = "^[a-zA-Z0-9\\-_]{4,10}$",
                message = "Custom alias must be 4–10 alphanumeric characters (hyphens and underscores allowed)")
        String customAlias,

        Instant expiresAt
) {}
