package com.linkhub.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        int status,
        String error,
        String message,
        String path,
        Instant timestamp,
        List<FieldError> fieldErrors
) {
    public ApiError(int status, String error, String message, String path) {
        this(status, error, message, path, Instant.now(), null);
    }

    public ApiError(int status, String error, String message, String path, List<FieldError> fieldErrors) {
        this(status, error, message, path, Instant.now(), fieldErrors);
    }

    public record FieldError(String field, String message) {}
}
