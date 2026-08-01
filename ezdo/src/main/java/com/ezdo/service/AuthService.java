package com.ezdo.service;

import com.ezdo.dto.*;
import com.ezdo.entity.User;
import com.ezdo.exception.OtpVerificationException;
import com.ezdo.repository.UserRepository;
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

    public OtpResponse requestOtp(OtpRequest request) {
        return otpService.requestOtp(normalizeEmail(request.email()));
    }

    @Transactional(noRollbackFor = OtpVerificationException.class)
    public VerifyOtpResponse verifyOtp(VerifyOtpRequest request) {
        String email = normalizeEmail(request.email());

        // Verify the OTP code
        otpService.verifyOtp(email, request.code());

        // Find or create user
        boolean isNewUser;
        User user = userRepository.findByEmail(email).orElse(null);

        if (user == null) {
            user = new User();
            user.setEmail(email);
            user.setIsNew(true);
        }

        // Set email verified if not already
        if (user.getEmailVerifiedAt() == null) {
            user.setEmailVerifiedAt(Instant.now());
        }

        user = userRepository.save(user);

        // Generate tokens
        String accessToken = jwtService.generateAccessToken(user);
        RefreshTokenService.RefreshTokenResult refreshResult =
                refreshTokenService.createRefreshToken(user.getId(), request.deviceId());

        return new VerifyOtpResponse(
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
                .orElseThrow(() -> new IllegalStateException("User not found for refresh token"));

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

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
