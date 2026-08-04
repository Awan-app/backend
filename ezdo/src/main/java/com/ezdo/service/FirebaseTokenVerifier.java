package com.ezdo.service;

import com.ezdo.exception.ErrorCodes;
import com.ezdo.exception.FirebaseAuthenticationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Verifies a Firebase ID token produced by any Firebase Auth client — web, Android or iOS.
 * <p>
 * Firebase signs these with Google's {@code securetoken@system} key pair, published as a
 * standard JWK Set — so plain Nimbus verification is enough and the firebase-admin SDK
 * (and its service-account key) is not needed. The only configuration is the public
 * project ID, which is also the token's audience.
 * <p>
 * Every check below is scoped to the Firebase <em>project</em>, never to a platform: the
 * per-platform OAuth client IDs are consumed inside the client SDK when it trades a provider
 * credential for a Firebase session, and never reach this token's {@code aud}. That is why a
 * mobile client needs no server-side change.
 */
@Slf4j
@Component
public class FirebaseTokenVerifier {

    private static final String JWK_SET_URI =
            "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com";
    private static final String ISSUER_PREFIX = "https://securetoken.google.com/";

    private final JwtDecoder decoder;

    public FirebaseTokenVerifier(@Value("${ezdo.firebase.project-id:}") String projectId) {
        if (projectId == null || projectId.isBlank()) {
            log.warn("Social sign-in disabled: ezdo.firebase.project-id is not set");
            this.decoder = null;
            return;
        }
        this.decoder = buildDecoder(projectId.trim());
    }

    public boolean isEnabled() {
        return decoder != null;
    }

    public FirebaseUserInfo verify(String idToken) {
        if (decoder == null) {
            throw new FirebaseAuthenticationException(
                "Social sign-in is not configured on this server.",
                503, ErrorCodes.FIREBASE_NOT_CONFIGURED);
        }

        Jwt jwt;
        try {
            jwt = decoder.decode(idToken);
        } catch (JwtException e) {
            // Deliberately opaque: the client learns the token was rejected, not which check failed.
            log.debug("Firebase ID token rejected", e);
            throw new FirebaseAuthenticationException(
                "The sign-in token could not be verified.", ErrorCodes.FIREBASE_TOKEN_INVALID);
        }

        String[] name = splitName(jwt.getClaimAsString("name"));
        return new FirebaseUserInfo(
            jwt.getSubject(),
            jwt.getClaimAsString("email"),
            claimAsBoolean(jwt, "email_verified"),
            name[0],
            name[1],
            signInProvider(jwt));
    }

    private static JwtDecoder buildDecoder(String projectId) {
        NimbusJwtDecoder nimbus = NimbusJwtDecoder.withJwkSetUri(JWK_SET_URI)
            .jwsAlgorithm(SignatureAlgorithm.RS256)
            .build();
        nimbus.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
            new JwtTimestampValidator(),
            new JwtIssuerValidator(ISSUER_PREFIX + projectId),
            audienceValidator(projectId))
        );
        return nimbus;
    }

    /**
     * The audience is what stops a genuine Firebase token minted for a different project
     * from being replayed against us.
     */
    private static OAuth2TokenValidator<Jwt> audienceValidator(String projectId) {
        return new JwtClaimValidator<List<String>>(
            JwtClaimNames.AUD, aud -> aud != null && aud.contains(projectId));
    }

    /** Firebase reports the underlying provider at {@code firebase.sign_in_provider}, e.g. {@code google.com}. */
    private static String signInProvider(Jwt jwt) {
        Map<String, Object> firebase = jwt.getClaimAsMap("firebase");
        if (firebase == null) {
            return null;
        }
        Object provider = firebase.get("sign_in_provider");
        return provider == null ? null : provider.toString();
    }

    /** Some providers send {@code email_verified} as the string {@code "true"} rather than a boolean. */
    private static boolean claimAsBoolean(Jwt jwt, String name) {
        Object value = jwt.getClaims().get(name);
        return switch (value) {
            case Boolean b -> b;
            case String s -> Boolean.parseBoolean(s);
            case null, default -> false;
        };
    }

    /**
     * Firebase ID tokens carry only the full display {@code name} — never {@code given_name} /
     * {@code family_name} — so split on the first space. Either half may be null.
     */
    private static String[] splitName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return new String[]{null, null};
        }
        String trimmed = fullName.trim();
        int split = trimmed.indexOf(' ');
        if (split < 0) {
            return new String[]{trimmed, null};
        }
        return new String[]{trimmed.substring(0, split), trimmed.substring(split + 1).trim()};
    }

    public record FirebaseUserInfo(
        String uid,
        String email,
        boolean emailVerified,
        String firstName,
        String lastName,
        String signInProvider
    ) {}
}
