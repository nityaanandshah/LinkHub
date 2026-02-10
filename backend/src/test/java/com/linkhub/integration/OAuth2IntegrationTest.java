package com.linkhub.integration;

import com.linkhub.auth.model.User;
import com.linkhub.auth.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for OAuth2 flow.
 * Since we can't easily simulate a full Google OAuth2 flow in tests,
 * we test the supporting infrastructure:
 * 1. OAuth2 endpoints are accessible (not blocked by security)
 * 2. User creation for OAuth2 users works correctly
 * 3. Security config allows OAuth2 paths
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OAuth2IntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Test
    @Order(1)
    void oAuth2LoginEndpointIsAccessible() throws Exception {
        // The OAuth2 authorization endpoint should redirect to Google (302)
        // rather than returning 403 (forbidden)
        mockMvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @Order(2)
    void googleUserCanBeCreatedProgrammatically() {
        // Test that a Google-style user can be created in the database
        User googleUser = new User();
        googleUser.setEmail("google-test@gmail.com");
        googleUser.setDisplayName("Google Test User");
        googleUser.setProvider(User.AuthProvider.GOOGLE);
        googleUser.setProviderId("google-sub-12345");
        googleUser.setRole(User.Role.USER);

        userRepository.save(googleUser);

        // Verify retrieval by provider
        Optional<User> found = userRepository.findByProviderAndProviderId(
                User.AuthProvider.GOOGLE, "google-sub-12345");
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("google-test@gmail.com");
        assertThat(found.get().getPasswordHash()).isNull(); // No password for OAuth2 users
    }

    @Test
    @Order(3)
    void existingLocalUserCanBeLinkedToGoogle() {
        // Create a local user first
        User localUser = new User("link-test@example.com", "hashedPassword", "Link Test");
        localUser = userRepository.save(localUser);

        assertThat(localUser.getProvider()).isEqualTo(User.AuthProvider.LOCAL);

        // Simulate linking Google account
        localUser.setProvider(User.AuthProvider.GOOGLE);
        localUser.setProviderId("google-sub-67890");
        userRepository.save(localUser);

        // Verify the link
        Optional<User> found = userRepository.findByProviderAndProviderId(
                User.AuthProvider.GOOGLE, "google-sub-67890");
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("link-test@example.com");
        assertThat(found.get().getPasswordHash()).isEqualTo("hashedPassword");
    }

    @Test
    @Order(4)
    void oAuth2CallbackPathIsNotBlocked() throws Exception {
        // The OAuth2 callback path should not be blocked by JWT filter
        // It will return an error because there's no valid code, but not 403
        mockMvc.perform(get("/login/oauth2/code/google")
                        .param("code", "fake-code")
                        .param("state", "fake-state"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    // Should not be 403 (blocked by security)
                    assertThat(status).isNotEqualTo(403);
                });
    }
}
