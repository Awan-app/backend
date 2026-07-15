package com.ezdo.service;

import com.ezdo.entity.RefreshToken;
import com.ezdo.exception.ErrorCodes;
import com.ezdo.exception.InvalidRefreshTokenException;
import com.ezdo.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${ezdo.refresh-token.expiry-days}")
    private int expiryDays;

    /**
     * Creates a new refresh token for the given user and device.
     * Revokes any existing active tokens for the same user+device pair first.
     */
    @Transactional
    public RefreshTokenResult createRefreshToken(UUID userId, UUID deviceId) {
        // Revoke any existing tokens for this user+device
        refreshTokenRepository.revokeByUserIdAndDeviceId(userId, deviceId, Instant.now());

        String rawToken = UUID.randomUUID().toString();
        String tokenHash = sha256(rawToken);

        RefreshToken rt = new RefreshToken();
        rt.setUserId(userId);
        rt.setDeviceId(deviceId);
        rt.setTokenHash(tokenHash);
        rt.setExpiresAt(Instant.now().plus(expiryDays, ChronoUnit.DAYS));
        refreshTokenRepository.save(rt);

        return new RefreshTokenResult(rawToken, rt);
    }

    /**
     * Rotates a refresh token: validates the old one, revokes it, and issues a new one.
     * If the old token was already revoked (reuse detection), revokes ALL tokens for the user.
     */
    @Transactional
    public RotateResult rotateRefreshToken(String rawToken, UUID deviceId) {
        String tokenHash = sha256(rawToken);

        RefreshToken existing = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidRefreshTokenException(
                        ErrorCodes.REFRESH_TOKEN_INVALID));

        // Reuse detection: token already revoked → compromised session
        if (existing.getRevokedAt() != null) {
            refreshTokenRepository.revokeAllByUserId(existing.getUserId(), Instant.now());
            throw new InvalidRefreshTokenException(
                    "Refresh token reuse detected. All sessions have been revoked.",
                    ErrorCodes.REFRESH_TOKEN_REUSE_DETECTED
            );
        }

        // Check expiry
        if (existing.getExpiresAt().isBefore(Instant.now())) {
            refreshTokenRepository.revokeAllByUserId(existing.getUserId(), Instant.now());
            throw new InvalidRefreshTokenException(ErrorCodes.REFRESH_TOKEN_EXPIRED);
        }

        // Validate device matches
        if (!existing.getDeviceId().equals(deviceId)) {
            throw new InvalidRefreshTokenException(ErrorCodes.REFRESH_TOKEN_INVALID);
        }

        // Revoke old token
        existing.setRevokedAt(Instant.now());
        refreshTokenRepository.save(existing);

        // Issue new token in the same user+device pair
        RefreshTokenResult newToken = createRefreshToken(existing.getUserId(), deviceId);
        return new RotateResult(existing.getUserId(), newToken.rawToken(), newToken.entity());
    }

    /**
     * Revokes all active refresh tokens for a given user+device pair (logout).
     */
    @Transactional
    public void revokeByUserAndDevice(UUID userId, UUID deviceId) {
        refreshTokenRepository.revokeByUserIdAndDeviceId(userId, deviceId, Instant.now());
    }

    private String sha256(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public record RefreshTokenResult(String rawToken, RefreshToken entity) {}
    public record RotateResult(UUID userId, String rawToken, RefreshToken entity) {}
}
