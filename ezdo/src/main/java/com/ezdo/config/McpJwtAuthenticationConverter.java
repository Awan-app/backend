package com.ezdo.config;

import com.ezdo.entity.User;
import com.ezdo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.token.Sha512DigestUtils;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class McpJwtAuthenticationConverter implements Converter<Jwt, UsernamePasswordAuthenticationToken> {

    private final UserRepository userRepository;

    @Override
    public UsernamePasswordAuthenticationToken convert(Jwt jwt) {
        String email = jwt.getClaimAsString("email");

        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, "Keycloak token missing 'email' claim", null));
        }

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, "No EZDO account found for email: " + email, null)));

        // Set the user's UUID as the principal exactly like JwtAuthenticationFilter does
        return new UsernamePasswordAuthenticationToken(
                user.getId(),
                jwt,
                List.of()
        );
    }
}
