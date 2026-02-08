package com.linkhub.ratelimit;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class RateLimitConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    public RateLimitConfig(RateLimitInterceptor rateLimitInterceptor) {
        this.rateLimitInterceptor = rateLimitInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/v1/urls/**")     // URL CRUD endpoints
                .addPathPatterns("/{shortCode}")        // Redirect endpoint
                .excludePathPatterns(
                        "/api/v1/auth/**",              // Auth endpoints — not rate limited here
                        "/swagger-ui/**",
                        "/api-docs/**",
                        "/actuator/**"
                );
    }
}
