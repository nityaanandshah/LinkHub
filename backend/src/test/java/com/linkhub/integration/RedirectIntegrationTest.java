package com.linkhub.integration;

import com.fasterxml.jackson.databind.JsonNode;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Redirect & Cache Integration Tests")
class RedirectIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private String accessToken;
    private String shortCode;

    @BeforeEach
    void setUp() throws Exception {
        // Register user
        RegisterRequest registerReq = new RegisterRequest(
                "redirect-" + System.nanoTime() + "@example.com",
                "Password123!",
                "Redirect User"
        );

        MvcResult authResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        AuthResponse authResp = objectMapper.readValue(
                authResult.getResponse().getContentAsString(), AuthResponse.class);
        accessToken = authResp.accessToken();

        // Create a URL
        CreateUrlRequest urlReq = new CreateUrlRequest(
                "https://www.example.com/redirect-target",
                null,
                null
        );

        MvcResult urlResult = mockMvc.perform(post("/api/v1/urls")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(urlReq)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode node = objectMapper.readTree(urlResult.getResponse().getContentAsString());
        shortCode = node.get("shortCode").asText();
    }

    @Test
    @DisplayName("Should redirect with 302 and correct Location header")
    void shouldRedirectCorrectly() throws Exception {
        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://www.example.com/redirect-target"));
    }

    @Test
    @DisplayName("Should return 404 for unknown short code")
    void shouldReturn404ForUnknown() throws Exception {
        mockMvc.perform(get("/zzzzzzzz"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should populate Redis cache on URL creation (write-through)")
    void shouldCacheOnCreate() throws Exception {
        String cachedUrl = redisTemplate.opsForValue().get("url:" + shortCode);
        assertThat(cachedUrl).isEqualTo("https://www.example.com/redirect-target");
    }

    @Test
    @DisplayName("Should use cache-aside on redirect after cache miss")
    void shouldUseCacheAsideOnRedirect() throws Exception {
        // Manually delete the cache entry
        redisTemplate.delete("url:" + shortCode);

        // Confirm cache is empty
        assertThat(redisTemplate.opsForValue().get("url:" + shortCode)).isNull();

        // Redirect should still work (DB fallback)
        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isFound());

        // After redirect, cache should be repopulated
        String cachedUrl = redisTemplate.opsForValue().get("url:" + shortCode);
        assertThat(cachedUrl).isEqualTo("https://www.example.com/redirect-target");
    }

    @Test
    @DisplayName("Should invalidate cache on URL delete")
    void shouldInvalidateCacheOnDelete() throws Exception {
        // Confirm cache exists
        assertThat(redisTemplate.opsForValue().get("url:" + shortCode)).isNotNull();

        // Delete the URL
        mockMvc.perform(delete("/api/v1/urls/" + shortCode)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        // Cache should be invalidated
        assertThat(redisTemplate.opsForValue().get("url:" + shortCode)).isNull();

        // Redirect should now fail
        mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should increment click counter in Redis on redirect")
    void shouldIncrementClickCounter() throws Exception {
        // Perform multiple redirects
        mockMvc.perform(get("/" + shortCode)).andExpect(status().isFound());
        mockMvc.perform(get("/" + shortCode)).andExpect(status().isFound());
        mockMvc.perform(get("/" + shortCode)).andExpect(status().isFound());

        // Check click counter in Redis
        String clicks = redisTemplate.opsForValue().get("clicks:" + shortCode);
        assertThat(clicks).isNotNull();
        assertThat(Long.parseLong(clicks)).isGreaterThanOrEqualTo(3);
    }
}
