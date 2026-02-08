package com.linkhub.url.controller;

import com.linkhub.url.model.Url;
import com.linkhub.url.repository.UrlRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/urls")
@Tag(name = "URLs", description = "URL shortening operations")
public class UrlController {

    private final UrlRepository urlRepository;

    public UrlController(UrlRepository urlRepository) {
        this.urlRepository = urlRepository;
    }

    @GetMapping
    @Operation(summary = "List user's URLs", description = "Paginated list of URLs created by the authenticated user")
    public ResponseEntity<Page<Url>> listUrls(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Long userId = (Long) authentication.getPrincipal();
        Page<Url> urls = urlRepository.findByUserIdAndIsActiveTrue(
                userId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return ResponseEntity.ok(urls);
    }
}
