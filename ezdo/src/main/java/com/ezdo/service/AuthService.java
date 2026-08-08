package com.ezdo.service;

import com.ezdo.dto.*;
import com.ezdo.entity.User;
import com.ezdo.exception.ErrorCodes;
import com.ezdo.exception.InvalidRefreshTokenException;
import com.ezdo.exception.OtpVerificationException;
import com.ezdo.repository.UserRepository;
import com.ezdo.util.EmailUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final OtpService otpService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserRepository userRepository;
    private final CategorySeedService categorySeedService;
    private final KeycloakUserSyncService keycloakUserSyncService;

    public OtpResponse requestOtp(OtpRequest request) {
        return otpService.requestOtp(EmailUtil.normalize(request.email()));
    }

    @Transactional(noRollbackFor = OtpVerificationException.class)
    public AuthResponse verifyOtp(VerifyOtpRequest request) {
        String email = EmailUtil.normalize(request.email());

        // Verify the OTP code
        otpService.verifyOtp(email, request.code());

        // Find or create user
        User user = userRepository.findByEmail(email).orElse(null);
        boolean isNewUser = user == null;

        if (isNewUser) {
            user = new User();
            user.setEmail(email);
            user.setIsNew(true);
        }

        // Set email verified if not already
        if (user.getEmailVerifiedAt() == null) {
            user.setEmailVerifiedAt(Instant.now());
        }

        user = userRepository.save(user);

        if (isNewUser) {
            categorySeedService.seedDefaults(user);
            // Sync to Keycloak so the user can authenticate via MCP/OAuth
            keycloakUserSyncService.createUser(email);
        }

        return issueTokens(user, request.deviceId());
    }

    /**
     * The single mint point for a session. Every login path goes through here so
     * the OTP and Google flows cannot drift apart.
     */
    public AuthResponse issueTokens(User user, UUID deviceId) {
        String accessToken = jwtService.generateAccessToken(user);
        RefreshTokenService.RefreshTokenResult refreshResult =
                refreshTokenService.createRefreshToken(user.getId(), deviceId);

        return new AuthResponse(
                accessToken,
                jwtService.getAccessTokenExpirySeconds(),
                refreshResult.rawToken(),
                new UserDto(
                    user.getId(),
                    user.getEmail(),
                    user.getIsNew() != null && user.getIsNew()
                )
        );
    }

    @Transactional
    public RefreshResponse refresh(RefreshRequest request) {
        RefreshTokenService.RotateResult result =
                refreshTokenService.rotateRefreshToken(request.refreshToken(), request.deviceId());

        User user = userRepository.findById(result.userId())
                .orElseThrow(() -> new InvalidRefreshTokenException(
                        "User not found for refresh token", ErrorCodes.REFRESH_TOKEN_INVALID));

        String accessToken = jwtService.generateAccessToken(user);

        return new RefreshResponse(
                accessToken,
                jwtService.getAccessTokenExpirySeconds(),
                result.rawToken()
        );
    }

    @Transactional
    public void logout(UUID userId, LogoutRequest request) {
        refreshTokenService.revokeByUserAndDevice(userId, request.deviceId());
    }
}
