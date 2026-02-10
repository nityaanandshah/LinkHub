package com.linkhub.auth.service;

import com.linkhub.auth.model.User;
import com.linkhub.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Custom OAuth2UserService that creates or links Google users to our local user model.
 * After successful Google authentication, issues JWT tokens through the OAuth2SuccessHandler.
 */
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final Logger log = LoggerFactory.getLogger(CustomOAuth2UserService.class);

    private final UserRepository userRepository;

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String googleId = oAuth2User.getAttribute("sub");

        if (email == null) {
            throw new OAuth2AuthenticationException("Email not found from Google OAuth2 provider");
        }

        // Check if user already exists by Google provider ID
        Optional<User> existingByProvider = userRepository.findByProviderAndProviderId(
                User.AuthProvider.GOOGLE, googleId);

        if (existingByProvider.isPresent()) {
            log.info("Existing Google user logged in: {}", email);
            return oAuth2User;
        }

        // Check if user exists with same email (local account)
        Optional<User> existingByEmail = userRepository.findByEmail(email);

        if (existingByEmail.isPresent()) {
            // Link Google to existing local account
            User user = existingByEmail.get();
            user.setProvider(User.AuthProvider.GOOGLE);
            user.setProviderId(googleId);
            if (user.getDisplayName() == null && name != null) {
                user.setDisplayName(name);
            }
            userRepository.save(user);
            log.info("Linked Google account to existing user: {}", email);
            return oAuth2User;
        }

        // Create new user
        User newUser = new User();
        newUser.setEmail(email);
        newUser.setDisplayName(name != null ? name : email.split("@")[0]);
        newUser.setProvider(User.AuthProvider.GOOGLE);
        newUser.setProviderId(googleId);
        newUser.setRole(User.Role.USER);
        // No password hash for OAuth2 users

        userRepository.save(newUser);
        log.info("New Google user registered: {}", email);

        return oAuth2User;
    }
}
