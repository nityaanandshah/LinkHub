package com.linkhub.url.controller;

import com.linkhub.url.dto.*;
import com.linkhub.url.service.QrCodeService;
import com.linkhub.url.service.UrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/urls")
@Tag(name = "URLs", description = "URL shortening operations")
public class UrlController {

    private final UrlService urlService;
    private final QrCodeService qrCodeService;

    public UrlController(UrlService urlService, QrCodeService qrCodeService) {
        this.urlService = urlService;
        this.qrCodeService = qrCodeService;
    }

    @PostMapping
    @Operation(summary = "Create short URL", description = "Create a new short URL from the key pool or custom alias")
    public ResponseEntity<CreateUrlResponse> createUrl(
            @Valid @RequestBody CreateUrlRequest request,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        CreateUrlResponse response = urlService.createUrl(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/bulk")
    @Operation(summary = "Bulk create URLs", description = "Create up to 100 short URLs in a single request")
    public ResponseEntity<List<CreateUrlResponse>> bulkCreate(
            @Valid @RequestBody BulkCreateRequest request,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        List<CreateUrlResponse> responses = urlService.bulkCreate(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    @GetMapping
    @Operation(summary = "List user's URLs", description = "Paginated list of URLs created by the authenticated user")
    public ResponseEntity<Page<UrlResponse>> listUrls(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long userId = (Long) authentication.getPrincipal();
        Page<UrlResponse> urls = urlService.listUserUrls(userId, page, size);
        return ResponseEntity.ok(urls);
    }

    @GetMapping("/{shortCode}")
    @Operation(summary = "Get URL metadata", description = "Retrieve metadata for a specific URL")
    public ResponseEntity<UrlResponse> getUrl(
            @PathVariable String shortCode,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        UrlResponse response = urlService.getUrlByShortCode(shortCode, userId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{shortCode}")
    @Operation(summary = "Update URL", description = "Update expiry or active status of a URL")
    public ResponseEntity<UrlResponse> updateUrl(
            @PathVariable String shortCode,
            @Valid @RequestBody UpdateUrlRequest request,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        UrlResponse response = urlService.updateUrl(shortCode, request, userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{shortCode}")
    @Operation(summary = "Delete URL", description = "Soft-delete a URL (deactivates it)")
    public ResponseEntity<Void> deleteUrl(
            @PathVariable String shortCode,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        urlService.deleteUrl(shortCode, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/{shortCode}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    @Operation(summary = "Generate QR code", description = "Generate a QR code PNG image for the short URL")
    public ResponseEntity<byte[]> getQrCode(
            @PathVariable String shortCode,
            @RequestParam(defaultValue = "300") int size,
            Authentication authentication
    ) {
        Long userId = (Long) authentication.getPrincipal();
        UrlResponse url = urlService.getUrlByShortCode(shortCode, userId);
        byte[] qrImage = qrCodeService.generateQrCode(url.shortUrl(), size);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(qrImage);
    }
}
