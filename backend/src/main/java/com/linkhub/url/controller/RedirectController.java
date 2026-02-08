package com.linkhub.url.controller;

import com.linkhub.analytics.dto.ClickEventMessage;
import com.linkhub.analytics.producer.ClickEventProducer;
import com.linkhub.url.cache.UrlCacheService;
import com.linkhub.url.model.Url;
import com.linkhub.url.service.UrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Optional;

@RestController
@Tag(name = "Redirect", description = "URL redirect endpoint")
public class RedirectController {

    private static final Logger log = LoggerFactory.getLogger(RedirectController.class);

    private final UrlService urlService;
    private final ClickEventProducer clickEventProducer;
    private final UrlCacheService cacheService;

    public RedirectController(UrlService urlService,
                              ClickEventProducer clickEventProducer,
                              UrlCacheService cacheService) {
        this.urlService = urlService;
        this.clickEventProducer = clickEventProducer;
        this.cacheService = cacheService;
    }

    @GetMapping("/{shortCode:[a-zA-Z0-9\\-_]{1,10}}")
    @Operation(summary = "Redirect to long URL",
            description = "Redirects the client to the original long URL. Uses Redis cache-aside for sub-50ms response.")
    public ResponseEntity<Void> redirect(
            @PathVariable String shortCode,
            HttpServletRequest request
    ) {
        Optional<Url> urlOpt = urlService.resolveForRedirect(shortCode);

        if (urlOpt.isEmpty()) {
            log.debug("Short code not found or expired: {}", shortCode);
            return ResponseEntity.notFound().build();
        }

        Url url = urlOpt.get();

        // Increment click count in Redis (buffered, non-blocking)
        cacheService.incrementClickCount(shortCode);

        // Fire click event to Kafka (async, non-blocking)
        ClickEventMessage event = ClickEventMessage.create(
                url.getId(),
                shortCode,
                getClientIp(request),
                request.getHeader(HttpHeaders.USER_AGENT),
                request.getHeader(HttpHeaders.REFERER)
        );
        clickEventProducer.publishClickEvent(event);

        // 302 Redirect
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(url.getLongUrl()))
                .build();
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
