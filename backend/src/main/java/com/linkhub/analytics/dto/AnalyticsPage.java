package com.linkhub.analytics.dto;

import java.util.List;

/**
 * Paginated analytics response wrapper.
 */
public record AnalyticsPage<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
    public static <T> AnalyticsPage<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        boolean hasNext = page < totalPages - 1;
        return new AnalyticsPage<>(content, page, size, totalElements, totalPages, hasNext);
    }
}
