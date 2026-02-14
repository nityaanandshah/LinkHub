package com.linkhub.url.service;

import com.linkhub.auth.model.User;
import com.linkhub.auth.repository.UserRepository;
import com.linkhub.common.exception.ResourceNotFoundException;
import com.linkhub.keygen.service.KeyGenService;
import com.linkhub.url.cache.UrlCacheService;
import com.linkhub.url.dto.*;
import com.linkhub.url.model.Url;
import com.linkhub.url.repository.UrlRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UrlService {

    private static final Logger log = LoggerFactory.getLogger(UrlService.class);

    private final UrlRepository urlRepository;
    private final UserRepository userRepository;
    private final KeyGenService keyGenService;
    private final UrlCacheService cacheService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public UrlService(UrlRepository urlRepository,
                      UserRepository userRepository,
                      KeyGenService keyGenService,
                      UrlCacheService cacheService) {
        this.urlRepository = urlRepository;
        this.userRepository = userRepository;
        this.keyGenService = keyGenService;
        this.cacheService = cacheService;
    }

    // ────────── CREATE ──────────

    @Transactional
    public CreateUrlResponse createUrl(CreateUrlRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        String shortCode;
        boolean isCustomAlias = false;

        if (request.customAlias() != null && !request.customAlias().isBlank()) {
            // Custom alias — validate uniqueness
            if (urlRepository.existsByShortCode(request.customAlias())) {
                throw new IllegalArgumentException("Custom alias '" + request.customAlias() + "' is already taken");
            }
            shortCode = request.customAlias();
            isCustomAlias = true;
        } else {
            // Allocate from key pool
            shortCode = keyGenService.allocateKey();
        }

        // Validate expiry is in the future
        if (request.expiresAt() != null && request.expiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Expiry date must be in the future");
        }

        Url url = new Url();
        url.setShortCode(shortCode);
        url.setLongUrl(request.longUrl());
        url.setUser(user);
        url.setCustomAlias(isCustomAlias);
        url.setExpiresAt(request.expiresAt());

        url = urlRepository.save(url);

        // Write-through cache
        cacheService.cacheOnCreate(url);

        log.info("URL created: shortCode={}, longUrl={}, userId={}", shortCode, request.longUrl(), userId);

        return new CreateUrlResponse(
                url.getShortCode(),
                baseUrl + "/" + url.getShortCode(),
                url.getLongUrl(),
                url.isCustomAlias(),
                url.getCreatedAt(),
                url.getExpiresAt(),
                "/api/v1/urls/" + url.getShortCode() + "/qr"
        );
    }

    // ────────── BULK CREATE ──────────

    @Transactional
    public List<CreateUrlResponse> bulkCreate(BulkCreateRequest request, Long userId) {
        List<CreateUrlResponse> responses = new ArrayList<>();
        for (CreateUrlRequest urlReq : request.urls()) {
            responses.add(createUrl(urlReq, userId));
        }
        return responses;
    }

    // ────────── READ ──────────

    public Page<UrlResponse> listUserUrls(Long userId, int page, int size) {
        Page<Url> urls = urlRepository.findByUserId(
                userId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return urls.map(url -> UrlResponse.from(url, baseUrl));
    }

    public UrlResponse getUrlByShortCode(String shortCode, Long userId) {
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("URL", "shortCode", shortCode));

        // Verify ownership
        if (!url.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("URL", "shortCode", shortCode);
        }

        return UrlResponse.from(url, baseUrl);
    }

    // ────────── REDIRECT (Cache-Aside) ──────────

    /**
     * Resolve a short code to a long URL for redirect.
     * Uses cache-aside pattern: check Redis first, fall back to DB, then populate cache.
     *
     * @return the Url entity (needed for click event), or empty if not found/expired/inactive
     */
    public Optional<Url> resolveForRedirect(String shortCode) {
        // 1. Try Redis cache
        Optional<String> cachedUrl = cacheService.getRedirectUrl(shortCode);
        if (cachedUrl.isPresent()) {
            // Cache hit — still need the Url entity for click event metadata
            // But we can optimize by returning a minimal object
            Url url = urlRepository.findByShortCode(shortCode).orElse(null);
            if (url != null && isUrlAccessible(url)) {
                return Optional.of(url);
            }
            // Cache had stale data — invalidate
            cacheService.invalidate(shortCode);
            return Optional.empty();
        }

        // 2. Cache miss — hit DB
        Optional<Url> urlOpt = urlRepository.findByShortCode(shortCode);
        if (urlOpt.isEmpty()) {
            return Optional.empty();
        }

        Url url = urlOpt.get();
        if (!isUrlAccessible(url)) {
            return Optional.empty();
        }

        // 3. Populate cache (cache-aside fill)
        cacheService.cacheRedirectUrl(shortCode, url.getLongUrl());

        return Optional.of(url);
    }

    // ────────── UPDATE ──────────

    @Transactional
    public UrlResponse updateUrl(String shortCode, UpdateUrlRequest request, Long userId) {
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("URL", "shortCode", shortCode));

        // Verify ownership
        if (!url.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("URL", "shortCode", shortCode);
        }

        if (request.longUrl() != null && !request.longUrl().isBlank()) {
            url.setLongUrl(request.longUrl());
        }

        if (request.expiresAt() != null) {
            if (request.expiresAt().isBefore(Instant.now())) {
                throw new IllegalArgumentException("Expiry date must be in the future");
            }
            url.setExpiresAt(request.expiresAt());
        }

        if (request.isActive() != null) {
            url.setActive(request.isActive());
        }

        url = urlRepository.save(url);

        // Write-through cache update (re-cache the new long URL)
        cacheService.cacheRedirectUrl(shortCode, url.getLongUrl());

        log.info("URL updated: shortCode={}, userId={}", shortCode, userId);

        return UrlResponse.from(url, baseUrl);
    }

    // ────────── DELETE (Soft) ──────────

    @Transactional
    public void deleteUrl(String shortCode, Long userId) {
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResourceNotFoundException("URL", "shortCode", shortCode));

        // Verify ownership
        if (!url.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("URL", "shortCode", shortCode);
        }

        url.setActive(false);
        urlRepository.save(url);

        // Eager cache invalidation
        cacheService.invalidate(shortCode);

        log.info("URL soft-deleted: shortCode={}, userId={}", shortCode, userId);
    }

    // ────────── HELPERS ──────────

    /**
     * Check if a URL is accessible (active and not expired).
     */
    private boolean isUrlAccessible(Url url) {
        if (!url.isActive()) {
            return false;
        }
        if (url.getExpiresAt() != null && url.getExpiresAt().isBefore(Instant.now())) {
            // Auto-deactivate expired URL
            url.setActive(false);
            urlRepository.save(url);
            cacheService.invalidate(url.getShortCode());
            return false;
        }
        return true;
    }
}
