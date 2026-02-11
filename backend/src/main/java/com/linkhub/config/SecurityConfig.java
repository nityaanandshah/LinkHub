package com.linkhub.config;

import com.linkhub.auth.filter.JwtAuthFilter;
import com.linkhub.auth.handler.OAuth2SuccessHandler;
import com.linkhub.auth.service.CustomOAuth2UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final CorsConfigurationSource corsConfigurationSource;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter,
                          CorsConfigurationSource corsConfigurationSource,
                          CustomOAuth2UserService customOAuth2UserService,
                          OAuth2SuccessHandler oAuth2SuccessHandler) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.corsConfigurationSource = corsConfigurationSource;
        this.customOAuth2UserService = customOAuth2UserService;
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/api/v1/auth/register", "/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
                        .requestMatchers("/api/v1/auth/oauth2/**").permitAll()

                        // OAuth2 login flow endpoints
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()

                        // Redirect endpoint — public (alphanumeric + hyphens + underscores, 1-10 chars)
                        .requestMatchers(HttpMethod.GET, "/{shortCode:[a-zA-Z0-9\\-_]{1,10}}").permitAll()

                        // System info endpoints (analytics lag, etc.)
                        .requestMatchers("/api/v1/system/**").permitAll()

                        // Swagger / Actuator
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/api-docs/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()

                        // Everything else requires auth
                        .anyRequest().authenticated()
                )
                // For API requests: return 401 instead of redirecting to OAuth2 login
                // This prevents CORS errors when JWT expires or is missing on API calls
                .exceptionHandling(ex -> ex
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                new AntPathRequestMatcher("/api/**")
                        )
                )
                // OAuth2 Login configuration (only for browser-initiated login flow)
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .userService(customOAuth2UserService)
                        )
                        .successHandler(oAuth2SuccessHandler)
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
