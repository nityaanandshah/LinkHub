package com.linkhub.auth.handler;

import com.linkhub.auth.model.RefreshToken;
import com.linkhub.auth.model.User;
import com.linkhub.auth.repository.RefreshTokenRepository;
import com.linkhub.auth.repository.UserRepository;
import com.linkhub.auth.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

/**
 * Handles successful OAuth2 authentication by generating JWT tokens
 * and redirecting to the frontend with tokens as URL parameters.
 */
@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2SuccessHandler.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    public OAuth2SuccessHandler(UserRepository userRepository,
                                RefreshTokenRepository refreshTokenRepository,
                                JwtService jwtService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found after OAuth2 authentication: " + email));

        // Generate JWT tokens
        String accessToken = jwtService.generateAccessToken(
                user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getEmail());

        // Store hashed refresh token
        RefreshToken storedToken = new RefreshToken(
                user,
                hashToken(refreshToken),
                Instant.now().plusMillis(jwtService.getRefreshTokenExpiration())
        );
        refreshTokenRepository.save(storedToken);

        log.info("OAuth2 login successful for user: {}", email);

        // Redirect to frontend with tokens
        String redirectUrl = frontendUrl + "/oauth2/callback"
                + "?accessToken=" + accessToken
                + "&refreshToken=" + refreshToken;

        response.sendRedirect(redirectUrl);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
