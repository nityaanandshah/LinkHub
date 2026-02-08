package com.linkhub.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkhub.auth.dto.AuthResponse;
import com.linkhub.auth.dto.RegisterRequest;
import com.linkhub.url.dto.CreateUrlRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Rate Limiting Integration Tests")
class RateLimitIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private String accessToken;

    @BeforeEach
    void setUp() throws Exception {
        // Register a test user
        RegisterRequest registerReq = new RegisterRequest(
                "ratelimit-" + System.nanoTime() + "@example.com",
                "Password123!",
                "Rate Limit User"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        AuthResponse authResp = objectMapper.readValue(
                result.getResponse().getContentAsString(), AuthResponse.class);
        accessToken = authResp.accessToken();
    }

    @Test
    @DisplayName("Should allow requests within rate limit")
    void shouldAllowWithinLimit() throws Exception {
        CreateUrlRequest request = new CreateUrlRequest(
                "https://www.example.com/ratelimit-ok", null, null);

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/v1/urls")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }
    }

    @Test
    @DisplayName("Should block requests exceeding rate limit for authenticated user")
    void shouldBlockExceedingUserLimit() throws Exception {
        CreateUrlRequest request = new CreateUrlRequest(
                "https://www.example.com/ratelimit-block", null, null);

        // Simulate exceeding user rate limit by pre-setting the counter
        // User limit = 100/min, so we set counter to 100
        Long userId = extractUserIdFromToken(accessToken);
        redisTemplate.opsForValue().set("rate:user:" + userId, "100");

        mockMvc.perform(post("/api/v1/urls")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value(
                        "Rate limit exceeded. Maximum 100 requests per minute."));
    }

    @Test
    @DisplayName("Should return 429 with correct error format")
    void shouldReturn429WithErrorFormat() throws Exception {
        // Pre-set the counter to exceed limit
        Long userId = extractUserIdFromToken(accessToken);
        redisTemplate.opsForValue().set("rate:user:" + userId, "100");

        mockMvc.perform(get("/api/v1/urls")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.error").value("Too Many Requests"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    private Long extractUserIdFromToken(String token) {
        // Decode JWT to extract userId (simple base64 parse of payload)
        try {
            String[] parts = token.split("\\.");
            String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
            var node = objectMapper.readTree(payload);
            return node.get("userId").asLong();
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract userId from token", e);
        }
    }
}
