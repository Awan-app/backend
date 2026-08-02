package com.ezdo.config;

import com.ezdo.entity.ApiKey;
import com.ezdo.repository.ApiKeyRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Authenticates requests that carry an {@code X-API-Key} header instead of a
 * JWT Bearer token.  Designed for MCP clients (Claude Desktop, Cursor, VS Code)
 * that store a long-lived API key in their configuration.
 *
 * <p>This filter runs <em>before</em> the JWT filter in the security chain.
 * If an API key is present and valid, it sets the {@code SecurityContext} so
 * the JWT filter (which checks for Bearer tokens) simply skips the request.
 */
@Component
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-Key";

    private final ApiKeyRepository apiKeyRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String apiKey = request.getHeader(API_KEY_HEADER);
            if (apiKey == null || apiKey.isBlank()) {
                apiKey = request.getParameter("api_key");
            }

            if (apiKey != null && !apiKey.isBlank()) {
                Optional<ApiKey> found = apiKeyRepository.findByKeyValueAndActiveTrue(apiKey);

                found.ifPresent(key -> {
                    UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                            key.getUser().getId().toString(),
                            null,
                            List.of()
                        );
                    authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                });
            }
        }

        filterChain.doFilter(request, response);
    }
}
