package com.ezdo.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Configuration
public class CorsConfig {

    @Value("${ezdo.cors.allowed-origins}")
    private String allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        List<String> origins = Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .map(s -> s.endsWith("/") ? s.substring(0, s.length() - 1) : s)
            .filter(s -> !s.isEmpty())
            .toList();

        if (origins.isEmpty()) {
            log.warn("ezdo.cors.allowed-origins is empty — every cross-origin browser request will be blocked");
        } else {
            log.info("CORS allowed origins: {}", origins);
        }
        configuration.setAllowedOrigins(origins);

        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        // No cookies involved in this auth flow (access token via header, refresh token in body),
        // so credentials aren't needed. Flip this to true only if you move to httpOnly cookies.
        configuration.setAllowCredentials(false);

        configuration.setMaxAge(3600L); // cache preflight for 1 hour

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);

        // MCP endpoints — allow Claude and Keycloak origins
        CorsConfiguration mcpConfig = new CorsConfiguration();
        mcpConfig.setAllowedOrigins(List.of("*"));
        mcpConfig.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        mcpConfig.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        mcpConfig.setAllowCredentials(false);
        source.registerCorsConfiguration("/mcp/**", mcpConfig);

        return source;
    }
}
