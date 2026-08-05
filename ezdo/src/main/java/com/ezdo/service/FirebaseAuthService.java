package com.ezdo.service;

import com.ezdo.dto.AuthResponse;
import com.ezdo.dto.FirebaseLoginRequest;
import com.ezdo.entity.User;
import com.ezdo.exception.ErrorCodes;
import com.ezdo.exception.FirebaseAuthenticationException;
import com.ezdo.repository.UserRepository;
import com.ezdo.service.FirebaseTokenVerifier.FirebaseUserInfo;
import com.ezdo.util.EmailUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Exchanges a verified Firebase ID token for our own session.
 * <p>
 * Accounts are keyed on the email address, the same key the OTP flow uses — so signing in
 * with Google or Apple using an address that already registered by OTP lands on the same
 * account. That is only safe because we refuse any token where the provider has not verified
 * the address.
 * <p>
 * Consequence worth knowing: Apple's "Hide My Email" hands us a
 * {@code @privaterelay.appleid.com} address, which by design cannot be correlated with the
 * user's real one. Such a sign-in creates a separate account, and no email-keyed scheme can
 * avoid that.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FirebaseAuthService {

    private final FirebaseTokenVerifier verifier;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final CategorySeedService categorySeedService;

    @Value("${ezdo.firebase.allowed-sign-in-providers}")
    private List<String> allowedSignInProviders;

    @PostConstruct
    void logProviderPolicy() {
        log.info("Social sign-in providers allowed: {}", allowedSignInProviders);
    }

    @Transactional
    public AuthResponse login(FirebaseLoginRequest request) {
        FirebaseUserInfo info = verifier.verify(request.idToken());

        if (!allowedSignInProviders.contains(info.signInProvider())) {
            throw new FirebaseAuthenticationException(
                    "This sign-in method is not supported.", ErrorCodes.FIREBASE_PROVIDER_NOT_ALLOWED);
        }

        String provider = providerLabel(info.signInProvider());

        String email = EmailUtil.normalize(info.email());
        if (email == null || email.isBlank()) {
            throw new FirebaseAuthenticationException(
                    "Your " + provider + " account did not provide an email address.",
                    ErrorCodes.FIREBASE_EMAIL_MISSING);
        }
        if (!info.emailVerified()) {
            throw new FirebaseAuthenticationException(
                    "Your " + provider + " account's email address is not verified.",
                    ErrorCodes.FIREBASE_EMAIL_NOT_VERIFIED);
        }

        Optional<User> existing = userRepository.findByEmail(email);
        boolean isNewUser = existing.isEmpty();

        User user = existing.orElseGet(() -> {
            User created = new User();
            created.setEmail(email);
            created.setIsNew(true);
            return created;
        });

        if (user.getEmailVerifiedAt() == null) {
            user.setEmailVerifiedAt(Instant.now());
        }

        if (user.getFirstName() == null && info.firstName() != null) {
            user.setFirstName(info.firstName());
        }
        if (user.getLastName() == null && info.lastName() != null) {
            user.setLastName(info.lastName());
        }

        user = userRepository.save(user);

        if (isNewUser) {
            categorySeedService.seedDefaults(user);
        }

        return authService.issueTokens(user, request.deviceId());
    }

    private static String providerLabel(String signInProvider) {
        return switch (signInProvider) {
            case "google.com" -> "Google";
            case "apple.com" -> "Apple";
            case null -> "sign-in";
            default -> signInProvider;
        };
    }
}
