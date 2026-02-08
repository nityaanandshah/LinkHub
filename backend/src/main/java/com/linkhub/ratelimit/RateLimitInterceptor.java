package com.linkhub.ratelimit;

import com.linkhub.common.exception.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(RateLimitInterceptor.class);

    private final RateLimitService rateLimitService;

    public RateLimitInterceptor(RateLimitService rateLimitService) {
        this.rateLimitService = rateLimitService;
    }

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler
    ) {
        String clientIp = getClientIp(request);
        String path = request.getRequestURI();

        // Check if this is a bulk endpoint
        boolean isBulk = path.contains("/bulk");

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.getPrincipal() instanceof Long userId) {
            // Authenticated user rate limiting
            if (isBulk) {
                if (!rateLimitService.isAllowedBulk(userId)) {
                    log.warn("Bulk rate limit exceeded for userId={}, ip={}", userId, clientIp);
                    throw new RateLimitExceededException(
                            "Bulk rate limit exceeded. Maximum 10 bulk requests per minute.");
                }
            } else {
                if (!rateLimitService.isAllowedByUser(userId)) {
                    log.warn("Rate limit exceeded for userId={}, ip={}", userId, clientIp);
                    throw new RateLimitExceededException(
                            "Rate limit exceeded. Maximum 100 requests per minute.");
                }
            }
        } else {
            // Anonymous IP rate limiting
            if (!rateLimitService.isAllowedByIp(clientIp)) {
                log.warn("IP rate limit exceeded for ip={}", clientIp);
                throw new RateLimitExceededException(
                        "Rate limit exceeded. Maximum 20 requests per minute for anonymous users.");
            }
        }

        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
