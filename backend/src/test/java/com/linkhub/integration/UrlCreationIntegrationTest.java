package com.linkhub.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkhub.auth.dto.AuthResponse;
import com.linkhub.auth.dto.RegisterRequest;
import com.linkhub.url.dto.BulkCreateRequest;
import com.linkhub.url.dto.CreateUrlRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("URL Creation Integration Tests")
class UrlCreationIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String accessToken;

    @BeforeEach
    void setUp() throws Exception {
        // Register a test user and get JWT token
        RegisterRequest registerRequest = new RegisterRequest(
                "urltest-" + System.nanoTime() + "@example.com",
                "Password123!",
                "Test User"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        AuthResponse authResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(), AuthResponse.class);
        accessToken = authResponse.accessToken();
    }

    @Test
    @DisplayName("Should create a short URL with generated key")
    void shouldCreateShortUrl() throws Exception {
        CreateUrlRequest request = new CreateUrlRequest(
                "https://www.example.com/very/long/path?param=value",
                null,
                null
        );

        mockMvc.perform(post("/api/v1/urls")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode").isNotEmpty())
                .andExpect(jsonPath("$.longUrl").value("https://www.example.com/very/long/path?param=value"))
                .andExpect(jsonPath("$.shortUrl").isNotEmpty())
                .andExpect(jsonPath("$.qrUrl").isNotEmpty())
                .andExpect(jsonPath("$.isCustomAlias").value(false));
    }

    @Test
    @DisplayName("Should create a short URL with custom alias")
    void shouldCreateWithCustomAlias() throws Exception {
        String alias = "test-" + System.nanoTime() % 10000;
        CreateUrlRequest request = new CreateUrlRequest(
                "https://www.example.com/custom",
                alias,
                null
        );

        mockMvc.perform(post("/api/v1/urls")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode").value(alias))
                .andExpect(jsonPath("$.isCustomAlias").value(true));
    }

    @Test
    @DisplayName("Should reject duplicate custom alias")
    void shouldRejectDuplicateAlias() throws Exception {
        String alias = "dup-" + System.nanoTime() % 10000;
        CreateUrlRequest request = new CreateUrlRequest(
                "https://www.example.com/first", alias, null);

        // First creation — should succeed
        mockMvc.perform(post("/api/v1/urls")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Second creation with same alias — should fail
        CreateUrlRequest dup = new CreateUrlRequest(
                "https://www.example.com/second", alias, null);

        mockMvc.perform(post("/api/v1/urls")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dup)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("already taken")));
    }

    @Test
    @DisplayName("Should create URL with expiry date")
    void shouldCreateWithExpiry() throws Exception {
        Instant expiry = Instant.now().plus(30, ChronoUnit.DAYS);
        CreateUrlRequest request = new CreateUrlRequest(
                "https://www.example.com/expires",
                null,
                expiry
        );

        mockMvc.perform(post("/api/v1/urls")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.expiresAt").isNotEmpty());
    }

    @Test
    @DisplayName("Should reject expired date in the past")
    void shouldRejectPastExpiry() throws Exception {
        Instant pastDate = Instant.now().minus(1, ChronoUnit.DAYS);
        CreateUrlRequest request = new CreateUrlRequest(
                "https://www.example.com/expired",
                null,
                pastDate
        );

        mockMvc.perform(post("/api/v1/urls")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should reject invalid URL format")
    void shouldRejectInvalidUrl() throws Exception {
        CreateUrlRequest request = new CreateUrlRequest(
                "not-a-valid-url",
                null,
                null
        );

        mockMvc.perform(post("/api/v1/urls")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should bulk create URLs")
    void shouldBulkCreate() throws Exception {
        BulkCreateRequest request = new BulkCreateRequest(List.of(
                new CreateUrlRequest("https://www.example.com/bulk1", null, null),
                new CreateUrlRequest("https://www.example.com/bulk2", null, null),
                new CreateUrlRequest("https://www.example.com/bulk3", null, null)
        ));

        mockMvc.perform(post("/api/v1/urls/bulk")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].shortCode").isNotEmpty())
                .andExpect(jsonPath("$[1].shortCode").isNotEmpty())
                .andExpect(jsonPath("$[2].shortCode").isNotEmpty());
    }

    @Test
    @DisplayName("Should list user URLs with pagination")
    void shouldListUserUrls() throws Exception {
        // Create a couple of URLs first
        CreateUrlRequest request = new CreateUrlRequest(
                "https://www.example.com/list-test", null, null);

        mockMvc.perform(post("/api/v1/urls")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // List URLs
        mockMvc.perform(get("/api/v1/urls")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    @DisplayName("Should require authentication for URL creation")
    void shouldRequireAuth() throws Exception {
        CreateUrlRequest request = new CreateUrlRequest(
                "https://www.example.com/noauth", null, null);

        mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    // With OAuth2 enabled, unauthenticated requests may get 302/401/403
                    org.assertj.core.api.Assertions.assertThat(status).isNotEqualTo(200);
                    org.assertj.core.api.Assertions.assertThat(status).isNotEqualTo(201);
                });
    }
}
